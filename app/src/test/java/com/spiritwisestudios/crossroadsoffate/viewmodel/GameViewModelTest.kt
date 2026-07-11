package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.spiritwisestudios.crossroadsoffate.data.models.ActivityType
import com.spiritwisestudios.crossroadsoffate.data.models.Condition
import com.spiritwisestudios.crossroadsoffate.data.models.Decision
import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMap
import com.spiritwisestudios.crossroadsoffate.data.models.ExplorationMapSet
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.LeadsTo
import com.spiritwisestudios.crossroadsoffate.data.models.LocationActivity
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntity
import com.spiritwisestudios.crossroadsoffate.data.models.MapEntityType
import com.spiritwisestudios.crossroadsoffate.data.models.MapPoint
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import com.spiritwisestudios.crossroadsoffate.util.TestDataFactory.createTestDecision
import com.spiritwisestudios.crossroadsoffate.util.TestDataFactory.createTestPlayerProgress
import com.spiritwisestudios.crossroadsoffate.util.TestDataFactory.createTestScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import junit.framework.TestCase.*

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GameViewModelTest {

    @Mock
    private lateinit var repository: GameRepository

    private lateinit var gameViewModel: GameViewModel
    private lateinit var testDispatcher: TestDispatcher

    private val defaultProgress = createTestPlayerProgress()
    private val defaultScenario = createTestScenario(
        id = "scenario1",
        location = "Test Location",
        text = "Test scenario text"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Set up default repository behavior
        runTest {
            whenever(repository.loadScenariosFromJson()).thenReturn(Unit)
            whenever(repository.initializeInteractiveMapLocations()).thenReturn(Unit)
            whenever(repository.loadExplorationMaps()).thenReturn(ExplorationMapSet())
            whenever(repository.getPlayerProgress(any())).thenReturn(defaultProgress)
            whenever(repository.savePlayerProgress(any())).thenReturn(Unit)
            whenever(repository.getScenarioById("scenario1")).thenReturn(defaultScenario)
            whenever(repository.getDiscoveredLocations(any(), any(), any())).thenReturn(emptyList())
        }

        // Create viewModel with application context and mock repository,
        // then drain its init coroutine so state is seeded through the real path
        val application = ApplicationProvider.getApplicationContext<Application>()
        gameViewModel = GameViewModel(application, repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsProgressAndScenarioFromRepository() {
        assertEquals(defaultProgress, gameViewModel.playerProgress.value)
        assertEquals("scenario1", gameViewModel.currentScenario.value?.id)
        assertTrue("Should start on title screen", gameViewModel.isOnTitleScreen.value)
    }

    @Test
    fun startNewGame_persistsFreshProgress_andLeavesTitleScreen() = runTest {
        whenever(repository.resetPlayerProgress()).thenReturn(Unit)

        gameViewModel.startNewGame()

        // startNewGame runs on Dispatchers.IO — wait for the persisted write,
        // then flush its main-dispatcher state updates
        verify(repository, timeout(5000)).resetPlayerProgress()
        verify(repository, timeout(5000)).savePlayerProgress(argThat { currentScenarioId == "scenario1" })
        val deadline = System.currentTimeMillis() + 5000
        while (gameViewModel.isOnTitleScreen.value && System.currentTimeMillis() < deadline) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(20)
        }

        assertFalse("Should leave title screen after starting a new game", gameViewModel.isOnTitleScreen.value)
        assertEquals("scenario1", gameViewModel.currentScenario.value?.id)
        assertEquals(emptyList<String>(), gameViewModel.playerProgress.value?.playerInventory)
    }

    @Test
    fun loadGame_loadsProgressAndUpdatesScreenState() = runTest {
        val savedProgress = createTestPlayerProgress(
            playerId = "default_player",
            currentScenarioId = "scenario2",
            playerInventory = listOf("sword", "potion"),
            visitedLocations = setOf("Town", "Forest")
        )
        val savedScenario = createTestScenario(id = "scenario2", location = "Forest")

        whenever(repository.getPlayerProgress("default_player")).thenReturn(savedProgress)
        whenever(repository.getScenarioById("scenario2")).thenReturn(savedScenario)

        gameViewModel.loadGame()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(savedProgress, gameViewModel.playerProgress.value)
        assertEquals("scenario2", gameViewModel.currentScenario.value?.id)
        assertEquals(setOf("sword", "potion"), gameViewModel.playerInventory.value)
        assertFalse("Should leave title screen after loading", gameViewModel.isOnTitleScreen.value)
    }

    @Test
    fun scenarioDisplay_usesFallbackText_whenConditionNotMet() = runTest {
        val gatedScenario = createTestScenario(
            id = "scenario2",
            location = "Gate",
            text = "A locked door blocks the way.",
            decisions = mapOf(
                "bottomRight" to Decision(
                    text = "Unlock the door",
                    fallbackText = "The door is locked",
                    condition = Condition(requiredItem = "key"),
                    leadsTo = LeadsTo.Simple("scenario3")
                )
            )
        )
        whenever(repository.getPlayerProgress("default_player"))
            .thenReturn(createTestPlayerProgress(currentScenarioId = "scenario2"))
        whenever(repository.getScenarioById("scenario2")).thenReturn(gatedScenario)

        gameViewModel.loadGame()
        testDispatcher.scheduler.advanceUntilIdle()

        val display = gameViewModel.scenarioDisplay.filterNotNull().first()
        assertEquals("Gate", display.locationName)
        assertEquals("A locked door blocks the way.", display.resolvedText)
        // Player has no key, so the gated decision shows its fallback label
        assertEquals("The door is locked", display.decisions.single().text)
        assertEquals("bottomRight", display.decisions.single().position)
    }

    @Test
    fun returnToTitle_updatesScreenState() {
        // Act
        gameViewModel.returnToTitle()

        // Assert
        assertTrue("Should be on title screen", gameViewModel.isOnTitleScreen.value)
        assertFalse("Character menu should be hidden", gameViewModel.isCharacterMenuVisible.value)
    }

    @Test
    fun showHideMap_togglesMapVisibility() {
        // Initially map should be hidden
        assertFalse("Map should initially be hidden", gameViewModel.isMapVisible.value)

        // Act - show map
        gameViewModel.showMap()

        // Assert
        assertTrue("Map should be visible after showMap()", gameViewModel.isMapVisible.value)

        // Act - hide map
        gameViewModel.hideMap()

        // Assert
        assertFalse("Map should be hidden after hideMap()", gameViewModel.isMapVisible.value)
    }

    @Test
    fun showHideCharacterMenu_togglesCharacterMenuVisibility() {
        // Initially character menu should be hidden
        assertFalse("Character menu should initially be hidden", gameViewModel.isCharacterMenuVisible.value)

        // Act - show character menu
        gameViewModel.showCharacterMenu()

        // Assert
        assertTrue("Character menu should be visible after showCharacterMenu()", gameViewModel.isCharacterMenuVisible.value)

        // Act - hide character menu
        gameViewModel.hideCharacterMenu()

        // Assert
        assertFalse("Character menu should be hidden after hideCharacterMenu()", gameViewModel.isCharacterMenuVisible.value)
    }

    // --- Exploration flow ---

    /**
     * A one-map catalog covering "Town Square" with the story marker right next
     * to spawn, plus an optional ACTIVITY entity (also within interact range).
     */
    private fun explorationCatalog(activityId: String? = null) = ExplorationMapSet(
        listOf(
            ExplorationMap(
                id = "test_town",
                name = "Test Town",
                locationNames = listOf("Town Square"),
                width = 400f,
                height = 400f,
                spawn = MapPoint(200f, 200f),
                entities = listOfNotNull(
                    MapEntity(
                        id = "story", type = MapEntityType.STORY, icon = "❗",
                        label = "Story", x = 220f, y = 200f
                    ),
                    activityId?.let {
                        MapEntity(
                            id = "activity_$it", type = MapEntityType.ACTIVITY, icon = "🎲",
                            label = "Play", x = 200f, y = 160f, activityId = it
                        )
                    }
                )
            )
        )
    )

    /** Builds a ViewModel whose catalog covers scenario2's location "Town Square". */
    private fun viewModelWithExploration(activityId: String? = null): GameViewModel = runBlockingStubs {
        whenever(repository.loadExplorationMaps()).thenReturn(explorationCatalog(activityId))
        val choice = createTestDecision(targetScenarioId = "scenario2")
        val start = createTestScenario(id = "scenario1", decisions = mapOf("bottomLeft" to choice))
        val next = createTestScenario(id = "scenario2", location = "Town Square")
        whenever(repository.getScenarioById("scenario1")).thenReturn(start)
        whenever(repository.getScenarioById("scenario2")).thenReturn(next)
    }

    /** Enters exploration on the test map, ready to tap entities. */
    private fun GameViewModel.startExploring() {
        onChoiceSelected("bottomLeft")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Precondition: should be exploring", isExploring.value)
    }

    private fun stubLocation(vararg activities: LocationActivity) {
        kotlinx.coroutines.runBlocking {
            whenever(repository.getInteractiveMapLocationById("test_town")).thenReturn(
                InteractiveMapLocation(
                    id = "test_town",
                    name = "Test Town",
                    description = "A town for tests",
                    scenarioId = "scenario2",
                    availableActivities = activities.toList()
                )
            )
        }
    }

    private fun runBlockingStubs(stubs: suspend () -> Unit): GameViewModel {
        kotlinx.coroutines.runBlocking { stubs() }
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(application, repository)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun onChoiceSelected_entersExploration_whenMapCoversNextLocation() {
        val viewModel = viewModelWithExploration()

        viewModel.onChoiceSelected("bottomLeft")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Choice should hand off to exploration", viewModel.isExploring.value)
        assertEquals("test_town", viewModel.explorationMap.value?.id)
        assertTrue("Pending story beat should show its marker", viewModel.isStoryMarkerVisible.value)
        assertEquals("scenario2", viewModel.currentScenario.value?.id)
    }

    @Test
    fun onChoiceSelected_showsScenarioDirectly_whenExplorationDisabled() {
        val viewModel = viewModelWithExploration()
        viewModel.setExplorationEnabled(false)

        viewModel.onChoiceSelected("bottomLeft")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("Exploration disabled: scenario should show directly", viewModel.isExploring.value)
        assertEquals("scenario2", viewModel.currentScenario.value?.id)
    }

    @Test
    fun reachingStoryMarker_returnsToScenarioView() {
        val viewModel = viewModelWithExploration()
        viewModel.onChoiceSelected("bottomLeft")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isExploring.value)

        // The marker sits 20 units from spawn — within interact range, so a tap
        // on it interacts without needing to walk
        viewModel.onExplorationTap(220f, 200f)

        assertFalse("Reaching the marker should end exploration", viewModel.isExploring.value)
        assertFalse(viewModel.isStoryMarkerVisible.value)
    }

    @Test
    fun skipExploration_returnsToScenarioView() {
        val viewModel = viewModelWithExploration()
        viewModel.onChoiceSelected("bottomLeft")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isExploring.value)

        viewModel.skipExploration()

        assertFalse(viewModel.isExploring.value)
        assertFalse(viewModel.isStoryMarkerVisible.value)
    }

    // --- Exploration activities ---

    @Test
    fun explorationActivity_launchesMatchingMiniGame_whenAvailable() {
        stubLocation(
            LocationActivity(
                id = "practice_lock", type = ActivityType.MINIGAME,
                name = "Practice lock picking", description = "Pick a training lock",
                difficulty = 1, isRepeatable = true
            )
        )
        val viewModel = viewModelWithExploration(activityId = "practice_lock")
        viewModel.startExploring()

        viewModel.onExplorationTap(200f, 160f) // activity entity is within interact range
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Mini-game should start", viewModel.isMiniGameActive.value)
        assertEquals("lockpicking_1_locks", viewModel.currentMiniGame.value?.id)
    }

    @Test
    fun explorationActivity_showsMissingItems_insteadOfLaunching() {
        stubLocation(
            LocationActivity(
                id = "sealed_deal", type = ActivityType.MINIGAME,
                name = "High-stakes lock job", description = "Requires credentials",
                requiredItems = listOf("merchant_seal")
            )
        )
        val viewModel = viewModelWithExploration(activityId = "sealed_deal")
        viewModel.startExploring()

        viewModel.onExplorationTap(200f, 160f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("Game must not start without required items", viewModel.isMiniGameActive.value)
        assertEquals("You need: merchant_seal.", viewModel.explorationDialog.value?.line)
    }

    @Test
    fun explorationActivity_directCompletion_marksCompleted_andBlocksRepeat() {
        stubLocation(
            LocationActivity(
                id = "town_chat", type = ActivityType.NPC_INTERACTION,
                name = "Gather Gossip", description = "Listen for rumors",
                isRepeatable = false
            )
        )
        val viewModel = viewModelWithExploration(activityId = "town_chat")
        viewModel.startExploring()

        viewModel.onExplorationTap(200f, 160f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Activity should be marked completed",
            viewModel.completedActivities.value.contains("town_chat"))
        assertTrue("Should confirm completion in the dialog bubble",
            viewModel.explorationDialog.value?.line.orEmpty().startsWith("Done!"))

        // Second attempt: non-repeatable, so it should refuse politely
        viewModel.dismissExplorationDialog()
        viewModel.onExplorationTap(200f, 160f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Nothing more to do here.", viewModel.explorationDialog.value?.line)
        assertFalse(viewModel.isMiniGameActive.value)
    }
}
