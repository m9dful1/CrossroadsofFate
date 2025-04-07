package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import com.spiritwisestudios.crossroadsoffate.ui.MapLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import org.robolectric.Shadows.shadowOf

/**
 * ViewModel responsible for managing game state and business logic.
 * Handles player progress, scenarios, inventory, quests, and UI state.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // Repository instance for data operations
    private val repository = GameRepository(GameDatabase.getDatabase(application), application)

    // State flows for observing game state changes
    private val _playerProgress = MutableStateFlow<PlayerProgress?>(null)
    val playerProgress: StateFlow<PlayerProgress?> = _playerProgress

    private val _currentScenario = MutableStateFlow<ScenarioEntity?>(null)
    val currentScenario: StateFlow<ScenarioEntity?> = _currentScenario

    private val _playerInventory = MutableStateFlow<Set<String>>(emptySet())
    val playerInventory: StateFlow<Set<String>> = _playerInventory

    // UI state flows
    private val _isMapVisible = MutableStateFlow(false)
    val isMapVisible: StateFlow<Boolean> = _isMapVisible

    private val _isCharacterMenuVisible = MutableStateFlow(false)
    val isCharacterMenuVisible: StateFlow<Boolean> = _isCharacterMenuVisible

    // Quest management state flows
    private val _activeQuests = MutableStateFlow<List<Quest>>(emptyList())
    val activeQuests: StateFlow<List<Quest>> = _activeQuests

    private val _completedQuests = MutableStateFlow<List<Quest>>(emptyList())
    val completedQuests: StateFlow<List<Quest>> = _completedQuests

    private val _availableLocations = MutableStateFlow<List<MapLocation>>(emptyList())
    val availableLocations: StateFlow<List<MapLocation>> = _availableLocations

    // Main quest definition
    private val mainQuest = Quest(
        id = "quest_main_1",
        title = "The Crossroads of Fate",
        description = "Begin your journey through the town and find your path",
        objectives = listOf(
            QuestObjective(
                id = "obj_1",
                description = "Leave your home",
                requiredScenarioId = "scenario4"
            ),
            QuestObjective(
                id = "obj_2",
                description = "Enter the town",
                requiredScenarioId = "scenario5"
            ),
            QuestObjective(
                id = "obj_3",
                description = "Make your choice at the crossroads",
                requiredScenarioId = "scenario8"
            )
        )
    )

    private val _isOnTitleScreen = MutableStateFlow(true)
    val isOnTitleScreen: StateFlow<Boolean> = _isOnTitleScreen

    // Initialize ViewModel
    init {
        viewModelScope.launch {
            try {
                repository.loadScenariosFromJson()
                loadPlayerProgress("default_player")
                updateAvailableLocations()
            } catch (e: Exception) {
                println("Initialization error: ${e.message}")
                // Handle initialization errors, e.g., show an error state
            }
        }
    }

    /**
     * Starts a new game by resetting progress and loading initial scenario
     */
    fun startNewGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.resetPlayerProgress()
                // Delay might not be reliable, ensure save completes before proceeding
                // await repository.resetPlayerProgress() // If resetPlayerProgress becomes suspend

                // Create a fresh PlayerProgress state directly
                val freshProgress = PlayerProgress(
                    playerId = "default_player",
                    currentScenarioId = "scenario1", // Start at scenario1
                    playerInventory = emptyList(),
                    activeQuests = listOf(mainQuest), // Start with the main quest
                    completedQuests = emptyList(),
                    visitedLocations = emptySet() // No locations visited initially
                )
                // Save the fresh state
                repository.savePlayerProgress(freshProgress)
                
                // Load the initial scenario after saving progress
                val initialScenario = repository.getScenarioById(freshProgress.currentScenarioId)

                // Update StateFlows on the main thread
                withContext(Dispatchers.Main) {
                    _playerProgress.value = freshProgress
                    _playerInventory.value = freshProgress.playerInventory.toSet()
                    _activeQuests.value = freshProgress.activeQuests
                    _completedQuests.value = freshProgress.completedQuests
                    _currentScenario.value = initialScenario // Set initial scenario
                    _isOnTitleScreen.value = false
                    updateAvailableLocations() // Update locations based on fresh state
                }
            } catch (e: Exception) {
                println("Error starting new game: ${e.message}")
            }
        }
    }

    /**
     * Loads an existing game save
     */
    fun loadGame() {
        viewModelScope.launch {
            try {
                loadPlayerProgress("default_player")
                // State updates happen within loadPlayerProgress's withContext(Main)
                // We just need to ensure the title screen flag is set correctly after loading
                withContext(Dispatchers.Main) {
                    _isOnTitleScreen.value = false // Set on main thread after loading
                }
            } catch (e: Exception) {
                 println("Error loading game: ${e.message}")
            }
        }
    }

    /**
     * Returns to title screen
     */
    fun returnToTitle() {
        _isOnTitleScreen.value = true
        hideCharacterMenu()
    }

    /**
     * Navigates to the title screen.
     * Convenience method with same functionality as returnToTitle.
     */
    fun navigateToTitleScreen() {
        returnToTitle()
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
                    val progressToSave = currentProgress.copy(
                        playerInventory = _playerInventory.value.toList(),
                        activeQuests = _activeQuests.value,
                        completedQuests = _completedQuests.value
                    )
                    repository.savePlayerProgress(progressToSave)
                } catch (e: Exception) {
                    println("Failed to save progress: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
             println("Warning: Attempted to save null player progress.")
        }
    }

    /**
     * Loads player progress from database or creates new progress if none exists
     */
    private suspend fun loadPlayerProgress(playerId: String) {
        try {
             val progress = repository.getPlayerProgress(playerId) ?: PlayerProgress(
                playerId = playerId,
                currentScenarioId = "scenario1",
                playerInventory = emptyList(),
                activeQuests = listOf(mainQuest),
                completedQuests = emptyList()
            )
            
            // Load the corresponding scenario
            val scenario = repository.getScenarioById(progress.currentScenarioId)

            // Update StateFlows on the main thread
            withContext(Dispatchers.Main) {
                _playerProgress.value = progress
                _playerInventory.value = progress.playerInventory.toSet()
                _activeQuests.value = progress.activeQuests
                _completedQuests.value = progress.completedQuests
                _currentScenario.value = scenario // Set the loaded scenario
                updateAvailableLocations() // Update locations after loading progress
            }
        } catch (e: Exception) {
            println("Error loading player progress: ${e.message}")
             // Handle error loading progress, maybe set default state?
             withContext(Dispatchers.Main) {
                 // Optionally reset to a known safe state or show error
                 _playerProgress.value = null // Indicate loading failed
                 _currentScenario.value = null
                 _playerInventory.value = emptySet()
                 _activeQuests.value = emptyList()
                 _completedQuests.value = emptyList()
             }
        }
    }

    /**
     * Loads a specific scenario by ID
     */
    fun loadScenarioById(id: String, onScenarioLoaded: (ScenarioEntity?) -> Unit) {
        viewModelScope.launch {
            var scenario: ScenarioEntity? = null
            try {
                scenario = repository.getScenarioById(id)
            } catch (e: Exception) {
                 println("Error loading scenario $id: ${e.message}")
            } finally {
                 // Switch to main thread to invoke callback safely
                 withContext(Dispatchers.Main) {
                      onScenarioLoaded(scenario)
                 }
            }
        }
    }

    /**
     * Updates game state after a player decision (Scenario and Visited Locations).
     */
    private fun updateScenarioState(nextScenario: ScenarioEntity, targetScenarioId: String) {
         val updatedVisitedLocations = (_playerProgress.value?.visitedLocations ?: emptySet())
            .toMutableSet()
            .apply {
                add(nextScenario.location)
            }

        _currentScenario.value = nextScenario
        _playerProgress.value = _playerProgress.value?.copy(
            currentScenarioId = targetScenarioId,
            visitedLocations = updatedVisitedLocations
            // Inventory and quests are handled separately
        ) ?: throw IllegalStateException("Player progress is null during scenario update")
        // saveProgress() will be called at the end of onChoiceSelected
    }

    /**
     * Updates available locations for the map
     */
     // Make suspend function to ensure repository call completes
    private suspend fun updateAvailableLocations() {
         try {
            val visitedLocations = repository.getVisitedLocations("default_player")
            val locations = mutableListOf<MapLocation>()

            // Example location - add more as needed
            locations.add(MapLocation(
                name = "Town Square",
                description = "The bustling heart of the town",
                scenarioId = "scenario6",
                isVisited = "Town Square" in visitedLocations
            ))
             // Add other potential locations based on game logic/discovery

            // Update StateFlow on main thread
            withContext(Dispatchers.Main) {
                _availableLocations.value = locations
            }
        } catch (e: Exception) {
             println("Error updating available locations: ${e.message}")
        }
    }

    /**
     * Handles travel to a specific location
     */
    fun travelToLocation(location: MapLocation) {
        viewModelScope.launch {
             try {
                val scenario = repository.getScenarioById(location.scenarioId)
                 if (scenario != null) {
                    withContext(Dispatchers.Main) {
                        _currentScenario.value = scenario
                        _playerProgress.value = _playerProgress.value?.copy(
                            currentScenarioId = location.scenarioId
                        )
                        hideMap()
                        saveProgress() // Save after traveling
                    }
                 } else {
                      println("Error: Scenario ${location.scenarioId} not found for travel.")
                 }
             } catch (e: Exception) {
                  println("Error traveling to ${location.name}: ${e.message}")
             }
        }
    }

    /**
     * Calculates the result of inventory modifications without updating state flows.
     * Returns the resulting inventory list.
     */
    private fun calculateInventoryChanges(currentInventory: List<String>, addItems: List<String> = emptyList(), removeItems: List<String> = emptyList()): List<String> {
        println("DEBUG: Calculating inventory changes from: $currentInventory")
        println("DEBUG: Requested items to add: $addItems")
        println("DEBUG: Requested items to remove: $removeItems")

        val updatedInventoryList = currentInventory.toMutableList()
        var changed = false

        // Process removals
        for (item in removeItems) {
            if (updatedInventoryList.remove(item)) {
                println("DEBUG: (Calc) Successfully removed item: $item")
                changed = true
            } else {
                println("DEBUG: (Calc) Failed to remove item: $item - not found")
            }
        }

        // Process additions
        for (item in addItems) {
            if (item.isNotBlank() && !updatedInventoryList.contains(item)) {
                updatedInventoryList.add(item)
                println("DEBUG: (Calc) Successfully added item: $item")
                changed = true
            }
        }

        val finalInventory = updatedInventoryList.toList()
        println("DEBUG: Calculated final inventory: $finalInventory")
        return finalInventory
    }

    /**
     * DEPRECATED: Directly modifies inventory and ensures all state flows are updated consistently.
     * Use calculateInventoryChanges and update state flows explicitly instead.
     */
    @Deprecated("Use calculateInventoryChanges and update state flows explicitly")
    private fun modifyInventory(addItems: List<String> = emptyList(), removeItems: List<String> = emptyList()) {
        val currentProgress = _playerProgress.value ?: return
        println("DEBUG: Before inventory modification - currentProgress inventory: ${currentProgress.playerInventory}")
        println("DEBUG: Before inventory modification - inventory flow: ${_playerInventory.value}")
        println("DEBUG: Requested items to add: $addItems")
        println("DEBUG: Requested items to remove: $removeItems")

        // Start with current inventory as lists for easier manipulation
        val updatedInventoryList = currentProgress.playerInventory.toMutableList()
        
        // Process removals
        var changed = false
        for (item in removeItems) {
            if (updatedInventoryList.remove(item)) {
                println("DEBUG: Successfully removed item: $item from inventory list")
                changed = true
            } else {
                println("DEBUG: Failed to remove item: $item - not found in list")
            }
        }
        
        // Process additions
        for (item in addItems) {
            if (item.isNotBlank() && !updatedInventoryList.contains(item)) {
                updatedInventoryList.add(item)
                println("DEBUG: Successfully added item: $item to inventory list")
                changed = true
            }
        }
        
        if (changed) {
            // Create final versions of the inventory
            val newInventoryList = updatedInventoryList.toList() // Immutable copy
            val newInventorySet = newInventoryList.toSet()
            
            println("DEBUG: Updated inventory list: $newInventoryList")
            println("DEBUG: Updated inventory set: $newInventorySet")
            
            // Update both the inventory and player progress
            _playerInventory.value = newInventorySet
            _playerProgress.value = currentProgress.copy(playerInventory = newInventoryList)
            
            println("DEBUG: After update - inventory flow: ${_playerInventory.value}")
            println("DEBUG: After update - player progress inventory: ${_playerProgress.value?.playerInventory}")
        } else {
            println("DEBUG: No inventory changes detected.")
        }
    }

    /**
     * Called when a player selects a choice. Handles all game state updates.
     */
    fun onChoiceSelected(position: String) {
        // Launch on the default viewModelScope dispatcher, which is controlled by runTest
        viewModelScope.launch {
            val currentScenario = _currentScenario.value
            val currentProgress = _playerProgress.value
            val currentInventory = _playerInventory.value
            val currentActiveQuests = _activeQuests.value
            val currentCompletedQuests = _completedQuests.value

            if (currentScenario == null || currentProgress == null) {
                println("Error: Scenario or Progress is null during choice selection")
                return@launch
            }

            val decision = currentScenario.decisions[position] ?: return@launch

            try {
                // 1. Evaluate Condition
                val conditionMet = decision.condition?.let {
                    it.requiredItem in currentInventory
                } ?: true // No condition means condition is met

                // 2. Determine Target Scenario ID
                val targetScenarioId = when (val leadsTo = decision.leadsTo) {
                    is LeadsTo.Simple -> leadsTo.scenarioId
                    is LeadsTo.Conditional -> if (conditionMet) leadsTo.ifConditionMet else leadsTo.ifConditionNotMet
                }

                // 3. Load the next scenario
                val nextScenario = repository.getScenarioById(targetScenarioId) ?: run {
                    println("Error: Next scenario '$targetScenarioId' not found.")
                    return@launch
                }

                // --- Calculate all state changes before applying --- 

                // 4. Calculate Inventory Changes
                val itemToAdd = currentScenario.itemGiven?.get(position)?.takeIf { it.isNotBlank() }
                val itemToRemove = if (conditionMet && decision.condition?.removeOnUse == true) {
                    decision.condition!!.requiredItem
                } else {
                    null
                }
                val finalInventoryList = calculateInventoryChanges(
                    currentInventory = currentProgress.playerInventory, // Use list from progress
                    addItems = listOfNotNull(itemToAdd),
                    removeItems = listOfNotNull(itemToRemove)
                )
                val finalInventorySet = finalInventoryList.toSet()

                // 5. Calculate Quest Updates
                var finalActiveQuests = currentActiveQuests
                var finalCompletedQuests = currentCompletedQuests
                currentActiveQuests.forEach { quest ->
                    quest.objectives.forEach { objective ->
                        if (objective.requiredScenarioId == targetScenarioId && !objective.isCompleted) {
                            val (updatedActive, updatedCompleted) = calculateQuestUpdates(
                                currentActiveQuests = finalActiveQuests, // Use potentially updated lists from previous iteration
                                currentCompletedQuests = finalCompletedQuests,
                                questId = quest.id,
                                objectiveId = objective.id
                            )
                            // Update lists for the next potential objective check in the same choice selection
                            finalActiveQuests = updatedActive
                            finalCompletedQuests = updatedCompleted
                        }
                    }
                }

                // 6. Calculate Visited Locations
                val finalVisitedLocations = currentProgress.visitedLocations.toMutableSet().apply {
                    add(nextScenario.location)
                }.toSet()

                // 7. Construct Final Player Progress
                val finalProgress = currentProgress.copy(
                    currentScenarioId = targetScenarioId,
                    playerInventory = finalInventoryList,
                    activeQuests = finalActiveQuests,
                    completedQuests = finalCompletedQuests,
                    visitedLocations = finalVisitedLocations
                )

                // --- Apply all state changes atomically ---
                println("DEBUG: Applying final state updates")
                _playerInventory.value = finalInventorySet
                _activeQuests.value = finalActiveQuests
                _completedQuests.value = finalCompletedQuests
                _currentScenario.value = nextScenario
                _playerProgress.value = finalProgress // The single final update
                println("DEBUG: Final _playerProgress.value set to: $finalProgress")

                // 8. Save final progress to repository
                saveProgress() // Reads the latest _playerProgress state
            } catch (e: Exception) {
                println("Error in choice selection: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Calculates the result of quest updates without modifying state flows.
     * Returns a Pair of updated active quests and completed quests lists.
     */
    private fun calculateQuestUpdates(
        currentActiveQuests: List<Quest>,
        currentCompletedQuests: List<Quest>,
        questId: String,
        objectiveId: String
    ): Pair<List<Quest>, List<Quest>> {
        println("DEBUG: Calculating quest updates for quest $questId, objective $objectiveId")
        val activeQuests = currentActiveQuests.toMutableList()
        val completedQuests = currentCompletedQuests.toMutableList()
        var questListChanged = false

        val questIndex = activeQuests.indexOfFirst { it.id == questId }
        if (questIndex != -1) {
            val quest = activeQuests[questIndex]
            var objectiveUpdated = false
            val updatedObjectives = quest.objectives.map {
                if (it.id == objectiveId && !it.isCompleted) {
                    objectiveUpdated = true
                    it.copy(isCompleted = true)
                } else {
                    it
                }
            }

            if (objectiveUpdated) {
                val updatedQuest = quest.copy(
                    objectives = updatedObjectives,
                    isCompleted = updatedObjectives.all { it.isCompleted }
                )

                if (updatedQuest.isCompleted) {
                    activeQuests.removeAt(questIndex)
                    if (completedQuests.none { it.id == updatedQuest.id }) {
                        completedQuests.add(updatedQuest)
                    }
                    println("DEBUG: (Calc) Quest ${updatedQuest.id} completed and moved.")
                } else {
                    activeQuests[questIndex] = updatedQuest
                    println("DEBUG: (Calc) Quest ${updatedQuest.id} objective $objectiveId completed.")
                }
                questListChanged = true
            }
        }

        val finalActive = activeQuests.toList()
        val finalCompleted = completedQuests.toList()
        println("DEBUG: Calculated final active quests: ${finalActive.map { it.id }}")
        println("DEBUG: Calculated final completed quests: ${finalCompleted.map { it.id }}")
        return Pair(finalActive, finalCompleted)
    }

    /**
     * Updates progress for a specific quest objective and modifies state flows.
     * Called externally (e.g., from UI) or internally when needed.
     */
    fun updateQuestProgress(questId: String, objectiveId: String) {
        // Use the calculation function internally
        val (updatedActive, updatedCompleted) = calculateQuestUpdates(
            currentActiveQuests = _activeQuests.value,
            currentCompletedQuests = _completedQuests.value,
            questId = questId,
            objectiveId = objectiveId
        )

        // Check if the lists actually changed before updating state flows
        if (updatedActive != _activeQuests.value || updatedCompleted != _completedQuests.value) {
            println("DEBUG: Applying quest updates from external call")
            _activeQuests.value = updatedActive
            _completedQuests.value = updatedCompleted

            // Update player progress with the new quest lists
            _playerProgress.value = _playerProgress.value?.copy(
                activeQuests = updatedActive,
                completedQuests = updatedCompleted
            )
            // Consider if saveProgress() should be called here too, depending on usage.
            // If called from UI interaction that expects persistence, maybe yes.
            // If called as part of a larger operation (like onChoiceSelected was), maybe no.
            // For now, let's assume separate calls might warrant a save.
            saveProgress()
        }
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
}