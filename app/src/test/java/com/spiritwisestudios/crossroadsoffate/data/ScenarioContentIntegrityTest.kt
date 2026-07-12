package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.spiritwisestudios.crossroadsoffate.data.models.GameJson
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.LeadsTo
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.logic.StatsManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStreamReader

/**
 * Structural integrity net for the shipped scenario graph, added with the
 * 2026-07-11 branching audit fixes: no orphaned content, no stat gate the
 * player cannot possibly earn, and every terminal scenario flagged as an
 * ending (so the ending screen shows instead of an infinite self-loop).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScenarioContentIntegrityTest {

    private lateinit var scenarios: List<ScenarioEntity>
    private lateinit var locations: List<InteractiveMapLocation>

    private data class ScenariosDoc(val scenarios: List<ScenarioEntity>)
    private data class LocationsDoc(val locations: List<InteractiveMapLocation>)

    @Before
    fun load() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        scenarios = context.assets.open("scenarios.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), ScenariosDoc::class.java)
        }.scenarios
        locations = context.assets.open("locations.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), LocationsDoc::class.java)
        }.locations
    }

    private fun decisionTargets(scenario: ScenarioEntity): Set<String> =
        scenario.decisions.values.flatMap { decision ->
            when (val leadsTo = decision.leadsTo) {
                is LeadsTo.Simple -> listOf(leadsTo.scenarioId)
                is LeadsTo.Conditional -> listOf(leadsTo.ifConditionMet, leadsTo.ifConditionNotMet)
            }
        }.toSet()

    @Test
    fun everyScenario_isReachable_fromStartOrMapTravel() {
        val byId = scenarios.associateBy { it.id }
        // Entry points: the opening scenario plus everything map travel can
        // start (first visits and revisit variants)
        val entryPoints = (setOf("scenario1") +
            locations.map { it.scenarioId } +
            locations.mapNotNull { it.revisitScenarioId })
            .filter { byId.containsKey(it) }

        val visited = mutableSetOf<String>()
        val frontier = ArrayDeque(entryPoints)
        while (frontier.isNotEmpty()) {
            val id = frontier.removeFirst()
            if (!visited.add(id)) continue
            byId.getValue(id).let { scenario ->
                decisionTargets(scenario).forEach {
                    if (byId.containsKey(it)) frontier.addLast(it)
                }
            }
        }

        val orphans = byId.keys - visited
        assertTrue("Unreachable scenarios (orphaned content): $orphans", orphans.isEmpty())
    }

    @Test
    fun everyStatGate_isEarnable() {
        val earnable = mutableMapOf<String, Int>()
        scenarios.forEach { scenario ->
            scenario.statsGranted?.values?.forEach { grants ->
                grants.forEach { (stat, amount) ->
                    if (amount > 0) earnable.merge(stat, amount, Int::plus)
                }
            }
        }

        scenarios.forEach { scenario ->
            scenario.decisions.values.forEach { decision ->
                val condition = decision.condition ?: return@forEach
                val stat = condition.requiredStat ?: return@forEach
                val min = condition.minStatValue ?: return@forEach
                val maxEarnable = (StatsManager.DEFAULT_STATS[stat] ?: 0) + (earnable[stat] ?: 0)
                assertTrue(
                    "${scenario.id} requires $stat >= $min but content only offers $maxEarnable",
                    maxEarnable >= min
                )
            }
        }
    }

    @Test
    fun scenariosThatOnlyLoopToThemselves_areMarkedAsEndings() {
        val selfLoops = scenarios
            .filter { decisionTargets(it) == setOf(it.id) }
            .map { it.id }.toSet()
        val flagged = scenarios.filter { it.isEnding }.map { it.id }.toSet()
        assertEquals("Self-looping scenarios and isEnding flags must match", selfLoops, flagged)
    }

    @Test
    fun revisitVariants_pointAtRealScenarios() {
        val ids = scenarios.map { it.id }.toSet()
        locations.mapNotNull { it.revisitScenarioId }.forEach { revisitId ->
            assertTrue("revisitScenarioId $revisitId is not in scenarios.json", ids.contains(revisitId))
        }
    }
}
