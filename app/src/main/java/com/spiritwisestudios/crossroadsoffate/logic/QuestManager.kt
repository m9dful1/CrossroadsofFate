package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.data.models.Quest
import com.spiritwisestudios.crossroadsoffate.data.models.QuestCompletionEvent
import com.spiritwisestudios.crossroadsoffate.data.models.QuestObjective
import com.spiritwisestudios.crossroadsoffate.data.models.QuestType
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the player's quests, including initialization, updates, and completion logic.
 */
class QuestManager {
    private val _activeQuests = MutableStateFlow<List<Quest>>(emptyList())
    val activeQuests: StateFlow<List<Quest>> = _activeQuests.asStateFlow()

    private val _completedQuests = MutableStateFlow<List<Quest>>(emptyList())
    val completedQuests: StateFlow<List<Quest>> = _completedQuests.asStateFlow()

    private val _questCompletionEvents = MutableSharedFlow<QuestCompletionEvent>(extraBufferCapacity = 5)
    val questCompletionEvents: SharedFlow<QuestCompletionEvent> = _questCompletionEvents.asSharedFlow()

    // --- Quest Definitions ---

    private val mainQuest = Quest(
        id = "quest_main_1",
        title = "The Crossroads of Fate",
        description = "Begin your journey through the town and find your path",
        objectives = listOf(
            QuestObjective(id = "obj_1", description = "Leave your home", requiredScenarioId = "scenario4"),
            QuestObjective(id = "obj_2", description = "Enter the town", requiredScenarioId = "scenario5"),
            QuestObjective(id = "obj_3", description = "Make your choice at the crossroads", requiredScenarioId = "scenario8")
        ),
        rewards = listOf("torch"),
        questType = QuestType.MAIN
    )

    private val guardPathQuest = Quest(
        id = "quest_path_guard",
        title = "Sworn Protector",
        description = "Prove yourself worthy of the guard's trust",
        objectives = listOf(
            QuestObjective(id = "guard_obj_1", description = "Complete combat training", requiredActivityId = "combat_training"),
            QuestObjective(id = "guard_obj_2", description = "Investigate disturbances", requiredActivityId = "guard_investigation"),
            QuestObjective(id = "guard_obj_3", description = "Earn the guard badge", requiredItemId = "guard_badge"),
            QuestObjective(id = "guard_obj_4", description = "Report to the captain", requiredScenarioId = "scenario13")
        ),
        rewards = listOf("guard_sword"),
        questType = QuestType.PATH
    )

    private val merchantPathQuest = Quest(
        id = "quest_path_merchant",
        title = "Fortune Seeker",
        description = "Build your merchant empire through trade and negotiation",
        objectives = listOf(
            QuestObjective(id = "merchant_obj_1", description = "Complete a high-stakes negotiation", requiredActivityId = "merchant_negotiation"),
            QuestObjective(id = "merchant_obj_2", description = "Obtain the merchant seal", requiredItemId = "merchant_seal"),
            QuestObjective(id = "merchant_obj_3", description = "Establish your trade route", requiredScenarioId = "scenario15")
        ),
        rewards = listOf("gold_purse"),
        questType = QuestType.PATH
    )

    private val adventurerPathQuest = Quest(
        id = "quest_path_adventurer",
        title = "Into the Unknown",
        description = "Explore the ancient ruins and uncover their secrets",
        objectives = listOf(
            QuestObjective(id = "adventurer_obj_1", description = "Explore the ancient ruins", requiredActivityId = "ruins_exploration"),
            QuestObjective(id = "adventurer_obj_2", description = "Decipher the ancient runes", requiredActivityId = "rune_puzzle"),
            QuestObjective(id = "adventurer_obj_3", description = "Recover an ancient artifact", requiredItemId = "ancient_artifact")
        ),
        rewards = listOf("explorer_compass"),
        questType = QuestType.PATH
    )

    private val outlawPathQuest = Quest(
        id = "quest_path_outlaw",
        title = "Shadow's Edge",
        description = "Walk the path of shadows and carve your own destiny",
        objectives = listOf(
            QuestObjective(id = "outlaw_obj_1", description = "Enter the criminal underworld", requiredScenarioId = "scenario12"),
            QuestObjective(id = "outlaw_obj_2", description = "Prove your worth to the syndicate", requiredScenarioId = "scenario16"),
            QuestObjective(id = "outlaw_obj_3", description = "Obtain the infernal mark", requiredItemId = "infernal_mark")
        ),
        rewards = listOf("shadow_cloak"),
        questType = QuestType.PATH
    )

    private val sideQuestLostTorch = Quest(
        id = "quest_side_torch",
        title = "The Lost Torch",
        description = "Find a torch to light your way into the unknown",
        objectives = listOf(
            QuestObjective(id = "torch_obj_1", description = "Acquire a torch", requiredItemId = "torch")
        ),
        rewards = emptyList(),
        locationsUnlocked = listOf("ancient_ruins"),
        questType = QuestType.SIDE
    )

    private val sideQuestMerchantFavor = Quest(
        id = "quest_side_merchant_favor",
        title = "Merchant's Favor",
        description = "Earn the trust of local merchants through repeated trade",
        objectives = listOf(
            QuestObjective(
                id = "merchant_favor_obj_1",
                description = "Complete trading activities",
                requiredActivityCount = 3,
                requiredActivityType = "TRADING"
            )
        ),
        rewards = listOf("merchant_seal"),
        questType = QuestType.SIDE
    )

    private val sideQuestRumors = Quest(
        id = "quest_side_rumors",
        title = "Rumors of the Past",
        description = "Gather information from the townsfolk about ancient secrets",
        objectives = listOf(
            QuestObjective(
                id = "rumors_obj_1",
                description = "Talk to townsfolk",
                requiredActivityCount = 3,
                requiredActivityType = "NPC_INTERACTION"
            )
        ),
        rewards = listOf("scholar_recommendation"),
        questType = QuestType.SIDE
    )

    private val sideQuests = listOf(sideQuestLostTorch, sideQuestMerchantFavor, sideQuestRumors)

    /**
     * Initializes the quest states from saved progress.
     * If no active quests are provided, it defaults to starting the main quest.
     */
    fun initialize(initialActive: List<Quest>, initialCompleted: List<Quest>) {
        if (initialActive.isEmpty() && initialCompleted.isEmpty()) {
            Timber.d("No quest data found, starting with main quest")
            _activeQuests.value = listOf(mainQuest)
            _completedQuests.value = emptyList()
        } else {
            Timber.d("Loaded %d active and %d completed quests", initialActive.size, initialCompleted.size)
            _activeQuests.value = initialActive
            _completedQuests.value = initialCompleted
        }
    }

    /**
     * Checks all active quests to see if any of their objectives are completed by reaching
     * the target scenario.
     */
    fun checkAndCompleteObjectives(targetScenarioId: String) {
        val objectivesToComplete = _activeQuests.value.flatMap { quest ->
            quest.objectives
                .filter { it.requiredScenarioId == targetScenarioId && !it.isCompleted }
                .map { quest.id to it.id }
        }

        for ((questId, objectiveId) in objectivesToComplete) {
            updateObjectiveStatus(questId, objectiveId)
        }
    }

    /**
     * Checks active quests for objectives matching an activity completion.
     */
    fun checkActivityObjectives(activityId: String, activityType: String?) {
        val updates = mutableListOf<Triple<String, String, QuestObjective?>>()

        for (quest in _activeQuests.value) {
            for (objective in quest.objectives) {
                if (objective.isCompleted) continue

                // Direct activity ID match
                if (objective.requiredActivityId == activityId) {
                    updates.add(Triple(quest.id, objective.id, null))
                    continue
                }

                // Count-based activity type match
                if (activityType != null &&
                    objective.requiredActivityType == activityType &&
                    objective.requiredActivityCount != null
                ) {
                    val newCount = objective.currentCount + 1
                    if (newCount >= objective.requiredActivityCount) {
                        updates.add(Triple(quest.id, objective.id, null))
                    } else {
                        updates.add(Triple(quest.id, objective.id, objective.copy(currentCount = newCount)))
                    }
                }
            }
        }

        for ((questId, objectiveId, partialUpdate) in updates) {
            if (partialUpdate != null) {
                // Increment count without completing
                updateObjectiveCount(questId, objectiveId, partialUpdate.currentCount)
            } else {
                updateObjectiveStatus(questId, objectiveId)
            }
        }
    }

    /**
     * Checks active quests for objectives requiring a specific item.
     */
    fun checkItemObjectives(itemId: String) {
        val objectivesToComplete = _activeQuests.value.flatMap { quest ->
            quest.objectives
                .filter { it.requiredItemId == itemId && !it.isCompleted }
                .map { quest.id to it.id }
        }

        for ((questId, objectiveId) in objectivesToComplete) {
            updateObjectiveStatus(questId, objectiveId)
        }
    }

    /**
     * Activates a quest if it's not already active or completed.
     */
    fun activateQuest(quest: Quest) {
        val alreadyActive = _activeQuests.value.any { it.id == quest.id }
        val alreadyCompleted = _completedQuests.value.any { it.id == quest.id }
        if (!alreadyActive && !alreadyCompleted) {
            _activeQuests.value = _activeQuests.value + quest
        }
    }

    /**
     * Activates the appropriate path quest based on the scenario the player chose at the crossroads.
     */
    fun activatePathQuest(scenarioId: String) {
        val quest = when (scenarioId) {
            "scenario9" -> guardPathQuest
            "scenario10" -> adventurerPathQuest
            "scenario11" -> merchantPathQuest
            "scenario12" -> outlawPathQuest
            else -> null
        }
        quest?.let { activateQuest(it) }
    }

    /**
     * Activates all side quests (idempotent).
     */
    fun activateSideQuests() {
        sideQuests.forEach { activateQuest(it) }
    }

    /**
     * Updates the count on a count-based objective without completing it.
     */
    private fun updateObjectiveCount(questId: String, objectiveId: String, newCount: Int) {
        val activeQuests = _activeQuests.value.toMutableList()
        val questIndex = activeQuests.indexOfFirst { it.id == questId }
        if (questIndex != -1) {
            val quest = activeQuests[questIndex]
            val updatedObjectives = quest.objectives.map {
                if (it.id == objectiveId) it.copy(currentCount = newCount) else it
            }
            activeQuests[questIndex] = quest.copy(objectives = updatedObjectives)
            _activeQuests.value = activeQuests.toList()
        }
    }

    /**
     * Updates the status of a specific quest objective. If the objective is completed,
     * it checks if the entire quest is now complete.
     */
    fun updateObjectiveStatus(questId: String, objectiveId: String): Pair<List<Quest>, List<Quest>> {
        val activeQuests = _activeQuests.value.toMutableList()
        val completedQuests = _completedQuests.value.toMutableList()

        val questIndex = activeQuests.indexOfFirst { it.id == questId }
        if (questIndex != -1) {
            val quest = activeQuests[questIndex]
            val updatedObjectives = quest.objectives.map {
                if (it.id == objectiveId && !it.isCompleted) it.copy(isCompleted = true) else it
            }

            if (updatedObjectives != quest.objectives) {
                val updatedQuest = quest.copy(
                    objectives = updatedObjectives,
                    isCompleted = updatedObjectives.all { it.isCompleted }
                )

                if (updatedQuest.isCompleted) {
                    activeQuests.removeAt(questIndex)
                    if (completedQuests.none { it.id == updatedQuest.id }) {
                        completedQuests.add(updatedQuest)
                    }
                    _questCompletionEvents.tryEmit(
                        QuestCompletionEvent(
                            quest = updatedQuest,
                            rewardItems = updatedQuest.rewards,
                            locationsUnlocked = updatedQuest.locationsUnlocked
                        )
                    )
                } else {
                    activeQuests[questIndex] = updatedQuest
                }

                _activeQuests.value = activeQuests.toList()
                _completedQuests.value = completedQuests.toList()
            }
        }
        return Pair(_activeQuests.value, _completedQuests.value)
    }

    /**
     * Gets the current lists of active and completed quests.
     */
    fun getQuestState(): Pair<List<Quest>, List<Quest>> {
        return Pair(_activeQuests.value, _completedQuests.value)
    }

    /**
     * Gets the default list of starting quests.
     */
    fun getStartingQuests(): List<Quest> {
        return listOf(mainQuest)
    }
}
