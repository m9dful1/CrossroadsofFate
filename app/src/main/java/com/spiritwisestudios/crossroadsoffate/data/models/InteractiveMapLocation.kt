package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Enhanced map location with interactive activities and connections to other locations.
 */
@Entity(tableName = "interactive_map_locations")
data class InteractiveMapLocation(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "scenarioId") val scenarioId: String,
    @ColumnInfo(name = "isVisited") val isVisited: Boolean = false,
    @ColumnInfo(name = "availableActivities") val availableActivities: List<LocationActivity> = emptyList(),
    @ColumnInfo(name = "requiredItems") val requiredItems: List<String> = emptyList(),
    @ColumnInfo(name = "timeToReach") val timeToReach: Int = 0,
    @ColumnInfo(name = "discoveryConditions") val discoveryConditions: List<String> = emptyList(),
    @ColumnInfo(name = "connections") val connections: List<String> = emptyList(),
    @ColumnInfo(name = "coordinateX") val coordinateX: Float = 0f,
    @ColumnInfo(name = "coordinateY") val coordinateY: Float = 0f
) {
    fun canBeDiscovered(playerInventory: Set<String>, visitedLocations: Set<String>): Boolean {
        return discoveryConditions.isEmpty() ||
               discoveryConditions.all { condition ->
                   playerInventory.contains(condition) || visitedLocations.contains(condition)
               }
    }
}