package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import androidx.room.ProvidedTypeConverter

/**
 * Entity class representing a scenario in the game.
 * Each scenario represents a specific story point with location, text, and choices.
 */
@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,                    // Unique identifier for the scenario
    @ColumnInfo(name = "location") val location: String,  // Name of the location where scenario takes place
    @ColumnInfo(name = "text") val text: String,         // Main narrative text of the scenario
    @ColumnInfo(name = "backgroundImage") val backgroundImage: String,  // Background image resource identifier
    @ColumnInfo(name = "isFixedBackground") val isFixedBackground: Boolean,  // Whether background is static
    @ColumnInfo(name = "decisions") val decisions: Map<String, Decision>,    // Available choices/decisions
    @ColumnInfo(name = "itemGiven") val itemGiven: Map<String, String>? = null  // Items given based on decisions
)

/**
 * Represents a choice/decision that player can make in a scenario.
 * Each decision can have conditions and leads to another scenario.
 */
data class Decision(
    val text: String,           // Text displayed for this decision
    // Alternative text shown if condition not met
    val fallbackText: String? = null,  // Text shown if condition not met
    // Optional condition that must be met
    val condition: Condition? = null,  // Condition required for this decision
    val leadsTo: LeadsTo       // Where this decision leads to
)

/**
 * Represents a condition that must be met for a decision.
 * Usually involves having a specific item in inventory.
 */
data class Condition(
    val requiredItem: String,   // Item required to make this choice
    val removeOnUse: Boolean    // Whether item should be removed after use
)

/**
 * Sealed class representing different types of scenario transitions.
 * Can be either Simple (direct to next scenario) or Conditional (based on conditions).
 */
sealed class LeadsTo {
    data class Simple(val scenarioId: String) : LeadsTo()  // Direct transition to next scenario
    data class Conditional(
        val ifConditionMet: String,      // Scenario ID if condition is met
        val ifConditionNotMet: String    // Scenario ID if condition is not met
    ) : LeadsTo()
}

/**
 * Room type converters for complex data types.
 * Handles conversion between objects and strings for database storage.
 */
@ProvidedTypeConverter
class Converters {
    // Configure Gson with custom LeadsTo deserializer and null handling
    private val gson = GsonBuilder()
        .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
        .serializeNulls()
        .create()

    // Convert decisions map to JSON string
    @TypeConverter
    fun fromDecisions(value: Map<String, Decision>): String =
        gson.toJson(value)

    // Convert JSON string back to decisions map
    @TypeConverter
    fun toDecisions(value: String): Map<String, Decision> =
        gson.fromJson(value, object : TypeToken<Map<String, Decision>>() {}.type)

    // Convert string map to JSON string
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? =
        value?.let { gson.toJson(it) }

    // Convert JSON string back to string map
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? =
        value?.let {
            try {
                gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
            } catch (e: Exception) {
                emptyMap() // Return empty map on error
            }
        }
}