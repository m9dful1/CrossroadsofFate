package com.spiritwisestudios.crossroadsoffate.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the player's character stats (strength, charisma, cunning, wisdom).
 * Follows the same pattern as InventoryManager.
 */
class StatsManager {
    private val _stats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val stats: StateFlow<Map<String, Int>> = _stats.asStateFlow()

    companion object {
        val DEFAULT_STATS = mapOf(
            "strength" to 1, "charisma" to 1, "cunning" to 1, "wisdom" to 1
        )
    }

    fun initialize(initialStats: Map<String, Int>) {
        _stats.value = DEFAULT_STATS + initialStats
    }

    fun addStat(stat: String, amount: Int) {
        val current = _stats.value.toMutableMap()
        current[stat] = maxOf(0, (current[stat] ?: 0) + amount)
        _stats.value = current.toMap()
    }

    fun getStat(stat: String): Int = _stats.value[stat] ?: 0

    fun getStatsMap(): Map<String, Int> = _stats.value
}
