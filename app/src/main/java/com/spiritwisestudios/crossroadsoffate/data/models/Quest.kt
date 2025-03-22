package com.spiritwisestudios.crossroadsoffate.data.models

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val objectives: List<QuestObjective>,
    val isCompleted: Boolean = false,
    val rewards: List<String> = emptyList()
)

data class QuestObjective(
    val id: String,
    val description: String,
    val isCompleted: Boolean = false,
    val requiredScenarioId: String? = null
)