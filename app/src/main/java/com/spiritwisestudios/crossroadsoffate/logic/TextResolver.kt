package com.spiritwisestudios.crossroadsoffate.logic

import timber.log.Timber

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

    // Content may not contain another opening {if: tag, so this only matches
    // INNERMOST blocks; resolveConditionalBlocks loops until none remain,
    // which pairs nested blocks with their own {/if} instead of the first one
    private val conditionalBlockRegex =
        Regex("""\{if:(!?)has:([^}]+)\}((?:(?!\{if:).)*?)\{/if\}""", RegexOption.DOT_MATCHES_ALL)

    private fun resolveConditionalBlocks(text: String, inventory: Set<String>): String {
        var result = text
        while (true) {
            val replaced = conditionalBlockRegex.replace(result) { match ->
                val negated = match.groupValues[1] == "!"
                val itemName = match.groupValues[2]
                val blockContent = match.groupValues[3]
                val hasItem = itemName in inventory
                val show = if (negated) !hasItem else hasItem
                if (show) blockContent else ""
            }
            if (replaced == result) return replaced
            result = replaced
        }
    }

    // Threshold may be negative (reputation goes below zero); either branch
    // text may be empty ("say nothing below the threshold" is valid authoring)
    private val conditionalStatRegex =
        Regex("""\{stat:([^:}|]+):(-?\d+):([^|}]*)\|([^}]*)\}""")
    private val conditionalRepRegex =
        Regex("""\{rep:([^:}|]+):(-?\d+):([^|}]*)\|([^}]*)\}""")

    private fun resolveConditionalInlineTokens(
        text: String,
        stats: Map<String, Int>,
        reputation: Map<String, Int>
    ): String {
        val statsResolved = replaceThresholdTokens(text, conditionalStatRegex, stats)
        return replaceThresholdTokens(statsResolved, conditionalRepRegex, reputation)
    }

    /** Replaces `{prefix:name:threshold:below|above}` tokens using [values]. */
    private fun replaceThresholdTokens(text: String, regex: Regex, values: Map<String, Int>): String =
        regex.replace(text) { match ->
            val name = match.groupValues[1]
            val threshold = match.groupValues[2].toInt()
            val belowText = match.groupValues[3]
            val aboveText = match.groupValues[4]
            if ((values[name] ?: 0) < threshold) belowText else aboveText
        }

    private val itemTokenRegex = Regex("""\{item:([^}]+)\}""")

    // Bare names only (no ':' or '|'): a malformed conditional token must fall
    // through to the strip stage, not read as a value token and render "0"
    private val simpleStatRegex = Regex("""\{stat:([^:}|]+)\}""")
    private val simpleRepRegex = Regex("""\{rep:([^:}|]+)\}""")

    private fun resolveValueTokens(
        text: String,
        inventory: Set<String>,
        stats: Map<String, Int>,
        reputation: Map<String, Int>
    ): String {
        val itemsResolved = itemTokenRegex.replace(text) { match ->
            val itemName = match.groupValues[1]
            if (itemName in inventory) formatItemName(itemName) else ""
        }
        val statsResolved = replaceValueTokens(itemsResolved, simpleStatRegex, stats)
        return replaceValueTokens(statsResolved, simpleRepRegex, reputation)
    }

    /** Replaces `{prefix:name}` tokens with the numeric value from [values] (0 if absent). */
    private fun replaceValueTokens(text: String, regex: Regex, values: Map<String, Int>): String =
        regex.replace(text) { match -> (values[match.groupValues[1]] ?: 0).toString() }

    private val unrecognizedTokenRegex = Regex("""\{[^}]*\}""")

    private fun stripUnrecognizedTokens(text: String): String {
        return unrecognizedTokenRegex.replace(text) { match ->
            Timber.w("Stripping unrecognized text token: %s", match.value)
            ""
        }
    }

    internal fun formatItemName(name: String): String {
        return name.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
    }
}
