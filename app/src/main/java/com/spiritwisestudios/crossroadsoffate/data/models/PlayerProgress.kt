package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "player_progress")
data class PlayerProgress(
    @PrimaryKey val playerId: String,
    @ColumnInfo(name = "currentScenarioId") val currentScenarioId: String,
    @TypeConverters(InventoryConverters::class) // FIX: Ensure Room handles List<String>
    @ColumnInfo(name = "playerInventory") val playerInventory: List<String>
)

class InventoryConverters {
    @TypeConverter
    fun fromInventory(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun toInventory(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, type)
    }
}