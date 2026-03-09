package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.spiritwisestudios.crossroadsoffate.data.models.QuestCompletionEvent
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton

@Composable
fun QuestRewardPopup(
    event: QuestCompletionEvent,
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
                    color = Color(0xFFFFD700),
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quest Completed!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = event.quest.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (event.rewardItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rewards:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                event.rewardItems.forEach { item ->
                    Text(
                        text = "+ $item",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            if (event.locationsUnlocked.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Locations Unlocked:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
                event.locationsUnlocked.forEach { location ->
                    Text(
                        text = "+ $location",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            DecisionButton(
                text = "Continue",
                modifier = Modifier.fillMaxWidth()
            ) {
                onDismiss()
            }
        }
    }
}
