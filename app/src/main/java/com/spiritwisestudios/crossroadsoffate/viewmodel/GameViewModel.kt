package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import com.spiritwisestudios.crossroadsoffate.ui.MapLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadScenariosFromJson()
            loadPlayerProgress("default_player")
            updateAvailableLocations()
        }
    }

    /**
     * Starts a new game by resetting progress and loading initial scenario
     */
    fun startNewGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.resetPlayerProgress()
                delay(100) // Ensure database operations complete
                loadPlayerProgress("default_player")
                _isOnTitleScreen.value = false
            } catch (e: Exception) {
                println("Error starting new game: ${e.message}")
            }
        }
    }

    /**
     * Loads an existing game save
     */
    fun loadGame() {
        viewModelScope.launch(Dispatchers.IO) {
            loadPlayerProgress("default_player")
            _isOnTitleScreen.value = false
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
     * Saves current game progress to database
     */
    private fun saveProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            _playerProgress.value?.let { progress ->
                try {
                    withContext(Dispatchers.IO) {
                        repository.savePlayerProgress(progress)
                    }
                } catch (e: Exception) {
                    println("Failed to save progress: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Loads player progress from database or creates new progress if none exists
     */
    private fun loadPlayerProgress(playerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val progress = repository.getPlayerProgress(playerId) ?: PlayerProgress(
                playerId = playerId,
                currentScenarioId = "scenario1",
                playerInventory = emptyList(),
                activeQuests = listOf(mainQuest),
                completedQuests = emptyList()
            )

            _playerProgress.value = progress
            _playerInventory.value = progress.playerInventory.toSet()
            _activeQuests.value = progress.activeQuests
            _completedQuests.value = progress.completedQuests

            loadScenarioById(progress.currentScenarioId) { scenario ->
                _currentScenario.value = scenario
            }
        }
    }

    /**
     * Loads a specific scenario by ID
     */
    fun loadScenarioById(id: String, onScenarioLoaded: (ScenarioEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val scenario = repository.getScenarioById(id)
            onScenarioLoaded(scenario)
        }
    }

    /**
     * Updates player inventory based on scenario decisions
     */
    private fun updateInventory(
        position: String,
        currentScenario: ScenarioEntity,
        decision: Decision
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val updatedInventory = _playerInventory.value.toMutableSet()

                // Handle item removal
                decision.condition?.let { condition ->
                    if (condition.removeOnUse && condition.requiredItem in updatedInventory) {
                        updatedInventory.remove(condition.requiredItem)
                    }
                }

                // Handle item addition
                currentScenario.itemGiven?.get(position)?.let { item ->
                    if (item.isNotBlank()) {
                        updatedInventory.add(item)
                    }
                }

                _playerInventory.value = updatedInventory
                _playerProgress.value = _playerProgress.value?.copy(
                    playerInventory = _playerInventory.value.toList()
                )
                saveProgress()

            } catch (e: Exception) {
                println("ERROR: Failed to update inventory: ${e.message}")
            }
        }
    }

    /**
     * Updates game state after a player decision
     */
    private suspend fun updateGameState(
        currentScenario: ScenarioEntity,
        nextScenario: ScenarioEntity,
        position: String,
        decision: Decision,
        targetScenarioId: String
    ) {
        try {
            updateInventory(position, currentScenario, decision)
            val updatedVisitedLocations = (_playerProgress.value?.visitedLocations ?: emptySet())
                .toMutableSet()
                .apply {
                    add(nextScenario.location)
                }

            withContext(Dispatchers.Main) {
                _currentScenario.value = nextScenario
                _playerProgress.value = _playerProgress.value?.copy(
                    currentScenarioId = targetScenarioId,
                    playerInventory = _playerInventory.value.toList(),
                    visitedLocations = updatedVisitedLocations
                ) ?: throw IllegalStateException("Player progress is null")

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        saveProgress()
                    } catch (e: Exception) {
                        println("ERROR: Failed to save game state: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("ERROR: Failed to update game state: ${e.message}")
            throw e
        }
    }

    /**
     * Updates available locations for the map
     */
    private fun updateAvailableLocations() {
        viewModelScope.launch {
            val visitedLocations = repository.getVisitedLocations("default_player")
            val locations = mutableListOf<MapLocation>()

            locations.add(MapLocation(
                name = "Town Square",
                description = "The bustling heart of the town",
                scenarioId = "scenario6",
                isVisited = "town_square" in visitedLocations
            ))

            _availableLocations.value = locations
        }
    }

    /**
     * Handles travel to a specific location
     */
    fun travelToLocation(location: MapLocation) {
        viewModelScope.launch {
            _currentScenario.value = repository.getScenarioById(location.scenarioId)
            _playerProgress.value = _playerProgress.value?.copy(
                currentScenarioId = location.scenarioId
            )
            hideMap()
        }
    }

    /**
     * Handles player choice selection and updates game state accordingly
     */
    fun onChoiceSelected(position: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentScenario = _currentScenario.value ?: return@launch
                val decision = currentScenario.decisions[position] ?: return@launch

                val targetScenarioId = when (val leadsTo = decision.leadsTo) {
                    is LeadsTo.Simple -> leadsTo.scenarioId
                    is LeadsTo.Conditional -> {
                        if (decision.condition?.requiredItem in _playerInventory.value) {
                            leadsTo.ifConditionMet
                        } else {
                            leadsTo.ifConditionNotMet
                        }
                    }
                }

                val nextScenario = repository.getScenarioById(targetScenarioId) ?: return@launch

                withContext(Dispatchers.Main) {
                    // Update inventory and game state
                    currentScenario.itemGiven?.get(position)?.let { item ->
                        if (item.isNotBlank()) {
                            _playerInventory.value += item
                        }
                    }

                    decision.condition?.let { condition ->
                        if (condition.removeOnUse && condition.requiredItem in _playerInventory.value) {
                            _playerInventory.value -= condition.requiredItem
                        }
                    }

                    updateGameState(
                        currentScenario,
                        nextScenario,
                        position,
                        decision,
                        targetScenarioId
                    )

                    // Update quest progress
                    _activeQuests.value.forEach { quest ->
                        quest.objectives.forEach { objective ->
                            if (objective.requiredScenarioId == targetScenarioId && !objective.isCompleted) {
                                updateQuestProgress(quest.id, objective.id)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                println("Error in choice selection: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Updates progress for a specific quest objective
     */
    fun updateQuestProgress(questId: String, objectiveId: String) {
        val currentQuests = _activeQuests.value.toMutableList()
        val questIndex = currentQuests.indexOfFirst { it.id == questId }

        if (questIndex != -1) {
            val quest = currentQuests[questIndex]
            val updatedObjectives = quest.objectives.map { objective ->
                if (objective.id == objectiveId) objective.copy(isCompleted = true)
                else objective
            }

            val updatedQuest = quest.copy(
                objectives = updatedObjectives,
                isCompleted = updatedObjectives.all { it.isCompleted }
            )

            if (updatedQuest.isCompleted) {
                currentQuests.removeAt(questIndex)
                _completedQuests.value = _completedQuests.value + updatedQuest
            } else {
                currentQuests[questIndex] = updatedQuest
            }

            _activeQuests.value = currentQuests
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