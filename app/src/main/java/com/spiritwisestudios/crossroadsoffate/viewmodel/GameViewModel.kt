package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(GameDatabase.getDatabase(application), application)

    private val _playerProgress = MutableStateFlow<PlayerProgress?>(null)
    val playerProgress: StateFlow<PlayerProgress?> = _playerProgress

    private val _currentScenario = MutableStateFlow<ScenarioEntity?>(null)
    val currentScenario: StateFlow<ScenarioEntity?> = _currentScenario

    private val _playerInventory = MutableStateFlow<Set<String>>(emptySet())
    val playerInventory: StateFlow<Set<String>> = _playerInventory

    private val _isMapVisible = MutableStateFlow(false)
    val isMapVisible: StateFlow<Boolean> = _isMapVisible

    private val _isCharacterMenuVisible = MutableStateFlow(false)
    val isCharacterMenuVisible: StateFlow<Boolean> = _isCharacterMenuVisible

    private val _activeQuests = MutableStateFlow<List<Quest>>(emptyList())
    val activeQuests: StateFlow<List<Quest>> = _activeQuests

    private val _completedQuests = MutableStateFlow<List<Quest>>(emptyList())
    val completedQuests: StateFlow<List<Quest>> = _completedQuests

    private val mainQuest = Quest(
        id = "main_quest",
        title = "The Path of Fate",
        description = "Your journey through the crossroads of fate begins.",
        objectives = listOf(
            QuestObjective(
                id = "start",
                description = "Begin your journey",
                requiredScenarioId = "scenario1"
            ),
            QuestObjective(
                id = "first_choice",
                description = "Make your first choice",
                requiredScenarioId = "scenario2"
            )
        )
    )

    // Add to your existing GameViewModel class
    private val _isOnTitleScreen = MutableStateFlow(true)
    val isOnTitleScreen: StateFlow<Boolean> = _isOnTitleScreen

    fun startNewGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.resetPlayerProgress()
                // Add delay to ensure database operations complete
                delay(100)
                loadPlayerProgress("default_player")
                _isOnTitleScreen.value = false
            } catch (e: Exception) {
                println("Error starting new game: ${e.message}")
            }
        }
    }

    fun loadGame() {
        viewModelScope.launch(Dispatchers.IO) {
            loadPlayerProgress("default_player")
            _isOnTitleScreen.value = false
        }
    }

    fun returnToTitle() {
        _isOnTitleScreen.value = true
        hideCharacterMenu()
    }
    init {
        viewModelScope.launch(Dispatchers.IO) {
            println("Initializing GameViewModel")
            repository.loadScenariosFromJson()
            println("Loading player progress for default_player")
            loadPlayerProgress("default_player")
        }
    }

    private fun saveProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            _playerProgress.value?.let { progress ->
                try {
                    repository.savePlayerProgress(progress)
                    println("Progress saved: currentScenario=${progress.currentScenarioId}, inventory=${progress.playerInventory}")
                } catch (e: Exception) {
                    println("Failed to save progress: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

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

            progress.currentScenarioId.let { scenarioId ->
                loadScenarioById(scenarioId) { scenario ->
                    _currentScenario.value = scenario
                }
            }
        }
    }

    fun loadScenarioById(id: String, onScenarioLoaded: (ScenarioEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val scenario = repository.getScenarioById(id)
            onScenarioLoaded(scenario)
        }
    }

    fun onChoiceSelected(position: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentScenarioValue = _currentScenario.value
            val decision = currentScenarioValue?.decisions?.get(position)

            if (decision != null) {
                val targetScenarioId = when (val leadsTo = decision.leadsTo) {
                    is LeadsTo.Simple -> leadsTo.scenarioId
                    is LeadsTo.Conditional -> {
                        if (_playerInventory.value.contains(decision.condition?.requiredItem)) {
                            leadsTo.ifConditionMet
                        } else {
                            leadsTo.ifConditionNotMet
                        }
                    }
                }

                val nextScenario = repository.getScenarioById(targetScenarioId)

                if (nextScenario != null) {
                    val updatedInventory = _playerInventory.value.toMutableSet()
                    decision.condition?.let { condition ->
                        if (condition.removeOnUse && updatedInventory.contains(condition.requiredItem)) {
                            updatedInventory.remove(condition.requiredItem)
                        }
                    }

                    _currentScenario.value = nextScenario
                    _playerInventory.value = updatedInventory
                    _playerProgress.value = _playerProgress.value?.copy(
                        currentScenarioId = targetScenarioId,
                        playerInventory = updatedInventory.toList()
                    )
                    saveProgress() // Ensure this is called after all state updates
                }
            }
        }
    }

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