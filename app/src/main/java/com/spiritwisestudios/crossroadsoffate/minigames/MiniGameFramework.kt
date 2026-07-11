package com.spiritwisestudios.crossroadsoffate.minigames

/**
 * Abstract base class for all mini-games in the system.
 * Provides a common interface for game initialization, input processing, and result generation.
 */
abstract class MiniGame {
    abstract val id: String
    abstract val name: String
    abstract val difficulty: Int // 1-5 scale
    abstract val description: String
    abstract val instructions: String
    
    /**
     * Initializes the mini-game and returns the initial state
     */
    abstract fun initialize(): MiniGameState
    
    /**
     * Processes player input and returns the updated game state
     */
    abstract fun processInput(currentState: MiniGameState, input: MiniGameInput): MiniGameState
    
    /**
     * Checks if the game is completed and returns the final result
     */
    abstract fun checkCompletion(state: MiniGameState): MiniGameResult?
    
    /**
     * Gets the current progress as a percentage (0.0 to 1.0)
     */
    abstract fun getProgress(state: MiniGameState): Float
}

/**
 * Represents the current state of a mini-game
 */
data class MiniGameState(
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val timeRemaining: Int? = null, // seconds
    val currentData: Map<String, Any> = emptyMap(),
    val score: Int = 0,
    val attempts: Int = 0,
    val maxAttempts: Int? = null
) {
    /**
     * Gets a data value with type casting
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String): T? {
        return currentData[key] as? T
    }
}

/**
 * Represents the final result of a completed mini-game
 */
data class MiniGameResult(
    val isCompleted: Boolean,
    val success: Boolean,
    val score: Int = 0,
    val finalProgress: Float = 0f,
    val timeSpent: Int = 0, // seconds
    val rewards: List<String> = emptyList(),
    val consequences: List<String> = emptyList(),
    val message: String = ""
)

/**
 * Sealed class representing different types of player input
 */
sealed class MiniGameInput {
    data class Choice(val choice: String) : MiniGameInput()
    object Confirm : MiniGameInput()
    object Cancel : MiniGameInput()
    object Slip : MiniGameInput()
}

/**
 * Interface for mini-game event listeners
 */
interface MiniGameListener {
    fun onGameStarted(gameId: String)
    fun onGameCompleted(gameId: String, result: MiniGameResult)
    fun onGameCancelled(gameId: String)
    fun onGameStateChanged(gameId: String, state: MiniGameState)
}