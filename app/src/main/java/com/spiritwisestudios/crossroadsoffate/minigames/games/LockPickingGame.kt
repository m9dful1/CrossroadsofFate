package com.spiritwisestudios.crossroadsoffate.minigames.games

import com.spiritwisestudios.crossroadsoffate.minigames.*
import kotlin.random.Random

/**
 * Lock-picking mini-game with Fallout-style mechanics and multi-phase difficulty.
 *
 * **Easy (1 lock):** Find sweet spot on top arch, hold it, swipe bottom arch to end.
 *
 * **Medium (2 locks):** Find sweet spot #1, hold, swipe bottom to 50% and hold.
 * Then slide top finger to sweet spot #2, hold, swipe bottom from 50% to end.
 * Lifting either finger at any point restarts the entire attempt.
 *
 * **Hard (3 locks):** Same pattern with 3 sweet spots and bottom checkpoints at 33%, 66%, 100%.
 * Lifting either finger restarts.
 *
 * Lock picks have durability — they break after [MAX_PICK_USES] failed attempts.
 *
 * @param lockCount Number of sweet spot phases (1=easy, 2=medium, 3=hard)
 * @param timeLimit Seconds allowed to complete
 */
class LockPickingGame(
    private val lockCount: Int = 1,
    private val timeLimit: Int = 60
) : MiniGame() {

    override val id: String = "lockpicking_${lockCount}_locks"
    override val name: String = "Lock Picking"
    override val difficulty: Int = minOf(5, lockCount + 1)
    override val description: String = when (lockCount) {
        1 -> "Pick a simple lock"
        2 -> "Pick a lock with two tumblers"
        else -> "Pick a complex lock with three tumblers"
    }
    override val instructions: String = when (lockCount) {
        1 -> """
            Pick the lock!

            • Swipe the top arch to find the sweet spot
            • When the phone vibrates, hold your finger steady
            • With another finger, swipe the bottom arch to unlock
        """.trimIndent()
        2 -> """
            Pick the lock! (Medium)

            • Find the first sweet spot on the top arch and hold it
            • Swipe the bottom arch to the middle and hold
            • Slide to the second sweet spot on the top arch
            • Swipe the bottom arch to the end to unlock
            • Don't lift either finger or you'll have to restart!
        """.trimIndent()
        else -> """
            Pick the lock! (Hard)

            • Find each sweet spot on the top arch in sequence
            • Advance the bottom arch to each checkpoint and hold
            • Don't lift either finger or you'll have to restart!
            • Complete all three positions to unlock
        """.trimIndent()
    }

    companion object {
        const val MAX_PICK_USES = 3
        private const val SWEET_SPOT_BASE_SIZE = 0.18f
        private const val SWEET_SPOT_SHRINK_PER_LOCK = 0.03f
        private const val SWEET_SPOT_MIN_SIZE = 0.08f
    }

    override fun initialize(): MiniGameState {
        val sweetSpotSize = (SWEET_SPOT_BASE_SIZE - SWEET_SPOT_SHRINK_PER_LOCK * (lockCount - 1))
            .coerceAtLeast(SWEET_SPOT_MIN_SIZE)

        // Generate all sweet spot positions upfront
        val sweetSpots = (0 until lockCount).map { generateSweetSpotCenter(sweetSpotSize) }
        // Bottom arch checkpoints: for N phases, checkpoints are at 1/N, 2/N, ... N/N
        val checkpoints = (1..lockCount).map { it.toFloat() / lockCount }

        return MiniGameState(
            isActive = true,
            timeRemaining = timeLimit,
            currentData = mapOf(
                "sweetSpots" to sweetSpots,
                "checkpoints" to checkpoints,
                "sweetSpotSize" to sweetSpotSize,
                "currentPhase" to 0,
                "totalPhases" to lockCount,
                "pickDurability" to MAX_PICK_USES,
                "startTime" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Processes high-level game events from the UI:
     * - Confirm = all phases completed, lock is open
     * - Slip = pick slipped / finger lifted — restart attempt, reduce durability
     */
    override fun processGameInput(currentState: MiniGameState, input: MiniGameInput): MiniGameState {
        return when (input) {
            is MiniGameInput.Confirm -> processLockOpened(currentState)
            is MiniGameInput.Slip -> processPickSlipped(currentState)
            else -> currentState
        }
    }

    private fun processLockOpened(state: MiniGameState): MiniGameState {
        // Lock fully opened — checkCompletion will detect this
        return state.copy(
            score = state.score + 100 * difficulty,
            currentData = state.currentData + mapOf(
                "currentPhase" to lockCount,
                "lastResult" to "SUCCESS"
            )
        )
    }

    private fun processPickSlipped(state: MiniGameState): MiniGameState {
        val durability = state.getData<Int>("pickDurability") ?: MAX_PICK_USES
        val sweetSpotSize = state.getData<Float>("sweetSpotSize") ?: SWEET_SPOT_BASE_SIZE
        val newDurability = durability - 1

        // Generate fresh sweet spots for the restart
        val newSweetSpots = (0 until lockCount).map { generateSweetSpotCenter(sweetSpotSize) }

        return state.copy(
            attempts = state.attempts + 1,
            currentData = state.currentData + mapOf(
                "currentPhase" to 0,
                "pickDurability" to newDurability,
                "sweetSpots" to newSweetSpots,
                "lastResult" to "SLIP"
            )
        )
    }

    override fun checkCompletion(state: MiniGameState): MiniGameResult? {
        val currentPhase = state.getData<Int>("currentPhase") ?: 0
        val durability = state.getData<Int>("pickDurability") ?: MAX_PICK_USES
        val totalAttempts = state.attempts
        val timeElapsed = getTimeElapsed(state)

        // All phases complete — success
        if (currentPhase >= lockCount) {
            return MiniGameResult(
                isCompleted = true,
                success = true,
                score = state.score + getBonusPoints(timeElapsed, totalAttempts),
                finalProgress = 1.0f,
                rewards = getRewards(totalAttempts),
                message = when (totalAttempts) {
                    0 -> "Perfect! Lock picked flawlessly!"
                    1 -> "Excellent! Only one slip."
                    else -> "Lock successfully picked."
                }
            )
        }

        // Pick broke
        if (durability <= 0) {
            return MiniGameResult(
                isCompleted = true,
                success = false,
                score = state.score,
                finalProgress = 0f,
                consequences = listOf("Lock pick broken"),
                message = "Your lock pick snapped!"
            )
        }

        // Time ran out
        if (timeElapsed >= timeLimit) {
            return MiniGameResult(
                isCompleted = true,
                success = false,
                score = state.score,
                finalProgress = 0f,
                message = "Time's up! Lock picking failed."
            )
        }

        return null
    }

    override fun getProgress(state: MiniGameState): Float {
        val currentPhase = state.getData<Int>("currentPhase") ?: 0
        return currentPhase.toFloat() / lockCount
    }

    private fun generateSweetSpotCenter(sweetSpotSize: Float): Float {
        val margin = sweetSpotSize / 2 + 0.05f
        return Random.nextFloat() * (1f - 2 * margin) + margin
    }

    private fun getTimeElapsed(state: MiniGameState): Int {
        val startTime = state.getData<Long>("startTime") ?: System.currentTimeMillis()
        return ((System.currentTimeMillis() - startTime) / 1000).toInt()
    }

    private fun getBonusPoints(timeElapsed: Int, totalAttempts: Int): Int {
        val timeBonus = maxOf(0, (timeLimit - timeElapsed) * 10)
        val perfectionBonus = if (totalAttempts == 0) 500 else 0
        return timeBonus + perfectionBonus
    }

    private fun getRewards(totalAttempts: Int): List<String> {
        val rewards = mutableListOf("experience", "coins")
        if (totalAttempts == 0) rewards.add("lockpick_tools")
        return rewards
    }
}
