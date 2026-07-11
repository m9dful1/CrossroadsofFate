package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMap
import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMapSet
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntity
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntityType
import com.spiritwisestudios.crossroadsoffate.data.models.MapObstacle
import com.spiritwisestudios.crossroadsoffate.data.models.MapPoint
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Tests for exploration movement, A* pathfinding around obstacles, and entity
 * interactions. Movement is simulated by stepping [ExplorationManager.update]
 * with fixed 16ms frames until the avatar comes to rest.
 */
class ExplorationManagerTest {

    private lateinit var manager: ExplorationManager

    // "alpha": 500x500 with a vertical wall from (200,0) to (220,400) leaving a
    // gap along the bottom. Spawn is top-left; the story marker is across the wall.
    private fun testCatalog(): ExplorationMapSet {
        val alpha = ExplorationMap(
            id = "alpha",
            name = "Alpha Town",
            locationNames = listOf("Alpha Town"),
            width = 500f,
            height = 500f,
            spawn = MapPoint(50f, 50f),
            obstacles = listOf(MapObstacle(x = 200f, y = 0f, width = 20f, height = 400f)),
            entities = listOf(
                MapEntity(id = "story", type = MapEntityType.STORY, icon = "❗", label = "Story", x = 450f, y = 450f),
                MapEntity(
                    id = "npc_guide", type = MapEntityType.NPC, icon = "🧙", label = "Guide",
                    x = 60f, y = 300f, dialog = listOf("Hello.", "Still here?")
                ),
                MapEntity(
                    id = "exit_beta", type = MapEntityType.EXIT, icon = "🚪", label = "To Beta",
                    x = 60f, y = 450f, targetMapId = "beta"
                ),
                MapEntity(
                    id = "activity_forge", type = MapEntityType.ACTIVITY, icon = "⚒", label = "Forge",
                    x = 300f, y = 60f, activityId = "forge_crafting"
                )
            )
        )
        val beta = ExplorationMap(
            id = "beta",
            name = "Beta Cave",
            locationNames = listOf("Beta Cave"),
            width = 300f,
            height = 300f,
            spawn = MapPoint(150f, 150f),
            entities = listOf(
                MapEntity(id = "story", type = MapEntityType.STORY, icon = "❗", label = "Story", x = 150f, y = 60f),
                MapEntity(
                    id = "exit_alpha", type = MapEntityType.EXIT, icon = "🚪", label = "Back",
                    x = 280f, y = 150f, targetMapId = "alpha"
                )
            )
        )
        return ExplorationMapSet(listOf(alpha, beta))
    }

    @Before
    fun setup() {
        manager = ExplorationManager()
        manager.loadCatalog(testCatalog())
    }

    /** Steps the simulation until the avatar stops moving. */
    private fun settle(maxSteps: Int = 5000) {
        var steps = 0
        do {
            manager.update(16)
            steps++
        } while (manager.playerState.value.isMoving && steps < maxSteps)
        assertTrue("Avatar never settled within $maxSteps steps", steps < maxSteps)
    }

    private fun playerDistanceTo(x: Float, y: Float): Float {
        val pos = manager.playerState.value.position
        return hypot((pos.x - x).toDouble(), (pos.y - y).toDouble()).toFloat()
    }

    @Test
    fun enterMapForLocation_unknownLocation_returnsFalse() {
        assertFalse(manager.enterMapForLocation("Nowhere"))
        assertNull(manager.currentMap.value)
    }

    @Test
    fun enterMapForLocation_loadsMapAtSpawn_withStoryMarker() {
        assertTrue(manager.enterMapForLocation("alpha town")) // case-insensitive
        assertEquals("alpha", manager.currentMap.value?.id)
        assertEquals(50f, manager.playerState.value.position.x, 1f)
        assertEquals(50f, manager.playerState.value.position.y, 1f)
        assertTrue(manager.storyMarkerVisible.value)
    }

    @Test
    fun tapToMove_walksToTappedPoint() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(150f, 60f)
        settle()
        assertTrue("Expected avatar near (150,60), was ${manager.playerState.value.position}",
            playerDistanceTo(150f, 60f) < 2f)
        assertFalse(manager.playerState.value.isMoving)
    }

    @Test
    fun update_withoutTarget_keepsAvatarStill() {
        manager.enterMapForLocation("Alpha Town")
        repeat(50) { manager.update(16) }
        assertEquals(50f, manager.playerState.value.position.x, 0.01f)
        assertEquals(50f, manager.playerState.value.position.y, 0.01f)
    }

    @Test
    fun pathfinding_routesAroundObstacle() {
        manager.enterMapForLocation("Alpha Town")
        // Straight line from (50,50) to (450,50) crosses the wall; the only way
        // around is under its southern end at y > 400.
        manager.onTap(450f, 50f)
        settle()
        assertTrue("Expected avatar near (450,50), was ${manager.playerState.value.position}",
            playerDistanceTo(450f, 50f) < 2f)
        val straightLine = 400f
        assertTrue(
            "Traveled ${manager.playerState.value.distanceTraveled}, expected a detour well beyond $straightLine",
            manager.playerState.value.distanceTraveled > straightLine + 100f
        )
    }

    @Test
    fun walkingToStoryMarker_firesListener() {
        var reached = false
        manager.setStoryReachedListener { reached = true }
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(450f, 450f)
        settle()
        assertTrue("Story listener should fire when the marker is reached", reached)
    }

    @Test
    fun consumedStoryMarker_isNotInteractive() {
        var reached = false
        manager.setStoryReachedListener { reached = true }
        manager.enterMapForLocation("Alpha Town")
        manager.markStoryConsumed()
        assertFalse(manager.storyMarkerVisible.value)

        manager.onTap(450f, 450f)
        settle()
        assertFalse("Consumed marker must not re-fire the listener", reached)
    }

    @Test
    fun tapOnNpc_walksOverAndCyclesDialog() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(60f, 300f)
        settle()

        val first = manager.activeDialog.value
        assertNotNull("Reaching an NPC should open dialog", first)
        assertEquals("Hello.", first?.line)
        assertEquals("Guide", first?.speakerName)
        assertEquals(2, first?.totalLines)

        manager.advanceDialog()
        assertEquals("Still here?", manager.activeDialog.value?.line)

        manager.advanceDialog()
        assertEquals("Hello.", manager.activeDialog.value?.line) // wraps around

        manager.dismissDialog()
        assertNull(manager.activeDialog.value)
    }

    @Test
    fun tapOnEntity_withinInteractRange_interactsImmediately() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(70f, 280f) // walk next to the NPC
        settle()
        manager.dismissDialog()

        manager.onTap(60f, 300f) // ~20 units away: no walking needed
        assertNotNull("Nearby tap should interact without settling", manager.activeDialog.value)
    }

    @Test
    fun tapOnActivityEntity_walksOverAndFiresActivityListener() {
        var launched: MapEntity? = null
        manager.setActivityListener { launched = it }
        manager.enterMapForLocation("Alpha Town")

        manager.onTap(300f, 60f)
        settle()

        assertEquals("forge_crafting", launched?.activityId)
        assertEquals("activity_forge", launched?.id)
    }

    @Test
    fun showMessage_displaysSystemDialog_andAdvanceIsNoOp() {
        manager.enterMapForLocation("Alpha Town")
        manager.showMessage("⚒", "Forge", "You need: hammer.")

        val dialog = manager.activeDialog.value
        assertEquals("Forge", dialog?.speakerName)
        assertEquals("You need: hammer.", dialog?.line)

        manager.advanceDialog() // not an NPC — should not change or crash
        assertEquals("You need: hammer.", manager.activeDialog.value?.line)

        manager.dismissDialog()
        assertNull(manager.activeDialog.value)
    }

    @Test
    fun tapOnExit_switchesMap_andSpawnsBesideEntry() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(60f, 450f)
        settle()

        assertEquals("beta", manager.currentMap.value?.id)
        // Entry should be near beta's reciprocal exit at (280,150), nudged inward
        assertTrue("Avatar should appear near the entry door", playerDistanceTo(280f, 150f) < 150f)
        assertFalse("Story beat lives on alpha, not beta", manager.storyMarkerVisible.value)
    }

    @Test
    fun reenterSameMap_keepsAvatarPosition() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(150f, 60f)
        settle()
        manager.markStoryConsumed()

        assertTrue(manager.enterMapForLocation("Alpha Town"))
        assertTrue("Position should be preserved on same-map re-entry",
            playerDistanceTo(150f, 60f) < 2f)
        assertTrue(manager.storyMarkerVisible.value)
    }

    @Test
    fun facing_followsHorizontalMovement() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(150f, 50f)
        settle()
        assertFalse("Walking right should face right", manager.playerState.value.facingLeft)

        manager.onTap(60f, 50f)
        settle()
        assertTrue("Walking left should face left", manager.playerState.value.facingLeft)
    }

    @Test
    fun reset_clearsMapAndDialog() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(60f, 300f)
        settle()
        manager.reset()

        assertNull(manager.currentMap.value)
        assertNull(manager.activeDialog.value)
        assertFalse(manager.storyMarkerVisible.value)
    }

    @Test
    fun debugEnterMap_loadsMapWithoutStoryMarker() {
        assertTrue(manager.debugEnterMap("beta"))
        assertEquals("beta", manager.currentMap.value?.id)
        assertFalse(manager.storyMarkerVisible.value)
        assertFalse(manager.debugEnterMap("gamma"))
    }

    @Test
    fun getAllMapIds_listsCatalog() {
        assertEquals(listOf("alpha", "beta"), manager.getAllMapIds())
    }

    @Test
    fun movement_isFrameRateIndependent() {
        manager.enterMapForLocation("Alpha Town")
        manager.onTap(150f, 50f)
        // One large frame is clamped to 100ms => 22 units of travel max per call
        manager.update(5000)
        val bigFrameTravel = manager.playerState.value.distanceTraveled
        assertTrue("Delta must be clamped, traveled $bigFrameTravel", abs(bigFrameTravel - 22f) < 1f)
    }
}
