package com.spiritwisestudios.crossroadsoffate.repository

import android.content.Context
import com.google.gson.GsonBuilder
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class GameRepository(private val database: GameDatabase, private val context: Context) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
        .create()

    data class ScenariosWrapper(
        val scenarios: List<ScenarioEntity>
    )

    suspend fun getPlayerProgress(playerId: String): PlayerProgress? {
        return withContext(Dispatchers.IO) {
            try {
                database.playerProgressDao().getPlayerProgress(playerId)
            } catch (e: Exception) {
                println("Error getting player progress: ${e.message}")
                null
            }
        }
    }

    suspend fun loadScenariosFromJson() {
        withContext(Dispatchers.IO) {
            try {
                context.assets.open("scenarios.json").use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<ScenariosWrapper>() {}.type
                    val wrapper = gson.fromJson<ScenariosWrapper>(reader, type)
                    database.scenarioDao().insertAll(wrapper.scenarios)
                    println("Loaded ${wrapper.scenarios.size} scenarios into database")
                }
            } catch (e: Exception) {
                println("Error loading scenarios: ${e.message}")
                throw e
            }
        }
    }

    suspend fun getScenarioById(id: String): ScenarioEntity? {
        return withContext(Dispatchers.IO) {
            try {
                database.scenarioDao().getScenarioById(id)
            } catch (e: Exception) {
                println("Error getting scenario $id: ${e.message}")
                null
            }
        }
    }

    suspend fun getOrCreatePlayerProgress(playerId: String): PlayerProgress {
        return withContext(Dispatchers.IO) {
            try {
                val existingProgress = database.playerProgressDao().getPlayerProgress(playerId)
                if (existingProgress != null) {
                    return@withContext existingProgress
                }

                val mainQuest = Quest(
                    id = "main_quest",
                    title = "The Path of Fate",
                    description = "Your journey through the crossroads of fate begins.",
                    objectives = listOf(
                        QuestObjective(
                            id = "start",
                            description = "Begin your journey",
                            requiredScenarioId = "scenario1"
                        )
                    )
                )

                val newProgress = PlayerProgress(
                    playerId = playerId,
                    currentScenarioId = "scenario1",
                    playerInventory = emptyList(),
                    activeQuests = listOf(mainQuest),
                    completedQuests = emptyList()
                )
                database.playerProgressDao().insertOrUpdate(newProgress)
                newProgress
            } catch (e: Exception) {
                println("Error getting/creating player progress: ${e.message}")
                createDefaultProgress(playerId)
            }
        }
    }

    private fun createDefaultProgress(playerId: String): PlayerProgress {
        return PlayerProgress(
            playerId = playerId,
            currentScenarioId = "scenario1",
            playerInventory = emptyList(),
            activeQuests = emptyList(),
            completedQuests = emptyList()
        )
    }

    suspend fun savePlayerProgress(progress: PlayerProgress) {
        withContext(Dispatchers.IO) {
            try {
                database.playerProgressDao().insertOrUpdate(progress)
                println("Player progress saved successfully")
            } catch (e: Exception) {
                println("Error saving player progress: ${e.message}")
                throw e
            }
        }
    }

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
                    println("Loaded ${wrapper.scenarios.size} scenarios into database")
                }

                // Create default progress
                val mainQuest = Quest(
                    id = "main_quest",
                    title = "The Path of Fate",
                    description = "Your journey through the crossroads of fate begins.",
                    objectives = listOf(
                        QuestObjective(
                            id = "start",
                            description = "Begin your journey",
                            requiredScenarioId = "scenario1"
                        )
                    )
                )

                val defaultProgress = PlayerProgress(
                    playerId = "default_player",
                    currentScenarioId = "scenario1",
                    playerInventory = emptyList(),
                    activeQuests = listOf(mainQuest),
                    completedQuests = emptyList()
                )

                newDb.playerProgressDao().insertOrUpdate(defaultProgress)
                println("Player progress reset successfully")

            } catch (e: Exception) {
                println("Error resetting player progress: ${e.message}")
                throw e
            }
        }
    }
}