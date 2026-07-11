package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMapSet
import com.spiritwisestudios.crossroadsoffate.data.models.GameJson
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntityType
import com.spiritwisestudios.crossroadsoffate.data.models.MapPoint
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.logic.ExplorationManager
import com.spiritwisestudios.crossroadsoffate.logic.NavGrid
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStreamReader

/**
 * Content-integrity net for assets/maps.json (authored by hand or via the map
 * editor in tools/map-editor). Validates the shipped catalog against the real
 * scenarios.json and the game's own navigation grid, so a bad edit fails CI
 * instead of soft-locking exploration at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExplorationMapCatalogTest {

    private lateinit var catalog: ExplorationMapSet
    private lateinit var scenarioLocations: Set<String>

    private data class ScenariosDoc(val scenarios: List<ScenarioEntity>)

    @Before
    fun load() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        catalog = context.assets.open("maps.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), ExplorationMapSet::class.java)
        }
        scenarioLocations = context.assets.open("scenarios.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), ScenariosDoc::class.java)
        }.scenarios.map { it.location }.toSet()
    }

    @Test
    fun catalogLoads_withUniqueMapIds() {
        assertTrue("Expected a non-trivial catalog", catalog.maps.size >= 10)
        assertEquals("Map ids must be unique",
            catalog.maps.size, catalog.maps.map { it.id }.toSet().size)
    }

    @Test
    fun everyScenarioLocation_hasAnExplorationMap() {
        val uncovered = scenarioLocations.filter { catalog.findMapForLocation(it) == null }
        assertTrue("Scenario locations without a map: $uncovered", uncovered.isEmpty())
    }

    @Test
    fun locationNames_areClaimedByAtMostOneMap() {
        val seen = mutableMapOf<String, String>()
        catalog.maps.forEach { map ->
            map.locationNames.forEach { loc ->
                val other = seen.put(loc.lowercase(), map.id)
                assertTrue("'$loc' claimed by both $other and ${map.id}", other == null)
            }
        }
    }

    @Test
    fun everyMap_hasExactlyOneStoryMarker() {
        catalog.maps.forEach { map ->
            assertEquals("${map.id}: expected exactly one STORY entity",
                1, map.entities.count { it.type == MapEntityType.STORY })
        }
    }

    @Test
    fun exits_targetExistingMaps_withReciprocalReturns() {
        catalog.maps.forEach { map ->
            map.entities.filter { it.type == MapEntityType.EXIT }.forEach { exit ->
                val target = catalog.findMapById(exit.targetMapId ?: "")
                assertTrue("${map.id}/${exit.id}: unknown target '${exit.targetMapId}'", target != null)
                val back = target!!.entities.any {
                    it.type == MapEntityType.EXIT && it.targetMapId == map.id
                }
                assertTrue("${map.id} -> ${target.id}: no exit back", back)
            }
        }
    }

    @Test
    fun spawnAndEntities_lieInsideBounds_offObstacles() {
        catalog.maps.forEach { map ->
            val points = listOf(Triple("spawn", map.spawn.x, map.spawn.y)) +
                map.entities.map { Triple(it.id, it.x, it.y) }
            points.forEach { (id, x, y) ->
                assertTrue("${map.id}/$id out of bounds",
                    x in 0f..map.width && y in 0f..map.height)
                assertFalse("${map.id}/$id sits inside an obstacle",
                    map.obstacles.any { it.contains(x, y) })
            }
        }
    }

    @Test
    fun everyEntity_isReachableFromSpawn_onTheGameNavGrid() {
        catalog.maps.forEach { map ->
            val grid = NavGrid.build(map, ExplorationManager.PLAYER_RADIUS)
            map.entities.forEach { entity ->
                val path = grid.findPath(map.spawn, MapPoint(entity.x, entity.y))
                assertTrue("${map.id}/${entity.id} is unreachable from spawn", path.isNotEmpty())
            }
        }
    }

    @Test
    fun themeColors_areValidHex() {
        val hex = Regex("^#[0-9A-Fa-f]{6}$")
        catalog.maps.forEach { map ->
            listOf(
                map.theme.ground, map.theme.groundDetail, map.theme.obstacleFill,
                map.theme.obstacleStroke, map.theme.accent
            ).forEach { color ->
                assertTrue("${map.id}: bad theme color '$color'", hex.matches(color))
            }
        }
    }
}
