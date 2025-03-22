package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "player_progress")
data class PlayerProgress(
    @PrimaryKey val playerId: String,
    @ColumnInfo(name = "currentScenarioId") val currentScenarioId: String,
    @TypeConverters(InventoryConverters::class)
    @ColumnInfo(name = "playerInventory") val playerInventory: List<String>,
    @TypeConverters(QuestConverters::class)
    @ColumnInfo(name = "activeQuests") val activeQuests: List<Quest> = emptyList(),
    @TypeConverters(QuestConverters::class)
    @ColumnInfo(name = "completedQuests") val completedQuests: List<Quest> = emptyList()
)

class QuestConverters {
    @TypeConverter
    fun fromQuests(quests: List<Quest>): String = Gson().toJson(quests)

    @TypeConverter
    fun toQuests(value: String): List<Quest> {
        val type = object : TypeToken<List<Quest>>() {}.type
        return Gson().fromJson(value, type)
    }
}

class InventoryConverters {
    @TypeConverter
    fun fromInventory(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun toInventory(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, type)
    }
}