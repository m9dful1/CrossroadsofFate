package com.spiritwisestudios.crossroadsoffate.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.PlayerProgressDao
import com.spiritwisestudios.crossroadsoffate.data.ScenarioDao
import com.spiritwisestudios.crossroadsoffate.data.models.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    private var database: GameDatabase? = null
    private var scenarioDao: ScenarioDao? = null
    private var playerProgressDao: PlayerProgressDao? = null
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, GameDatabase::class.java
        )
        .allowMainThreadQueries()
        .addTypeConverter(Converters())
        .addTypeConverter(InventoryConverters())
        .addTypeConverter(QuestConverters())
        .addTypeConverter(VisitedLocationsConverter())
        .build()
        
        scenarioDao = database?.scenarioDao()
        playerProgressDao = database?.playerProgressDao()
    }
    
    @After
    @Throws(IOException::class)
    fun cleanup() {
        database?.close()
        database = null
        scenarioDao = null
        playerProgressDao = null
    }
    
    @Test
    fun saveAndLoadScenario() = runBlocking {
        // Arrange
        val scenario = ScenarioEntity(
            id = "test_integration",
            location = "Integration Test",
            text = "Integration test scenario",
            backgroundImage = "test_bg",
            isFixedBackground = false,
            decisions = mapOf(
                "topLeft" to Decision(
                    text = "Test Decision",
                    leadsTo = LeadsTo.Simple("next")
                )
            )
        )
        
        // Act
        scenarioDao?.insertAll(listOf(scenario))
        val loaded = scenarioDao?.getScenarioById("test_integration")
        
        // Assert
        assertNotNull(loaded)
        assertEquals("Integration Test", loaded?.location)
        assertEquals(1, loaded?.decisions?.size)
        
        val decision = loaded?.decisions?.get("topLeft")
        assertEquals("Test Decision", decision?.text)
    }
    
    @Test
    fun saveAndUpdatePlayerProgress() = runBlocking {
        // Arrange
        val initialProgress = PlayerProgress(
            playerId = "test_player",
            currentScenarioId = "scenario1",
            playerInventory = listOf("item1"),
            activeQuests = listOf(
                Quest(
                    id = "quest1",
                    title = "Test Quest",
                    description = "Test quest description",
                    objectives = listOf(
                        QuestObjective(
                            id = "obj1",
                            description = "Test objective",
                            isCompleted = false
                        )
                    )
                )
            )
        )
        
        // Act - Save initial progress
        playerProgressDao?.insertOrUpdate(initialProgress)
        
        // Get and modify
        val loaded = playerProgressDao?.getPlayerProgress("test_player")
        assertNotNull(loaded)
        
        val updatedQuests = loaded!!.activeQuests.map { quest ->
            if (quest.id == "quest1") {
                quest.copy(
                    objectives = quest.objectives.map { obj ->
                        if (obj.id == "obj1") obj.copy(isCompleted = true)
                        else obj
                    }
                )
            } else quest
        }
        
        val updatedProgress = loaded.copy(
            currentScenarioId = "scenario2",
            playerInventory = loaded.playerInventory + "item2",
            activeQuests = updatedQuests
        )
        
        // Save updated progress
        playerProgressDao?.insertOrUpdate(updatedProgress)
        
        // Load again
        val finalProgress = playerProgressDao?.getPlayerProgress("test_player")
        
        // Assert
        assertNotNull(finalProgress)
        assertEquals("scenario2", finalProgress?.currentScenarioId)
        assertEquals(2, finalProgress?.playerInventory?.size)
        assertTrue("item2" in finalProgress?.playerInventory.orEmpty())
        
        val quest = finalProgress?.activeQuests?.find { it.id == "quest1" }
        assertNotNull(quest)
        val objective = quest?.objectives?.find { it.id == "obj1" }
        assertNotNull(objective)
        assertTrue(objective?.isCompleted == true)
    }
} 