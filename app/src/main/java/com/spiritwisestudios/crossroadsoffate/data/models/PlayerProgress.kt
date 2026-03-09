package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the player's saved game state, including their current progress,
 * inventory, quests, visited locations, and completed activities.
 *
 * @property playerId Unique identifier for the player (e.g., "default_player")
 * @property currentScenarioId The ID of the scenario the player is currently in
 * @property playerInventory List of items in player's inventory
 * @property activeQuests List of active (ongoing) quests
 * @property completedQuests List of completed quests
 * @property visitedLocations Set of locations the player has visited
 * @property completedActivities Set of activity IDs that have been completed
 * @property discoveredLocations Set of location IDs that have been discovered/unlocked
 */
@Entity(tableName = "player_progress")
data class PlayerProgress(
    @PrimaryKey val playerId: String,
    @ColumnInfo(name = "currentScenarioId") val currentScenarioId: String,
    @ColumnInfo(name = "playerInventory") val playerInventory: List<String>,
    @ColumnInfo(name = "activeQuests") val activeQuests: List<Quest> = emptyList(),
    @ColumnInfo(name = "completedQuests") val completedQuests: List<Quest> = emptyList(),
    @ColumnInfo(name = "visitedLocations") val visitedLocations: Set<String> = emptySet(),
    @ColumnInfo(name = "completedActivities") val completedActivities: Set<String> = emptySet(),
    @ColumnInfo(name = "discoveredLocations") val discoveredLocations: Set<String> = emptySet(),
    @ColumnInfo(name = "playerStats") val playerStats: Map<String, Int> = emptyMap(),
    @ColumnInfo(name = "playerReputation") val playerReputation: Map<String, Int> = emptyMap()
)