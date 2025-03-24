package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton

/**
 * Data class representing a location on the game map
 * @property name The display name of the location
 * @property description A brief description of the location
 * @property scenarioId The ID of the scenario associated with this location
 * @property isVisited Whether the player has previously visited this location
 */
data class MapLocation(
    val name: String,
    val description: String,
    val scenarioId: String,
    val isVisited: Boolean = false
)

/**
 * Composable that displays the map screen with available locations
 * @param locations List of locations that can be visited
 * @param onBack Callback for when the back button is pressed
 * @param onLocationSelected Callback for when a location is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    locations: List<MapLocation>,
    onBack: () -> Unit,
    onLocationSelected: (MapLocation) -> Unit
) {
    // Main container with semi-transparent black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top app bar with title and back button
            TopAppBar(
                title = { Text("  Map", color = Color.White) },
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

            // Scrollable list of visited locations
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(locations) { location ->
                    // Only show locations that have been visited
                    if (location.isVisited) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column {
                                // Location button that triggers navigation when clicked
                                DecisionButton(
                                    text = location.name,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    onLocationSelected(location)
                                }
                                // Location description text
                                Text(
                                    text = location.description,
                                    color = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}