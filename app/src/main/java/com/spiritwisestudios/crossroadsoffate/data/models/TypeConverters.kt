package com.spiritwisestudios.crossroadsoffate.data.models

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import timber.log.Timber

/**
 * Room type converters for complex data types used throughout the application.
 * This single class provides all necessary conversions to Room.
 *
 * Every deserializer follows the same policy: corrupt persisted JSON is logged
 * and replaced with a safe default rather than crashing the app.
 */
@ProvidedTypeConverter
class Converters {
    private val gson = GameJson.gson

    /**
     * Deserializes [json] to [T], logging and returning [default] on parse failure
     * so one corrupt column never crashes the app.
     */
    private inline fun <reified T> safeFromJson(json: String, default: T): T = try {
        gson.fromJson<T>(json, object : TypeToken<T>() {}.type) ?: default
    } catch (e: Exception) {
        Timber.e(e, "Corrupt persisted JSON for %s; using default", T::class.java.simpleName)
        default
    }

    // --- ScenarioEntity Converters ---

    @TypeConverter
    fun fromDecisions(value: Map<String, Decision>): String =
        gson.toJson(value)

    @TypeConverter
    fun toDecisions(value: String): Map<String, Decision> =
        safeFromJson(value, emptyMap())

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? =
        value?.let { safeFromJson(it, emptyMap()) }

    // --- PlayerProgress Converters ---

    @TypeConverter
    fun fromQuests(quests: List<Quest>): String = gson.toJson(quests)

    @TypeConverter
    fun toQuests(value: String): List<Quest> =
        safeFromJson(value, emptyList())

    @TypeConverter
    fun fromInventory(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toInventory(value: String): List<String> =
        safeFromJson(value, emptyList())

    @TypeConverter
    fun fromSet(locations: Set<String>): String = gson.toJson(locations)

    @TypeConverter
    fun toSet(value: String): Set<String> =
        safeFromJson(value, emptySet())

    // --- Interactive Map Location Converters ---

    @TypeConverter
    fun fromLocationActivities(value: List<LocationActivity>): String =
        gson.toJson(value)

    @TypeConverter
    fun toLocationActivities(value: String): List<LocationActivity> =
        safeFromJson(value, emptyList())

    // --- Stats Converters ---

    @TypeConverter
    fun fromIntMap(value: Map<String, Int>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toIntMap(value: String?): Map<String, Int>? =
        value?.let { safeFromJson(it, emptyMap()) }

    @TypeConverter
    fun fromNestedIntMap(value: Map<String, Map<String, Int>>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toNestedIntMap(value: String?): Map<String, Map<String, Int>>? =
        value?.let { safeFromJson(it, emptyMap()) }

    // Note: List<String> conversions already handled by fromInventory/toInventory methods
}