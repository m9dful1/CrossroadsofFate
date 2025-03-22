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

@Composable
fun DecisionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            )
            .background(
                Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.widthIn(min = 37.dp, max = 200.dp), // Adjust max as needed
            textAlign = TextAlign.Center
        )
    }
}
