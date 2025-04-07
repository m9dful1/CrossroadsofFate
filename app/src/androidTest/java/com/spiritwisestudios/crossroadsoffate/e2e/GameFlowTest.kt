package com.spiritwisestudios.crossroadsoffate.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.spiritwisestudios.crossroadsoffate.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class GameFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun startNewGameFlow_reachesFirstObjective() {
        // Start new game
        composeTestRule.onNodeWithText("New Game").performClick()
        
        // Wait for first scenario to load
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Your Bedroom").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Make choices to reach the home front door (scenario4)
        composeTestRule.onNodeWithText("Get up promptly and prepare methodically").performClick()
        
        // Wait for next scenario
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Dressing Room").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        composeTestRule.onNodeWithText("Choose a neat outfit and tidy up").performClick()
        
        // Wait for next scenario (home front door)
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText("Home - Front Door").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Assert we've reached the first quest objective
        // To validate this, we need to check character menu/quest log
        // This depends on having UI elements to open character menu and view quests
        
        // Example (adjust based on actual UI implementation):
        /*
        // Open character menu
        composeTestRule.onNodeWithContentDescription("Character Menu").performClick()
        
        // Navigate to quests tab
        composeTestRule.onNodeWithText("Quests").performClick()
        
        // Verify first objective is completed
        composeTestRule.onNodeWithText("Leave your home")
            .assertExists()
            .assertIsDisplayed()
        
        // Check for completion indicator (this depends on your UI implementation)
        composeTestRule.onAllNodesWithTag("objective_complete")
            .onFirst()
            .assertExists()
        */
    }
    
    @Test
    fun gameNavigation_canVisitTownSquare() {
        // Start new game
        composeTestRule.onNodeWithText("New Game").performClick()
        
        // Navigate through the beginning scenarios
        navigateToLocation("Your Bedroom")
        composeTestRule.onNodeWithText("Get up promptly and prepare methodically").performClick()
        
        navigateToLocation("Dressing Room")
        composeTestRule.onNodeWithText("Choose a neat outfit and tidy up").performClick()
        
        navigateToLocation("Home - Front Door")
        composeTestRule.onNodeWithText("Wave goodbye warmly and step out with purpose").performClick()
        
        navigateToLocation("Town Entrance")
        composeTestRule.onNodeWithText("Greet everyone with polite warmth").performClick()
        
        // Verify we've reached the town square
        navigateToLocation("Town Square")
        
        // Make a choice in the town square
        composeTestRule.onNodeWithText("Offer help to an elderly neighbor").performClick()
        
        // Further assertions based on the game flow
    }
    
    /**
     * Helper method to wait for a location to be displayed
     */
    private fun navigateToLocation(locationName: String) {
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNodeWithText(locationName).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.onNodeWithText(locationName).assertIsDisplayed()
    }

    @Test
    fun holyKeyPath_reachesHolySanctum() {
        // Start new game
        composeTestRule.onNodeWithText("New Game").performClick()

        // Navigate explicitly based on scenarios.json (topLeft path)
        navigateToLocation("Your Bedroom") // Scenario 1
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Dressing Room") // Scenario 2
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Home - Front Door") // Scenario 4
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Town Entrance") // Scenario 5
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Town Square") // Scenario 6
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Market") // Scenario 7

        // Get Holy Key (topLeft)
        composeTestRule.onNodeWithText("Help repair the stall and earn his trust").performClick()
        navigateToLocation("Town Crossroads") // Scenario 8

        // Choose Guard Path (topLeft)
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Guard Training Grounds") // Scenario 9
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Guard Patrol") // Scenario 13
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Quiet Reflecting Area") // Scenario 29

        // Choose Honorable Path (topLeft)
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Sacred Temple Approach") // Scenario 41

        // Approach Door (topLeft)
        composeTestRule.onNodeWithTag("topLeftDecisionButton").performClick()
        navigateToLocation("Celestial Door") // Scenario 42

        // Use Holy Key (topLeft)
        composeTestRule.onNodeWithText("Enter the realm of eternal light").performClick()

        // Verify navigation to Holy Sanctum (Scenario 45)
        navigateToLocation("Eternal Celestial Court") // Scenario 45 Location Name
    }

    @Test
    fun infernalMarkPath_reachesUnholySanctum() {
        // Start new game
        composeTestRule.onNodeWithText("New Game").performClick()

        // Navigate explicitly based on scenarios.json (bottomRight path)
        navigateToLocation("Your Bedroom") // Scenario 1
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        // Scenario 3 location is also "Your Bedroom", use waitForIdle
        composeTestRule.waitForIdle()
        // Explicitly check we are in Scenario 3 by finding a unique text if possible, or just proceed
        // For now, assume the click worked and proceed to next action in Scenario 3
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Home - Front Door") // Scenario 4
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Town Entrance") // Scenario 5
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Town Square") // Scenario 6
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Market") // Scenario 7

        // Get Infernal Mark (bottomRight)
        composeTestRule.onNodeWithText("Sneakily pickpocket near the stall for quick gain").performClick()
        navigateToLocation("Town Crossroads") // Scenario 8

        // Choose Outlaw Path (bottomRight)
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Shadowed Alley") // Scenario 12
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Underground Hideout") // Scenario 16
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Quiet Reflecting Area") // Scenario 29

        // Choose Dark Path (bottomRight)
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Cursed Ruin Approach") // Scenario 43

        // Use Infernal Mark (bottomRight)
        composeTestRule.onNodeWithText("Steady your dark resolve and press on").performClick()
        navigateToLocation("Infernal Portal") // Scenario 44

        // Enter Portal (bottomRight)
        composeTestRule.onNodeWithTag("bottomRightDecisionButton").performClick()
        navigateToLocation("Abyssal Throne") // Scenario 46
    }
} 