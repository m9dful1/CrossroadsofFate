package com.spiritwisestudios.crossroadsoffate.data.models

data class QuestCompletionEvent(
    val quest: Quest,
    val rewardItems: List<String>,
    val locationsUnlocked: List<String>
)
