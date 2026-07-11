package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiritwisestudios.crossroadsoffate.data.models.*
import com.spiritwisestudios.crossroadsoffate.logic.ActivityManager
import com.spiritwisestudios.crossroadsoffate.logic.GameAudioManager
import com.spiritwisestudios.crossroadsoffate.logic.InventoryManager
import com.spiritwisestudios.crossroadsoffate.logic.MiniGameManager
import com.spiritwisestudios.crossroadsoffate.logic.QuestManager
import com.spiritwisestudios.crossroadsoffate.logic.ReputationManager
import com.spiritwisestudios.crossroadsoffate.logic.StatsManager
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGame
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameInput
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameResult
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ViewModel responsible for managing game state and business logic.
 * Handles player progress, scenarios, inventory, quests, and UI state.
 */
class GameViewModel(
    application: Application,
    private val repository: GameRepository,
) : AndroidViewModel(application) {

    companion object {
        private const val DEFAULT_PLAYER_ID = "default_player"
        private const val DEBUG_PLAYER_ID = "debug_player"
        private const val STARTING_SCENARIO_ID = "scenario1"
    }

    // Logic managers
    private val inventoryManager = InventoryManager()
    private val questManager = QuestManager()
    private val activityManager = ActivityManager()
    private val miniGameManager = MiniGameManager()
    private val statsManager = StatsManager()
    private val reputationManager = ReputationManager()
    private val audioManager = GameAudioManager(application)

    // State flows for observing game state changes
    private val _playerProgress = MutableStateFlow<PlayerProgress?>(null)
    val playerProgress: StateFlow<PlayerProgress?> = _playerProgress

    private val _currentScenario = MutableStateFlow<ScenarioEntity?>(null)
    val currentScenario: StateFlow<ScenarioEntity?> = _currentScenario

    val playerInventory: StateFlow<Set<String>> = inventoryManager.inventory
    val playerStats: StateFlow<Map<String, Int>> = statsManager.stats
    val playerReputation: StateFlow<Map<String, Int>> = reputationManager.reputation

    // Audio state
    val musicVolume: StateFlow<Float> = audioManager.musicVolume
    val sfxVolume: StateFlow<Float> = audioManager.sfxVolume
    val isMuted: StateFlow<Boolean> = audioManager.isMuted

    // UI state flows
    private val _isMapVisible = MutableStateFlow(false)
    val isMapVisible: StateFlow<Boolean> = _isMapVisible

    private val _isCharacterMenuVisible = MutableStateFlow(false)
    val isCharacterMenuVisible: StateFlow<Boolean> = _isCharacterMenuVisible

    // Quest management state flows
    val activeQuests: StateFlow<List<Quest>> = questManager.activeQuests
    val completedQuests: StateFlow<List<Quest>> = questManager.completedQuests

    // Interactive map location state flows
    private val _interactiveMapLocations = MutableStateFlow<List<InteractiveMapLocation>>(emptyList())
    val interactiveMapLocations: StateFlow<List<InteractiveMapLocation>> = _interactiveMapLocations

    // Activity management state flows
    val completedActivities: StateFlow<Set<String>> = activityManager.completedActivities
    val activityResults: StateFlow<List<ActivityResult>> = activityManager.activityResults
    val unlockedLocations: StateFlow<Set<String>> = activityManager.unlockedLocations

    // Mini-game management state flows
    val currentMiniGame = miniGameManager.currentMiniGame
    val currentMiniGameState = miniGameManager.currentGameState
    val lastMiniGameResult = miniGameManager.lastResult
    val isMiniGameActive = miniGameManager.isGameActive

    // Quest reward notification
    private val _questRewardNotification = MutableStateFlow<QuestCompletionEvent?>(null)
    val questRewardNotification: StateFlow<QuestCompletionEvent?> = _questRewardNotification.asStateFlow()

    private val _isOnTitleScreen = MutableStateFlow(true)
    val isOnTitleScreen: StateFlow<Boolean> = _isOnTitleScreen

    // Initialize ViewModel
    init {
        audioManager.initialize()

        viewModelScope.launch {
            try {
                repository.loadScenariosFromJson()
                repository.initializeInteractiveMapLocations()
                loadPlayerProgress(DEFAULT_PLAYER_ID)
                updateInteractiveMapLocations()
                
                // Set up mini-game activity listener
                miniGameManager.setActivityListener { gameId, result ->
                    handleMiniGameResult(gameId, result)
                }

                // Observe quest completion events for rewards
                launch {
                    questManager.questCompletionEvents.collect { event ->
                        handleQuestCompletion(event)
                    }
                }

                // Play menu music on startup
                audioManager.playMusic("menu")
            } catch (e: Exception) {
                Timber.e(e, "Initialization error")
                // Handle initialization errors, e.g., show an error state
            }
        }
    }

    /**
     * Creates a fresh save for a new game or debug session.
     */
    private fun createFreshProgress(
        playerId: String,
        inventory: List<String> = emptyList()
    ): PlayerProgress = PlayerProgress(
        playerId = playerId,
        currentScenarioId = STARTING_SCENARIO_ID,
        playerInventory = inventory,
        activeQuests = questManager.getStartingQuests(),
        completedQuests = emptyList(),
        visitedLocations = emptySet(),
        completedActivities = emptySet(),
        discoveredLocations = emptySet(),
        playerStats = StatsManager.DEFAULT_STATS,
        playerReputation = ReputationManager.DEFAULT_REPUTATION
    )

    /**
     * Publishes a progress snapshot to the UI and re-initializes every logic manager from it.
     */
    private fun applyProgressToManagers(progress: PlayerProgress) {
        _playerProgress.value = progress
        inventoryManager.initialize(progress.playerInventory)
        questManager.initialize(progress.activeQuests, progress.completedQuests)
        activityManager.initialize(progress.completedActivities, progress.discoveredLocations)
        statsManager.initialize(progress.playerStats)
        reputationManager.initialize(progress.playerReputation)
    }

    /**
     * Starts a new game by resetting progress and loading initial scenario
     */
    fun startNewGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.resetPlayerProgress()

                // Ensure scenarios are loaded after reset
                repository.loadScenariosFromJson()

                val freshProgress = createFreshProgress(DEFAULT_PLAYER_ID)
                repository.savePlayerProgress(freshProgress)

                // Load the initial scenario after saving progress
                val initialScenario = repository.getScenarioById(freshProgress.currentScenarioId)

                // Update StateFlows on the main thread
                withContext(Dispatchers.Main) {
                    applyProgressToManagers(freshProgress)
                    _currentScenario.value = initialScenario // Set initial scenario
                    _isOnTitleScreen.value = false
                    initialScenario?.let { audioManager.playMusicForLocation(it.location) }
                    updateInteractiveMapLocations() // Update interactive map locations
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting new game")
            }
        }
    }

    /**
     * Loads an existing game save
     */
    fun loadGame() {
        viewModelScope.launch {
            try {
                loadPlayerProgress(DEFAULT_PLAYER_ID)
                // State updates happen within loadPlayerProgress's withContext(Main)
                // We just need to ensure the title screen flag is set correctly after loading
                withContext(Dispatchers.Main) {
                    _isOnTitleScreen.value = false // Set on main thread after loading
                    _currentScenario.value?.let { audioManager.playMusicForLocation(it.location) }
                }
            } catch (e: Exception) {
                 Timber.e(e, "Error loading game")
            }
        }
    }

    /**
     * Returns to title screen
     */
    fun returnToTitle() {
        _isOnTitleScreen.value = true
        hideCharacterMenu()
        audioManager.playMusic("menu")
    }

    /**
     * Saves current game progress to database
     */
    private fun saveProgress() {
        val currentProgress = _playerProgress.value
        if (currentProgress != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Make sure to save the latest state including inventory
                    val (activeQ, completedQ) = questManager.getQuestState()
                    val progressToSave = currentProgress.copy(
                        playerInventory = inventoryManager.getInventoryList(),
                        activeQuests = activeQ,
                        completedQuests = completedQ,
                        completedActivities = activityManager.getCompletedActivitiesList(),
                        discoveredLocations = activityManager.getUnlockedLocationsList(),
                        playerStats = statsManager.getStatsMap(),
                        playerReputation = reputationManager.getReputationMap()
                    )
                    repository.savePlayerProgress(progressToSave)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save progress")
                }
            }
        } else {
             Timber.w("Attempted to save null player progress")
        }
    }

    /**
     * Loads player progress from database or creates new progress if none exists
     */
    private suspend fun loadPlayerProgress(playerId: String) {
        try {
            val progress = repository.getPlayerProgress(playerId) ?: createFreshProgress(playerId)

            // Load the corresponding scenario
            val scenario = repository.getScenarioById(progress.currentScenarioId)

            // Update StateFlows on the main thread
            withContext(Dispatchers.Main) {
                applyProgressToManagers(progress)
                _currentScenario.value = scenario // Set the loaded scenario
                updateInteractiveMapLocations() // Update interactive map locations
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading player progress")
             // Handle error loading progress, maybe set default state?
             withContext(Dispatchers.Main) {
                 // Optionally reset to a known safe state or show error
                 _playerProgress.value = null // Indicate loading failed
                 _currentScenario.value = null
                 inventoryManager.initialize(emptyList())
                 questManager.initialize(emptyList(), emptyList())
                 statsManager.initialize(emptyMap())
                 reputationManager.initialize(emptyMap())
             }
        }
    }

    /**
     * Updates progress for a specific quest objective and modifies state flows.
     * Called externally (e.g., from UI) or internally when needed.
     */
    fun updateQuestProgress(questId: String, objectiveId: String) {
        questManager.updateObjectiveStatus(questId, objectiveId)
        saveProgress()
    }

    // UI visibility control functions
    fun showMap() {
        _isMapVisible.value = true
    }

    fun hideMap() {
        _isMapVisible.value = false
    }

    fun showCharacterMenu() {
        _isCharacterMenuVisible.value = true
    }

    fun hideCharacterMenu() {
        _isCharacterMenuVisible.value = false
    }

    fun dismissQuestRewardNotification() {
        _questRewardNotification.value = null
    }

    private fun handleQuestCompletion(event: QuestCompletionEvent) {
        event.rewardItems.forEach { inventoryManager.addItem(it) }
        event.locationsUnlocked.forEach { activityManager.unlockLocation(it) }
        _questRewardNotification.value = event
        audioManager.playSfx("quest_completed")
        saveProgress()
    }

    fun onChoiceSelected(position: String) {
        viewModelScope.launch {
            val currentScenario = _currentScenario.value
            val currentProgress = _playerProgress.value

            if (currentScenario == null || currentProgress == null) {
                Timber.e("Scenario or Progress is null during choice selection")
                return@launch
            }

            val decision = currentScenario.decisions[position] ?: return@launch

            try {
                val conditionMet = decision.condition?.isMet(
                    inventoryManager.getInventorySet(),
                    statsManager.getStatsMap(),
                    reputationManager.getReputationMap()
                ) ?: true

                val targetScenarioId = when (val leadsTo = decision.leadsTo) {
                    is LeadsTo.Simple -> leadsTo.scenarioId
                    is LeadsTo.Conditional -> if (conditionMet) leadsTo.ifConditionMet else leadsTo.ifConditionNotMet
                }

                val nextScenario = repository.getScenarioById(targetScenarioId) ?: run {
                    Timber.e("Next scenario '%s' not found", targetScenarioId)
                    return@launch
                }

               if (conditionMet) {
                   currentScenario.itemGiven?.get(position)?.let { item ->
                       inventoryManager.addItem(item)
                       questManager.checkItemObjectives(item)
                       audioManager.playSfx("item_acquired")
                   }

                   // Grant stats for the chosen position
                   currentScenario.statsGranted?.get(position)?.forEach { (stat, amount) ->
                       statsManager.addStat(stat, amount)
                   }

                   // Apply reputation changes for the chosen position
                   currentScenario.reputationChanges?.get(position)?.forEach { (faction, amount) ->
                       reputationManager.adjustReputation(faction, amount)
                   }

                   // Remove item if the item should be removed on use
                   if (decision.condition?.removeOnUse == true) {
                       decision.condition.requiredItem?.let { inventoryManager.removeItem(it) }
                   }
               }

               questManager.checkAndCompleteObjectives(targetScenarioId)

               // Activate path quest when leaving the crossroads
               if (currentScenario.id == QuestManager.CROSSROADS_SCENARIO_ID) {
                   questManager.activatePathQuest(targetScenarioId)
               }

               // Activate side quests when reaching the town
               if (targetScenarioId == QuestManager.TOWN_SCENARIO_ID) {
                   questManager.activateSideQuests()
               }

               val visitedLocations = currentProgress.visitedLocations.toMutableSet().apply {
                    add(nextScenario.location)
                }.toSet()

               val (activeQ, completedQ) = questManager.getQuestState()
               _playerProgress.value = currentProgress.copy(
                    currentScenarioId = targetScenarioId,
                    playerInventory = inventoryManager.getInventoryList(),
                    activeQuests = activeQ,
                    completedQuests = completedQ,
                    visitedLocations = visitedLocations,
                    playerStats = statsManager.getStatsMap(),
                    playerReputation = reputationManager.getReputationMap()
                )

                _currentScenario.value = nextScenario
                audioManager.playMusicForLocation(nextScenario.location)
                saveProgress()
            } catch (e: Exception) {
                Timber.e(e, "Error in choice selection")
            }
        }
    }

    // --- Interactive Map Location Methods ---

    /**
     * Updates the interactive map locations based on current game state
     */
    private suspend fun updateInteractiveMapLocations() {
        try {
            val currentProgress = _playerProgress.value
            if (currentProgress != null) {
                val discoveredLocations = repository.getDiscoveredLocations(
                    playerInventory = inventoryManager.getInventorySet(),
                    visitedLocations = currentProgress.visitedLocations,
                    unlockedLocations = activityManager.getUnlockedLocationsList()
                )
                
                withContext(Dispatchers.Main) {
                    _interactiveMapLocations.value = discoveredLocations
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating interactive map locations")
        }
    }

    /**
     * Handles traveling to an interactive map location
     */
    fun travelToInteractiveLocation(location: InteractiveMapLocation) {
        viewModelScope.launch {
            try {
                // Mark location as visited
                repository.updateLocationVisitStatus(location.id, true)
                
                // Travel to the associated scenario
                val scenario = repository.getScenarioById(location.scenarioId)
                if (scenario != null) {
                    questManager.checkAndCompleteObjectives(scenario.id)
                    val (updatedActive, updatedCompleted) = questManager.getQuestState()

                    val currentProgress = _playerProgress.value
                    withContext(Dispatchers.Main) {
                        _currentScenario.value = scenario
                        _playerProgress.value = currentProgress?.copy(
                            currentScenarioId = location.scenarioId,
                            activeQuests = updatedActive,
                            completedQuests = updatedCompleted,
                            visitedLocations = currentProgress.visitedLocations + location.name
                        )
                        hideMap()
                        saveProgress() // Save after traveling
                        updateInteractiveMapLocations() // Refresh map state
                    }
                } else {
                    Timber.e("Scenario %s not found for interactive travel", location.scenarioId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error traveling to interactive location %s", location.name)
            }
        }
    }

    /**
     * Starts an activity at a specific location
     */
    fun startActivity(locationId: String, activityId: String) {
        viewModelScope.launch {
            try {
                val location = repository.getInteractiveMapLocationById(locationId)
                if (location != null) {
                    val activity = location.availableActivities.find { it.id == activityId }
                    if (activity != null) {
                        when (activity.type) {
                            ActivityType.MINIGAME -> {
                                // Launch mini-game based on activity requirements
                                val gameId = selectMiniGameForActivity(activity)
                                if (gameId != null) {
                                    miniGameManager.startMiniGame(gameId)
                                } else {
                                    // Fallback to direct completion
                                    completeActivityDirectly(activityId, locationId)
                                }
                            }
                            ActivityType.TRADING -> {
                                // Launch trading mini-game
                                miniGameManager.startMiniGame("trading_balanced")
                            }
                            else -> {
                                // For other activity types, complete directly for now
                                completeActivityDirectly(activityId, locationId)
                            }
                        }
                    } else {
                        Timber.w("Activity %s not found at location %s", activityId, locationId)
                    }
                } else {
                    Timber.w("Location %s not found", locationId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting activity")
            }
        }
    }

    /**
     * Gets available activities for a specific location
     */
    fun getAvailableActivitiesForLocation(locationId: String): List<LocationActivity> {
        val location = _interactiveMapLocations.value.find { it.id == locationId }
        return if (location != null) {
            activityManager.getAvailableActivities(location, inventoryManager.getInventorySet())
        } else {
            emptyList()
        }
    }

    /**
     * Checks if a location has any available activities
     */
    fun hasAvailableActivities(locationId: String): Boolean {
        val location = _interactiveMapLocations.value.find { it.id == locationId }
        return if (location != null) {
            activityManager.hasAvailableActivities(location, inventoryManager.getInventorySet())
        } else {
            false
        }
    }

    /**
     * Gets the completion rate for a specific location
     */
    fun getLocationCompletionRate(locationId: String): Float {
        val location = _interactiveMapLocations.value.find { it.id == locationId }
        return if (location != null) {
            activityManager.getLocationCompletionRate(location)
        } else {
            0f
        }
    }

    /**
     * Gets activities by type for a specific location
     */
    fun getActivitiesByType(locationId: String, activityType: ActivityType): List<LocationActivity> {
        val location = _interactiveMapLocations.value.find { it.id == locationId }
        return if (location != null) {
            activityManager.getActivitiesByType(location, activityType, inventoryManager.getInventorySet())
        } else {
            emptyList()
        }
    }

    /**
     * Looks up the ActivityType name for a given activity ID from loaded locations.
     */
    private fun findActivityType(activityId: String): String? {
        for (location in _interactiveMapLocations.value) {
            val activity = location.availableActivities.find { it.id == activityId }
            if (activity != null) return activity.type.name
        }
        return null
    }

    // --- Mini-Game Management Methods ---

    /**
     * Selects an appropriate mini-game based on activity details
     */
    private fun selectMiniGameForActivity(activity: LocationActivity): String? {
        return when {
            activity.name.contains("lock", ignoreCase = true) -> {
                when (activity.difficulty) {
                    1, 2 -> "lockpicking_1_locks"
                    3 -> "lockpicking_2_locks"
                    4, 5 -> "lockpicking_3_locks"
                    else -> "lockpicking_2_locks"
                }
            }
            activity.name.contains("trade", ignoreCase = true) -> "trading_balanced"
            activity.name.contains("negotiate", ignoreCase = true) -> "trading_friendly"
            else -> null
        }
    }

    /**
     * Applies a mini-game or direct-completion result to the game state:
     * inventory and quest effects, then persist and refresh the map.
     */
    private suspend fun applyActivityResult(activityId: String, result: MiniGameResult) {
        val activityResult = activityManager.processMiniGameResult(activityId, result)

        activityResult.itemsGained.forEach { item ->
            inventoryManager.addItem(item)
            questManager.checkItemObjectives(item)
        }
        activityResult.itemsLost.forEach { item ->
            inventoryManager.removeItem(item)
        }

        // Determine activity type from the location data
        val activityType = findActivityType(activityId)
        questManager.checkActivityObjectives(activityId, activityType)

        withContext(Dispatchers.Main) {
            saveProgress()
            updateInteractiveMapLocations()
        }
    }

    /**
     * Handles the result of a completed mini-game
     */
    private fun handleMiniGameResult(gameId: String, result: MiniGameResult) {
        viewModelScope.launch {
            try {
                applyActivityResult(gameId, result)
            } catch (e: Exception) {
                Timber.e(e, "Error handling mini-game result")
            }
        }
    }

    /**
     * Completes an activity directly without a mini-game
     */
    private fun completeActivityDirectly(activityId: String, locationId: String) {
        viewModelScope.launch {
            try {
                val result = miniGameManager.createDirectCompletionResult(activityId, success = true)
                applyActivityResult(activityId, result)
            } catch (e: Exception) {
                Timber.e(e, "Error completing activity directly")
            }
        }
    }

    /**
     * Starts a specific mini-game by ID
     */
    fun startMiniGame(gameId: String): Boolean {
        return miniGameManager.startMiniGame(gameId)
    }

    /**
     * Processes input for the current mini-game
     */
    fun processMiniGameInput(input: MiniGameInput): Boolean {
        return miniGameManager.processInput(input)
    }

    /**
     * Cancels the current mini-game
     */
    fun cancelCurrentMiniGame() {
        miniGameManager.cancelCurrentGame()
    }

    /**
     * Gets all available mini-games
     */
    fun getAvailableMiniGames(): List<MiniGame> {
        return miniGameManager.getAllMiniGames()
    }

    /**
     * Clears the last mini-game result (for UI state management)
     */
    fun clearLastMiniGameResult() {
        miniGameManager.clearLastResult()
    }

    /**
     * Gets the current progress of the active mini-game
     */
    fun getCurrentMiniGameProgress(): Float {
        return miniGameManager.getCurrentProgress()
    }

    /**
     * Updates animation state for the current mini-game (called per frame from UI)
     */
    fun updateMiniGameAnimation() {
        miniGameManager.updateAnimationTick()
    }

    // --- Audio Management Methods ---

    fun playSfx(name: String) {
        audioManager.playSfx(name)
    }

    fun setMusicVolume(volume: Float) {
        audioManager.setMusicVolume(volume)
    }

    fun setSfxVolume(volume: Float) {
        audioManager.setSfxVolume(volume)
    }

    fun toggleMute() {
        audioManager.toggleMute()
    }

    fun onLifecyclePause() {
        audioManager.onPause()
    }

    fun onLifecycleResume() {
        audioManager.onResume()
    }

    fun playMenuMusic() {
        audioManager.playMusic("menu")
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }

    // ========================
    // Debug Menu Methods
    // ========================

    private var _isDebugSession = false
    val isDebugSession: Boolean get() = _isDebugSession

    fun debugStartSession() {
        if (_isDebugSession) return
        _isDebugSession = true
        applyProgressToManagers(createFreshProgress(DEBUG_PLAYER_ID, inventory = listOf("torch")))
        _isOnTitleScreen.value = false
        viewModelScope.launch {
            _currentScenario.value = repository.getScenarioById(STARTING_SCENARIO_ID)
            updateInteractiveMapLocations()
        }
    }

    fun debugEndSession() {
        if (!_isDebugSession) return
        _isDebugSession = false
        _isOnTitleScreen.value = true
        hideMap()
        hideCharacterMenu()
        cancelCurrentMiniGame()
        viewModelScope.launch { loadPlayerProgress(DEFAULT_PLAYER_ID) }
        audioManager.playMusic("menu")
    }

    fun debugAddItem(item: String) { ensureDebugSession(); inventoryManager.addItem(item) }
    fun debugRemoveItem(item: String) { ensureDebugSession(); inventoryManager.removeItem(item) }
    fun debugClearInventory() { ensureDebugSession(); inventoryManager.initialize(emptyList()) }
    fun debugModifyStat(stat: String, delta: Int) { ensureDebugSession(); statsManager.addStat(stat, delta) }
    fun debugModifyReputation(faction: String, delta: Int) { ensureDebugSession(); reputationManager.adjustReputation(faction, delta) }

    fun debugActivateQuest(id: String) {
        ensureDebugSession()
        when (id) {
            "main" -> questManager.initialize(questManager.getStartingQuests(), emptyList())
            "side_all" -> questManager.activateSideQuests()
            "path_guard" -> questManager.activatePathQuest(QuestManager.GUARD_PATH_SCENARIO_ID)
            "path_merchant" -> questManager.activatePathQuest(QuestManager.MERCHANT_PATH_SCENARIO_ID)
            "path_adventurer" -> questManager.activatePathQuest(QuestManager.ADVENTURER_PATH_SCENARIO_ID)
            "path_outlaw" -> questManager.activatePathQuest(QuestManager.OUTLAW_PATH_SCENARIO_ID)
        }
    }

    fun debugCompleteNextObjective(questId: String) {
        ensureDebugSession()
        val quest = questManager.activeQuests.value.find { it.id == questId } ?: return
        val next = quest.objectives.firstOrNull { !it.isCompleted } ?: return
        questManager.updateObjectiveStatus(questId, next.id)
    }

    fun debugJumpToScenario(scenarioId: String) {
        ensureDebugSession()
        viewModelScope.launch {
            val scenario = repository.getScenarioById(scenarioId)
            if (scenario != null) {
                withContext(Dispatchers.Main) {
                    _currentScenario.value = scenario
                    _playerProgress.value = _playerProgress.value?.copy(currentScenarioId = scenarioId)
                }
            } else {
                Timber.w("Debug: scenario %s not found", scenarioId)
            }
        }
    }

    fun debugUnlockAllLocations() {
        ensureDebugSession()
        viewModelScope.launch {
            repository.getAllInteractiveMapLocations().forEach { activityManager.unlockLocation(it.id) }
            updateInteractiveMapLocations()
        }
    }

    fun debugGetAllScenarioIds(callback: (List<String>) -> Unit) {
        viewModelScope.launch {
            val ids = repository.getAllScenarioIds()
            withContext(Dispatchers.Main) { callback(ids) }
        }
    }

    fun debugResetProgress() {
        viewModelScope.launch {
            repository.resetPlayerProgress()
            debugStartSession()
        }
    }

    fun debugPlayMusic(track: String) { audioManager.playMusic(track) }

    private fun ensureDebugSession() { if (!_isDebugSession) debugStartSession() }
}