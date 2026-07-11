package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameInput
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MiniGameManagerTest {

    private lateinit var manager: MiniGameManager

    @Before
    fun setup() {
        manager = MiniGameManager()
    }

    @Test
    fun registry_containsDefaultGames() {
        val ids = manager.getAllMiniGames().map { it.id }

        assertTrue(ids.containsAll(listOf(
            "lockpicking_1_locks", "lockpicking_2_locks", "lockpicking_3_locks",
            "trading_friendly", "trading_balanced", "trading_greedy", "trading_stubborn"
        )))
    }

    @Test
    fun startMiniGame_withKnownId_activatesGame() {
        assertTrue(manager.startMiniGame("lockpicking_1_locks"))

        assertTrue(manager.isGameActive.value)
        assertEquals("lockpicking_1_locks", manager.currentMiniGame.value?.id)
        assertNotNull(manager.currentGameState.value)
    }

    @Test
    fun startMiniGame_withUnknownId_returnsFalse() {
        assertFalse(manager.startMiniGame("does_not_exist"))
        assertFalse(manager.isGameActive.value)
    }

    @Test
    fun processInput_withoutActiveGame_returnsFalse() {
        assertFalse(manager.processInput(MiniGameInput.Confirm))
    }

    @Test
    fun processInput_confirm_completesLockPickingAndNotifiesListener() {
        var notified: Pair<String, MiniGameResult>? = null
        manager.setActivityListener { gameId, result -> notified = gameId to result }
        manager.startMiniGame("lockpicking_1_locks")

        assertTrue(manager.processInput(MiniGameInput.Confirm))

        val result = manager.lastResult.value
        assertNotNull(result)
        assertTrue(result!!.success)
        assertFalse(manager.isGameActive.value)
        assertNull(manager.currentMiniGame.value)
        assertEquals("lockpicking_1_locks", notified?.first)
        assertTrue(notified!!.second.success)
    }

    @Test
    fun cancelCurrentGame_clearsSessionWithoutResult() {
        manager.startMiniGame("trading_balanced")
        manager.cancelCurrentGame()

        assertFalse(manager.isGameActive.value)
        assertNull(manager.currentMiniGame.value)
        assertNull(manager.currentGameState.value)
        assertNull(manager.lastResult.value)
    }

    @Test
    fun clearLastResult_resetsResultState() {
        manager.startMiniGame("lockpicking_1_locks")
        manager.processInput(MiniGameInput.Confirm)
        assertNotNull(manager.lastResult.value)

        manager.clearLastResult()
        assertNull(manager.lastResult.value)
    }

    @Test
    fun getCurrentProgress_startsAtZero() {
        manager.startMiniGame("lockpicking_2_locks")
        assertEquals(0f, manager.getCurrentProgress(), 0.001f)
    }

    @Test
    fun createDirectCompletionResult_reflectsSuccessFlag() {
        val success = manager.createDirectCompletionResult("npc_chat", success = true)
        assertTrue(success.isCompleted)
        assertTrue(success.success)
        assertEquals(100, success.score)
        assertEquals(listOf("coins", "experience"), success.rewards)

        val failure = manager.createDirectCompletionResult("npc_chat", success = false)
        assertFalse(failure.success)
        assertEquals(0, failure.score)
        assertTrue(failure.rewards.isEmpty())
    }
}
