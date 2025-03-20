package com.spiritwisestudios.crossroadsoffate.repository

    import android.content.Context
    import com.google.gson.GsonBuilder
    import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
    import com.spiritwisestudios.crossroadsoffate.data.models.*
    import com.google.gson.reflect.TypeToken
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import java.io.InputStreamReader

    class GameRepository(private val db: GameDatabase, private val context: Context) {

        private val gson = GsonBuilder()
            .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
            .create()

        suspend fun loadScenariosFromJson() {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.assets.open("scenarios.json")
                    val reader = InputStreamReader(inputStream)
                    val jsonContent = reader.readText()
                    println("Raw JSON content: $jsonContent")

                    val type = object : TypeToken<Map<String, List<ScenarioEntity>>>() {}.type
                    val scenariosMap: Map<String, List<ScenarioEntity>> =
                        gson.fromJson(jsonContent, type)
                    val scenarios = scenariosMap["scenarios"] ?: emptyList()

                    println("Parsed scenarios count: ${scenarios.size}")
                    scenarios.forEach { println("Scenario: ${it.id} - ${it.location}") }

                    db.scenarioDao().insertScenarios(scenarios)
                    println("Scenarios inserted into database")

                    val count = db.scenarioDao().getScenarioCount()
                    println("Total scenarios in database: $count")
                } catch (e: Exception) {
                    println("Error loading scenarios: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        suspend fun getScenarioById(id: String): ScenarioEntity? {
            return db.scenarioDao().getScenarioById(id)
        }

        suspend fun getOrCreatePlayerProgress(playerId: String): PlayerProgress {
            var progress = db.playerProgressDao().getPlayerProgress(playerId)
            if (progress == null) {
                progress = PlayerProgress(
                    playerId = playerId,
                    currentScenarioId = "scenario1", // Fixed: Changed from "0001" to "scenario1"
                    playerInventory = emptyList()
                )
                db.playerProgressDao().insertPlayerProgress(progress)
            }
            return progress
        }

        suspend fun resetPlayerProgress() {
            withContext(Dispatchers.IO) {
                try {
                    // Clear existing progress
                    val defaultProgress = PlayerProgress(
                        playerId = "default_player",
                        currentScenarioId = "scenario1",
                        playerInventory = emptyList()
                    )
                    db.playerProgressDao().insertPlayerProgress(defaultProgress)
                    println("Reset player progress to scenario1")
                } catch (e: Exception) {
                    println("Error resetting player progress: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }