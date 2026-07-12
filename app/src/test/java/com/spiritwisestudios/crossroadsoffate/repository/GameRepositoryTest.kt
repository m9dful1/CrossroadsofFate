package com.spiritwisestudios.crossroadsoffate.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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

    @Test
    fun loadExplorationMaps_readsCatalogFromAsset() = runBlocking {
        val catalog = repository.loadExplorationMaps()

        assertEquals(15, catalog.maps.size)
        assertNotNull(catalog.findMapById("town_square"))
        // Scenario location strings resolve to maps (the full coverage sweep lives
        // in ExplorationMapCatalogTest; this just proves the repository wiring)
        assertEquals("town_square", catalog.findMapForLocation("Town Square")?.id)
        assertEquals("home", catalog.findMapForLocation("Your Bedroom")?.id)
    }

    @Test
    fun initializeInteractiveMapLocations_seedsFromAsset() = runBlocking {
        repository.initializeInteractiveMapLocations()

        val locations = repository.getAllInteractiveMapLocations()
        assertEquals(12, locations.size)
        val ids = locations.map { it.id }.toSet()
        assertEquals(
            setOf(
                "town_square", "merchant_quarters", "council_chamber", "guard_training_grounds",
                "wilderness_trail", "mentor_cottage", "shadow_alley", "criminal_hideout",
                "ancient_ruins", "scholars_retreat", "sacred_temple", "cursed_ruins"
            ),
            ids
        )
        // Activities deserialize with their typed fields intact
        val townSquare = locations.first { it.id == "town_square" }
        assertEquals("scenario6", townSquare.scenarioId)
        assertNotNull(townSquare.availableActivities.firstOrNull { it.id == "town_square_trading" })
    }

    @Test
    fun initializeInteractiveMapLocations_reseedsWithoutDuplicating() = runBlocking {
        repository.initializeInteractiveMapLocations()
        repository.initializeInteractiveMapLocations()

        assertEquals(12, repository.getAllInteractiveMapLocations().size)
    }

    @Test
    fun initializeInteractiveMapLocations_refreshesStaleRows_onAppUpdate() = runBlocking {
        repository.initializeInteractiveMapLocations()
        // Simulate a row left behind by an older app version / previous playthrough
        val stale = repository.getInteractiveMapLocationById("town_square")!!
            .copy(description = "outdated description", isVisited = true)
        database.interactiveMapLocationDao().insertLocations(listOf(stale))

        repository.initializeInteractiveMapLocations()

        val refreshed = repository.getInteractiveMapLocationById("town_square")!!
        assertFalse("Re-seed must restore asset content", refreshed.description == "outdated description")
        assertFalse("Re-seed must clear stored visit flags", refreshed.isVisited)
    }

    @Test
    fun getDiscoveredLocations_derivesVisitedFromTheSave_notTheSharedTable() = runBlocking {
        repository.initializeInteractiveMapLocations()
        // A previous save marked the item-gated ruins visited in the shared table
        val leaked = repository.getInteractiveMapLocationById("ancient_ruins")!!
            .copy(isVisited = true)
        database.interactiveMapLocationDao().insertLocations(listOf(leaked))

        // A brand-new game (no items, nothing visited or unlocked) must not see it
        val freshGame = repository.getDiscoveredLocations(emptySet(), emptySet(), emptySet())
        assertFalse(
            "Fresh save must not inherit another save's discoveries",
            freshGame.any { it.id == "ancient_ruins" }
        )

        // The save that actually visited it (tracked by name in PlayerProgress) sees it
        val returningGame = repository.getDiscoveredLocations(emptySet(), setOf("Ancient Ruins"), emptySet())
        assertTrue(returningGame.first { it.id == "ancient_ruins" }.isVisited)
    }
}
