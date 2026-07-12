package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMap
import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMapSet
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntity
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntityType
import com.spiritwisestudios.crossroadsoffate.data.models.MapPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The avatar's state on the current exploration map, in world units.
 * [distanceTraveled] accumulates while walking so the UI can drive the walk-cycle animation.
 */
data class ExplorationPlayerState(
    val position: MapPoint = MapPoint(),
    val facingLeft: Boolean = false,
    val isMoving: Boolean = false,
    val distanceTraveled: Float = 0f
)

/**
 * A single NPC dialog line currently shown in the exploration HUD.
 */
data class ExplorationDialog(
    val entityId: String,
    val icon: String,
    val speakerName: String,
    val line: String,
    val lineNumber: Int,
    val totalLines: Int
)

/**
 * Owns the free-roam exploration state between story beats: which map is loaded,
 * where the avatar is, tap-to-move pathfinding around obstacles, and interactions
 * with map entities (story marker, NPCs, exits between maps).
 *
 * Movement is time-based: the UI calls [update] once per frame with the elapsed
 * milliseconds, keeping this class free of clocks and fully unit-testable.
 */
class ExplorationManager {

    companion object {
        /** Collision radius of the avatar; obstacles are inflated by this for pathing. */
        const val PLAYER_RADIUS = 12f

        /** Walk speed in world units per second. */
        const val WALK_SPEED = 220f

        /** How close a tap must land to an entity to count as tapping it. */
        const val ENTITY_TAP_RADIUS = 48f

        /** The avatar stops and interacts once it gets this close to its target entity. */
        const val INTERACT_RANGE = 42f

        /** Navigation grid resolution in world units. */
        internal const val NAV_CELL = 25f
    }

    private var catalog = ExplorationMapSet()

    private val _currentMap = MutableStateFlow<ExplorationMap?>(null)
    val currentMap: StateFlow<ExplorationMap?> = _currentMap.asStateFlow()

    private val _playerState = MutableStateFlow(ExplorationPlayerState())
    val playerState: StateFlow<ExplorationPlayerState> = _playerState.asStateFlow()

    private val _activeDialog = MutableStateFlow<ExplorationDialog?>(null)
    val activeDialog: StateFlow<ExplorationDialog?> = _activeDialog.asStateFlow()

    /** True while the pending story beat lives on the currently loaded map. */
    private val _storyMarkerVisible = MutableStateFlow(false)
    val storyMarkerVisible: StateFlow<Boolean> = _storyMarkerVisible.asStateFlow()

    private var storyMapId: String? = null
    private var storyReachedListener: (() -> Unit)? = null
    private var activityListener: ((MapEntity) -> Unit)? = null

    private var navGrid: NavGrid? = null
    private var waypoints = ArrayDeque<MapPoint>()
    private var pendingEntityId: String? = null
    private val npcLineIndices = mutableMapOf<String, Int>()

    fun loadCatalog(mapSet: ExplorationMapSet) {
        catalog = mapSet
        Timber.d("Exploration catalog loaded: %d maps", mapSet.maps.size)
    }

    fun hasMapForLocation(location: String): Boolean =
        catalog.findMapForLocation(location) != null

    /** Called when the avatar reaches the story marker; set by the ViewModel. */
    fun setStoryReachedListener(listener: (() -> Unit)?) {
        storyReachedListener = listener
    }

    /** Called when the avatar reaches an ACTIVITY entity; set by the ViewModel. */
    fun setActivityListener(listener: ((MapEntity) -> Unit)?) {
        activityListener = listener
    }

    /**
     * Shows a one-off message in the dialog bubble (activity feedback like
     * "you need a torch"). Not tied to an NPC, so advancing is a no-op and
     * dismissing just closes it.
     */
    fun showMessage(icon: String, title: String, line: String) {
        _activeDialog.value = ExplorationDialog(
            entityId = "system_message",
            icon = icon,
            speakerName = title,
            line = line,
            lineNumber = 1,
            totalLines = 1
        )
    }

    /**
     * Enters exploration for a scenario location. If the avatar is already on the
     * matching map (consecutive story beats in the same place), its position is kept;
     * otherwise the map loads fresh at its spawn point.
     *
     * @return false when no map covers this location (caller should skip exploration)
     */
    fun enterMapForLocation(location: String): Boolean {
        val map = catalog.findMapForLocation(location) ?: run {
            Timber.w("No exploration map for location '%s'", location)
            return false
        }
        storyMapId = map.id
        if (_currentMap.value?.id == map.id) {
            _storyMarkerVisible.value = true
        } else {
            enterMap(map, map.spawn)
        }
        return true
    }

    /**
     * Marks the pending story beat as consumed (marker reached or exploration skipped).
     * The map and avatar position are kept so play resumes seamlessly next beat.
     */
    fun markStoryConsumed() {
        storyMapId = null
        _storyMarkerVisible.value = false
        waypoints.clear()
        pendingEntityId = null
        _activeDialog.value = null
        _playerState.value = _playerState.value.copy(isMoving = false)
    }

    /** Clears all exploration state (used when returning to the title screen). */
    fun reset() {
        markStoryConsumed()
        _currentMap.value = null
        navGrid = null
        npcLineIndices.clear()
    }

    /**
     * Loads a map by id directly (debug tooling). Any pending story beat is
     * preserved: jumping back to the story's map shows its marker again, so a
     * debug detour can never strand the narrative.
     */
    fun debugEnterMap(mapId: String): Boolean {
        val map = catalog.findMapById(mapId) ?: return false
        enterMap(map, map.spawn)
        return true
    }

    fun getAllMapIds(): List<String> = catalog.maps.map { it.id }

    private fun enterMap(map: ExplorationMap, spawnPoint: MapPoint) {
        val grid = NavGrid.build(map, PLAYER_RADIUS)
        navGrid = grid
        waypoints.clear()
        pendingEntityId = null
        _activeDialog.value = null
        _currentMap.value = map
        _storyMarkerVisible.value = map.id == storyMapId
        _playerState.value = ExplorationPlayerState(position = grid.nearestWalkable(spawnPoint))
    }

    /**
     * Handles a tap in world coordinates: tapping on (or near) an interactive entity
     * walks the avatar over and interacts on arrival; anywhere else just walks there.
     */
    fun onTap(x: Float, y: Float) {
        val map = _currentMap.value ?: return
        val grid = navGrid ?: return
        _activeDialog.value = null

        val tapX = x.coerceIn(PLAYER_RADIUS, map.width - PLAYER_RADIUS)
        val tapY = y.coerceIn(PLAYER_RADIUS, map.height - PLAYER_RADIUS)

        val target = interactiveEntities(map)
            .map { it to distance(it.x, it.y, tapX, tapY) }
            .filter { (_, d) -> d <= ENTITY_TAP_RADIUS }
            .minByOrNull { (_, d) -> d }
            ?.first

        pendingEntityId = target?.id
        val from = _playerState.value.position
        val dest = grid.nearestWalkable(MapPoint(target?.x ?: tapX, target?.y ?: tapY))

        // Already close enough to the tapped entity? Interact immediately.
        if (target != null && distance(from.x, from.y, target.x, target.y) <= INTERACT_RANGE) {
            waypoints.clear()
            interactWith(target)
            return
        }

        val path = grid.findPath(from, dest)
        waypoints.clear()
        waypoints.addAll(path)
        if (path.isEmpty()) pendingEntityId = null
    }

    /**
     * Advances movement by [deltaMillis]. Call once per rendered frame.
     */
    fun update(deltaMillis: Long) {
        if (deltaMillis <= 0) return
        val map = _currentMap.value ?: return
        val state = _playerState.value

        if (waypoints.isEmpty()) {
            if (state.isMoving) _playerState.value = state.copy(isMoving = false)
            return
        }

        var remaining = WALK_SPEED * (min(deltaMillis, 100L) / 1000f)
        var pos = state.position
        var facingLeft = state.facingLeft
        var traveled = state.distanceTraveled

        while (remaining > 0f && waypoints.isNotEmpty()) {
            val next = waypoints.first()
            val dx = next.x - pos.x
            val dy = next.y - pos.y
            val dist = sqrt(dx * dx + dy * dy)
            if (abs(dx) > 0.5f) facingLeft = dx < 0

            if (dist <= remaining) {
                pos = next
                traveled += dist
                remaining -= dist
                waypoints.removeFirst()
            } else {
                pos = MapPoint(pos.x + dx / dist * remaining, pos.y + dy / dist * remaining)
                traveled += remaining
                remaining = 0f
            }

            // Close enough to the target entity? Stop early and interact.
            val pending = pendingEntityId?.let { id -> map.entities.firstOrNull { it.id == id } }
            if (pending != null && distance(pos.x, pos.y, pending.x, pending.y) <= INTERACT_RANGE) {
                waypoints.clear()
                _playerState.value = ExplorationPlayerState(pos, facingLeft, false, traveled)
                interactWith(pending)
                return
            }
        }

        val arrived = waypoints.isEmpty()
        _playerState.value = ExplorationPlayerState(pos, facingLeft, !arrived, traveled)
        if (arrived) pendingEntityId = null
    }

    /** Shows the tapped NPC's next dialog line (dialog bubble tap). */
    fun advanceDialog() {
        val dialog = _activeDialog.value ?: return
        val entity = _currentMap.value?.entities?.firstOrNull { it.id == dialog.entityId } ?: return
        showNpcLine(entity)
    }

    fun dismissDialog() {
        _activeDialog.value = null
    }

    private fun interactiveEntities(map: ExplorationMap): List<MapEntity> =
        map.entities.filter { it.type != MapEntityType.STORY || _storyMarkerVisible.value }

    private fun interactWith(entity: MapEntity) {
        pendingEntityId = null
        when (entity.type) {
            MapEntityType.STORY -> {
                if (_storyMarkerVisible.value) {
                    Timber.d("Story marker reached on map %s", _currentMap.value?.id)
                    storyReachedListener?.invoke()
                }
            }
            MapEntityType.NPC -> showNpcLine(entity)
            MapEntityType.EXIT -> followExit(entity)
            MapEntityType.ACTIVITY -> activityListener?.invoke(entity)
        }
    }

    private fun showNpcLine(entity: MapEntity) {
        val lines = entity.dialog?.takeIf { it.isNotEmpty() } ?: listOf("...")
        // Keyed per map: entity ids are only unique within one map
        val key = "${_currentMap.value?.id}:${entity.id}"
        val index = npcLineIndices.getOrDefault(key, 0) % lines.size
        _activeDialog.value = ExplorationDialog(
            entityId = entity.id,
            icon = entity.icon,
            speakerName = entity.label,
            line = lines[index],
            lineNumber = index + 1,
            totalLines = lines.size
        )
        npcLineIndices[key] = index + 1
    }

    private fun followExit(entity: MapEntity) {
        val fromMapId = _currentMap.value?.id
        val targetId = entity.targetMapId ?: return
        val target = catalog.findMapById(targetId) ?: run {
            Timber.w("Exit '%s' points to unknown map '%s'", entity.id, targetId)
            return
        }
        // Spawn beside the reciprocal exit in the target map, nudged toward its center
        // so the avatar doesn't immediately re-trigger the doorway.
        val entry = target.entities.firstOrNull {
            it.type == MapEntityType.EXIT && it.targetMapId == fromMapId
        }
        val spawn = if (entry != null) {
            val towardCenterX = if (entry.x < target.width / 2) 1f else -1f
            val towardCenterY = if (entry.y < target.height / 2) 1f else -1f
            MapPoint(
                entry.x + towardCenterX * (INTERACT_RANGE + PLAYER_RADIUS + 12f),
                entry.y + towardCenterY * (INTERACT_RANGE + PLAYER_RADIUS + 12f)
            )
        } else {
            target.spawn
        }
        enterMap(target, spawn)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Coarse walkability grid over a map with A* pathfinding and line-of-sight smoothing.
 * Built once per map load; obstacles are inflated by the avatar radius so paths keep
 * clearance from walls.
 */
internal class NavGrid private constructor(
    private val cols: Int,
    private val rows: Int,
    private val cell: Float,
    private val blocked: BooleanArray,
    private val width: Float,
    private val height: Float
) {

    companion object {
        fun build(map: ExplorationMap, inflate: Float): NavGrid {
            val cell = ExplorationManager.NAV_CELL
            val cols = max(1, (map.width / cell).toInt())
            val rows = max(1, (map.height / cell).toInt())
            val blocked = BooleanArray(cols * rows)
            for (cy in 0 until rows) {
                for (cx in 0 until cols) {
                    val centerX = (cx + 0.5f) * cell
                    val centerY = (cy + 0.5f) * cell
                    blocked[cy * cols + cx] = map.obstacles.any {
                        it.contains(centerX, centerY, inflate)
                    }
                }
            }
            return NavGrid(cols, rows, cell, blocked, map.width, map.height)
        }
    }

    private fun isBlocked(cx: Int, cy: Int): Boolean =
        cx < 0 || cy < 0 || cx >= cols || cy >= rows || blocked[cy * cols + cx]

    private fun cellX(x: Float): Int = (x / cell).toInt().coerceIn(0, cols - 1)
    private fun cellY(y: Float): Int = (y / cell).toInt().coerceIn(0, rows - 1)

    private fun center(cx: Int, cy: Int): MapPoint =
        MapPoint((cx + 0.5f) * cell, (cy + 0.5f) * cell)

    /** Returns [point] if walkable, otherwise the center of the nearest walkable cell (BFS). */
    fun nearestWalkable(point: MapPoint): MapPoint {
        val startX = cellX(point.x)
        val startY = cellY(point.y)
        if (!isBlocked(startX, startY)) return clampToBounds(point)

        val visited = BooleanArray(cols * rows)
        val queue = ArrayDeque<Int>()
        queue.add(startY * cols + startX)
        visited[startY * cols + startX] = true
        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val cx = idx % cols
            val cy = idx / cols
            if (!blocked[idx]) return center(cx, cy)
            for ((nx, ny) in listOf(cx - 1 to cy, cx + 1 to cy, cx to cy - 1, cx to cy + 1)) {
                if (nx in 0 until cols && ny in 0 until rows && !visited[ny * cols + nx]) {
                    visited[ny * cols + nx] = true
                    queue.add(ny * cols + nx)
                }
            }
        }
        return clampToBounds(point)
    }

    private fun clampToBounds(p: MapPoint): MapPoint = MapPoint(
        p.x.coerceIn(0f, width),
        p.y.coerceIn(0f, height)
    )

    /**
     * A* over the grid (8-directional, no diagonal corner cutting) followed by
     * greedy line-of-sight smoothing. Returns an empty list when unreachable.
     */
    fun findPath(from: MapPoint, to: MapPoint): List<MapPoint> {
        val start = cellX(from.x) to cellY(from.y)
        val goalPoint = nearestWalkable(to)
        val goal = cellX(goalPoint.x) to cellY(goalPoint.y)
        if (start == goal) return listOf(goalPoint)

        val startIdx = start.second * cols + start.first
        val goalIdx = goal.second * cols + goal.first
        val g = FloatArray(cols * rows) { Float.MAX_VALUE }
        val cameFrom = IntArray(cols * rows) { -1 }
        val closed = BooleanArray(cols * rows)
        g[startIdx] = 0f

        val open = PriorityQueue<Pair<Int, Float>>(compareBy { it.second })
        open.add(startIdx to heuristic(start.first, start.second, goal.first, goal.second))

        while (open.isNotEmpty()) {
            val (idx, _) = open.poll() ?: break
            if (closed[idx]) continue
            if (idx == goalIdx) break
            closed[idx] = true
            val cx = idx % cols
            val cy = idx / cols

            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    val ny = cy + dy
                    if (isBlocked(nx, ny)) continue
                    // No cutting corners diagonally past a blocked cell
                    if (dx != 0 && dy != 0 && (isBlocked(cx + dx, cy) || isBlocked(cx, cy + dy))) continue
                    val nIdx = ny * cols + nx
                    if (closed[nIdx]) continue
                    val step = if (dx != 0 && dy != 0) 1.41421f else 1f
                    val tentative = g[idx] + step
                    if (tentative < g[nIdx]) {
                        g[nIdx] = tentative
                        cameFrom[nIdx] = idx
                        open.add(nIdx to tentative + heuristic(nx, ny, goal.first, goal.second))
                    }
                }
            }
        }

        if (cameFrom[goalIdx] == -1 && startIdx != goalIdx) return emptyList()

        // Reconstruct cell path, then smooth
        val cellPath = mutableListOf<MapPoint>()
        var idx = goalIdx
        while (idx != -1 && idx != startIdx) {
            cellPath.add(center(idx % cols, idx / cols))
            idx = cameFrom[idx]
        }
        cellPath.reverse()
        cellPath.add(goalPoint)

        return smooth(from, cellPath)
    }

    private fun heuristic(x1: Int, y1: Int, x2: Int, y2: Int): Float {
        val dx = abs(x1 - x2)
        val dy = abs(y1 - y2)
        return (dx + dy) + (1.41421f - 2f) * min(dx, dy)
    }

    /** Greedy string-pulling: from each point, jump to the farthest visible waypoint. */
    private fun smooth(start: MapPoint, path: List<MapPoint>): List<MapPoint> {
        if (path.size <= 1) return path
        val result = mutableListOf<MapPoint>()
        var anchor = start
        var i = 0
        while (i < path.size) {
            var farthest = i
            for (j in path.size - 1 downTo i + 1) {
                if (hasLineOfSight(anchor, path[j])) {
                    farthest = j
                    break
                }
            }
            result.add(path[farthest])
            anchor = path[farthest]
            i = farthest + 1
        }
        return result
    }

    /** Walkability check by sampling along the segment at half-cell resolution. */
    private fun hasLineOfSight(a: MapPoint, b: MapPoint): Boolean {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.001f) return true
        val steps = (dist / (cell / 2f)).toInt() + 1
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            if (isBlocked(cellX(a.x + dx * t), cellY(a.y + dy * t))) return false
        }
        return true
    }
}
