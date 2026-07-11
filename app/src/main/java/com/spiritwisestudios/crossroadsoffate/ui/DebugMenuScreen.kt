package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.ui.components.GameCard
import com.spiritwisestudios.crossroadsoffate.ui.components.VolumeSlider
import com.spiritwisestudios.crossroadsoffate.ui.minigames.MiniGameOverlay
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

@Composable
fun DebugMenuScreen(
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    onShowErrorLogger: () -> Unit
) {
    val playerInventory by gameViewModel.playerInventory.collectAsState()
    val playerStats by gameViewModel.playerStats.collectAsState()
    val playerReputation by gameViewModel.playerReputation.collectAsState()
    val activeQuests by gameViewModel.activeQuests.collectAsState()
    val completedQuests by gameViewModel.completedQuests.collectAsState()
    val currentScenario by gameViewModel.currentScenario.collectAsState()
    val playerProgress by gameViewModel.playerProgress.collectAsState()
    val isMiniGameActive by gameViewModel.isMiniGameActive.collectAsState()
    val lastMiniGameResult by gameViewModel.lastMiniGameResult.collectAsState()
    val unlockedLocations by gameViewModel.unlockedLocations.collectAsState()
    val musicVolume by gameViewModel.musicVolume.collectAsState()
    val sfxVolume by gameViewModel.sfxVolume.collectAsState()
    val isMuted by gameViewModel.isMuted.collectAsState()

    var scenarioIds by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        gameViewModel.debugGetAllScenarioIds { ids -> scenarioIds = ids }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DecisionButton(text = "Back", modifier = Modifier.width(80.dp)) { onBack() }
                Text(
                    text = "Debug Menu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debug Session (always visible, not collapsible)
            DebugCard {
                Text("Debug Session", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (gameViewModel.isDebugSession) "Active | Scenario: ${currentScenario?.id ?: "none"}"
                    else "Inactive",
                    color = if (gameViewModel.isDebugSession) Color.Green else Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                DecisionButton(
                    text = if (gameViewModel.isDebugSession) "Restart Session" else "Start Debug Session",
                    modifier = Modifier.fillMaxWidth()
                ) { gameViewModel.debugStartSession() }
            }

            // Mini-Games
            DebugSection(title = "Mini-Games") {
                val miniGames = remember { gameViewModel.getAvailableMiniGames() }
                miniGames.forEach { game ->
                    DecisionButton(
                        text = "${game.name} (Diff: ${game.difficulty})",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { gameViewModel.startMiniGame(game.id) }
                }
            }

            // Quests
            DebugSection(title = "Quests") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DecisionButton(text = "Main Quest", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugActivateQuest("main")
                    }
                    DecisionButton(text = "All Side", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugActivateQuest("side_all")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DecisionButton(text = "Guard", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugActivateQuest("path_guard")
                    }
                    DecisionButton(text = "Merchant", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugActivateQuest("path_merchant")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DecisionButton(text = "Adventurer", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugActivateQuest("path_adventurer")
                    }
                    DecisionButton(text = "Outlaw", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugActivateQuest("path_outlaw")
                    }
                }
                if (activeQuests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Active Quests:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    activeQuests.forEach { quest ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quest.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            DecisionButton(text = "Next Obj") {
                                gameViewModel.debugCompleteNextObjective(quest.id)
                            }
                        }
                    }
                }
                if (completedQuests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Completed: ${completedQuests.joinToString { it.title }}",
                        color = Color.Green.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            // Inventory
            DebugSection(title = "Inventory") {
                if (playerInventory.isNotEmpty()) {
                    Text(
                        "Current: ${playerInventory.joinToString(", ")}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                val commonItems = listOf(
                    "torch", "guard_badge", "merchant_seal", "ancient_artifact",
                    "infernal_mark", "shadow_cloak", "holy_key", "ancient_map",
                    "scholar_recommendation", "lockpick_tools"
                )
                // Show items in rows of 3
                commonItems.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { item ->
                            DecisionButton(
                                text = item.replace("_", " "),
                                modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                            ) { gameViewModel.debugAddItem(item) }
                        }
                        // Fill remaining space if row is not full
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                var customItem by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = customItem,
                        onValueChange = { customItem = it },
                        placeholder = { Text("Custom item", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.DarkGray,
                            unfocusedContainerColor = Color.DarkGray
                        ),
                        singleLine = true
                    )
                    DecisionButton(text = "Add") {
                        if (customItem.isNotBlank()) {
                            gameViewModel.debugAddItem(customItem.trim())
                            customItem = ""
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                DecisionButton(text = "Clear All", modifier = Modifier.fillMaxWidth()) {
                    gameViewModel.debugClearInventory()
                }
            }

            // Stats
            DebugSection(title = "Stats") {
                listOf("strength", "charisma", "cunning", "wisdom").forEach { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stat.replaceFirstChar { it.uppercase() }}: ${playerStats[stat] ?: 0}",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        DecisionButton(text = "-1") { gameViewModel.debugModifyStat(stat, -1) }
                        Spacer(modifier = Modifier.width(4.dp))
                        DecisionButton(text = "+1") { gameViewModel.debugModifyStat(stat, 1) }
                    }
                }
            }

            // Reputation
            DebugSection(title = "Reputation") {
                listOf("guard", "merchant", "scholar", "underworld").forEach { faction ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${faction.replaceFirstChar { it.uppercase() }}: ${playerReputation[faction] ?: 0}",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        DecisionButton(text = "-5") { gameViewModel.debugModifyReputation(faction, -5) }
                        Spacer(modifier = Modifier.width(4.dp))
                        DecisionButton(text = "+5") { gameViewModel.debugModifyReputation(faction, 5) }
                    }
                }
            }

            // Interactive Map
            DebugSection(title = "Interactive Map") {
                DecisionButton(text = "Unlock All Locations", modifier = Modifier.fillMaxWidth()) {
                    gameViewModel.debugUnlockAllLocations()
                }
                if (unlockedLocations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Unlocked: ${unlockedLocations.joinToString(", ")}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // Scenario Navigation
            DebugSection(title = "Scenario Navigation") {
                var scenarioInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = scenarioInput,
                        onValueChange = { scenarioInput = it },
                        placeholder = { Text("scenario ID", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.DarkGray,
                            unfocusedContainerColor = Color.DarkGray
                        ),
                        singleLine = true
                    )
                    DecisionButton(text = "Go") {
                        if (scenarioInput.isNotBlank()) {
                            gameViewModel.debugJumpToScenario(scenarioInput.trim())
                            scenarioInput = ""
                        }
                    }
                }
                if (scenarioIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Quick Jump:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    scenarioIds.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { id ->
                                DecisionButton(
                                    text = id.removePrefix("scenario"),
                                    modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                                ) { gameViewModel.debugJumpToScenario(id) }
                            }
                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Save/Load
            DebugSection(title = "Save/Load") {
                DecisionButton(text = "Reset Progress", modifier = Modifier.fillMaxWidth()) {
                    gameViewModel.debugResetProgress()
                }
                Spacer(modifier = Modifier.height(8.dp))
                var showState by remember { mutableStateOf(false) }
                DecisionButton(
                    text = if (showState) "Hide State" else "View State",
                    modifier = Modifier.fillMaxWidth()
                ) { showState = !showState }
                AnimatedVisibility(visible = showState) {
                    Text(
                        text = playerProgress?.toString() ?: "No progress data",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Audio
            DebugSection(title = "Audio") {
                VolumeSlider(label = "Music", value = musicVolume, onValueChange = { gameViewModel.setMusicVolume(it) }, labelWidth = 50.dp)
                VolumeSlider(label = "SFX", value = sfxVolume, onValueChange = { gameViewModel.setSfxVolume(it) }, labelWidth = 50.dp)
                DecisionButton(
                    text = if (isMuted) "Unmute" else "Mute",
                    modifier = Modifier.fillMaxWidth()
                ) { gameViewModel.toggleMute() }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DecisionButton(text = "Menu", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugPlayMusic("menu")
                    }
                    DecisionButton(text = "Town", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugPlayMusic("town")
                    }
                    DecisionButton(text = "Wild", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugPlayMusic("wilderness")
                    }
                    DecisionButton(text = "Mystery", modifier = Modifier.weight(1f)) {
                        gameViewModel.debugPlayMusic("mystery")
                    }
                }
            }

            // Error Logger
            DebugSection(title = "Error Logger") {
                DecisionButton(text = "Open Error Logger", modifier = Modifier.fillMaxWidth()) {
                    onShowErrorLogger()
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }

        // Mini-game overlay (renders on top when active)
        if (isMiniGameActive || lastMiniGameResult != null) {
            MiniGameOverlay(gameViewModel = gameViewModel)
        }
    }
}

@Composable
private fun DebugCard(content: @Composable ColumnScope.() -> Unit) {
    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = 12.dp,
        content = content
    )
}

@Composable
private fun DebugSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = 0.dp
    ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = if (expanded) "[-]" else "[+]",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    content()
                }
            }
    }
}
