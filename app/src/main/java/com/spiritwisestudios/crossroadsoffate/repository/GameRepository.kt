package com.spiritwisestudios.crossroadsoffate.repository

    import android.content.Context
    import com.google.gson.GsonBuilder
    import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
    import com.spiritwisestudios.crossroadsoffate.data.models.*
    import com.google.gson.reflect.TypeToken
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import java.io.InputStreamReader

    /**
     * Repository class that handles all data operations between the database and the rest of the application
     * @param database The Room database instance
     * @param context Android application context
     */
    class GameRepository(private val database: GameDatabase, private val context: Context) {
        // Configure Gson with custom LeadsTo deserializer
        private val gson = GsonBuilder()
            .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
            .create()

        // Wrapper class for parsing JSON scenarios file
        data class ScenariosWrapper(
            val scenarios: List<ScenarioEntity>
        )

        /**
         * Retrieves player progress from the database
         * @param playerId The unique identifier of the player
         * @return PlayerProgress object or null if not found
         */
        suspend fun getPlayerProgress(playerId: String): PlayerProgress? {
            return withContext(Dispatchers.IO) {
                try {
                    database.playerProgressDao().getPlayerProgress(playerId)
                } catch (e: Exception) {
                    null
                }
            }
        }

        /**
         * Loads scenarios from JSON file in assets and stores them in the database
         * @throws Exception if JSON parsing or database operation fails
         */
        suspend fun loadScenariosFromJson() {
            withContext(Dispatchers.IO) {
                try {
                    context.assets.open("scenarios.json").use { inputStream ->
                        val reader = InputStreamReader(inputStream)
                        val type = object : TypeToken<ScenariosWrapper>() {}.type
                        val wrapper = gson.fromJson<ScenariosWrapper>(reader, type)
                        wrapper.scenarios.forEach { scenario ->
                            if (scenario.itemGiven != null) {
                                // Future implementation for item handling
                            }
                        }
                        database.scenarioDao().insertAll(wrapper.scenarios)
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        /**
         * Retrieves a specific scenario from the database
         * @param id The unique identifier of the scenario
         * @return ScenarioEntity object or null if not found
         */
        suspend fun getScenarioById(id: String): ScenarioEntity? {
            return withContext(Dispatchers.IO) {
                try {
                    val scenario = database.scenarioDao().getScenarioById(id)
                    scenario
                } catch (e: Exception) {
                    println("Error getting scenario $id: ${e.message}")
                    null
                }
            }
        }

        /**
         * Saves or updates player progress in the database
         * @param progress The PlayerProgress object to save
         * @throws Exception if database operation fails
         */
        suspend fun savePlayerProgress(progress: PlayerProgress) {
            withContext(Dispatchers.IO) {
                try {
                    database.playerProgressDao().insertOrUpdate(progress)
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        /**
         * Resets the game database and reloads initial scenarios
         * @throws Exception if database operation or JSON parsing fails
         */
        suspend fun resetPlayerProgress() {
            withContext(Dispatchers.IO) {
                try {
                    // Close and clear database
                    database.close()
                    GameDatabase.clearDatabase(context)

                    // Get fresh database instance
                    val newDb = GameDatabase.getDatabase(context)

                    // Load scenarios first
                    context.assets.open("scenarios.json").use { inputStream ->
                        val reader = InputStreamReader(inputStream)
                        val type = object : TypeToken<ScenariosWrapper>() {}.type
                        val wrapper = gson.fromJson<ScenariosWrapper>(reader, type)
                        newDb.scenarioDao().insertAll(wrapper.scenarios)
                    }

                } catch (e: Exception) {
                    throw e
                }
            }
        }

        /**
         * Retrieves the set of locations visited by the player
         * @param playerId The unique identifier of the player
         * @return Set of visited location names or empty set if none found
         */
        suspend fun getVisitedLocations(playerId: String): Set<String> {
            return withContext(Dispatchers.IO) {
                getPlayerProgress(playerId)?.visitedLocations ?: emptySet()
            }
        }
    }