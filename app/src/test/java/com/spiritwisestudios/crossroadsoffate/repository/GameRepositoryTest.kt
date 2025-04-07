package com.spiritwisestudios.crossroadsoffate.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], manifest = Config.NONE)
class GameRepositoryTest {
    private lateinit var database: GameDatabase
    private lateinit var repository: GameRepository
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Use in-memory database for testing with all required type converters
        database = Room.inMemoryDatabaseBuilder(
            context, GameDatabase::class.java
        )
        .addTypeConverter(Converters())
        .addTypeConverter(InventoryConverters())
        .addTypeConverter(QuestConverters())
        .addTypeConverter(VisitedLocationsConverter())
        .fallbackToDestructiveMigration()
        .build()
        
        repository = GameRepository(database, context)
    }
    
    @After
    fun cleanup() {
        database.close()
    }
    
    @Test
    fun getPlayerProgress_returnsProgress_whenExists() = runBlocking {
        // Arrange
        val progress = PlayerProgress(
            playerId = "test_player",
            currentScenarioId = "scenario1",
            playerInventory = listOf("item1")
        )
        database.playerProgressDao().insertOrUpdate(progress)
        
        // Act
        val result = repository.getPlayerProgress("test_player")
        
        // Assert
        assertNotNull(result)
        assertEquals("test_player", result?.playerId)
        assertEquals("scenario1", result?.currentScenarioId)
        assertEquals(listOf("item1"), result?.playerInventory)
    }
    
    @Test
    fun getPlayerProgress_returnsNull_whenNoProgress() = runBlocking {
        // Act
        val result = repository.getPlayerProgress("nonexistent_player")
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun savePlayerProgress_insertsNewProgress_whenNotExists() = runBlocking {
        // Arrange
        val progress = PlayerProgress(
            playerId = "new_player",
            currentScenarioId = "scenario1",
            playerInventory = emptyList()
        )
        
        // Act
        repository.savePlayerProgress(progress)
        
        // Assert
        val saved = database.playerProgressDao().getPlayerProgress("new_player")
        assertNotNull(saved)
        assertEquals("scenario1", saved?.currentScenarioId)
    }
    
    @Test
    fun savePlayerProgress_updatesExistingProgress() = runBlocking {
        // Arrange
        val initial = PlayerProgress(
            playerId = "update_player",
            currentScenarioId = "scenario1",
            playerInventory = emptyList()
        )
        database.playerProgressDao().insertOrUpdate(initial)
        
        val updated = initial.copy(currentScenarioId = "scenario2")
        
        // Act
        repository.savePlayerProgress(updated)
        
        // Assert
        val result = database.playerProgressDao().getPlayerProgress("update_player")
        assertEquals("scenario2", result?.currentScenarioId)
    }
    
    @Test
    fun getScenarioById_returnsScenario_whenExists() = runBlocking {
        // Arrange
        val scenario = ScenarioEntity(
            id = "test_scenario",
            location = "Test Location",
            text = "Test scenario text",
            backgroundImage = "test_bg",
            isFixedBackground = false,
            decisions = emptyMap()
        )
        database.scenarioDao().insertAll(listOf(scenario))
        
        // Act
        val result = repository.getScenarioById("test_scenario")
        
        // Assert
        assertNotNull(result)
        assertEquals("Test Location", result?.location)
        assertEquals("Test scenario text", result?.text)
    }
    
    @Test
    fun getScenarioById_returnsNull_whenNotExists() = runBlocking {
        // Act
        val result = repository.getScenarioById("nonexistent_scenario")
        
        // Assert
        assertNull(result)
    }
} 