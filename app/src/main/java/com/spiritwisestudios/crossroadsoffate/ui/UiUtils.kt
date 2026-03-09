package com.spiritwisestudios.crossroadsoffate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.spiritwisestudios.crossroadsoffate.R

/**
 * A utility composable that dynamically resolves a drawable resource by its name.
 * This decouples the UI from hardcoded resource IDs.
 *
 * @param name The name of the drawable resource (e.g., "town_square").
 * @return A Painter for the resolved drawable, or a default painter if not found.
 */
@Composable
fun painterForName(name: String): Painter {
    val context = LocalContext.current
    val resourceId = remember(name) {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        // If the resource is not found, getIdentifier returns 0. Fallback to default.
        if (id == 0) R.drawable.default_bg else id
    }
    return painterResource(id = resourceId)
} 