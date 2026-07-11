package com.spiritwisestudios.crossroadsoffate.minigames

import com.spiritwisestudios.crossroadsoffate.minigames.games.LockPickingData
import com.spiritwisestudios.crossroadsoffate.minigames.games.LockPickingGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPickingGameTest {

    private fun completedPhaseState(game: LockPickingGame): MiniGameState =
        game.processInput(game.initialize(), MiniGameInput.Confirm)

    @Test
    fun initialize_setsUpPhasesDurabilityAndTimer() {
        val game = LockPickingGame(lockCount = 2, timeLimit = 75)
        val state = game.initialize()

        assertTrue(state.isActive)
        assertEquals(75, state.timeRemaining)
        val data = state.data as LockPickingData
        assertEquals(0, data.currentPhase)
        assertEquals(2, data.totalPhases)
        assertEquals(LockPickingGame.MAX_PICK_USES, data.pickDurability)
        assertEquals(2, data.sweetSpots.size)
        assertEquals(listOf(0.5f, 1.0f), data.checkpoints)
    }

    @Test
    fun slip_decrementsDurabilityAndResetsPhase() {
        val game = LockPickingGame(lockCount = 1, timeLimit = 45)
        val slipped = game.processInput(game.initialize(), MiniGameInput.Slip)

        val data = slipped.data as LockPickingData
        assertEquals(1, slipped.attempts)
        assertEquals(LockPickingGame.MAX_PICK_USES - 1, data.pickDurability)
        assertEquals(0, data.currentPhase)
        assertEquals("SLIP", data.lastResult)
        // No completion yet — durability remains
        assertNull(game.checkCompletion(slipped))
    }

    @Test
    fun exhaustedDurability_breaksPickAndFailsGame() {
        val game = LockPickingGame(lockCount = 1, timeLimit = 45)
        var state = game.initialize()
        repeat(LockPickingGame.MAX_PICK_USES) {
            state = game.processInput(state, MiniGameInput.Slip)
        }

        val result = game.checkCompletion(state)
        assertNotNull(result)
        assertFalse(result!!.success)
        assertTrue(result.consequences.contains("Lock pick broken"))
    }

    @Test
    fun confirm_completesLockSuccessfully() {
        val game = LockPickingGame(lockCount = 1, timeLimit = 45)
        val opened = completedPhaseState(game)

        val result = game.checkCompletion(opened)
        assertNotNull(result)
        assertTrue(result!!.success)
        assertEquals(1.0f, result.finalProgress, 0.001f)
        // Flawless run grants the bonus tool reward and the perfect message
        assertTrue(result.rewards.contains("lockpick_tools"))
        assertEquals("Perfect! Lock picked flawlessly!", result.message)
    }

    @Test
    fun confirm_afterOneSlip_omitsPerfectionReward() {
        val game = LockPickingGame(lockCount = 1, timeLimit = 45)
        var state = game.initialize()
        state = game.processInput(state, MiniGameInput.Slip)
        state = game.processInput(state, MiniGameInput.Confirm)

        val result = game.checkCompletion(state)
        assertNotNull(result)
        assertTrue(result!!.success)
        assertFalse(result.rewards.contains("lockpick_tools"))
        assertEquals("Excellent! Only one slip.", result.message)
    }

    @Test
    fun cancel_deactivatesGame() {
        val game = LockPickingGame(lockCount = 1, timeLimit = 45)
        val cancelled = game.processInput(game.initialize(), MiniGameInput.Cancel)
        assertFalse(cancelled.isActive)
    }

    @Test
    fun input_afterCompletion_isIgnored() {
        val game = LockPickingGame(lockCount = 1, timeLimit = 45)
        val completed = game.initialize().copy(isCompleted = true)

        val after = game.processInput(completed, MiniGameInput.Slip)
        assertEquals(completed, after)
    }

    @Test
    fun getProgress_tracksPhaseCompletion() {
        val game = LockPickingGame(lockCount = 2, timeLimit = 75)
        val state = game.initialize()

        assertEquals(0f, game.getProgress(state), 0.001f)
        val opened = game.processInput(state, MiniGameInput.Confirm)
        assertEquals(1.0f, game.getProgress(opened), 0.001f)
    }

    @Test
    fun difficultyAndId_deriveFromLockCount() {
        assertEquals("lockpicking_3_locks", LockPickingGame(lockCount = 3).id)
        assertEquals(2, LockPickingGame(lockCount = 1).difficulty)
        assertEquals(4, LockPickingGame(lockCount = 3).difficulty)
    }
}
