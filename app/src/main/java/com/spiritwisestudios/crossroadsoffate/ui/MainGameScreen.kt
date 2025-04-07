package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.R
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

/**
 * Main game screen composable that handles the display of scenarios, decisions, and UI elements
 * @param gameViewModel ViewModel containing game logic and state
 */
@Composable
fun MainGameScreen(gameViewModel: GameViewModel) {
    // Collect state values from the ViewModel
    val isMapVisible = gameViewModel.isMapVisible.collectAsState().value
    val isCharacterMenuVisible = gameViewModel.isCharacterMenuVisible.collectAsState().value
    val playerProgress = gameViewModel.playerProgress.collectAsState().value
    val playerInventory = gameViewModel.playerInventory.collectAsState().value
    var currentScenario by remember { mutableStateOf<ScenarioEntity?>(null) }

    // Load the current scenario based on player progress
    LaunchedEffect(playerProgress?.currentScenarioId) {
        playerProgress?.currentScenarioId?.let { scenarioId ->
            gameViewModel.loadScenarioById(scenarioId) { scenario ->
                currentScenario = scenario
            }
        }
    }

    // Update quest progress when a scenario is loaded
    LaunchedEffect(currentScenario?.id) {
        currentScenario?.id?.let { scenarioId ->
            // Check for quest objectives that match this scenario
            gameViewModel.activeQuests.value.forEach { quest ->
                quest.objectives.forEach { objective ->
                    if (objective.requiredScenarioId == scenarioId && !objective.isCompleted) {
                        gameViewModel.updateQuestProgress(quest.id, objective.id)
                    }
                }
            }
        }
    }

    // Main container for game screen
    Box(modifier = Modifier.fillMaxSize()) {
        currentScenario?.let { scenario ->
            //
            val backgroundId = when (scenario.backgroundImage) {
                "hell_bg" -> R.drawable.hell_bg
                "heaven_bg" -> R.drawable.heaven_bg
                "bedroom_morning" -> R.drawable.bedroom_morning
                "dressing_room" -> R.drawable.dressing_room
                "bedroom_sleep" -> R.drawable.bedroom_sleep
                "front_door" -> R.drawable.front_door
                "town_entrance" -> R.drawable.town_entrance
                "town_square" -> R.drawable.town_square
                "market" -> R.drawable.market
                "town_crossroads" -> R.drawable.town_crossroads
                "guard_training" -> R.drawable.guard_training
                "wilderness_trail" -> R.drawable.wilderness_trail
                "merchant_quarters" -> R.drawable.merchant_quarters
                "shadow_alley" -> R.drawable.shadow_alley
                "guard_patrol" -> R.drawable.guard_patrol
                "ancient_ruins" -> R.drawable.ancient_ruins
                "trade_lane" -> R.drawable.trade_lane
                "criminal_hideout" -> R.drawable.criminal_hideout
                "reflecting_area" -> R.drawable.reflecting_area
                "mentor_cottage" -> R.drawable.mentor_cottage
                "town_hall" -> R.drawable.town_hall
                "wilderness_camp" -> R.drawable.wilderness_camp
                "merchant_guild" -> R.drawable.merchant_guild
                "criminal_underworld" -> R.drawable.criminal_underworld
                "council_chamber" -> R.drawable.council_chamber
                "wilderness_archive" -> R.drawable.wilderness_archive
                "guild_meeting" -> R.drawable.guild_meeting
                "underground_syndicate" -> R.drawable.underground_syndicate
                "scholars_retreat" -> R.drawable.scholars_retreat
                "future_threshold" -> R.drawable.future_threshold
                else -> R.drawable.default_bg
            }

            // Display background image
            Image(
                painter = painterResource(id = backgroundId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Display scenario text and decisions
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    // Apply layout modifiers in specific order for proper layering
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        //.fillMaxWidth()
                        .background(
                            color = Color.DarkGray.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scenario.location,
                            fontSize = 24.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scenario.text,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Display decision buttons based on scenario decisions
            scenario.decisions.forEach { (position, decision) ->
                // Check if the decision has a condition and if the player has the required item
                val displayText = if (decision.condition != null && decision.condition.requiredItem in playerInventory) {
                    decision.text
                } else {
                    decision.fallbackText ?: decision.text
                }
                // Determine the alignment based on the position
                val alignment = when (position) {
                    "topLeft" -> Alignment.TopStart
                    "topRight" -> Alignment.TopEnd
                    "bottomLeft" -> Alignment.BottomStart
                    "bottomRight" -> Alignment.BottomEnd
                    else -> Alignment.Center
                }

                // Display the decision button
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .padding(
                            start = if (position.endsWith("Left")) 16.dp else 0.dp,
                            end = if (position.endsWith("Right")) 16.dp else 0.dp,
                            top = if (position.startsWith("top")) 64.dp else 0.dp,
                            bottom = if (position.startsWith("bottom")) 64.dp else 0.dp
                        )
                        .widthIn(max = 150.dp)
                ) {
                    DecisionButton(
                        text = displayText,
                        modifier = Modifier.wrapContentSize().testTag("${position}DecisionButton")
                    ) {
                        gameViewModel.onChoiceSelected(position)
                    }
                }
            }

            // Display map or character menu if visible
            if (isMapVisible) {
                MapScreen(
                    locations = listOf(
                        MapLocation(
                            name = "Town Square",
                            description = "A bustling marketplace full of traders and goods.",
                            scenarioId = "town_square_revisit",
                            isVisited = playerProgress?.visitedLocations?.contains("Market") == true
                        ),
                        // Add other locations as needed
                    ),
                    onBack = { gameViewModel.hideMap() },
                    onLocationSelected = { location ->
                        gameViewModel.travelToLocation(location)
                    }
                )
                // Display character menu
            } else if (isCharacterMenuVisible) {
                CharacterMenuScreen(
                    onBack = { gameViewModel.hideCharacterMenu() },
                    gameViewModel = gameViewModel
                )
            } else {
                // Map Screen Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    DecisionButton(
                        text = "🗺",
                        modifier = Modifier.width(37.dp)
                    ) {
                        gameViewModel.showMap()                    }
                }

                // Character Menu Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                ) {
                    DecisionButton(
                        text = "👤",
                        modifier = Modifier.width(37.dp)
                    ) {
                        gameViewModel.showCharacterMenu()
                    }
                }
            }
        }
    }
}