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
import com.spiritwisestudios.crossroadsoffate.data.models.ActivityResult
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton

/**
 * Screen that displays the results of completing an activity
 * This provides a break in the narrative flow and shows player progress
 */
@Composable
fun ActivityResultScreen(
    activityResult: ActivityResult,
    activityName: String,
    onContinue: () -> Unit
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
                    color = if (activityResult.success) Color.Green else Color.Red,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Activity completion header
            Text(
                text = if (activityResult.success) "🎉 Activity Completed!" else "❌ Activity Failed",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (activityResult.success) Color.Green else Color.Red,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Activity name
            Text(
                text = activityName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score display (if applicable)
            if (activityResult.score > 0) {
                Text(
                    text = "Score: ${activityResult.score}",
                    fontSize = 18.sp,
                    color = Color.Cyan,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Items gained
            if (activityResult.itemsGained.isNotEmpty()) {
                Text(
                    text = "Items Gained:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                activityResult.itemsGained.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Items lost
            if (activityResult.itemsLost.isNotEmpty()) {
                Text(
                    text = "Items Used:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow
                )
                activityResult.itemsLost.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Experience gained
            if (activityResult.experienceGained > 0) {
                Text(
                    text = "Experience Gained: +${activityResult.experienceGained} XP",
                    fontSize = 16.sp,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Secrets revealed
            if (activityResult.secretsRevealed.isNotEmpty()) {
                Text(
                    text = "Secrets Discovered:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Magenta
                )
                activityResult.secretsRevealed.forEach { secret ->
                    Text(
                        text = "• $secret",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // New locations unlocked
            if (activityResult.newLocationsUnlocked.isNotEmpty()) {
                Text(
                    text = "New Locations Unlocked:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
                activityResult.newLocationsUnlocked.forEach { location ->
                    Text(
                        text = "• $location",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Continue button
            DecisionButton(
                text = "Continue Adventure",
                modifier = Modifier.fillMaxWidth()
            ) {
                onContinue()
            }
        }
    }
} 