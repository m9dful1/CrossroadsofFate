package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.Decision
import com.spiritwisestudios.crossroadsoffate.data.models.PlayerProgress
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.data.models.LeadsTo
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import kotlinx.coroutines.Dispatchers
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

init {
    viewModelScope.launch(Dispatchers.IO) {
        println("Initializing GameViewModel")
        repository.loadScenariosFromJson()
        repository.resetPlayerProgress() // Add this line
        println("Loading player progress for default_player")
        loadPlayerProgress("default_player")
    }
}

private fun loadPlayerProgress(playerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val progress = repository.getOrCreatePlayerProgress(playerId)
            println("Loaded player progress: $progress")
            _playerProgress.value = progress
            _playerInventory.value = progress.playerInventory.toSet()

            val scenario = repository.getScenarioById(progress.currentScenarioId)
            println("Loaded scenario: $scenario")
            scenario?.let {
                println("Setting current scenario: ${it.location}")
                _currentScenario.value = it
            } ?: println("No scenario found with ID: ${progress.currentScenarioId}")
        } catch (e: Exception) {
            println("Error loading player progress: ${e.message}")
            e.printStackTrace()
        }
    }
}

    fun loadScenarioById(scenarioId: String, onScenarioLoaded: (ScenarioEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val scenario = repository.getScenarioById(scenarioId)
            onScenarioLoaded(scenario)
        }
    }

    fun onChoiceSelected(position: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val progress = _playerProgress.value ?: return@launch
            val scenario = repository.getScenarioById(progress.currentScenarioId) ?: return@launch
            val decision = scenario.decisions[position] ?: return@launch

            val conditionMet = hasConditionMet(decision)
            val nextScenarioId = when (val leadsTo = decision.leadsTo) {
                is LeadsTo.Simple -> leadsTo.scenarioId
                is LeadsTo.Conditional -> if (conditionMet) leadsTo.ifConditionMet else leadsTo.ifConditionNotMet
                else -> null
            } ?: return@launch

            val updatedInventory = progress.playerInventory.toMutableSet()
            if (decision.condition?.removeOnUse == true && decision.condition.requiredItem in updatedInventory) {
                updatedInventory.remove(decision.condition.requiredItem)
            }

            val nextScenario = repository.getScenarioById(nextScenarioId)
            if (nextScenario != null) {
                val updatedProgress = progress.copy(
                    currentScenarioId = nextScenarioId,
                    playerInventory = updatedInventory.toList()
                )
                _playerProgress.value = updatedProgress
                _playerInventory.value = updatedInventory
                _currentScenario.value = nextScenario

                println("Moved to scenario: ${nextScenario.location}")
            }
        }
    }

    private fun hasConditionMet(decision: Decision): Boolean {
        return decision.condition?.requiredItem in _playerInventory.value
    }
}