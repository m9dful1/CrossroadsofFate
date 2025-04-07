package com.spiritwisestudios.crossroadsoffate.data.models

    import androidx.room.*
    import com.google.gson.Gson
    import com.google.gson.reflect.TypeToken

    // Entity class representing player's progress in the game
    // Stores current scenario, inventory, quests and visited locations
    @Entity(tableName = "player_progress")
    data class PlayerProgress(
        // Unique identifier for the player
        @PrimaryKey val playerId: String,
        // ID of the current scenario the player is in
        @ColumnInfo(name = "currentScenarioId") val currentScenarioId: String,
        // List of items in player's inventory
        @TypeConverters(InventoryConverters::class)
        @ColumnInfo(name = "playerInventory") val playerInventory: List<String>,
        // List of active (ongoing) quests
        @TypeConverters(QuestConverters::class)
        @ColumnInfo(name = "activeQuests") val activeQuests: List<Quest> = emptyList(),
        // List of completed quests
        @TypeConverters(QuestConverters::class)
        @ColumnInfo(name = "completedQuests") val completedQuests: List<Quest> = emptyList(),
        // Set of locations the player has visited
        @TypeConverters(VisitedLocationsConverter::class)
        @ColumnInfo(name = "visitedLocations") val visitedLocations: Set<String> = emptySet()
    )

    // Converter class for handling Quest list serialization/deserialization
    @ProvidedTypeConverter
    class QuestConverters {
        // Convert List<Quest> to JSON string for storage
        @TypeConverter
        fun fromQuests(quests: List<Quest>): String = Gson().toJson(quests)

        // Convert stored JSON string back to List<Quest>
        @TypeConverter
        fun toQuests(value: String): List<Quest> {
            val type = object : TypeToken<List<Quest>>() {}.type
            return Gson().fromJson(value, type)
        }
    }

    // Converter class for handling inventory list serialization/deserialization
    @ProvidedTypeConverter
    class InventoryConverters {
        // Convert List<String> to JSON string for storage
        @TypeConverter
        fun fromInventory(value: List<String>): String = Gson().toJson(value)

        // Convert stored JSON string back to List<String>
        @TypeConverter
        fun toInventory(value: String): List<String> {
            val type = object : TypeToken<List<String>>() {}.type
            return Gson().fromJson(value, type)
        }
    }

    // Converter class for handling visited locations set serialization/deserialization
    @ProvidedTypeConverter
    class VisitedLocationsConverter {
        // Convert Set<String> to JSON string for storage
        @TypeConverter
        fun fromSet(locations: Set<String>): String = Gson().toJson(locations)

        // Convert stored JSON string back to Set<String>
        // Returns empty set if conversion fails
        @TypeConverter
        fun toSet(value: String): Set<String> = try {
            val type = object : TypeToken<Set<String>>() {}.type
            Gson().fromJson(value, type)
        } catch (e: Exception) {
            emptySet()
        }
    }