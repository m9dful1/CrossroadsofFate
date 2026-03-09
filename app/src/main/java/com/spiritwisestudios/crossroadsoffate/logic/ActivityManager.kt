package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages location-based activities and their completion status.
 * Handles activity availability, completion tracking, and reward distribution.
 */
class ActivityManager {

    // Private mutable state for completed activities
    private val _completedActivities = MutableStateFlow<Set<String>>(emptySet())
    val completedActivities: StateFlow<Set<String>> = _completedActivities.asStateFlow()

    // Private mutable state for activity results
    private val _activityResults = MutableStateFlow<List<ActivityResult>>(emptyList())
    val activityResults: StateFlow<List<ActivityResult>> = _activityResults.asStateFlow()

    // Private mutable state for unlocked locations
    private val _unlockedLocations = MutableStateFlow<Set<String>>(emptySet())
    val unlockedLocations: StateFlow<Set<String>> = _unlockedLocations.asStateFlow()

    /**
     * Initializes the activity manager with existing progress data
     */
    fun initialize(
        completedActivities: Set<String>,
        unlockedLocations: Set<String> = emptySet()
    ) {
        _completedActivities.value = completedActivities
        _unlockedLocations.value = unlockedLocations
    }

    /**
     * Unlocks a location by adding it to the unlocked locations set.
     */
    fun unlockLocation(locationId: String) {
        _unlockedLocations.value = _unlockedLocations.value + locationId
    }

    /**
     * Checks if a specific activity is completed
     */
    fun isActivityCompleted(activityId: String): Boolean {
        return _completedActivities.value.contains(activityId)
    }

    /**
     * Gets all available activities for a location based on player inventory and requirements
     */
    fun getAvailableActivities(
        location: InteractiveMapLocation,
        playerInventory: Set<String>
    ): List<LocationActivity> {
        return location.availableActivities.filter { activity ->
            // Activity is available if:
            // 1. Not completed OR is repeatable
            // 2. Player has required items
            val isAvailable = (!isActivityCompleted(activity.id) || activity.isRepeatable)
            val hasRequiredItems = activity.canAccess(playerInventory)
            
            isAvailable && hasRequiredItems
        }
    }

    /**
     * Completes an activity and processes its results
     */
    fun completeActivity(
        activityId: String,
        success: Boolean,
        score: Int = 0,
        itemsGained: List<String> = emptyList(),
        itemsLost: List<String> = emptyList(),
        experienceGained: Int = 0,
        secretsRevealed: List<String> = emptyList(),
        newLocationsUnlocked: List<String> = emptyList()
    ): ActivityResult {
        // Create the activity result
        val result = ActivityResult(
            activityId = activityId,
            success = success,
            score = score,
            itemsGained = itemsGained,
            itemsLost = itemsLost,
            experienceGained = experienceGained,
            secretsRevealed = secretsRevealed,
            newLocationsUnlocked = newLocationsUnlocked
        )

        // Add to completed activities if successful
        if (success) {
            _completedActivities.value = _completedActivities.value + activityId
        }

        // Add to activity results history
        _activityResults.value = _activityResults.value + result

        // Unlock new locations
        if (newLocationsUnlocked.isNotEmpty()) {
            _unlockedLocations.value = _unlockedLocations.value + newLocationsUnlocked.toSet()
        }

        return result
    }

    /**
     * Processes a mini-game result and updates activity status
     */
    fun processMiniGameResult(
        activityId: String,
        miniGameResult: com.spiritwisestudios.crossroadsoffate.minigames.MiniGameResult
    ): ActivityResult {
        return completeActivity(
            activityId = activityId,
            success = miniGameResult.success,
            score = miniGameResult.score,
            itemsGained = miniGameResult.rewards,
            itemsLost = miniGameResult.consequences,
            experienceGained = if (miniGameResult.success) 10 else 5,
            secretsRevealed = emptyList(), // Mini-games don't directly reveal secrets
            newLocationsUnlocked = emptyList() // Mini-games don't directly unlock locations
        )
    }

    /**
     * Gets the completion rate for activities at a specific location
     */
    fun getLocationCompletionRate(location: InteractiveMapLocation): Float {
        if (location.availableActivities.isEmpty()) return 1.0f
        
        val completedCount = location.availableActivities.count { activity ->
            isActivityCompleted(activity.id)
        }
        
        return completedCount.toFloat() / location.availableActivities.size
    }

    /**
     * Gets all activities that have been completed
     */
    fun getCompletedActivitiesList(): Set<String> {
        return _completedActivities.value
    }

    /**
     * Gets all unlocked locations
     */
    fun getUnlockedLocationsList(): Set<String> {
        return _unlockedLocations.value
    }

    /**
     * Gets recent activity results (for UI display)
     */
    fun getRecentActivityResults(limit: Int = 10): List<ActivityResult> {
        return _activityResults.value.takeLast(limit)
    }

    /**
     * Checks if a location should be visible on the map based on discovery conditions
     */
    fun isLocationDiscovered(
        location: InteractiveMapLocation,
        playerInventory: Set<String>,
        visitedLocations: Set<String>
    ): Boolean {
        return location.canBeDiscovered(playerInventory, visitedLocations) ||
               _unlockedLocations.value.contains(location.id)
    }

    /**
     * Updates an activity's completion status (for loading saved games)
     */
    fun updateActivityCompletion(activityId: String, isCompleted: Boolean) {
        if (isCompleted) {
            _completedActivities.value = _completedActivities.value + activityId
        } else {
            _completedActivities.value = _completedActivities.value - activityId
        }
    }

    /**
     * Resets all activity progress (for new games)
     */
    fun reset() {
        _completedActivities.value = emptySet()
        _activityResults.value = emptyList()
        _unlockedLocations.value = emptySet()
    }

    /**
     * Gets activities filtered by type
     */
    fun getActivitiesByType(
        location: InteractiveMapLocation,
        activityType: ActivityType,
        playerInventory: Set<String>
    ): List<LocationActivity> {
        return getAvailableActivities(location, playerInventory)
            .filter { it.type == activityType }
    }

    /**
     * Checks if any activities are available at a location
     */
    fun hasAvailableActivities(
        location: InteractiveMapLocation,
        playerInventory: Set<String>
    ): Boolean {
        return getAvailableActivities(location, playerInventory).isNotEmpty()
    }
} 