package com.spiritwisestudios.crossroadsoffate.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors

/**
 * Standard screen header: translucent bar with a Back button and title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text("  $title", color = Color.White) },
        navigationIcon = {
            DecisionButton(
                text = "Back",
                modifier = Modifier.width(80.dp)
            ) { onBack() }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GameColors.PanelBackground
        )
    )
}
