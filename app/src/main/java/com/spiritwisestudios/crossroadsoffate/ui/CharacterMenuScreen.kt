package com.spiritwisestudios.crossroadsoffate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.data.models.QuestType
import com.spiritwisestudios.crossroadsoffate.logic.TextResolver
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.ui.components.GameCard
import com.spiritwisestudios.crossroadsoffate.ui.components.GameTopBar
import com.spiritwisestudios.crossroadsoffate.ui.components.VolumeSlider
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

/**
 * Composable that displays the character menu screen with player stats, inventory, and quests.
 *
 * @param onBack Callback function to handle back navigation
 * @param gameViewModel ViewModel that holds the game state and logic
 */
@Composable
fun CharacterMenuScreen(
    onBack: () -> Unit,
    gameViewModel: GameViewModel
) {
    // Collect current state values from the ViewModel
    val playerInventory by gameViewModel.playerInventory.collectAsState()
    val activeQuests by gameViewModel.activeQuests.collectAsState()
    val completedQuests by gameViewModel.completedQuests.collectAsState()
    val playerStats by gameViewModel.playerStats.collectAsState()
    val playerReputation by gameViewModel.playerReputation.collectAsState()
    val musicVolume by gameViewModel.musicVolume.collectAsState()
    val sfxVolume by gameViewModel.sfxVolume.collectAsState()
    val isMuted by gameViewModel.isMuted.collectAsState()
    val currentScenario by gameViewModel.currentScenario.collectAsState()

    // System back closes the overlay instead of exiting the app
    BackHandler { onBack() }

    // Main container with semi-transparent black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.OverlayScrim)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed header — stays visible while the sections below scroll
            GameTopBar(title = "Character Menu", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Return to title button
                DecisionButton(
                    text = "Return to Title",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    gameViewModel.returnToTitle()
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Character information sections
                listOf(
                    // Stats section showing current location and character stats
                    "Stats" to listOf(
                        "Current Location: ${currentScenario?.location ?: "Unknown"}",
                        ""
                    ) + playerStats.entries.sortedBy { it.key }.map { (stat, value) ->
                        "${stat.replaceFirstChar { it.uppercase() }}: $value"
                    },
                    // Reputation section showing faction standings
                    "Reputation" to playerReputation.entries.sortedBy { it.key }.map { (faction, value) ->
                        "${faction.replaceFirstChar { it.uppercase() }}: $value"
                    },
                    // Inventory section: ids display the way scenario text
                    // renders them ("holy_key" -> "Holy Key")
                    "Inventory" to playerInventory.map { TextResolver.formatItemName(it) },
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
                    GameCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
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
                                color = GameColors.TextSecondary,
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

                // Audio Settings section
                GameCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Audio Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    VolumeSlider(
                        label = "Music",
                        value = musicVolume,
                        onValueChange = { gameViewModel.setMusicVolume(it) }
                    )
                    VolumeSlider(
                        label = "SFX",
                        value = sfxVolume,
                        onValueChange = { gameViewModel.setSfxVolume(it) }
                    )

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
