package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.spiritwisestudios.crossroadsoffate.data.models.GameJson
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.logic.QuestManager
import com.spiritwisestudios.crossroadsoffate.logic.ReputationManager
import com.spiritwisestudios.crossroadsoffate.logic.StatsManager
import com.spiritwisestudios.crossroadsoffate.logic.TextResolver
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStreamReader

/**
 * Content-integrity net for dynamic-text tokens in scenarios.json (grammar in
 * docs/scenario-authoring-guide.md, resolved by [TextResolver]). The resolver
 * silently strips anything it cannot parse, so a typo in a token or a stat
 * name ships as missing or wrong text with no runtime signal — this test
 * fails CI instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScenarioTokenIntegrityTest {

    /** Items the mini-game engine itself can grant, independent of content. */
    private val engineGrantedItems = setOf("coins", "experience", "lockpick_tools", "reputation")

    private lateinit var scenarios: List<ScenarioEntity>
    private lateinit var grantableItems: Set<String>

    private data class ScenariosDoc(val scenarios: List<ScenarioEntity>)
    private data class LocationsDoc(val locations: List<InteractiveMapLocation>)

    @Before
    fun load() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        scenarios = context.assets.open("scenarios.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), ScenariosDoc::class.java)
        }.scenarios

        val activityRewards = context.assets.open("locations.json").use {
            GameJson.gson.fromJson(InputStreamReader(it), LocationsDoc::class.java)
        }.locations.flatMap { loc -> loc.availableActivities.flatMap { it.rewards } }

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
            activityRewards + questRewards + engineGrantedItems).toSet()
    }

    /** Every player-visible text that goes through TextResolver, labeled by origin. */
    private fun allTexts(): List<Pair<String, String>> = scenarios.flatMap { scenario ->
        listOf("${scenario.id}/text" to scenario.text) +
            scenario.decisions.flatMap { (position, decision) ->
                listOfNotNull(
                    "${scenario.id}/$position/text" to decision.text,
                    decision.fallbackText?.let { "${scenario.id}/$position/fallbackText" to it }
                )
            }
    }

    // Mirrors TextResolver's token grammar; names are additionally checked
    // against the canonical stat/faction/item sets so typos fail here
    private val statNames = StatsManager.DEFAULT_STATS.keys
    private val factionNames = ReputationManager.DEFAULT_REPUTATION.keys

    private fun isValidToken(token: String): Boolean {
        Regex("""\{if:!?has:([^}]+)\}""").matchEntire(token)?.let {
            return it.groupValues[1] in grantableItems
        }
        if (token == "{/if}") return true
        Regex("""\{item:([^}]+)\}""").matchEntire(token)?.let {
            return it.groupValues[1] in grantableItems
        }
        Regex("""\{stat:([^:}|]+):-?\d+:[^|}]*\|[^}]*\}""").matchEntire(token)?.let {
            return it.groupValues[1] in statNames
        }
        Regex("""\{rep:([^:}|]+):-?\d+:[^|}]*\|[^}]*\}""").matchEntire(token)?.let {
            return it.groupValues[1] in factionNames
        }
        Regex("""\{stat:([^:}|]+)\}""").matchEntire(token)?.let {
            return it.groupValues[1] in statNames
        }
        Regex("""\{rep:([^:}|]+)\}""").matchEntire(token)?.let {
            return it.groupValues[1] in factionNames
        }
        return false
    }

    @Test
    fun everyToken_matchesTheGrammar_withKnownNames() {
        val tokenRegex = Regex("""\{[^}]*\}""")
        val bad = allTexts().flatMap { (origin, text) ->
            tokenRegex.findAll(text)
                .filterNot { isValidToken(it.value) }
                .map { "$origin: ${it.value}" }
        }
        assertTrue(
            "Tokens that would be stripped or mis-resolved at runtime:\n${bad.joinToString("\n")}",
            bad.isEmpty()
        )
    }

    @Test
    fun conditionalBlocks_areBalanced() {
        // An unclosed {if:has:x} loses its tag but leaks its gated content to
        // every player; a stray {/if} means an earlier block ended too soon
        val unbalanced = allTexts().mapNotNull { (origin, text) ->
            val opens = Regex("""\{if:""").findAll(text).count()
            val closes = Regex("""\{/if\}""").findAll(text).count()
            if (opens != closes) "$origin: $opens {if:} vs $closes {/if}" else null
        }
        assertTrue("Unbalanced conditional blocks:\n${unbalanced.joinToString("\n")}",
            unbalanced.isEmpty())
    }

    @Test
    fun everyText_resolvesCleanly_underRepresentativeStates() {
        val states = listOf(
            Triple(emptySet<String>(), statNames.associateWith { 0 }, factionNames.associateWith { -5 }),
            Triple(grantableItems, statNames.associateWith { 10 }, factionNames.associateWith { 5 })
        )
        val leftovers = allTexts().flatMap { (origin, text) ->
            states.mapNotNull { (inventory, stats, reputation) ->
                val resolved = TextResolver.resolve(text, inventory, stats, reputation)
                if (resolved.contains('{') || resolved.contains('}')) {
                    "$origin resolved to: $resolved"
                } else null
            }
        }
        assertTrue("Texts with unresolved braces:\n${leftovers.joinToString("\n")}",
            leftovers.isEmpty())
    }

    @Test
    fun everyItemGate_hasALiveItemSource() {
        val missing = scenarios.flatMap { scenario ->
            scenario.decisions.mapNotNull { (position, decision) ->
                decision.condition?.requiredItem?.takeUnless { it in grantableItems }
                    ?.let { "${scenario.id}/$position requires '$it'" }
            }
        }
        assertTrue(
            "Item gates no content can ever open:\n${missing.joinToString("\n")}",
            missing.isEmpty()
        )
    }
}
