package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.data.models.Quest
import com.spiritwisestudios.crossroadsoffate.data.models.QuestCompletionEvent
import com.spiritwisestudios.crossroadsoffate.data.models.QuestObjective
import com.spiritwisestudios.crossroadsoffate.data.models.QuestType
import junit.framework.TestCase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class QuestManagerTest {

    private lateinit var questManager: QuestManager
    private val testQuest = Quest(
        id = "test_quest",
        title = "Test Quest",
        description = "A quest for testing.",
        objectives = listOf(
            QuestObjective(id = "obj1", description = "Objective 1", requiredScenarioId = "scene1"),
            QuestObjective(id = "obj2", description = "Objective 2", requiredScenarioId = "scene2")
        )
    )

    @Before
    fun setup() {
        questManager = QuestManager()
    }

    @Test
    fun initialize_withEmptyLists_startsMainQuest() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        val activeQuests = questManager.activeQuests.first()
        val completedQuests = questManager.completedQuests.first()

        assertEquals(1, activeQuests.size)
        assertEquals("quest_main_1", activeQuests.first().id)
        assertTrue(completedQuests.isEmpty())
    }

    @Test
    fun initialize_withExistingQuests_setsStateCorrectly() = runBlocking {
        val completedQuest = testQuest.copy(id = "completed_quest", isCompleted = true)
        questManager.initialize(listOf(testQuest), listOf(completedQuest))

        val activeQuests = questManager.activeQuests.first()
        val completedQuests = questManager.completedQuests.first()

        assertEquals(1, activeQuests.size)
        assertEquals("test_quest", activeQuests.first().id)
        assertEquals(1, completedQuests.size)
        assertEquals("completed_quest", completedQuests.first().id)
    }

    @Test
    fun updateObjectiveStatus_completesObjective() = runBlocking {
        questManager.initialize(listOf(testQuest), emptyList())
        questManager.updateObjectiveStatus("test_quest", "obj1")

        val activeQuests = questManager.activeQuests.first()
        val updatedQuest = activeQuests.find { it.id == "test_quest" }

        assertNotNull(updatedQuest)
        assertTrue(updatedQuest!!.objectives.find { it.id == "obj1" }!!.isCompleted)
        assertFalse(updatedQuest.isCompleted)
    }

    @Test
    fun updateObjectiveStatus_completesQuestWhenAllObjectivesDone() = runBlocking {
        questManager.initialize(listOf(testQuest), emptyList())
        questManager.updateObjectiveStatus("test_quest", "obj1")
        questManager.updateObjectiveStatus("test_quest", "obj2")

        val activeQuests = questManager.activeQuests.first()
        val completedQuests = questManager.completedQuests.first()

        assertTrue(activeQuests.none { it.id == "test_quest" })
        assertEquals(1, completedQuests.size)
        assertTrue(completedQuests.first().isCompleted)
    }

    @Test
    fun checkAndCompleteObjectives_updatesQuestBasedOnScenario() = runBlocking {
        questManager.initialize(listOf(testQuest), emptyList())
        questManager.checkAndCompleteObjectives("scene1")

        val activeQuests = questManager.activeQuests.first()
        val updatedQuest = activeQuests.find { it.id == "test_quest" }

        assertNotNull(updatedQuest)
        assertTrue(updatedQuest!!.objectives.find { it.id == "obj1" }!!.isCompleted)
    }

    @Test
    fun getQuestState_returnsCurrentQuestLists() = runBlocking {
        questManager.initialize(listOf(testQuest), emptyList())
        val (active, completed) = questManager.getQuestState()
        assertEquals(1, active.size)
        assertTrue(completed.isEmpty())
    }

    // --- New trigger method tests ---

    @Test
    fun checkActivityObjectives_completesActivityBasedObjective() = runBlocking {
        val quest = Quest(
            id = "activity_quest",
            title = "Activity Quest",
            description = "Test",
            objectives = listOf(
                QuestObjective(id = "act_obj", description = "Do activity", requiredActivityId = "combat_training")
            )
        )
        questManager.initialize(listOf(quest), emptyList())

        questManager.checkActivityObjectives("combat_training", "MINIGAME")

        val active = questManager.activeQuests.first()
        // Quest should be completed (moved out of active)
        assertTrue(active.none { it.id == "activity_quest" })
        val completed = questManager.completedQuests.first()
        assertEquals(1, completed.size)
        assertEquals("activity_quest", completed.first().id)
    }

    @Test
    fun checkItemObjectives_completesItemBasedObjective() = runBlocking {
        val quest = Quest(
            id = "item_quest",
            title = "Item Quest",
            description = "Test",
            objectives = listOf(
                QuestObjective(id = "item_obj", description = "Get item", requiredItemId = "guard_badge"),
                QuestObjective(id = "other_obj", description = "Do something", requiredScenarioId = "scene99")
            )
        )
        questManager.initialize(listOf(quest), emptyList())

        questManager.checkItemObjectives("guard_badge")

        val active = questManager.activeQuests.first()
        val updatedQuest = active.find { it.id == "item_quest" }
        assertNotNull(updatedQuest)
        assertTrue(updatedQuest!!.objectives.find { it.id == "item_obj" }!!.isCompleted)
        assertFalse(updatedQuest.objectives.find { it.id == "other_obj" }!!.isCompleted)
    }

    @Test
    fun countBasedObjective_incrementsAndCompletesAtThreshold() = runBlocking {
        val quest = Quest(
            id = "count_quest",
            title = "Count Quest",
            description = "Test",
            objectives = listOf(
                QuestObjective(
                    id = "count_obj",
                    description = "Trade 3 times",
                    requiredActivityCount = 3,
                    requiredActivityType = "TRADING"
                )
            )
        )
        questManager.initialize(listOf(quest), emptyList())

        // First trade
        questManager.checkActivityObjectives("some_trade_1", "TRADING")
        var active = questManager.activeQuests.first()
        var obj = active.find { it.id == "count_quest" }!!.objectives.first()
        assertEquals(1, obj.currentCount)
        assertFalse(obj.isCompleted)

        // Second trade
        questManager.checkActivityObjectives("some_trade_2", "TRADING")
        active = questManager.activeQuests.first()
        obj = active.find { it.id == "count_quest" }!!.objectives.first()
        assertEquals(2, obj.currentCount)
        assertFalse(obj.isCompleted)

        // Third trade — should complete
        questManager.checkActivityObjectives("some_trade_3", "TRADING")
        active = questManager.activeQuests.first()
        assertTrue(active.none { it.id == "count_quest" })
        val completed = questManager.completedQuests.first()
        assertTrue(completed.any { it.id == "count_quest" })
    }

    @Test
    fun activateQuest_isIdempotent() = runBlocking {
        val quest = Quest(
            id = "new_quest",
            title = "New Quest",
            description = "Test",
            objectives = emptyList()
        )
        questManager.initialize(emptyList(), emptyList())

        questManager.activateQuest(quest)
        questManager.activateQuest(quest) // duplicate call

        val active = questManager.activeQuests.first()
        assertEquals(2, active.size) // main quest + new_quest, no duplicates
        assertEquals(1, active.count { it.id == "new_quest" })
    }

    @Test
    fun activateQuest_doesNotActivateCompletedQuest() = runBlocking {
        val quest = Quest(
            id = "done_quest",
            title = "Done",
            description = "Test",
            objectives = emptyList(),
            isCompleted = true
        )
        questManager.initialize(emptyList(), listOf(quest))

        questManager.activateQuest(quest)

        val active = questManager.activeQuests.first()
        assertTrue(active.none { it.id == "done_quest" })
    }

    @Test
    fun activatePathQuest_guardPath() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        questManager.activatePathQuest("scenario9")

        val active = questManager.activeQuests.first()
        assertTrue(active.any { it.id == "quest_path_guard" })
    }

    @Test
    fun activatePathQuest_adventurerPath() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        questManager.activatePathQuest("scenario10")

        val active = questManager.activeQuests.first()
        assertTrue(active.any { it.id == "quest_path_adventurer" })
    }

    @Test
    fun activatePathQuest_merchantPath() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        questManager.activatePathQuest("scenario11")

        val active = questManager.activeQuests.first()
        assertTrue(active.any { it.id == "quest_path_merchant" })
    }

    @Test
    fun activatePathQuest_outlawPath() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        questManager.activatePathQuest("scenario12")

        val active = questManager.activeQuests.first()
        assertTrue(active.any { it.id == "quest_path_outlaw" })
    }

    @Test
    fun activateSideQuests_activatesAllThree() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        questManager.activateSideQuests()

        val active = questManager.activeQuests.first()
        assertTrue(active.any { it.id == "quest_side_torch" })
        assertTrue(active.any { it.id == "quest_side_merchant_favor" })
        assertTrue(active.any { it.id == "quest_side_rumors" })
    }

    @Test
    fun activateSideQuests_isIdempotent() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        questManager.activateSideQuests()
        questManager.activateSideQuests()

        val active = questManager.activeQuests.first()
        assertEquals(1, active.count { it.id == "quest_side_torch" })
        assertEquals(1, active.count { it.id == "quest_side_merchant_favor" })
        assertEquals(1, active.count { it.id == "quest_side_rumors" })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun questCompletion_emitsCompletionEvent() = runTest {
        val events = mutableListOf<QuestCompletionEvent>()
        val quest = Quest(
            id = "event_quest",
            title = "Event Quest",
            description = "Test",
            objectives = listOf(
                QuestObjective(id = "ev_obj", description = "Do it", requiredScenarioId = "scene_x")
            ),
            rewards = listOf("reward_item"),
            locationsUnlocked = listOf("secret_place")
        )
        questManager.initialize(listOf(quest), emptyList())

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            questManager.questCompletionEvents.collect { events.add(it) }
        }

        questManager.checkAndCompleteObjectives("scene_x")

        assertEquals(1, events.size)
        assertEquals("event_quest", events.first().quest.id)
        assertEquals(listOf("reward_item"), events.first().rewardItems)
        assertEquals(listOf("secret_place"), events.first().locationsUnlocked)

        job.cancel()
    }

    @Test
    fun mainQuest_hasCorrectQuestType() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        val mainQuest = questManager.activeQuests.first().find { it.id == "quest_main_1" }
        assertNotNull(mainQuest)
        assertEquals(QuestType.MAIN, mainQuest!!.questType)
    }

    @Test
    fun mainQuest_hasRewards() = runBlocking {
        questManager.initialize(emptyList(), emptyList())
        val mainQuest = questManager.activeQuests.first().find { it.id == "quest_main_1" }
        assertNotNull(mainQuest)
        assertTrue(mainQuest!!.rewards.isNotEmpty())
        assertTrue(mainQuest.rewards.contains("torch"))
    }
}
