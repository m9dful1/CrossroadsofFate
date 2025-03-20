package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterMenuScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TopAppBar(title = { Text("Character Menu") }, navigationIcon = {
            Button(onClick = onBack) {
                Text("Back")
            }
        })
        // Stats Section
        Text(text = "Stats:")
        Text(text = "Health: 100")
        Text(text = "Experience: 2500")
        Text(text = "Level: 5")
        // Inventory Section
        Text(text = "Inventory:")
        Text(text = "Sword, Shield, Potion")
        // Active Quests Section
        Text(text = "Active Quests:")
        Text(text = "1. Find the Lost Relic")
        Text(text = "2. Rescue the Village Elder")
        // Settings Section
        Text(text = "Settings:")
        Text(text = "Sound: On")
        Text(text = "Display: High Contrast")
    }
}
