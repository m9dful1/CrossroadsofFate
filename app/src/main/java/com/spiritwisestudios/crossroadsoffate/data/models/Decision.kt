package com.spiritwisestudios.crossroadsoffate.data.models

/**
 * Represents a choice/decision that player can make in a scenario.
 * Each decision can have conditions and leads to another scenario.
 */
data class Decision(
    val text: String,
    val fallbackText: String? = null,
    val condition: Condition? = null,
    val leadsTo: LeadsTo
)

/**
 * Represents a condition that must be met for a decision.
 * Usually involves having a specific item in inventory.
 */
data class Condition(
    val requiredItem: String? = null,
    val removeOnUse: Boolean = false,
    val requiredStat: String? = null,
    val minStatValue: Int? = null,
    val requiredReputation: String? = null,
    val minReputationValue: Int? = null
) {
    fun isMet(inventory: Set<String>, stats: Map<String, Int>, reputation: Map<String, Int> = emptyMap()): Boolean {
        val itemMet = requiredItem?.let { it in inventory } ?: true
        val statMet = requiredStat?.let { stat ->
            minStatValue?.let { min -> (stats[stat] ?: 0) >= min } ?: true
        } ?: true
        val repMet = requiredReputation?.let { faction ->
            minReputationValue?.let { min -> (reputation[faction] ?: 0) >= min } ?: true
        } ?: true
        return itemMet && statMet && repMet
    }
}

/**
 * Sealed class representing different types of scenario transitions.
 * Can be either Simple (direct to next scenario) or Conditional (based on conditions).
 */
sealed class LeadsTo {
    data class Simple(val scenarioId: String) : LeadsTo()
    data class Conditional(
        val ifConditionMet: String,
        val ifConditionNotMet: String
    ) : LeadsTo()
}
