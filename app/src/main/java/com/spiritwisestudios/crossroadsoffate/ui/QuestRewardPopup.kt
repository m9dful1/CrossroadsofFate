package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritwisestudios.crossroadsoffate.data.models.QuestCompletionEvent
import com.spiritwisestudios.crossroadsoffate.ui.components.ResultDialog
import com.spiritwisestudios.crossroadsoffate.ui.components.RewardList
import com.spiritwisestudios.crossroadsoffate.ui.theme.GameColors

@Composable
fun QuestRewardPopup(
    event: QuestCompletionEvent,
    onDismiss: () -> Unit
) {
    ResultDialog(
        accentColor = GameColors.Gold,
        onDismiss = onDismiss
    ) {
        Text(
            text = "Quest Completed!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GameColors.Gold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = event.quest.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        if (event.rewardItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            RewardList(label = "Rewards:", labelColor = Color.Green, items = event.rewardItems)
        }

        if (event.locationsUnlocked.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            RewardList(label = "Locations Unlocked:", labelColor = Color.Cyan, items = event.locationsUnlocked)
        }
    }
}
