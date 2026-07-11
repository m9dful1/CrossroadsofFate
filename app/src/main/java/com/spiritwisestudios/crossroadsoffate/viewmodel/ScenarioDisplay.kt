package com.spiritwisestudios.crossroadsoffate.viewmodel

/**
 * Presentation model for the main game screen: scenario text and decision
 * labels fully resolved against the player's inventory/stats/reputation,
 * so the UI layer renders without evaluating game rules.
 */
data class ScenarioDisplay(
    val locationName: String,
    val resolvedText: String,
    val backgroundImage: String,
    val decisions: List<DisplayDecision>
)

/**
 * A single decision button: screen position key and its resolved label
 * (fallback text already substituted when the condition is not met).
 */
data class DisplayDecision(
    val position: String,
    val text: String
)
