package com.spiritwisestudios.crossroadsoffate.logic

/**
 * Stateless utility that resolves dynamic placeholder tokens in scenario text
 * based on player inventory, stats, and reputation.
 */
object TextResolver {

    fun resolve(
        text: String,
        inventory: Set<String>,
        stats: Map<String, Int>,
        reputation: Map<String, Int>
    ): String {
        var result = text
        // 1. Process conditional blocks first (they may contain inner tokens)
        result = resolveConditionalBlocks(result, inventory)
        // 2. Process conditional inline tokens (stat/rep with threshold)
        result = resolveConditionalInlineTokens(result, stats, reputation)
        // 3. Process simple value tokens
        result = resolveValueTokens(result, inventory, stats, reputation)
        // 4. Strip any remaining unrecognized tokens
        result = stripUnrecognizedTokens(result)
        return result
    }

    private val conditionalBlockRegex =
        Regex("""\{if:(!?)has:([^}]+)\}(.*?)\{/if\}""", RegexOption.DOT_MATCHES_ALL)

    private fun resolveConditionalBlocks(text: String, inventory: Set<String>): String {
        return conditionalBlockRegex.replace(text) { match ->
            val negated = match.groupValues[1] == "!"
            val itemName = match.groupValues[2]
            val blockContent = match.groupValues[3]
            val hasItem = itemName in inventory
            val show = if (negated) !hasItem else hasItem
            if (show) blockContent else ""
        }
    }

    private val conditionalStatRegex =
        Regex("""\{stat:([^:]+):(\d+):([^|]+)\|([^}]+)\}""")
    private val conditionalRepRegex =
        Regex("""\{rep:([^:]+):(\d+):([^|]+)\|([^}]+)\}""")

    private fun resolveConditionalInlineTokens(
        text: String,
        stats: Map<String, Int>,
        reputation: Map<String, Int>
    ): String {
        var result = conditionalStatRegex.replace(text) { match ->
            val name = match.groupValues[1]
            val threshold = match.groupValues[2].toInt()
            val belowText = match.groupValues[3]
            val aboveText = match.groupValues[4]
            val value = stats[name] ?: 0
            if (value < threshold) belowText else aboveText
        }
        result = conditionalRepRegex.replace(result) { match ->
            val name = match.groupValues[1]
            val threshold = match.groupValues[2].toInt()
            val belowText = match.groupValues[3]
            val aboveText = match.groupValues[4]
            val value = reputation[name] ?: 0
            if (value < threshold) belowText else aboveText
        }
        return result
    }

    private val itemTokenRegex = Regex("""\{item:([^}]+)\}""")
    private val simpleStatRegex = Regex("""\{stat:([^}]+)\}""")
    private val simpleRepRegex = Regex("""\{rep:([^}]+)\}""")

    private fun resolveValueTokens(
        text: String,
        inventory: Set<String>,
        stats: Map<String, Int>,
        reputation: Map<String, Int>
    ): String {
        var result = itemTokenRegex.replace(text) { match ->
            val itemName = match.groupValues[1]
            if (itemName in inventory) formatItemName(itemName) else ""
        }
        result = simpleStatRegex.replace(result) { match ->
            val name = match.groupValues[1]
            (stats[name] ?: 0).toString()
        }
        result = simpleRepRegex.replace(result) { match ->
            val name = match.groupValues[1]
            (reputation[name] ?: 0).toString()
        }
        return result
    }

    private val unrecognizedTokenRegex = Regex("""\{[^}]*\}""")

    private fun stripUnrecognizedTokens(text: String): String {
        return unrecognizedTokenRegex.replace(text, "")
    }

    internal fun formatItemName(name: String): String {
        return name.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
    }
}
