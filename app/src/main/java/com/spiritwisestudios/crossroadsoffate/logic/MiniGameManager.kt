package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.minigames.*
import com.spiritwisestudios.crossroadsoffate.minigames.games.LockPickingGame
import com.spiritwisestudios.crossroadsoffate.minigames.games.TradingGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages mini-game instances, state, and coordination with the activity system.
 * Follows the same architectural patterns as InventoryManager and QuestManager.
 */
class MiniGameManager : MiniGameListener {
    
    // Registry of available mini-games
    private val miniGameRegistry = mutableMapOf<String, MiniGame>()
    
    // Current game state
    private val _currentMiniGame = MutableStateFlow<MiniGame?>(null)
    val currentMiniGame: StateFlow<MiniGame?> = _currentMiniGame.asStateFlow()
    
    private val _currentGameState = MutableStateFlow<MiniGameState?>(null)
    val currentGameState: StateFlow<MiniGameState?> = _currentGameState.asStateFlow()
    
    private val _lastResult = MutableStateFlow<MiniGameResult?>(null)
    val lastResult: StateFlow<MiniGameResult?> = _lastResult.asStateFlow()
    
    // Game session tracking
    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()
    
    private var gameStartTime: Long = 0L
    
    // External listener for activity completion events
    private var activityListener: ((String, MiniGameResult) -> Unit)? = null
    
    init {
        registerDefaultMiniGames()
    }
    
    /**
     * Registers default mini-games available in the system
     */
    private fun registerDefaultMiniGames() {
        // Register built-in mini-games
        registerMiniGame(LockPickingGame(lockCount = 1, timeLimit = 45)) // Easy: 1 sweet spot
        registerMiniGame(LockPickingGame(lockCount = 2, timeLimit = 75)) // Medium: 2 phases
        registerMiniGame(LockPickingGame(lockCount = 3, timeLimit = 90)) // Hard: 3 phases
        
        registerMiniGame(TradingGame(itemValue = 50, npcPersonality = TradingGame.NPCPersonality.FRIENDLY))
        registerMiniGame(TradingGame(itemValue = 100, npcPersonality = TradingGame.NPCPersonality.BALANCED))
        registerMiniGame(TradingGame(itemValue = 150, npcPersonality = TradingGame.NPCPersonality.GREEDY))
        registerMiniGame(TradingGame(itemValue = 200, npcPersonality = TradingGame.NPCPersonality.STUBBORN))
        
        // TODO: Add more mini-games
        // registerMiniGame(MemoryPuzzle())
        // registerMiniGame(PatternMatchingGame())
    }
    
    /**
     * Registers a new mini-game in the system
     */
    fun registerMiniGame(miniGame: MiniGame) {
        miniGameRegistry[miniGame.id] = miniGame
    }
    
    /**
     * Gets a mini-game by ID
     */
    fun getMiniGame(gameId: String): MiniGame? {
        return miniGameRegistry[gameId]
    }
    
    /**
     * Gets all available mini-games
     */
    fun getAllMiniGames(): List<MiniGame> {
        return miniGameRegistry.values.toList()
    }
    
    /**
     * Starts a mini-game session
     */
    fun startMiniGame(gameId: String): Boolean {
        val miniGame = miniGameRegistry[gameId] ?: return false
        
        if (_isGameActive.value) {
            // End current game first
            endCurrentGame()
        }
        
        try {
            val initialState = miniGame.initialize()
            _currentMiniGame.value = miniGame
            _currentGameState.value = initialState
            _isGameActive.value = true
            gameStartTime = System.currentTimeMillis()
            
            onGameStarted(gameId)
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error starting mini-game %s", gameId)
            return false
        }
    }
    
    /**
     * Processes input for the current mini-game
     */
    fun processInput(input: MiniGameInput): Boolean {
        val currentGame = _currentMiniGame.value ?: return false
        val currentState = _currentGameState.value ?: return false
        
        if (!_isGameActive.value || currentState.isCompleted) {
            return false
        }
        
        try {
            val newState = currentGame.processInput(currentState, input)
            _currentGameState.value = newState
            
            onGameStateChanged(currentGame.id, newState)
            
            // Check for completion
            val result = currentGame.checkCompletion(newState)
            if (result != null) {
                completeGame(result)
            }
            
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error processing input for mini-game %s", currentGame.id)
            return false
        }
    }
    
    /**
     * Gets the current progress of the active mini-game
     */
    fun getCurrentProgress(): Float {
        val currentGame = _currentMiniGame.value ?: return 0f
        val currentState = _currentGameState.value ?: return 0f
        return currentGame.getProgress(currentState)
    }
    
    /**
     * Cancels the current mini-game
     */
    fun cancelCurrentGame() {
        val currentGame = _currentMiniGame.value
        if (currentGame != null) {
            onGameCancelled(currentGame.id)
        }
        endCurrentGame()
    }
    
    /**
     * Completes the current mini-game with a result
     */
    private fun completeGame(result: MiniGameResult) {
        val currentGame = _currentMiniGame.value ?: return
        val timeSpent = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()
        
        val finalResult = result.copy(timeSpent = timeSpent)
        _lastResult.value = finalResult
        
        onGameCompleted(currentGame.id, finalResult)
        endCurrentGame()
    }
    
    /**
     * Ends the current game session
     */
    private fun endCurrentGame() {
        _currentMiniGame.value = null
        _currentGameState.value = null
        _isGameActive.value = false
        gameStartTime = 0L
    }
    
    /**
     * Sets a listener for activity completion events
     */
    fun setActivityListener(listener: (String, MiniGameResult) -> Unit) {
        activityListener = listener
    }
    
    /**
     * Updates animation state for games that require per-frame updates (e.g., lock picking pin movement).
     * Also checks for time-based completion conditions.
     */
    fun updateAnimationTick() {
        val currentGame = _currentMiniGame.value ?: return
        val currentState = _currentGameState.value ?: return

        if (!_isGameActive.value || currentState.isCompleted) return

        // Check for time-based completion
        val result = currentGame.checkCompletion(currentState)
        if (result != null) {
            completeGame(result)
        }
    }

    /**
     * Clears the last result (for UI state management)
     */
    fun clearLastResult() {
        _lastResult.value = null
    }
    
    // MiniGameListener implementation
    override fun onGameStarted(gameId: String) {
        Timber.d("Mini-game started: %s", gameId)
    }
    
    override fun onGameCompleted(gameId: String, result: MiniGameResult) {
        Timber.d("Mini-game completed: %s, Success: %s, Score: %d", gameId, result.success, result.score)
        activityListener?.invoke(gameId, result)
    }
    
    override fun onGameCancelled(gameId: String) {
        Timber.d("Mini-game cancelled: %s", gameId)
    }
    
    override fun onGameStateChanged(gameId: String, state: MiniGameState) {
    }
    
    /**
     * Gets mini-games by category
     */
    fun getMiniGamesByCategory(category: MiniGameCategory): List<MiniGame> {
        return miniGameRegistry.values.filter { game ->
            when (category) {
                MiniGameCategory.SKILL -> game.id.contains("timing") || game.id.contains("reflex")
                MiniGameCategory.PUZZLE -> game.id.contains("puzzle") || game.id.contains("logic")
                MiniGameCategory.SOCIAL -> game.id.contains("trading") || game.id.contains("negotiation")
                MiniGameCategory.CHANCE -> game.id.contains("chance") || game.id.contains("luck")
                MiniGameCategory.EXPLORATION -> game.id.contains("exploration") || game.id.contains("investigation")
            }
        }
    }
    
    /**
     * Gets recommended mini-games based on player progress
     */
    fun getRecommendedMiniGames(playerLevel: Int, completedGames: Set<String>): List<MiniGame> {
        return miniGameRegistry.values.filter { game ->
            game.id !in completedGames && game.difficulty <= (playerLevel + 1)
        }.sortedBy { it.difficulty }
    }
    
    /**
     * Creates a simple result for testing (placeholder functionality)
     */
    fun createTestResult(gameId: String, success: Boolean = true): MiniGameResult {
        return MiniGameResult(
            isCompleted = true,
            success = success,
            score = if (success) 100 else 0,
            finalProgress = if (success) 1.0f else 0.5f,
            timeSpent = 30,
            rewards = if (success) listOf("coins", "experience") else emptyList(),
            message = if (success) "Well done!" else "Better luck next time!"
        )
    }
} 