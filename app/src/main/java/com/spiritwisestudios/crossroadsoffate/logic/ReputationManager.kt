package com.spiritwisestudios.crossroadsoffate.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the player's faction reputation (guard, merchant, scholar, underworld).
 * Follows the same pattern as StatsManager.
 */
class ReputationManager {
    private val _reputation = MutableStateFlow<Map<String, Int>>(emptyMap())
    val reputation: StateFlow<Map<String, Int>> = _reputation.asStateFlow()

    companion object {
        val DEFAULT_REPUTATION = mapOf(
            "guard" to 0, "merchant" to 0, "scholar" to 0, "underworld" to 0
        )
    }

    fun initialize(initialReputation: Map<String, Int>) {
        _reputation.value = DEFAULT_REPUTATION + initialReputation
    }

    fun adjustReputation(faction: String, amount: Int) {
        val current = _reputation.value.toMutableMap()
        current[faction] = (current[faction] ?: 0) + amount
        _reputation.value = current.toMap()
    }

    fun getReputation(faction: String): Int = _reputation.value[faction] ?: 0

    fun getReputationMap(): Map<String, Int> = _reputation.value
}
