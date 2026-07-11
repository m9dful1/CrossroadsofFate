package com.spiritwisestudios.crossroadsoffate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors

/**
 * Full-screen modal used for mini-game results and quest rewards:
 * dark scrim, centered bordered panel, and a dismiss button at the bottom.
 *
 * @param accentColor Border color signalling the outcome (success green, gold, etc.)
 * @param dismissText Label of the dismiss button
 * @param onDismiss Invoked when the dismiss button is tapped
 * @param content Panel body, rendered between the scrim and the dismiss button
 */
@Composable
fun ResultDialog(
    accentColor: Color,
    dismissText: String = "Continue",
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.OverlayScrim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(
                    color = GameColors.DialogBackground,
                    shape = MaterialTheme.shapes.large
                )
                .border(
                    width = 2.dp,
                    color = accentColor,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()

            Spacer(modifier = Modifier.height(24.dp))

            DecisionButton(
                text = dismissText,
                modifier = Modifier.fillMaxWidth()
            ) {
                onDismiss()
            }
        }
    }
}

/**
 * Labeled list of reward/consequence lines inside a [ResultDialog].
 */
@Composable
fun RewardList(label: String, labelColor: Color, items: List<String>, prefix: String = "+") {
    Text(
        text = label,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = labelColor
    )
    items.forEach { item ->
        Text(
            text = "$prefix $item",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}
