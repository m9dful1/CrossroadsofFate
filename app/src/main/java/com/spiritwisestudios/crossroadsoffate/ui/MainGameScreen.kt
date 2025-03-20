package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton
import com.spiritwisestudios.crossroadsoffate.R
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainGameScreen(gameViewModel: GameViewModel) {
    val playerProgress = gameViewModel.playerProgress.collectAsState().value
    val playerInventory = gameViewModel.playerInventory.collectAsState().value
    var currentScenario by remember { mutableStateOf<ScenarioEntity?>(null) }

    LaunchedEffect(playerProgress?.currentScenarioId) {
        playerProgress?.currentScenarioId?.let { scenarioId ->
            gameViewModel.loadScenarioById(scenarioId) { scenario ->
                currentScenario = scenario
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        currentScenario?.let { scenario ->
            val backgroundId = when (scenario.backgroundImage) {
                "hell_bg" -> R.drawable.hell_bg
                "heaven_bg" -> R.drawable.heaven_bg
                else -> R.drawable.default_bg
            }
            Image(
                painter = painterResource(id = backgroundId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = scenario.location, fontSize = 24.sp, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = scenario.text, fontSize = 18.sp, color = Color.White)
            }

            scenario.decisions.forEach { (position, decision) ->
                val displayText = if (decision.condition?.requiredItem in playerInventory) {
                    decision.text
                } else {
                    decision.fallbackText ?: decision.text
                }
                val alignment = when (position) {
                    "topLeft" -> Alignment.TopStart
                    "topRight" -> Alignment.TopEnd
                    "bottomLeft" -> Alignment.BottomStart
                    "bottomRight" -> Alignment.BottomEnd
                    else -> Alignment.Center
                }
                DecisionButton(
                    text = displayText,
                    modifier = Modifier.align(alignment).padding(16.dp)
                ) {
                    gameViewModel.onChoiceSelected(position)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            Button(onClick = { /* TODO: Implement navigation to Character Menu Screen */ }) {
                Text(text = "Character")
            }
        }

        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) {
            Button(onClick = { /* TODO: Implement navigation to Map Submenu Screen */ }) {
                Text(text = "Map")
            }
        }
    }
}