package com.spiritwisestudios.crossroadsoffate.data.models

/**
 * Represents an individual activity available at a map location.
 * Activities can be mini-games, puzzles, NPC interactions, etc.
 */
data class LocationActivity(
    val id: String,
    val type: ActivityType,
    val name: String,
    val description: String,
    val rewards: List<String> = emptyList(),
    val requiredItems: List<String> = emptyList(),
    val difficulty: Int = 1,
    val estimatedDuration: Int = 5,
    val isRepeatable: Boolean = false
) {
    fun canAccess(playerInventory: Set<String>): Boolean {
        return requiredItems.isEmpty() ||
               requiredItems.all { requiredItem -> playerInventory.contains(requiredItem) }
    }
}

/**
 * Enumeration of different types of activities available at locations
 */
enum class ActivityType {
    MINIGAME,
    PUZZLE,
    NPC_INTERACTION,
    EXPLORATION,
    TRADING,
    INVESTIGATION,
    CRAFTING,
    COMBAT
}

/**
 * Represents the result of completing a location activity
 */
data class ActivityResult(
    val activityId: String,
    val success: Boolean,
    val score: Int = 0,
    val itemsGained: List<String> = emptyList(),
    val itemsLost: List<String> = emptyList(),
    val experienceGained: Int = 0,
    val secretsRevealed: List<String> = emptyList(),
    val newLocationsUnlocked: List<String> = emptyList()
)
