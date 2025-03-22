package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spiritwisestudios.crossroadsoffate.ui.components.DecisionButton

data class MapLocation(val name: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(locations: List<MapLocation>, onBack: () -> Unit, onLocationSelected: (MapLocation) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Map", color = Color.White) },
                navigationIcon = {
                    DecisionButton(
                        text = "Back",
                        modifier = Modifier.width(80.dp)
                    ) { onBack() }
        },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.DarkGray.copy(alpha = 0.7f),
                )
            )
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(locations) { location ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                   ){
                    DecisionButton(
                        text = location.name,
                        modifier = Modifier.fillMaxWidth()
                    )   {
                        onLocationSelected(location)
                        }
                    }
                }
            }
        }
    }
}
