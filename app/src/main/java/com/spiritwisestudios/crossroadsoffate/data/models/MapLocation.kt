package com.spiritwisestudios.crossroadsoffate.data.models

/**
 * Data class representing a location on the game map.
 */
data class MapLocation(
    val name: String,
    val description: String,
    val scenarioId: String,
    val isVisited: Boolean = false
)
