package com.spiritwisestudios.crossroadsoffate.repository

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStreamReader

/**
 * Repository class that handles all data operations between the database and the rest of the application.
 */
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
                null
            }
        }
    }

    suspend fun loadScenariosFromJson() {
        withContext(Dispatchers.IO) {
            context.assets.open("scenarios.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<ScenariosWrapper>() {}.type
                val wrapper = gson.fromJson<ScenariosWrapper>(reader, type)
                database.scenarioDao().insertAll(wrapper.scenarios)
            }
        }
    }

    suspend fun getScenarioById(id: String): ScenarioEntity? {
        return withContext(Dispatchers.IO) {
            try {
                database.scenarioDao().getScenarioById(id)
            } catch (e: Exception) {
                Timber.e(e, "Error getting scenario %s", id)
                null
            }
        }
    }

    suspend fun savePlayerProgress(progress: PlayerProgress) {
        withContext(Dispatchers.IO) {
            database.playerProgressDao().insertOrUpdate(progress)
        }
    }

    suspend fun resetPlayerProgress() {
        withContext(Dispatchers.IO) {
            try {
                database.playerProgressDao().deleteAll()
            } catch (e: Exception) {
                Timber.e(e, "Error resetting player progress")
                throw e
            }
        }
    }

    // --- Interactive Map Location Methods ---

    suspend fun initializeInteractiveMapLocations() {
        withContext(Dispatchers.IO) {
            try {
                val existingLocations = database.interactiveMapLocationDao().getAllLocations()
                if (existingLocations.isEmpty()) {
                    val defaultLocations = createDefaultInteractiveLocations()
                    database.interactiveMapLocationDao().insertLocations(defaultLocations)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing interactive map locations")
                throw e
            }
        }
    }

    private fun createDefaultInteractiveLocations(): List<InteractiveMapLocation> {
        return listOf(
            // --- Town Center Region ---
            InteractiveMapLocation(
                id = "town_square",
                name = "Town Square",
                description = "A bustling marketplace full of traders and goods.",
                scenarioId = "scenario6",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "town_square_trading",
                        type = ActivityType.TRADING,
                        name = "Trade with Merchants",
                        description = "Negotiate with local merchants for better prices.",
                        difficulty = 2,
                        estimatedDuration = 10,
                        rewards = listOf("coins", "information"),
                        isRepeatable = true
                    ),
                    LocationActivity(
                        id = "town_square_gossip",
                        type = ActivityType.NPC_INTERACTION,
                        name = "Gather Gossip",
                        description = "Listen to local rumors and news.",
                        difficulty = 1,
                        estimatedDuration = 5,
                        rewards = listOf("rumors"),
                        isRepeatable = true
                    )
                ),
                connections = listOf("merchant_quarters", "guard_training_grounds", "council_chamber", "mentor_cottage", "shadow_alley"),
                coordinateX = 0.5f,
                coordinateY = 0.5f
            ),
            InteractiveMapLocation(
                id = "merchant_quarters",
                name = "Merchant Quarters",
                description = "Where the wealthy traders conduct their business.",
                scenarioId = "scenario11",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "merchant_negotiation",
                        type = ActivityType.MINIGAME,
                        name = "High-Stakes Negotiation",
                        description = "Engage in complex trade negotiations.",
                        difficulty = 4,
                        estimatedDuration = 15,
                        requiredItems = listOf("merchant_seal"),
                        rewards = listOf("rare_item", "merchant_favor")
                    )
                ),
                connections = listOf("town_square", "council_chamber", "criminal_hideout"),
                coordinateX = 0.3f,
                coordinateY = 0.4f
            ),
            InteractiveMapLocation(
                id = "council_chamber",
                name = "Council Chamber",
                description = "The seat of political power where the town's future is decided.",
                scenarioId = "scenario35",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "council_discourse",
                        type = ActivityType.NPC_INTERACTION,
                        name = "Political Discourse",
                        description = "Engage with council members about the town's affairs.",
                        difficulty = 3,
                        estimatedDuration = 15,
                        rewards = listOf("political_insight")
                    ),
                    LocationActivity(
                        id = "council_trade",
                        type = ActivityType.TRADING,
                        name = "Council Trade",
                        description = "Negotiate trade agreements with political allies.",
                        difficulty = 4,
                        estimatedDuration = 10,
                        rewards = listOf("council_favor"),
                        isRepeatable = true
                    )
                ),
                discoveryConditions = listOf("Town Hall"),
                connections = listOf("town_square", "merchant_quarters"),
                coordinateX = 0.5f,
                coordinateY = 0.3f
            ),

            // --- Outskirts Region ---
            InteractiveMapLocation(
                id = "guard_training_grounds",
                name = "Guard Training Grounds",
                description = "Where the town guard practices their skills.",
                scenarioId = "scenario9",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "combat_training",
                        type = ActivityType.COMBAT,
                        name = "Combat Training",
                        description = "Test your skills against the town guard.",
                        difficulty = 3,
                        estimatedDuration = 12,
                        rewards = listOf("combat_skill", "guard_respect")
                    ),
                    LocationActivity(
                        id = "guard_investigation",
                        type = ActivityType.INVESTIGATION,
                        name = "Investigate Disturbances",
                        description = "Help the guards solve local mysteries.",
                        difficulty = 3,
                        estimatedDuration = 20,
                        rewards = listOf("clues", "guard_badge")
                    )
                ),
                connections = listOf("town_square", "wilderness_trail"),
                coordinateX = 0.7f,
                coordinateY = 0.3f
            ),
            InteractiveMapLocation(
                id = "wilderness_trail",
                name = "Wilderness Trail",
                description = "A winding path through untamed wilderness connecting the town to distant places.",
                scenarioId = "scenario10",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "trail_exploration",
                        type = ActivityType.EXPLORATION,
                        name = "Trail Exploration",
                        description = "Scout the wilderness paths for hidden secrets.",
                        difficulty = 2,
                        estimatedDuration = 15,
                        rewards = listOf("herbs", "ancient_map")
                    ),
                    LocationActivity(
                        id = "wilderness_combat",
                        type = ActivityType.COMBAT,
                        name = "Wilderness Dangers",
                        description = "Fight off wild creatures lurking along the trail.",
                        difficulty = 3,
                        estimatedDuration = 10,
                        rewards = listOf("beast_hide"),
                        isRepeatable = true
                    )
                ),
                connections = listOf("guard_training_grounds", "ancient_ruins", "scholars_retreat", "cursed_ruins"),
                coordinateX = 0.8f,
                coordinateY = 0.5f
            ),
            InteractiveMapLocation(
                id = "mentor_cottage",
                name = "Mentor's Cottage",
                description = "A peaceful dwelling where a wise mentor offers guidance and knowledge.",
                scenarioId = "scenario30",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "mentor_wisdom",
                        type = ActivityType.NPC_INTERACTION,
                        name = "Mentor's Wisdom",
                        description = "Seek counsel from the wise mentor about your journey.",
                        difficulty = 1,
                        estimatedDuration = 10,
                        rewards = listOf("mentor_advice", "scholar_recommendation")
                    ),
                    LocationActivity(
                        id = "mentor_crafting",
                        type = ActivityType.CRAFTING,
                        name = "Mentor's Workshop",
                        description = "Learn to craft useful items under the mentor's guidance.",
                        difficulty = 2,
                        estimatedDuration = 15,
                        rewards = listOf("crafted_charm")
                    )
                ),
                connections = listOf("town_square", "scholars_retreat"),
                coordinateX = 0.3f,
                coordinateY = 0.7f
            ),

            // --- Hidden Region ---
            InteractiveMapLocation(
                id = "shadow_alley",
                name = "Shadowed Alley",
                description = "A dark, narrow alley where the underworld conducts its business.",
                scenarioId = "scenario12",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "shadow_investigation",
                        type = ActivityType.INVESTIGATION,
                        name = "Shadow Investigation",
                        description = "Investigate the shady dealings in the alley.",
                        difficulty = 3,
                        estimatedDuration = 15,
                        rewards = listOf("underworld_intel")
                    ),
                    LocationActivity(
                        id = "underworld_contacts",
                        type = ActivityType.NPC_INTERACTION,
                        name = "Underworld Contacts",
                        description = "Make connections with the underworld figures.",
                        difficulty = 2,
                        estimatedDuration = 10,
                        rewards = listOf("underworld_favor"),
                        isRepeatable = true
                    )
                ),
                discoveryConditions = listOf("Shadowed Alley"),
                connections = listOf("town_square", "criminal_hideout", "cursed_ruins"),
                coordinateX = 0.15f,
                coordinateY = 0.5f
            ),
            InteractiveMapLocation(
                id = "criminal_hideout",
                name = "Criminal Hideout",
                description = "A concealed den of thieves and smugglers hidden beneath the city.",
                scenarioId = "scenario16",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "hideout_lockpicking",
                        type = ActivityType.MINIGAME,
                        name = "Lock Picking Challenge",
                        description = "Pick locks to access hidden stashes and secret rooms.",
                        difficulty = 4,
                        estimatedDuration = 10,
                        rewards = listOf("stolen_goods", "lockpick_set")
                    ),
                    LocationActivity(
                        id = "hideout_intel",
                        type = ActivityType.INVESTIGATION,
                        name = "Hideout Intel",
                        description = "Gather intelligence from the criminal underground.",
                        difficulty = 3,
                        estimatedDuration = 15,
                        rewards = listOf("criminal_ledger", "infernal_mark")
                    )
                ),
                discoveryConditions = listOf("Shadowed Alley"),
                connections = listOf("shadow_alley", "merchant_quarters"),
                coordinateX = 0.1f,
                coordinateY = 0.3f
            ),

            // --- Remote Region ---
            InteractiveMapLocation(
                id = "ancient_ruins",
                name = "Ancient Ruins",
                description = "Mysterious ruins from a forgotten civilization.",
                scenarioId = "scenario14",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "ruins_exploration",
                        type = ActivityType.EXPLORATION,
                        name = "Explore the Ruins",
                        description = "Search for ancient artifacts and secrets.",
                        difficulty = 4,
                        estimatedDuration = 25,
                        rewards = listOf("ancient_artifact", "knowledge"),
                        requiredItems = listOf("torch")
                    ),
                    LocationActivity(
                        id = "rune_puzzle",
                        type = ActivityType.PUZZLE,
                        name = "Decipher Ancient Runes",
                        description = "Solve the puzzle of the ancient script.",
                        difficulty = 5,
                        estimatedDuration = 30,
                        rewards = listOf("ancient_knowledge", "magical_essence", "holy_key")
                    )
                ),
                discoveryConditions = listOf("ancient_map"),
                connections = listOf("wilderness_trail", "sacred_temple"),
                coordinateX = 0.9f,
                coordinateY = 0.75f
            ),
            InteractiveMapLocation(
                id = "scholars_retreat",
                name = "Scholar's Retreat",
                description = "A secluded haven of knowledge where scholars study ancient mysteries.",
                scenarioId = "scenario37",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "ancient_translations",
                        type = ActivityType.PUZZLE,
                        name = "Ancient Translations",
                        description = "Help decipher ancient texts and uncover lost knowledge.",
                        difficulty = 4,
                        estimatedDuration = 20,
                        rewards = listOf("translated_scroll", "scholar_recommendation")
                    ),
                    LocationActivity(
                        id = "library_archives",
                        type = ActivityType.EXPLORATION,
                        name = "Library Archives",
                        description = "Explore the vast library's dusty shelves for hidden tomes.",
                        difficulty = 2,
                        estimatedDuration = 15,
                        rewards = listOf("rare_book", "ancient_knowledge")
                    )
                ),
                discoveryConditions = listOf("scholar_recommendation"),
                connections = listOf("wilderness_trail", "mentor_cottage"),
                coordinateX = 0.6f,
                coordinateY = 0.8f
            ),
            InteractiveMapLocation(
                id = "sacred_temple",
                name = "Sacred Temple",
                description = "An ancient temple radiating divine energy, sealed behind sacred wards.",
                scenarioId = "scenario41",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "temple_mysteries",
                        type = ActivityType.PUZZLE,
                        name = "Temple Mysteries",
                        description = "Solve the sacred puzzles guarding the temple's inner sanctum.",
                        difficulty = 5,
                        estimatedDuration = 25,
                        rewards = listOf("celestial_artifact", "divine_blessing")
                    ),
                    LocationActivity(
                        id = "sacred_archives",
                        type = ActivityType.EXPLORATION,
                        name = "Sacred Archives",
                        description = "Explore the temple's sacred archives for divine knowledge.",
                        difficulty = 3,
                        estimatedDuration = 15,
                        rewards = listOf("sacred_text")
                    )
                ),
                discoveryConditions = listOf("holy_key"),
                connections = listOf("ancient_ruins"),
                coordinateX = 0.85f,
                coordinateY = 0.9f
            ),
            InteractiveMapLocation(
                id = "cursed_ruins",
                name = "Cursed Ruins",
                description = "Crumbling ruins tainted by dark magic, filled with dangerous creatures.",
                scenarioId = "scenario43",
                isVisited = false,
                availableActivities = listOf(
                    LocationActivity(
                        id = "cursed_guardians",
                        type = ActivityType.COMBAT,
                        name = "Cursed Guardians",
                        description = "Battle the cursed beings that guard the ruins' dark secrets.",
                        difficulty = 5,
                        estimatedDuration = 15,
                        rewards = listOf("cursed_relic", "dark_essence")
                    ),
                    LocationActivity(
                        id = "dark_secrets",
                        type = ActivityType.INVESTIGATION,
                        name = "Dark Secrets",
                        description = "Investigate the ruins' dark history and uncover forbidden knowledge.",
                        difficulty = 4,
                        estimatedDuration = 20,
                        rewards = listOf("forbidden_tome", "shadow_crystal")
                    )
                ),
                discoveryConditions = listOf("infernal_mark"),
                connections = listOf("wilderness_trail", "shadow_alley"),
                coordinateX = 0.15f,
                coordinateY = 0.8f
            )
        )
    }

    suspend fun getAllScenarioIds(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                database.scenarioDao().getAllScenarioIds()
            } catch (e: Exception) {
                Timber.e(e, "Error getting scenario IDs")
                emptyList()
            }
        }
    }

    suspend fun getAllInteractiveMapLocations(): List<InteractiveMapLocation> {
        return withContext(Dispatchers.IO) {
            try {
                database.interactiveMapLocationDao().getAllLocations()
            } catch (e: Exception) {
                Timber.e(e, "Error getting interactive map locations")
                emptyList()
            }
        }
    }

    suspend fun getInteractiveMapLocationById(locationId: String): InteractiveMapLocation? {
        return withContext(Dispatchers.IO) {
            try {
                database.interactiveMapLocationDao().getLocationById(locationId)
            } catch (e: Exception) {
                Timber.e(e, "Error getting interactive map location %s", locationId)
                null
            }
        }
    }

    suspend fun updateLocationVisitStatus(locationId: String, isVisited: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                database.interactiveMapLocationDao().updateVisitStatus(locationId, isVisited)
            } catch (e: Exception) {
                Timber.e(e, "Error updating location visit status")
                throw e
            }
        }
    }

    suspend fun updateLocationActivities(locationId: String, activities: List<LocationActivity>) {
        withContext(Dispatchers.IO) {
            try {
                val activitiesJson = gson.toJson(activities)
                database.interactiveMapLocationDao().updateLocationActivities(locationId, activitiesJson)
            } catch (e: Exception) {
                Timber.e(e, "Error updating location activities")
                throw e
            }
        }
    }

    suspend fun getDiscoveredLocations(
        playerInventory: Set<String>,
        visitedLocations: Set<String>,
        unlockedLocations: Set<String>
    ): List<InteractiveMapLocation> {
        return withContext(Dispatchers.IO) {
            try {
                val allLocations = database.interactiveMapLocationDao().getAllLocations()
                allLocations.filter { location ->
                    location.canBeDiscovered(playerInventory, visitedLocations) ||
                    unlockedLocations.contains(location.id) ||
                    location.isVisited
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting discovered locations")
                emptyList()
            }
        }
    }
}
