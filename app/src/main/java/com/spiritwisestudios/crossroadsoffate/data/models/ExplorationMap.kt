package com.spiritwisestudios.crossroadsoffate.data.models

/**
 * A point in map world units. Maps declare their own width/height; the UI scales
 * world units to screen pixels, so authored coordinates are resolution-independent.
 */
data class MapPoint(
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * Per-map color palette, authored as #RRGGBB hex strings in maps.json so the
 * map editor can edit them with plain color inputs.
 */
data class MapTheme(
    val ground: String = "#6B8F5A",
    val groundDetail: String = "#5C7C4D",
    val obstacleFill: String = "#5B4A3A",
    val obstacleStroke: String = "#3E3228",
    val accent: String = "#FFD700"
)

/**
 * An axis-aligned rectangle the player cannot walk through (building, rock, water).
 * Optional icon/label make it double as a visual landmark.
 */
data class MapObstacle(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val icon: String? = null,
    val label: String? = null
) {
    fun contains(px: Float, py: Float, inflate: Float = 0f): Boolean =
        px >= x - inflate && px <= x + width + inflate &&
        py >= y - inflate && py <= y + height + inflate
}

/**
 * Purely visual, non-colliding scenery (trees, lanterns, crates) drawn as an icon.
 */
data class MapDecor(
    val x: Float = 0f,
    val y: Float = 0f,
    val icon: String = "",
    val scale: Float = 1f
)

/**
 * What happens when the player walks up to a [MapEntity] and interacts with it.
 */
enum class MapEntityType {
    /** Advances the story: opens the pending scenario. Shown only on the map the story is waiting on. */
    STORY,

    /** Ambient character; interacting cycles through [MapEntity.dialog] lines. */
    NPC,

    /** Doorway/path to another map; walking to it loads [MapEntity.targetMapId]. */
    EXIT
}

/**
 * An interactive object placed on a map: the story marker, an ambient NPC, or an exit.
 */
data class MapEntity(
    val id: String = "",
    val type: MapEntityType = MapEntityType.NPC,
    val icon: String = "❓",
    val label: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val targetMapId: String? = null,
    val dialog: List<String>? = null
)

/**
 * A walkable exploration map for one place in the world. [locationNames] lists the
 * scenario `location` strings this map represents, so several story beats can share
 * one physical space (e.g. "Town Square", "Town Entrance", "Town Crossroads").
 */
data class ExplorationMap(
    val id: String = "",
    val name: String = "",
    val locationNames: List<String> = emptyList(),
    val width: Float = 1000f,
    val height: Float = 600f,
    val theme: MapTheme = MapTheme(),
    val spawn: MapPoint = MapPoint(),
    val obstacles: List<MapObstacle> = emptyList(),
    val decor: List<MapDecor> = emptyList(),
    val entities: List<MapEntity> = emptyList()
) {
    val storyEntity: MapEntity? get() = entities.firstOrNull { it.type == MapEntityType.STORY }
}

/**
 * Root of assets/maps.json. Also the in-memory catalog the game queries.
 */
data class ExplorationMapSet(
    val maps: List<ExplorationMap> = emptyList()
) {
    fun findMapById(id: String): ExplorationMap? = maps.firstOrNull { it.id == id }

    /** Finds the map whose [ExplorationMap.locationNames] contains the scenario location string. */
    fun findMapForLocation(location: String): ExplorationMap? =
        maps.firstOrNull { map -> map.locationNames.any { it.equals(location, ignoreCase = true) } }
}
