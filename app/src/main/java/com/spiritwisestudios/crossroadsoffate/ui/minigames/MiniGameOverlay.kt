package com.spiritwisestudios.crossroadsoffate.ui.minigames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameInput
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameResult
import com.spiritwisestudios.crossroadsoffate.minigames.games.LockPickingGame
import com.spiritwisestudios.crossroadsoffate.minigames.games.TradingGame
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
import kotlinx.coroutines.android.awaitFrame

/**
 * Routes to the appropriate mini-game screen based on the active game type.
 * Also shows the result screen after a mini-game completes.
 */
@Composable
fun MiniGameOverlay(gameViewModel: GameViewModel) {
    val isMiniGameActive by gameViewModel.isMiniGameActive.collectAsState()
    val currentMiniGame by gameViewModel.currentMiniGame.collectAsState()
    val currentGameState by gameViewModel.currentMiniGameState.collectAsState()
    val lastResult by gameViewModel.lastMiniGameResult.collectAsState()

    // Remember the game name so we can show it on the result screen after the game is cleared
    var lastGameName by remember { mutableStateOf("Mini-Game") }
    LaunchedEffect(currentMiniGame) {
        currentMiniGame?.let { lastGameName = it.name }
    }

    // Animation loop for lock picking (updates pin positions per frame)
    LaunchedEffect(isMiniGameActive, currentMiniGame?.id) {
        if (isMiniGameActive && currentMiniGame is LockPickingGame) {
            while (true) {
                awaitFrame()
                gameViewModel.updateMiniGameAnimation()
            }
        }
    }

    // Priority 1: Show result screen if a game just completed
    val capturedResult = lastResult
    if (capturedResult != null) {
        MiniGameResultScreen(
            result = capturedResult,
            gameName = lastGameName,
            onDismiss = { gameViewModel.clearLastMiniGameResult() }
        )
    }
    // Priority 2: Show active game screen
    else if (isMiniGameActive && currentGameState != null) {
        when (currentMiniGame) {
            is LockPickingGame -> LockPickingScreen(
                gameState = currentGameState!!,
                onLockPicked = { gameViewModel.processMiniGameInput(MiniGameInput.Confirm) },
                onPickSlipped = { gameViewModel.processMiniGameInput(MiniGameInput.Tap(0f, 0f)) },
                onCancel = { gameViewModel.cancelCurrentMiniGame() }
            )

            is TradingGame -> TradingScreen(
                gameState = currentGameState!!,
                onChoice = { choice -> gameViewModel.processMiniGameInput(MiniGameInput.Choice(choice)) },
                onAcceptDeal = { gameViewModel.processMiniGameInput(MiniGameInput.Confirm) },
                onCancel = { gameViewModel.cancelCurrentMiniGame() }
            )

            else -> {
                // Unsupported game type — auto-cancel
                LaunchedEffect(Unit) {
                    gameViewModel.cancelCurrentMiniGame()
                }
            }
        }
    }
}

/**
 * Displays the result of a completed mini-game with score, rewards, and a dismiss button.
 */
@Composable
fun MiniGameResultScreen(
    result: MiniGameResult,
    gameName: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(
                    color = Color.DarkGray.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.large
                )
                .border(
                    width = 2.dp,
                    color = if (result.success) Color.Green else Color.Red,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = if (result.success) "Success!" else "Failed",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (result.success) Color.Green else Color.Red,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = gameName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Message
            if (result.message.isNotEmpty()) {
                Text(
                    text = result.message,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Score
            if (result.score > 0) {
                Text(
                    text = "Score: ${result.score}",
                    fontSize = 18.sp,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Time spent
            if (result.timeSpent > 0) {
                Text(
                    text = "Time: ${result.timeSpent}s",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Rewards
            if (result.rewards.isNotEmpty()) {
                Text(
                    text = "Rewards:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                result.rewards.forEach { reward ->
                    Text(
                        text = "  + $reward",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Consequences
            if (result.consequences.isNotEmpty()) {
                Text(
                    text = "Consequences:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow
                )
                result.consequences.forEach { consequence ->
                    Text(
                        text = "  - $consequence",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss button
            DecisionButton(
                text = "Continue",
                modifier = Modifier.fillMaxWidth()
            ) {
                onDismiss()
            }
        }
    }
}
