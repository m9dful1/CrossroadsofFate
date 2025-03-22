package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.R
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainGameScreen(gameViewModel: GameViewModel) {
    val isMapVisible = gameViewModel.isMapVisible.collectAsState().value
    val isCharacterMenuVisible = gameViewModel.isCharacterMenuVisible.collectAsState().value
    val playerProgress = gameViewModel.playerProgress.collectAsState().value
    val playerInventory = gameViewModel.playerInventory.collectAsState().value
    var currentScenario by remember { mutableStateOf<ScenarioEntity?>(null) }

    LaunchedEffect(playerProgress?.currentScenarioId) {
        playerProgress?.currentScenarioId?.let { scenarioId ->
            gameViewModel.loadScenarioById(scenarioId) { scenario ->
                currentScenario = scenario
            }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        currentScenario?.let { scenario ->
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
            Image(
                painter = painterResource(id = backgroundId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
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

            scenario.decisions.forEach { (position, decision) ->
                val displayText = if (decision.condition?.requiredItem in playerInventory) {
                    decision.text
                } else {
                    decision.fallbackText ?: decision.text
                }
                val alignment = when (position) {
                    "topLeft" -> Alignment.TopStart
                    "topRight" -> Alignment.TopEnd
                    "bottomLeft" -> Alignment.BottomStart
                    "bottomRight" -> Alignment.BottomEnd
                    else -> Alignment.Center
                }

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
                        modifier = Modifier.wrapContentSize()
                    ) {
                        gameViewModel.onChoiceSelected(position)
                    }
                }
            }

            if (isMapVisible) {
                MapScreen(
                    locations = listOf(/* Add your map locations here */),
                    onBack = { gameViewModel.hideMap() },
                    onLocationSelected = { location ->
                        // Handle location selection
                        gameViewModel.hideMap()
                    }
                )
            } else if (isCharacterMenuVisible) {
                CharacterMenuScreen(
                    onBack = { gameViewModel.hideCharacterMenu() },
                    gameViewModel = gameViewModel
                )
            } else {
                // Map Button
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

                // Character Button
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