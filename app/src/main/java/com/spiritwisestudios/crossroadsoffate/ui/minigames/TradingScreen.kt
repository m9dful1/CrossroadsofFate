package com.spiritwisestudios.crossroadsoffate.ui.minigames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.minigames.MiniGameState
import com.spiritwisestudios.crossroadsoffate.minigames.games.TradingGame
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton

/**
 * Trading negotiation mini-game screen.
 * Player negotiates with an NPC merchant through dialogue-style approach choices.
 */
@Composable
fun TradingScreen(
    gameState: MiniGameState,
    onChoice: (String) -> Unit,
    onAcceptDeal: () -> Unit,
    onCancel: () -> Unit
) {
    val currentPrice = gameState.getData<Int>("currentPrice") ?: 0
    val originalPrice = gameState.getData<Int>("originalPrice") ?: 0
    val round = gameState.getData<Int>("round") ?: 1
    val moodScore = gameState.getData<Int>("moodScore") ?: 0
    val lastResponse = gameState.getData<String>("lastResponse")
    val maxRounds = gameState.maxAttempts ?: 6

    val savings = originalPrice - currentPrice
    val savingsPercent = if (originalPrice > 0) (savings.toFloat() / originalPrice * 100).toInt() else 0

    // Map mood score to visual elements
    val (moodText, moodColor) = when {
        moodScore >= 3 -> "Happy" to Color.Green
        moodScore >= 0 -> "Neutral" to Color.Yellow
        moodScore >= -2 -> "Annoyed" to Color(0xFFFF8C00)
        else -> "Angry" to Color.Red
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header ---
            Text(
                text = "Trading Negotiation",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Round counter
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(maxRounds) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(12.dp)
                            .background(
                                when {
                                    i < round - 1 -> Color.White.copy(alpha = 0.6f)
                                    i == round - 1 -> Color.Yellow
                                    else -> Color(0xFF555555)
                                },
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Round $round/$maxRounds",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Merchant info ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.DarkGray.copy(alpha = 0.7f),
                        MaterialTheme.shapes.medium
                    )
                    .border(
                        width = 1.dp,
                        color = moodColor.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            ) {
                Column {
                    // Mood indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Merchant",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mood: ",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(moodColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .border(1.dp, moodColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = moodText,
                                    color = moodColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // NPC response
                    if (lastResponse != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"$lastResponse\"",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"Welcome! Let's talk business.\"",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Price display ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF1A1A2E).copy(alpha = 0.8f),
                        MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Current Price",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$currentPrice coins",
                        color = Color.Cyan,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (savings > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saved $savings coins ($savingsPercent% off)",
                            color = Color.Green,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Original: $originalPrice coins",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Approach choices ---
            Text(
                text = "Choose your approach:",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            TradingGame.AVAILABLE_CHOICES.forEach { (key, label) ->
                ApproachButton(
                    text = label,
                    onClick = { onChoice(key) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Action buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DecisionButton(
                    text = "Accept Deal",
                    modifier = Modifier.weight(1f)
                ) {
                    onAcceptDeal()
                }
                DecisionButton(
                    text = "Walk Away",
                    modifier = Modifier.weight(1f)
                ) {
                    onCancel()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ApproachButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF2A2A3E).copy(alpha = 0.8f),
                MaterialTheme.shapes.medium
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        DecisionButton(
            text = text,
            modifier = Modifier.fillMaxWidth()
        ) {
            onClick()
        }
    }
}
