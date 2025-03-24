package com.spiritwisestudios.crossroadsoffate.data.models

/**
 * Represents a quest in the game with its properties and objectives.
 *
 * @property id Unique identifier for the quest
 * @property title Display name of the quest
 * @property description Detailed explanation of the quest's purpose
 * @property objectives List of tasks/objectives that need to be completed
 * @property isCompleted Indicates whether all objectives are completed
 * @property rewards List of items or benefits granted upon quest completion
 */
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val objectives: List<QuestObjective>,
    val isCompleted: Boolean = false,
    val rewards: List<String> = emptyList()
)

/**
 * Represents an individual objective within a quest.
 *
 * @property id Unique identifier for the objective
 * @property description Text describing what needs to be done
 * @property isCompleted Indicates whether this objective is completed
 * @property requiredScenarioId Optional ID of the scenario that completes this objective
 */
data class QuestObjective(
    val id: String,
    val description: String,
    val isCompleted: Boolean = false,
    val requiredScenarioId: String? = null
)