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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.logic.TextResolver
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.ui.minigames.MiniGameOverlay
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

/**
 * Main game screen composable that handles the display of scenarios, decisions, and UI elements
 * @param gameViewModel ViewModel containing game logic and state
 */
@Composable
fun MainGameScreen(gameViewModel: GameViewModel) {
    // Collect state values from the ViewModel
    val isMapVisible by gameViewModel.isMapVisible.collectAsState()
    val isCharacterMenuVisible by gameViewModel.isCharacterMenuVisible.collectAsState()
    val playerInventory by gameViewModel.playerInventory.collectAsState()
    val currentScenario by gameViewModel.currentScenario.collectAsState()
    val playerStats by gameViewModel.playerStats.collectAsState()
    val playerReputation by gameViewModel.playerReputation.collectAsState()
    val isMiniGameActive by gameViewModel.isMiniGameActive.collectAsState()
    val lastMiniGameResult by gameViewModel.lastMiniGameResult.collectAsState()

    // Main container for game screen
    Box(modifier = Modifier.fillMaxSize()) {
        currentScenario?.let { scenario ->
            //
            // Display background image
            Image(
                painter = painterForName(name = scenario.backgroundImage),
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
                            text = TextResolver.resolve(scenario.text, playerInventory, playerStats, playerReputation),
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
                val conditionMet = decision.condition?.isMet(playerInventory, playerStats, playerReputation) ?: true
                val displayText = TextResolver.resolve(
                    if (conditionMet) decision.text else decision.fallbackText ?: decision.text,
                    playerInventory, playerStats, playerReputation
                )
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
                        gameViewModel.playSfx("button_tap")
                        gameViewModel.onChoiceSelected(position)
                    }
                }
            }

            // Display map or character menu if visible
            if (isMapVisible) {
                InteractiveMapScreen(
                    gameViewModel = gameViewModel,
                    onBack = { gameViewModel.hideMap() }
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
                        modifier = Modifier
                            .width(37.dp)
                            .semantics { contentDescription = "Open map" }
                    ) {
                        gameViewModel.showMap()
                    }
                }

                // Character Menu Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                ) {
                    DecisionButton(
                        text = "👤",
                        modifier = Modifier
                            .width(37.dp)
                            .semantics { contentDescription = "Open character menu" }
                    ) {
                        gameViewModel.showCharacterMenu()
                    }
                }
            }
        }

        // Mini-game overlay (renders on top of everything when a mini-game is active)
        if (isMiniGameActive || lastMiniGameResult != null) {
            MiniGameOverlay(gameViewModel = gameViewModel)
        }

        // Quest reward popup overlay
        val questRewardNotification by gameViewModel.questRewardNotification.collectAsState()
        questRewardNotification?.let { event ->
            QuestRewardPopup(
                event = event,
                onDismiss = { gameViewModel.dismissQuestRewardNotification() }
            )
        }
    }
}