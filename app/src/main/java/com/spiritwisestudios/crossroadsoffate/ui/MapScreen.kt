package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class MapLocation(val name: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(locations: List<MapLocation>, onBack: () -> Unit, onLocationSelected: (MapLocation) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Map") }, navigationIcon = {
            Button(onClick = onBack) { Text("Back") }
        })
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(locations) { location ->
                Row(modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clickable { onLocationSelected(location) }
                ) {
                    Text(text = location.name)
                }
            }
        }
    }
}
