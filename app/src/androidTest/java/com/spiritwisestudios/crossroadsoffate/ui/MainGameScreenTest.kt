package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.spiritwisestudios.crossroadsoffate.MainActivity
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

//import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainGameScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockViewModel: GameViewModel

    @Before
    fun setupViewModelMock() {
        // This setup would ideally involve injecting a mock ViewModel
        // For now, we'll get the real ViewModel and perform assertions on the UI
        val activity = composeTestRule.activity
        val viewModel = ViewModelProvider(activity)[GameViewModel::class.java]

        // In a real test scenario with proper DI, we would use:
        // mockViewModel = mock(GameViewModel::class.java)
        // and inject it into our composable
    }

    @Test
    fun titleScreen_displaysGameTitle() {
        // Wait until the title text node exists and is displayed
        composeTestRule.waitUntil(timeoutMillis = 10000) { // Increased timeout slightly
            try {
                composeTestRule
                    .onNodeWithText("Crossroads\nof Fate")
                    .assertExists() // Check if node exists first
                composeTestRule
                    .onNodeWithText("Crossroads\nof Fate")
                    .isDisplayed() // Then check if it's displayed
            } catch (e: AssertionError) {
                false // Node doesn't exist or isn't displayed yet
            }
        }
        // Final assertion to confirm display (redundant but confirms wait succeeded)
        composeTestRule.onNodeWithText("Crossroads\nof Fate").assertIsDisplayed()
    }

    @Test
    fun titleScreen_displaysButtons() {
        // Check that title screen buttons are displayed
        composeTestRule.onNodeWithText("New Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Load Game").assertIsDisplayed()
    }

    // This test requires actual gameplay and state changes
    // In a real implementation with proper dependency injection,
    // it would be more comprehensive
    @Test
    fun newGameButton_navigatesToFirstScenario() {
        // Click the New Game button
        composeTestRule.onNodeWithText("New Game").performClick()

        // Wait for an element from the first game scenario to appear
        // Assuming "Your Bedroom" is the location text of the first scenario
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Your Bedroom") // Check for first scenario element
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Check that we're no longer on the title screen
        composeTestRule.onNodeWithText("New Game").assertDoesNotExist()
    }
}