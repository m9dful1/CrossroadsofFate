package com.spiritwisestudios.crossroadsoffate.logic

import com.spiritwisestudios.crossroadsoffate.data.models.ActivityType
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.LocationActivity
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActivityManagerTest {

    private lateinit var activityManager: ActivityManager

    private fun testActivity(
        id: String,
        type: ActivityType = ActivityType.MINIGAME,
        requiredItems: List<String> = emptyList(),
        isRepeatable: Boolean = false
    ) = LocationActivity(
        id = id,
        type = type,
        name = "Activity $id",
        description = "Test activity",
        requiredItems = requiredItems,
        isRepeatable = isRepeatable
    )

    private fun testLocation(activities: List<LocationActivity>) = InteractiveMapLocation(
        id = "loc1",
        name = "Test Location",
        description = "A test location",
        scenarioId = "scenario1",
        availableActivities = activities
    )

    @Before
    fun setup() {
        activityManager = ActivityManager()
        activityManager.initialize(emptySet())
    }

    @Test
    fun initialize_restoresSavedState() {
        activityManager.initialize(setOf("a1", "a2"), setOf("loc_hidden"))
        assertEquals(setOf("a1", "a2"), activityManager.getCompletedActivitiesList())
        assertEquals(setOf("loc_hidden"), activityManager.getUnlockedLocationsList())
    }

    @Test
    fun unlockLocation_addsToUnlockedSet() {
        activityManager.unlockLocation("ancient_ruins")
        assertTrue(activityManager.unlockedLocations.value.contains("ancient_ruins"))
    }

    @Test
    fun getAvailableActivities_excludesCompletedNonRepeatable() {
        val location = testLocation(listOf(testActivity("a1"), testActivity("a2")))
        activityManager.completeActivity("a1", success = true)

        val available = activityManager.getAvailableActivities(location, emptySet())
        assertEquals(listOf("a2"), available.map { it.id })
    }

    @Test
    fun getAvailableActivities_keepsCompletedRepeatable() {
        val location = testLocation(listOf(testActivity("a1", isRepeatable = true)))
        activityManager.completeActivity("a1", success = true)

        val available = activityManager.getAvailableActivities(location, emptySet())
        assertEquals(listOf("a1"), available.map { it.id })
    }

    @Test
    fun getAvailableActivities_requiresItems() {
        val location = testLocation(listOf(testActivity("a1", requiredItems = listOf("torch"))))

        assertTrue(activityManager.getAvailableActivities(location, emptySet()).isEmpty())
        assertEquals(1, activityManager.getAvailableActivities(location, setOf("torch")).size)
    }

    @Test
    fun completeActivity_success_marksCompletedAndUnlocksLocations() {
        val result = activityManager.completeActivity(
            activityId = "a1",
            success = true,
            newLocationsUnlocked = listOf("loc_new")
        )

        assertTrue(result.success)
        assertTrue(activityManager.isActivityCompleted("a1"))
        assertTrue(activityManager.unlockedLocations.value.contains("loc_new"))
        assertEquals(1, activityManager.activityResults.value.size)
    }

    @Test
    fun completeActivity_failure_recordsResultButNotCompletion() {
        val result = activityManager.completeActivity("a1", success = false)

        assertFalse(result.success)
        assertFalse(activityManager.isActivityCompleted("a1"))
        assertEquals(1, activityManager.activityResults.value.size)
    }

    @Test
    fun processMiniGameResult_mapsRewardsAndExperience() {
        val miniGameResult = MiniGameResult(
            isCompleted = true,
            success = true,
            score = 250,
            rewards = listOf("coins", "experience"),
            consequences = emptyList()
        )

        val result = activityManager.processMiniGameResult("a1", miniGameResult)

        assertTrue(result.success)
        assertEquals(250, result.score)
        assertEquals(listOf("coins", "experience"), result.itemsGained)
        assertEquals(10, result.experienceGained)
    }

    @Test
    fun processMiniGameResult_failureGrantsReducedExperienceAndConsequences() {
        val miniGameResult = MiniGameResult(
            isCompleted = true,
            success = false,
            consequences = listOf("lockpick_broken")
        )

        val result = activityManager.processMiniGameResult("a1", miniGameResult)

        assertFalse(result.success)
        assertEquals(listOf("lockpick_broken"), result.itemsLost)
        assertEquals(5, result.experienceGained)
    }

    @Test
    fun getLocationCompletionRate_reflectsProgress() {
        val location = testLocation(listOf(testActivity("a1"), testActivity("a2")))

        assertEquals(0f, activityManager.getLocationCompletionRate(location), 0.001f)

        activityManager.completeActivity("a1", success = true)
        assertEquals(0.5f, activityManager.getLocationCompletionRate(location), 0.001f)

        activityManager.completeActivity("a2", success = true)
        assertEquals(1.0f, activityManager.getLocationCompletionRate(location), 0.001f)
    }

    @Test
    fun getLocationCompletionRate_isFullForLocationWithoutActivities() {
        assertEquals(1.0f, activityManager.getLocationCompletionRate(testLocation(emptyList())), 0.001f)
    }

    @Test
    fun getActivitiesByType_filtersAvailableActivities() {
        val location = testLocation(
            listOf(
                testActivity("mini", type = ActivityType.MINIGAME),
                testActivity("trade", type = ActivityType.TRADING)
            )
        )

        val trading = activityManager.getActivitiesByType(location, ActivityType.TRADING, emptySet())
        assertEquals(listOf("trade"), trading.map { it.id })
    }

    @Test
    fun hasAvailableActivities_matchesAvailability() {
        val location = testLocation(listOf(testActivity("a1")))

        assertTrue(activityManager.hasAvailableActivities(location, emptySet()))
        activityManager.completeActivity("a1", success = true)
        assertFalse(activityManager.hasAvailableActivities(location, emptySet()))
    }
}
