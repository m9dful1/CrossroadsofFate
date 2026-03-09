package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single scenario in the game, including its text, location,
 * decisions, and other associated data.
 */
@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "backgroundImage") val backgroundImage: String,
    @ColumnInfo(name = "isFixedBackground") val isFixedBackground: Boolean = false,
    @ColumnInfo(name = "decisions") val decisions: Map<String, Decision>,
    @ColumnInfo(name = "itemGiven") val itemGiven: Map<String, String>? = null,
    @ColumnInfo(name = "statsGranted") val statsGranted: Map<String, Map<String, Int>>? = null,
    @ColumnInfo(name = "reputationChanges") val reputationChanges: Map<String, Map<String, Int>>? = null
)