package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.R
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton

/**
 * Composable that displays the game's title screen with options to start a new game or load a saved one.
 *
 * @param onNewGame Callback triggered when the New Game button is pressed
 * @param onLoadGame Callback triggered when the Load Game button is pressed
 * @param onShowErrorLogger Callback triggered when the Error Logger button is pressed
 */
@Composable
fun TitleScreen(
    onNewGame: () -> Unit,
    onLoadGame: () -> Unit,
    onShowErrorLogger: () -> Unit
) {
    // Root container filling the entire screen
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.town_crossroads),
            contentDescription = null, // Decorative image, no need for description
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Crop image to fill the entire screen
        )

        // Semi-transparent black overlay to improve text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Main content column containing title and buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game title text
            Text(
                text = "Crossroads\nof Fate",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = 56.sp,
                maxLines = 2
            )

            // Spacing between title and buttons
            Spacer(modifier = Modifier.height(32.dp))

            // New Game button
            DecisionButton(
                text = "New Game",
                modifier = Modifier.padding(vertical = 8.dp)
            ) { onNewGame() }

            // Spacing between buttons
            Spacer(modifier = Modifier.height(16.dp))

            // Load Game button
            DecisionButton(
                text = "Load Game",
                modifier = Modifier.padding(vertical = 8.dp)
            ) { onLoadGame() }
            
            // Spacing between buttons
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error Logger button
            DecisionButton(
                text = "Error Logger",
                modifier = Modifier.padding(vertical = 8.dp)
            ) { onShowErrorLogger() }
        }
    }
}