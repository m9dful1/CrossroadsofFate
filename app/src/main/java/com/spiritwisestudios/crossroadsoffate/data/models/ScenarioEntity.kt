package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "backgroundImage") val backgroundImage: String,
    @ColumnInfo(name = "isFixedBackground") val isFixedBackground: Boolean,
    @ColumnInfo(name = "decisions") val decisions: Map<String, Decision>
)

data class Decision(
    val text: String,
    val fallbackText: String?,
    val condition: Condition?,
    val leadsTo: LeadsTo
)

data class Condition(
    val requiredItem: String,
    val removeOnUse: Boolean
)

sealed class LeadsTo {
    data class Simple(val scenarioId: String) : LeadsTo()
    data class Conditional(
        val ifConditionMet: String,
        val ifConditionNotMet: String
    ) : LeadsTo()
}

class Converters {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
        .create()

    @TypeConverter
    fun fromDecisions(value: Map<String, Decision>): String = gson.toJson(value)

    @TypeConverter
    fun toDecisions(value: String): Map<String, Decision> {
        val type = object : TypeToken<Map<String, Decision>>() {}.type
        return gson.fromJson(value, type)
    }
}