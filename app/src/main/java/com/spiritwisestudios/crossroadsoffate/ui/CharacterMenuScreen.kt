package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.data.models.QuestType
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

/**
 * Composable that displays the character menu screen with player stats, inventory, and quests.
 *
 * @param onBack Callback function to handle back navigation
 * @param gameViewModel ViewModel that holds the game state and logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterMenuScreen(
    onBack: () -> Unit,
    gameViewModel: GameViewModel
) {
    // Collect current state values from the ViewModel
    val playerInventory = gameViewModel.playerInventory.collectAsState().value
    val activeQuests = gameViewModel.activeQuests.collectAsState().value
    val completedQuests = gameViewModel.completedQuests.collectAsState().value
    val playerStats = gameViewModel.playerStats.collectAsState().value
    val playerReputation = gameViewModel.playerReputation.collectAsState().value
    val musicVolume = gameViewModel.musicVolume.collectAsState().value
    val sfxVolume = gameViewModel.sfxVolume.collectAsState().value
    val isMuted = gameViewModel.isMuted.collectAsState().value

    // Main container with semi-transparent black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top app bar with back button and title
            TopAppBar(
                title = { Text("  Character Menu", color = Color.White) },
                navigationIcon = {
                    DecisionButton(
                        text = "Back",
                        modifier = Modifier.width(80.dp)
                    ) { onBack() }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.DarkGray.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Return to title button
            DecisionButton(
                text = "Return to Title",
                modifier = Modifier.fillMaxWidth()
            ) {
                gameViewModel.returnToTitle()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Character information sections
            listOf(
                // Stats section showing current location and character stats
                "Stats" to listOf(
                    "Current Location: ${gameViewModel.currentScenario.collectAsState().value?.location ?: "Unknown"}",
                    ""
                ) + playerStats.entries.sortedBy { it.key }.map { (stat, value) ->
                    "${stat.replaceFirstChar { it.uppercase() }}: $value"
                },
                // Reputation section showing faction standings
                "Reputation" to playerReputation.entries.sortedBy { it.key }.map { (faction, value) ->
                    "${faction.replaceFirstChar { it.uppercase() }}: $value"
                },
                // Inventory section showing all items
                "Inventory" to playerInventory.toList(),
                // Active quests section with objectives and completion status
                "Active Quests" to if (activeQuests.isEmpty()) {
                    listOf("No active quests")
                } else {
                    activeQuests.flatMap { quest ->
                        val typeLabel = when (quest.questType) {
                            QuestType.MAIN -> "[Main]"
                            QuestType.PATH -> "[Path]"
                            QuestType.SIDE -> "[Side]"
                        }
                        listOf("$typeLabel ${quest.title}:") + quest.objectives.map { objective ->
                            val progress = if (objective.requiredActivityCount != null) {
                                " (${objective.currentCount}/${objective.requiredActivityCount})"
                            } else ""
                            ". ${objective.description}$progress ${if (objective.isCompleted) "✓" else ""}"
                        }
                    }
                },
                // Completed quests section
                "Completed Quests" to if (completedQuests.isEmpty()) {
                    listOf("No completed quests")
                } else {
                    completedQuests.map { it.title }
                }
            ).forEach { (title, items) ->
                // Individual section container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            Color.DarkGray.copy(alpha = 0.7f),
                            MaterialTheme.shapes.medium
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.7f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        // Section title
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Display empty inventory message or list items
                        if (items.isEmpty() && title == "Inventory") {
                            Text(
                                text = "No items in inventory",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            // Display list of items
                            items.forEach { item ->
                                Text(
                                    text = item,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Audio Settings section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(
                        Color.DarkGray.copy(alpha = 0.7f),
                        MaterialTheme.shapes.medium
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.7f),
                        MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Audio Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Music volume slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Music",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.width(60.dp)
                        )
                        Slider(
                            value = musicVolume,
                            onValueChange = { gameViewModel.setMusicVolume(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.8f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // SFX volume slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SFX",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.width(60.dp)
                        )
                        Slider(
                            value = sfxVolume,
                            onValueChange = { gameViewModel.setSfxVolume(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.8f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mute toggle
                    DecisionButton(
                        text = if (isMuted) "Unmute" else "Mute",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        gameViewModel.toggleMute()
                    }
                }
            }
        }
    }
}