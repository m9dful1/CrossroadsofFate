package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Room type converters for complex data types used throughout the application.
 * This single class provides all necessary conversions to Room.
 */
@ProvidedTypeConverter
class Converters {
    // Configure Gson with custom LeadsTo deserializer and null handling
    private val gson = GsonBuilder()
        .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
        .serializeNulls()
        .create()

    // --- ScenarioEntity Converters ---

    @TypeConverter
    fun fromDecisions(value: Map<String, Decision>): String =
        gson.toJson(value)

    @TypeConverter
    fun toDecisions(value: String): Map<String, Decision> =
        gson.fromJson(value, object : TypeToken<Map<String, Decision>>() {}.type)

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? =
        value?.let {
            try {
                gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
            } catch (e: Exception) {
                emptyMap() // Return empty map on error
            }
        }

    // --- PlayerProgress Converters ---

    @TypeConverter
    fun fromQuests(quests: List<Quest>): String = gson.toJson(quests)

    @TypeConverter
    fun toQuests(value: String): List<Quest> {
        val type = object : TypeToken<List<Quest>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromInventory(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toInventory(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromSet(locations: Set<String>): String = gson.toJson(locations)

    @TypeConverter
    fun toSet(value: String): Set<String> = try {
        val type = object : TypeToken<Set<String>>() {}.type
        gson.fromJson(value, type)
    } catch (e: Exception) {
        emptySet()
    }

    // --- Interactive Map Location Converters ---

    @TypeConverter
    fun fromLocationActivities(value: List<LocationActivity>): String = 
        gson.toJson(value)

    @TypeConverter
    fun toLocationActivities(value: String): List<LocationActivity> = try {
        val type = object : TypeToken<List<LocationActivity>>() {}.type
        gson.fromJson(value, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    // --- Stats Converters ---

    @TypeConverter
    fun fromIntMap(value: Map<String, Int>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toIntMap(value: String?): Map<String, Int>? = value?.let {
        try {
            gson.fromJson(it, object : TypeToken<Map<String, Int>>() {}.type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromNestedIntMap(value: Map<String, Map<String, Int>>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toNestedIntMap(value: String?): Map<String, Map<String, Int>>? = value?.let {
        try {
            gson.fromJson(it, object : TypeToken<Map<String, Map<String, Int>>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    // Note: List<String> conversions already handled by fromInventory/toInventory methods
} 