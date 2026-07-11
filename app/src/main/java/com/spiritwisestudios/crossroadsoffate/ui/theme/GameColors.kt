package com.spiritwisestudios.crossroadsoffate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared color tokens for the game's dark, panel-based UI.
 * Screens should use these instead of re-deriving the same literals.
 */
object GameColors {
    /** Standard translucent panel background (cards, sections, top bars). */
    val PanelBackground = Color.DarkGray.copy(alpha = 0.7f)

    /** Slightly lighter panel used by map cards. */
    val PanelBackgroundLight = Color.DarkGray.copy(alpha = 0.6f)

    /** Opaque-ish dialog body background. */
    val DialogBackground = Color.DarkGray.copy(alpha = 0.8f)

    /** Full-screen scrim behind overlays and dialogs. */
    val OverlayScrim = Color.Black.copy(alpha = 0.9f)

    /** Standard 1dp panel border. */
    val Border = Color.White.copy(alpha = 0.7f)

    /** Subdued border for inactive elements. */
    val BorderFaint = Color.White.copy(alpha = 0.3f)

    /** Secondary/dimmed text. */
    val TextSecondary = Color.White.copy(alpha = 0.7f)

    /** Accent for quest rewards and celebratory headers. */
    val Gold = Color(0xFFFFD700)

    /** Accent for partial progress. */
    val Orange = Color(0xFFFF8C00)
}
