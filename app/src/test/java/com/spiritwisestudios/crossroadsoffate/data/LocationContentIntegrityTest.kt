package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.spiritwisestudios.crossroadsoffate.data.models.GameJson
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.logic.QuestManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStreamReader

/**
 * Content-integrity net for locations.json (the interactive map). A discovery
 * condition matches either an inventory item or a visited scenario-location
 * string, so a rename on either side silently makes the location
 * undiscoverable forever — this test fails CI instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocationContentIntegrityTest {

    /** Items the mini-game engine itself can grant, independent of content. */
    private val engineGrantedItems = setOf("coins", "experience", "lockpick_tools", "reputation")

    private lateinit var locations: List<InteractiveMapLocation>
    private lateinit var scenarioIds: Set<String>
    private lateinit var scenarioLocations: Set<String>
    private lateinit var grantableItems: Set<String>

    private data class ScenariosDoc(val scenarios: List<ScenarioEntity>)
    private data class LocationsDoc(val locations: List<InteractiveMapLocation>)

    @Before
    fun load() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenarios = context.assets.open("scenarios.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), ScenariosDoc::class.java)
        }.scenarios
        scenarioIds = scenarios.map { it.id }.toSet()
        scenarioLocations = scenarios.map { it.location }.toSet()

        locations = context.assets.open("locations.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), LocationsDoc::class.java)
        }.locations

        val questManager = QuestManager()
        questManager.initialize(emptyList(), emptyList())
        questManager.activateSideQuests()
        listOf(
            QuestManager.GUARD_PATH_SCENARIO_ID,
            QuestManager.ADVENTURER_PATH_SCENARIO_ID,
            QuestManager.MERCHANT_PATH_SCENARIO_ID,
            QuestManager.OUTLAW_PATH_SCENARIO_ID
        ).forEach { questManager.activatePathQuest(it) }
        val questRewards = questManager.activeQuests.value.flatMap { it.rewards }

        grantableItems = (scenarios.flatMap { it.itemGiven?.values ?: emptyList() } +
            locations.flatMap { loc -> loc.availableActivities.flatMap { it.rewards } } +
            questRewards + engineGrantedItems).toSet()
    }

    @Test
    fun everyDiscoveryCondition_isSatisfiable() {
        val unsatisfiable = locations.flatMap { location ->
            location.discoveryConditions
                .filterNot { it in grantableItems || it in scenarioLocations }
                .map { "${location.id}: '$it' is neither an obtainable item nor a scenario location" }
        }
        assertTrue(
            "Locations that can never be discovered:\n${unsatisfiable.joinToString("\n")}",
            unsatisfiable.isEmpty()
        )
    }

    @Test
    fun everyLocationScenario_exists() {
        // The reachability BFS in ScenarioContentIntegrityTest filters out
        // dangling entry points, so a bad scenarioId must be caught here
        val dangling = locations.flatMap { location ->
            listOfNotNull(
                location.scenarioId.takeUnless { it in scenarioIds },
                location.revisitScenarioId?.takeUnless { it in scenarioIds }
            ).map { "${location.id} -> $it" }
        }
        assertTrue("Locations pointing at missing scenarios:\n${dangling.joinToString("\n")}",
            dangling.isEmpty())
    }

    @Test
    fun connections_referenceRealLocations_reciprocally() {
        val byId = locations.associateBy { it.id }
        val bad = locations.flatMap { location ->
            location.connections.mapNotNull { targetId ->
                val target = byId[targetId]
                when {
                    target == null -> "${location.id} -> unknown '$targetId'"
                    location.id !in target.connections ->
                        "${location.id} -> $targetId has no connection back"
                    else -> null
                }
            }
        }
        assertTrue("Broken map connections:\n${bad.joinToString("\n")}", bad.isEmpty())
    }

    @Test
    fun activityIds_areGloballyUnique() {
        // Completion state is keyed by activity id alone; a duplicate across
        // locations would mark both complete when either one finishes
        val allIds = locations.flatMap { loc -> loc.availableActivities.map { it.id } }
        assertEquals("Duplicate activity ids across locations",
            allIds.size, allIds.toSet().size)
    }
}
