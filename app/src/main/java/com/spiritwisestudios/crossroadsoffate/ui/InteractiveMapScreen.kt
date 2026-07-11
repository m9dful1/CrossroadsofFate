package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.data.models.ActivityType
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import com.spiritwisestudios.crossroadsoffate.data.models.LocationActivity
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.ui.components.GameCard
import com.spiritwisestudios.crossroadsoffate.ui.components.GameTopBar
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

/**
 * Enhanced interactive map screen that displays locations with activities
 * @param gameViewModel The game view model containing interactive map data
 * @param onBack Callback when back button is pressed
 */
@Composable
fun InteractiveMapScreen(
    gameViewModel: GameViewModel,
    onBack: () -> Unit
) {
    // Collect interactive map locations from the view model
    val interactiveLocations by gameViewModel.interactiveMapLocations.collectAsState()
    val playerInventory by gameViewModel.playerInventory.collectAsState()
    val completedActivities by gameViewModel.completedActivities.collectAsState()
    
    // State for showing location details
    var selectedLocation by remember { mutableStateOf<InteractiveMapLocation?>(null) }

    // Main container with semi-transparent black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.OverlayScrim)
    ) {
        val currentSelection = selectedLocation
        if (currentSelection != null) {
            // Recomputes when inventory or completion state changes so the
            // activity list stays in sync with the observed flows
            val detailActivities = remember(currentSelection, playerInventory, completedActivities) {
                gameViewModel.getAvailableActivitiesForLocation(currentSelection.id)
            }
            // Show location detail view
            LocationDetailView(
                location = currentSelection,
                availableActivities = detailActivities,
                gameViewModel = gameViewModel,
                onBack = { selectedLocation = null },
                onStartActivity = { activityId ->
                    gameViewModel.startActivity(currentSelection.id, activityId)
                    selectedLocation = null // Close detail view after starting activity
                }
            )
        } else {
            // Show main map view
            Column(modifier = Modifier.fillMaxSize()) {
                GameTopBar(title = "Interactive Map", onBack = onBack)

                // Header text
                Text(
                    text = "Tap locations to see available activities",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )

                // Scrollable list of interactive locations
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(interactiveLocations) { location ->
                        // Keyed on the observed flows so badges update reactively
                        val availableActivities = remember(location, playerInventory, completedActivities) {
                            gameViewModel.getAvailableActivitiesForLocation(location.id)
                        }
                        val completionRate = remember(location, completedActivities) {
                            gameViewModel.getLocationCompletionRate(location.id)
                        }
                        LocationCard(
                            location = location,
                            availableActivities = availableActivities,
                            completionRate = completionRate,
                            onClick = { selectedLocation = location }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual location card showing basic info and activity indicators
 */
@Composable
fun LocationCard(
    location: InteractiveMapLocation,
    availableActivities: List<LocationActivity>,
    completionRate: Float,
    onClick: () -> Unit
) {
    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundColor = GameColors.PanelBackgroundLight,
        borderColor = if (availableActivities.isNotEmpty()) Color.Yellow.copy(alpha = 0.7f) else GameColors.BorderFaint,
        contentPadding = 12.dp
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = location.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Activity indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Available activities count
                    if (availableActivities.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Yellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = availableActivities.size.toString(),
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Completion indicator
                    if (location.availableActivities.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completionRate >= 1.0f -> Color.Green
                                        completionRate > 0f -> GameColors.Orange
                                        else -> Color.Red.copy(alpha = 0.6f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(completionRate * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Status and action button
            Spacer(modifier = Modifier.height(8.dp))
            
            DecisionButton(
                text = when {
                    availableActivities.isNotEmpty() -> "View Activities (${availableActivities.size})"
                    location.isVisited -> "Travel Here"
                    else -> "Visit Location"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                onClick()
            }
    }
}

/**
 * Detailed view of a location showing all available activities
 */
@Composable
fun LocationDetailView(
    location: InteractiveMapLocation,
    availableActivities: List<LocationActivity>,
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    onStartActivity: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(title = location.name, onBack = onBack)

        // Location description
        GameCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = GameColors.PanelBackgroundLight,
            borderColor = GameColors.BorderFaint
        ) {
            Text(
                text = location.description,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Travel to location button (if not current scenario)
        DecisionButton(
            text = "Travel to ${location.name}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            gameViewModel.travelToInteractiveLocation(location)
            onBack()
        }
        
        // Activities section
        if (availableActivities.isNotEmpty()) {
            Text(
                text = "Available Activities:",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(availableActivities) { activity ->
                    ActivityCard(
                        activity = activity,
                        onStartActivity = { onStartActivity(activity.id) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activities currently available at this location.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Individual activity card
 */
@Composable
fun ActivityCard(
    activity: LocationActivity,
    onStartActivity: () -> Unit
) {
    GameCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundColor = getActivityTypeColor(activity.type).copy(alpha = 0.3f),
        borderColor = getActivityTypeColor(activity.type),
        contentPadding = 12.dp
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activity.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // Activity type indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = getActivityTypeColor(activity.type),
                            shape = androidx.compose.material3.MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = getActivityTypeEmoji(activity.type),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Activity details
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Difficulty: ${activity.difficulty}/5",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = "~${activity.estimatedDuration} min",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            // Start activity button
            Spacer(modifier = Modifier.height(8.dp))
            DecisionButton(
                text = "Start Activity",
                modifier = Modifier.fillMaxWidth()
            ) {
                onStartActivity()
            }
    }
}

/**
 * Get color for activity type
 */
fun getActivityTypeColor(type: ActivityType): Color {
    return when (type) {
        ActivityType.MINIGAME -> Color.Cyan
        ActivityType.PUZZLE -> Color.Magenta
        ActivityType.NPC_INTERACTION -> Color.Green
        ActivityType.EXPLORATION -> Color.Yellow
        ActivityType.TRADING -> Color.Blue
        ActivityType.INVESTIGATION -> Color.Red
        ActivityType.CRAFTING -> GameColors.Orange
        ActivityType.COMBAT -> Color(0xFF8B0000) // Dark Red
    }
}

/**
 * Get emoji for activity type
 */
fun getActivityTypeEmoji(type: ActivityType): String {
    return when (type) {
        ActivityType.MINIGAME -> "🎮"
        ActivityType.PUZZLE -> "🧩"
        ActivityType.NPC_INTERACTION -> "💬"
        ActivityType.EXPLORATION -> "🔍"
        ActivityType.TRADING -> "💰"
        ActivityType.INVESTIGATION -> "🕵"
        ActivityType.CRAFTING -> "🔨"
        ActivityType.COMBAT -> "⚔️"
    }
} 