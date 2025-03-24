package com.spiritwisestudios.crossroadsoffate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A custom button composable used for player decisions/choices in the game.
 *
 * @param text The text to display on the button
 * @param modifier Optional modifier for customizing the button's layout
 * @param onClick Callback function triggered when button is clicked
 */
@Composable
fun DecisionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        // Apply layout modifiers in specific order for proper layering
        modifier = modifier
            // Add white border with transparency
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            )
            // Add semi-transparent black background
            .background(
                Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            )
            // Make the box clickable
            .clickable { onClick() }
            // Add padding inside the button
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Text content of the button
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.White,
            // Constrain text width between 37dp and 200dp
            modifier = Modifier.widthIn(min = 37.dp, max = 200.dp),
            textAlign = TextAlign.Center
        )
    }
}