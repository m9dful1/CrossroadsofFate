package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.spiritwisestudios.crossroadsoffate.data.models.GameJson
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.LocationActivity
import com.spiritwisestudios.crossroadsoffate.data.models.Quest
import com.spiritwisestudios.crossroadsoffate.data.models.QuestObjective
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.logic.QuestManager
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStreamReader

/**
 * Content-integrity net for the quest system: every quest defined in
 * [QuestManager] must be completable with the shipped scenarios.json and
 * locations.json. Item objectives need a live item source (scenario grant,
 * activity reward, or another completable quest's reward), activity objectives
 * need a real — and reachable — activity, and count objectives need enough
 * (or repeatable) activities of the required type.
 *
 * This is the test form of the 2026-07-11 quest audit, which found four of
 * nine quests unachievable; it fails if content or reward plumbing regresses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QuestContentReachabilityTest {

    /** Items the mini-game engine itself can grant, independent of content. */
    private val engineGrantedItems = setOf("coins", "experience", "lockpick_tools", "reputation")

    private lateinit var scenarioIds: Set<String>
    private lateinit var scenarioItemGrants: Set<String>
    private lateinit var activities: Map<String, LocationActivity>
    private lateinit var allQuests: List<Quest>

    private data class ScenariosDoc(val scenarios: List<ScenarioEntity>)
    private data class LocationsDoc(val locations: List<InteractiveMapLocation>)

    @Before
    fun load() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scenarios = context.assets.open("scenarios.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), ScenariosDoc::class.java)
        }.scenarios
        scenarioIds = scenarios.map { it.id }.toSet()
        scenarioItemGrants = scenarios.flatMap { it.itemGiven?.values ?: emptyList() }.toSet()

        activities = context.assets.open("locations.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), LocationsDoc::class.java)
        }.locations.flatMap { it.availableActivities }.associateBy { it.id }

        // Enumerate every quest the game can hand out via the public API
        val questManager = QuestManager()
        questManager.initialize(emptyList(), emptyList())
        questManager.activateSideQuests()
        listOf(
            QuestManager.GUARD_PATH_SCENARIO_ID,
            QuestManager.ADVENTURER_PATH_SCENARIO_ID,
            QuestManager.MERCHANT_PATH_SCENARIO_ID,
            QuestManager.OUTLAW_PATH_SCENARIO_ID
        ).forEach { questManager.activatePathQuest(it) }
        allQuests = questManager.activeQuests.value
    }

    @Test
    fun everyQuest_isCompletableWithShippedContent() {
        assertTrue("Expected the full quest roster", allQuests.size >= 8)

        // Fixpoint: a completable quest's rewards become obtainable items,
        // which may unlock further quests (torch, merchant seal...)
        val obtainable = (scenarioItemGrants +
            activities.values.flatMap { it.rewards } +
            engineGrantedItems).toMutableSet()

        val pending = allQuests.toMutableList()
        var progressed = true
        while (progressed) {
            progressed = false
            val iterator = pending.iterator()
            while (iterator.hasNext()) {
                val quest = iterator.next()
                if (quest.objectives.all { isSatisfiable(it, obtainable) }) {
                    obtainable.addAll(quest.rewards)
                    iterator.remove()
                    progressed = true
                }
            }
        }

        val diagnostics = pending.map { quest ->
            val blocked = quest.objectives.filterNot { isSatisfiable(it, obtainable) }
            "${quest.id}: blocked on ${blocked.map { it.id + " (" + describe(it) + ")" }}"
        }
        assertTrue("Unachievable quests:\n${diagnostics.joinToString("\n")}", pending.isEmpty())
    }

    private fun isSatisfiable(objective: QuestObjective, obtainable: Set<String>): Boolean = when {
        objective.requiredScenarioId != null ->
            scenarioIds.contains(objective.requiredScenarioId)

        objective.requiredItemId != null ->
            obtainable.contains(objective.requiredItemId)

        objective.requiredActivityId != null -> {
            val activity = activities[objective.requiredActivityId]
            activity != null && activity.requiredItems.all { obtainable.contains(it) }
        }

        objective.requiredActivityCount != null && objective.requiredActivityType != null -> {
            val ofType = activities.values.filter { it.type.name == objective.requiredActivityType }
            ofType.any { it.isRepeatable } || ofType.size >= objective.requiredActivityCount
        }

        else -> true
    }

    private fun describe(objective: QuestObjective): String = when {
        objective.requiredScenarioId != null -> "needs scenario ${objective.requiredScenarioId}"
        objective.requiredItemId != null -> "no source for item ${objective.requiredItemId}"
        objective.requiredActivityId != null -> "activity ${objective.requiredActivityId} missing or item-gated"
        else -> "needs ${objective.requiredActivityCount} of type ${objective.requiredActivityType}"
    }
}
