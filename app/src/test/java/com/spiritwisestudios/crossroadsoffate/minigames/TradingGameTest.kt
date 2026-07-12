package com.spiritwisestudios.crossroadsoffate.minigames

import com.spiritwisestudios.crossroadsoffate.minigames.games.TradingData
import com.spiritwisestudios.crossroadsoffate.minigames.games.TradingGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingGameTest {

    @Test
    fun initialize_setsPricesFromItemValue() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.BALANCED)
        val state = game.initialize()

        val data = state.data as TradingData
        assertEquals(150, data.currentPrice)
        assertEquals(150, data.originalPrice)
        // BALANCED requires 25% savings off the opening price: 150 * 0.75 = 112
        assertEquals(112, data.targetPrice)
        assertEquals(1, data.round)
        assertFalse(data.dealClosed)
    }

    @Test
    fun idAndDifficulty_deriveFromPersonality() {
        assertEquals("trading_friendly", TradingGame(npcPersonality = TradingGame.NPCPersonality.FRIENDLY).id)
        assertEquals(4, TradingGame(npcPersonality = TradingGame.NPCPersonality.GREEDY).difficulty)
        assertEquals(2, TradingGame(npcPersonality = TradingGame.NPCPersonality.FRIENDLY).difficulty)
    }

    @Test
    fun politeChoice_withFriendlyMerchant_lowersPriceAndImprovesMood() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.FRIENDLY)
        val state = game.processInput(game.initialize(), MiniGameInput.Choice("polite"))

        val data = state.data as TradingData
        assertEquals(135, data.currentPrice) // 150 - 15
        assertEquals(1, data.moodScore)
        assertEquals(2, data.round)
        assertEquals(1, state.attempts)
        assertNull(game.checkCompletion(state))
    }

    @Test
    fun unknownChoice_leavesStateUnchanged() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.BALANCED)
        val initial = game.initialize()
        assertEquals(initial, game.processInput(initial, MiniGameInput.Choice("bribe")))
    }

    @Test
    fun repeatedThreats_makeMerchantWalkAway() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.FRIENDLY)
        var state = game.initialize()
        // FRIENDLY reacts to threats with -2 mood each; two threats cross the -3 threshold
        state = game.processInput(state, MiniGameInput.Choice("threat"))
        state = game.processInput(state, MiniGameInput.Choice("threat"))

        assertTrue((state.data as TradingData).walkAway)
        val result = game.checkCompletion(state)
        assertNotNull(result)
        assertFalse(result!!.success)
        assertTrue(result.consequences.contains("reputation_loss"))
    }

    @Test
    fun acceptingFullPrice_closesDealWithoutSuccess() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.BALANCED)
        val state = game.processInput(game.initialize(), MiniGameInput.Confirm)

        assertTrue((state.data as TradingData).dealClosed)
        val result = game.checkCompletion(state)
        assertNotNull(result)
        // Paid the full 150 against a target of 80 — deal closed but not a success
        assertFalse(result!!.success)
        assertEquals(1.0f, result.finalProgress, 0.001f)
    }

    @Test
    fun skillfulNegotiation_reachesTargetPrice() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.FRIENDLY)
        var state = game.initialize()
        // Four polite rounds against a friendly merchant: 150 -> 135 -> 120 -> 98 -> 76
        repeat(4) { state = game.processInput(state, MiniGameInput.Choice("polite")) }
        assertEquals(76, (state.data as TradingData).currentPrice)

        state = game.processInput(state, MiniGameInput.Confirm)
        val result = game.checkCompletion(state)
        assertNotNull(result)
        assertTrue(result!!.success) // 76 <= target of 105 (30% off 150)
        assertTrue(result.rewards.contains("reputation")) // saved ~49%
    }

    @Test
    fun exhaustingRounds_closesDealAtCurrentPrice_insteadOfFailing() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.STUBBORN)
        var state = game.initialize()
        // Six polite rounds barely move a stubborn merchant: 150 -> 136
        repeat(6) { state = game.processInput(state, MiniGameInput.Choice("polite")) }

        val result = game.checkCompletion(state)
        assertNotNull(result)
        // The merchant ends the haggling but the purchase still happens at the
        // price on the table — using every round is not an automatic loss
        assertTrue(result!!.message.contains("impatient"))
        assertEquals(1.0f, result.finalProgress, 0.001f)
        // 136 misses the stubborn target of 135 by a coin, so not a *successful* deal
        assertFalse(result.success)
    }

    @Test
    fun everyPersonality_hasAWinningStrategy() {
        // Registry pairings from MiniGameManager: personality to item value
        val registry = mapOf(
            TradingGame.NPCPersonality.FRIENDLY to 50,
            TradingGame.NPCPersonality.BALANCED to 100,
            TradingGame.NPCPersonality.GREEDY to 150,
            TradingGame.NPCPersonality.STUBBORN to 200
        )
        val choices = TradingGame.AVAILABLE_CHOICES.map { it.first }

        for ((personality, itemValue) in registry) {
            val game = TradingGame(itemValue = itemValue, npcPersonality = personality)

            fun winnable(state: com.spiritwisestudios.crossroadsoffate.minigames.MiniGameState): Boolean {
                // Try accepting right now
                val accepted = game.processInput(state, MiniGameInput.Confirm)
                if (game.checkCompletion(accepted)?.success == true) return true
                // Session over (walk-away or out of rounds) without a win
                if (game.checkCompletion(state) != null) return false
                // Otherwise negotiate on with every possible approach
                return choices.any { choice ->
                    val next = game.processInput(state, MiniGameInput.Choice(choice))
                    if (next === state) false else winnable(next)
                }
            }

            assertTrue(
                "Trading vs $personality (value $itemValue) must be winnable by some strategy",
                winnable(game.initialize())
            )
        }
    }

    @Test
    fun cancel_deactivatesGame() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.BALANCED)
        val cancelled = game.processInput(game.initialize(), MiniGameInput.Cancel)
        assertFalse(cancelled.isActive)
    }

    @Test
    fun getProgress_tracksRounds() {
        val game = TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.BALANCED)
        var state = game.initialize()
        assertEquals(0f, game.getProgress(state), 0.001f)

        state = game.processInput(state, MiniGameInput.Choice("polite"))
        assertEquals(1f / 6f, game.getProgress(state), 0.001f)
    }
}
