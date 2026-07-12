package com.spiritwisestudios.crossroadsoffate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel

/**
 * Shown when the current scenario is flagged isEnding: the closing text plus a
 * short summary of the journey and a way back to the title screen.
 */
@Composable
fun EndingScreen(gameViewModel: GameViewModel) {
    val display by gameViewModel.scenarioDisplay.collectAsState()
    val stats by gameViewModel.playerStats.collectAsState()
    val completedQuests by gameViewModel.completedQuests.collectAsState()
    val ending = display ?: return

    // System back returns to the title like the on-screen button
    BackHandler { gameViewModel.returnToTitle() }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterForName(ending.backgroundImage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Darker overlay than the main screen: the ending text is the focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "The End",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = ending.locationName,
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = ending.resolvedText,
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            Text(
                text = stats.entries.joinToString("   ") { (stat, value) ->
                    "${stat.replaceFirstChar { it.uppercase() }} $value"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Quests completed: ${completedQuests.size}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 8.dp)
            )
            DecisionButton(
                text = "Return to Title",
                modifier = Modifier.padding(top = 32.dp)
            ) { gameViewModel.returnToTitle() }
        }
    }
}
