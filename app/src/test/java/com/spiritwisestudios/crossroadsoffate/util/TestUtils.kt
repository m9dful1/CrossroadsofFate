package com.spiritwisestudios.crossroadsoffate.util

import android.content.Context
import androidx.room.Room
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.Decision
import com.spiritwisestudios.crossroadsoffate.data.models.LeadsTo
import com.spiritwisestudios.crossroadsoffate.data.models.PlayerProgress
import com.spiritwisestudios.crossroadsoffate.data.models.Quest
import com.spiritwisestudios.crossroadsoffate.data.models.QuestObjective
import com.spiritwisestudios.crossroadsoffate.data.models.QuestType
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.data.models.Condition
import com.spiritwisestudios.crossroadsoffate.data.models.Converters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper utilities for database testing
 */
object TestDatabaseUtil {
    /**
     * Creates an in-memory test database
     */
    fun createTestDatabase(context: Context): GameDatabase {
        return Room.inMemoryDatabaseBuilder(
            context, GameDatabase::class.java
        )
        .addTypeConverter(Converters())
        .allowMainThreadQueries()
        .build()
    }
    
    /**
     * Populates database with test scenarios
     */
    suspend fun populateTestScenarios(database: GameDatabase) {
        withContext(Dispatchers.IO) {
            val scenarios = listOf(
                TestDataFactory.createTestScenario(
                    id = "scenario1",
                    location = "Test Location 1",
                    text = "First test scenario"
                ),
                TestDataFactory.createTestScenario(
                    id = "scenario2", 
                    location = "Test Location 2",
                    text = "Second test scenario"
                ),
                TestDataFactory.createTestScenario(
                    id = "scenario3",
                    location = "Test Location 3",
                    text = "Third test scenario"
                )
            )
            
            database.scenarioDao().insertAll(scenarios)
        }
    }
}

/**
 * Factory for creating test data objects
 */
object TestDataFactory {
    /**
     * Creates a test scenario with optional parameters
     */
    fun createTestScenario(
        id: String = "test_scenario",
        location: String = "Test Location",
        text: String = "Test scenario text",
        decisions: Map<String, Decision> = emptyMap(),
        backgroundImage: String = "test_bg",
        isFixedBackground: Boolean = false,
        itemGiven: Map<String, String>? = null
    ): ScenarioEntity {
        return ScenarioEntity(
            id = id,
            location = location,
            text = text,
            backgroundImage = backgroundImage,
            isFixedBackground = isFixedBackground,
            decisions = decisions,
            itemGiven = itemGiven
        )
    }
    
    /**
     * Creates a test decision with optional parameters
     */
    fun createTestDecision(
        text: String = "Test decision",
        targetScenarioId: String = "next_scenario",
        fallbackText: String? = null,
        withCondition: Boolean = false,
        removeOnUse: Boolean = false,
        requiredItem: String = "test_item"
    ): Decision {
        val condition = if (withCondition) {
            Condition(requiredItem, removeOnUse)
        } else null
        
        return Decision(
            text = text,
            leadsTo = LeadsTo.Simple(targetScenarioId),
            fallbackText = fallbackText,
            condition = condition
        )
    }
    
    /**
     * Creates a test player progress snapshot with optional parameters
     */
    fun createTestPlayerProgress(
        playerId: String = "test_player",
        currentScenarioId: String = "scenario1",
        playerInventory: List<String> = emptyList(),
        activeQuests: List<Quest> = emptyList(),
        completedQuests: List<Quest> = emptyList(),
        visitedLocations: Set<String> = emptySet()
    ): PlayerProgress {
        return PlayerProgress(
            playerId = playerId,
            currentScenarioId = currentScenarioId,
            playerInventory = playerInventory,
            activeQuests = activeQuests,
            completedQuests = completedQuests,
            visitedLocations = visitedLocations
        )
    }

    /**
     * Creates a test quest with a single scenario-gated objective by default
     */
    fun createTestQuest(
        id: String = "test_quest",
        title: String = "Test Quest",
        objectives: List<QuestObjective> = listOf(
            QuestObjective(id = "obj1", description = "Test objective", requiredScenarioId = "scenario2")
        ),
        rewards: List<String> = emptyList(),
        questType: QuestType = QuestType.SIDE
    ): Quest {
        return Quest(
            id = id,
            title = title,
            description = "Quest for testing",
            objectives = objectives,
            rewards = rewards,
            questType = questType
        )
    }

    /**
     * Creates a test decision with conditional branching
     */
    fun createConditionalDecision(
        text: String = "Test conditional decision",
        fallbackText: String? = null,
        requiredItem: String = "test_item",
        removeOnUse: Boolean = false,
        ifConditionMet: String = "success_scenario",
        ifConditionNotMet: String = "failure_scenario"
    ): Decision {
        return Decision(
            text = text,
            leadsTo = LeadsTo.Conditional(ifConditionMet, ifConditionNotMet),
            fallbackText = fallbackText,
            condition = Condition(requiredItem, removeOnUse)
        )
    }
} 