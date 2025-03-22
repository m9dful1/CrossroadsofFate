package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterMenuScreen(
    onBack: () -> Unit,
    gameViewModel: GameViewModel
                        ) {
    val playerInventory = gameViewModel.playerInventory.collectAsState().value
    val activeQuests = gameViewModel.activeQuests.collectAsState().value
    val completedQuests = gameViewModel.completedQuests.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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

            DecisionButton(
                text = "Return to Title",
                modifier = Modifier.fillMaxWidth()
            ) {
                gameViewModel.returnToTitle()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Character Info Section
            listOf(
                "Stats" to listOf(
                    "Current Location: ${gameViewModel.currentScenario.collectAsState().value?.location ?: "Unknown"}"
                ),
                "Inventory" to playerInventory.toList(),
                "Active Quests" to if (activeQuests.isEmpty()) {
                    listOf("No active quests")
                } else {
                    activeQuests.flatMap { quest ->
                        listOf("${quest.title}:") + quest.objectives.map { objective ->
                            ". ${objective.description} ${if (objective.isCompleted) "✓" else ""}"
                        }
                    }
                },
                "Completed Quests" to if (completedQuests.isEmpty()) {
                    listOf("No completed quests")
                } else {
                    completedQuests.map { it.title }
                }
            ).forEach { (title, items) ->
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
                        // In CharacterMenuScreen.kt, add to the Column content
                        Spacer(modifier = Modifier.height(32.dp))
                        DecisionButton(
                            text = "Return to Title",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            gameViewModel.returnToTitle()
                        }

                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (items.isEmpty() && title == "Inventory") {
                            Text(
                                text = "No items in inventory",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
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
        }
    }
}
