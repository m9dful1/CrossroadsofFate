package com.spiritwisestudios.crossroadsoffate.repository

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStreamReader

/**
 * Repository class that handles all data operations between the database and the rest of the application.
 */
class GameRepository(private val database: GameDatabase, private val context: Context) {

    private val gson = GameJson.gson

    data class ScenariosWrapper(
        val scenarios: List<ScenarioEntity>
    )

    data class LocationsWrapper(
        val locations: List<InteractiveMapLocation>
    )

    /**
     * Runs a read on the IO dispatcher; failures are logged and mapped to [default]
     * so callers always get a usable value.
     */
    private suspend fun <T> dbRead(operation: String, default: T, block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                Timber.e(e, "Error %s", operation)
                default
            }
        }

    /**
     * Runs a write on the IO dispatcher; failures are logged and rethrown so
     * callers can react to a failed persist.
     */
    private suspend fun <T> dbWrite(operation: String, block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                Timber.e(e, "Error %s", operation)
                throw e
            }
        }

    suspend fun getPlayerProgress(playerId: String): PlayerProgress? =
        dbRead("getting player progress $playerId", null) {
            database.playerProgressDao().getPlayerProgress(playerId)
        }

    suspend fun loadScenariosFromJson() {
        dbWrite("loading scenarios.json") {
            context.assets.open("scenarios.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<ScenariosWrapper>() {}.type
                val wrapper = gson.fromJson<ScenariosWrapper>(reader, type)
                database.scenarioDao().insertAll(wrapper.scenarios)
            }
        }
    }

    suspend fun getScenarioById(id: String): ScenarioEntity? =
        dbRead("getting scenario $id", null) {
            database.scenarioDao().getScenarioById(id)
        }

    suspend fun savePlayerProgress(progress: PlayerProgress) {
        dbWrite("saving player progress") {
            database.playerProgressDao().insertOrUpdate(progress)
        }
    }

    suspend fun resetPlayerProgress() {
        dbWrite("resetting player progress") {
            database.playerProgressDao().deleteAll()
        }
    }

    // --- Interactive Map Location Methods ---

    /**
     * Seeds the interactive map from assets/locations.json, mirroring how
     * scenarios load from scenarios.json. Runs on every app start: the table
     * holds static content only (visited state lives in PlayerProgress), so
     * re-seeding keeps installed games in sync with content updates.
     */
    suspend fun initializeInteractiveMapLocations() {
        dbWrite("initializing interactive map locations") {
            context.assets.open("locations.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<LocationsWrapper>() {}.type
                val wrapper = gson.fromJson<LocationsWrapper>(reader, type)
                database.interactiveMapLocationDao().insertLocations(wrapper.locations)
            }
        }
    }

    /**
     * Loads the exploration map catalog from assets/maps.json.
     * Returns an empty catalog on failure so exploration degrades gracefully
     * (the game falls back to showing scenarios directly).
     */
    suspend fun loadExplorationMaps(): ExplorationMapSet =
        dbRead("loading maps.json", ExplorationMapSet()) {
            context.assets.open("maps.json").use { inputStream ->
                gson.fromJson(InputStreamReader(inputStream), ExplorationMapSet::class.java)
            }
        }

    suspend fun getAllScenarioIds(): List<String> =
        dbRead("getting scenario IDs", emptyList()) {
            database.scenarioDao().getAllScenarioIds()
        }

    suspend fun getAllInteractiveMapLocations(): List<InteractiveMapLocation> =
        dbRead("getting interactive map locations", emptyList()) {
            database.interactiveMapLocationDao().getAllLocations()
        }

    suspend fun getInteractiveMapLocationById(locationId: String): InteractiveMapLocation? =
        dbRead("getting interactive map location $locationId", null) {
            database.interactiveMapLocationDao().getLocationById(locationId)
        }

    /**
     * Returns the locations visible on the interactive map for the current save.
     * Visited state is derived from [visitedLocations] (per-save, keyed by
     * location name) rather than the shared locations table, so one save's
     * discoveries never leak into another.
     */
    suspend fun getDiscoveredLocations(
        playerInventory: Set<String>,
        visitedLocations: Set<String>,
        unlockedLocations: Set<String>
    ): List<InteractiveMapLocation> =
        dbRead("getting discovered locations", emptyList()) {
            database.interactiveMapLocationDao().getAllLocations()
                .map { it.copy(isVisited = visitedLocations.contains(it.name)) }
                .filter { location ->
                    location.isVisited ||
                    location.canBeDiscovered(playerInventory, visitedLocations) ||
                    unlockedLocations.contains(location.id)
                }
        }
}
