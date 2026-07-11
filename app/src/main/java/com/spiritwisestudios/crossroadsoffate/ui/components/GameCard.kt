package com.spiritwisestudios.crossroadsoffate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors

/**
 * Standard translucent panel used across screens: rounded background,
 * 1dp border, and padded column content.
 *
 * @param modifier Layout modifier for the card itself (width, outer padding)
 * @param backgroundColor Panel fill
 * @param borderColor Panel border
 * @param contentPadding Inner padding around the content column
 */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GameColors.PanelBackground,
    borderColor: Color = GameColors.Border,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
            .padding(contentPadding),
        content = content
    )
}
