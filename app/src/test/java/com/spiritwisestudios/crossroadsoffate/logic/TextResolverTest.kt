package com.spiritwisestudios.crossroadsoffate.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TextResolverTest {

    private val emptyInventory = emptySet<String>()
    private val emptyStats = emptyMap<String, Int>()
    private val emptyRep = emptyMap<String, Int>()

    @Test
    fun `plain text passes through unchanged`() {
        val text = "A wise mentor challenges you."
        assertEquals(text, TextResolver.resolve(text, emptyInventory, emptyStats, emptyRep))
    }

    @Test
    fun `empty string returns empty`() {
        assertEquals("", TextResolver.resolve("", emptyInventory, emptyStats, emptyRep))
    }

    // --- Item tokens ---

    @Test
    fun `item token replaced with formatted name when in inventory`() {
        val result = TextResolver.resolve(
            "Your {item:holy_key} glows.",
            setOf("holy_key"), emptyStats, emptyRep
        )
        assertEquals("Your Holy Key glows.", result)
    }

    @Test
    fun `item token replaced with empty string when not in inventory`() {
        val result = TextResolver.resolve(
            "Your {item:holy_key} glows.",
            emptyInventory, emptyStats, emptyRep
        )
        assertEquals("Your  glows.", result)
    }

    // --- Stat value tokens ---

    @Test
    fun `stat value token replaced with numeric value`() {
        val result = TextResolver.resolve(
            "Strength at {stat:strength}.",
            emptyInventory, mapOf("strength" to 3), emptyRep
        )
        assertEquals("Strength at 3.", result)
    }

    @Test
    fun `stat value defaults to 0 when missing`() {
        val result = TextResolver.resolve(
            "Strength at {stat:strength}.",
            emptyInventory, emptyStats, emptyRep
        )
        assertEquals("Strength at 0.", result)
    }

    // --- Rep value tokens ---

    @Test
    fun `rep value token replaced with numeric value`() {
        val result = TextResolver.resolve(
            "Guard rep: {rep:guard}.",
            emptyInventory, emptyStats, mapOf("guard" to 2)
        )
        assertEquals("Guard rep: 2.", result)
    }

    // --- Stat conditional ---

    @Test
    fun `stat conditional shows below text when below threshold`() {
        val result = TextResolver.resolve(
            "{stat:wisdom:3:You have much to learn.|You are wise.}",
            emptyInventory, mapOf("wisdom" to 2), emptyRep
        )
        assertEquals("You have much to learn.", result)
    }

    @Test
    fun `stat conditional shows above text when at threshold`() {
        val result = TextResolver.resolve(
            "{stat:wisdom:3:You have much to learn.|You are wise.}",
            emptyInventory, mapOf("wisdom" to 3), emptyRep
        )
        assertEquals("You are wise.", result)
    }

    @Test
    fun `stat conditional shows above text when above threshold`() {
        val result = TextResolver.resolve(
            "{stat:wisdom:3:You have much to learn.|You are wise.}",
            emptyInventory, mapOf("wisdom" to 5), emptyRep
        )
        assertEquals("You are wise.", result)
    }

    // --- Rep conditional ---

    @Test
    fun `rep conditional shows below text when below threshold`() {
        val result = TextResolver.resolve(
            "{rep:guard:2:They ignore you.|They salute.}",
            emptyInventory, emptyStats, mapOf("guard" to 1)
        )
        assertEquals("They ignore you.", result)
    }

    @Test
    fun `rep conditional shows above text when at threshold`() {
        val result = TextResolver.resolve(
            "{rep:guard:2:They ignore you.|They salute.}",
            emptyInventory, emptyStats, mapOf("guard" to 2)
        )
        assertEquals("They salute.", result)
    }

    // --- Conditional blocks ---

    @Test
    fun `conditional block shown when player has item`() {
        val result = TextResolver.resolve(
            "Door.{if:has:torch} It is lit.{/if}",
            setOf("torch"), emptyStats, emptyRep
        )
        assertEquals("Door. It is lit.", result)
    }

    @Test
    fun `conditional block hidden when player lacks item`() {
        val result = TextResolver.resolve(
            "Door.{if:has:torch} It is lit.{/if}",
            emptyInventory, emptyStats, emptyRep
        )
        assertEquals("Door.", result)
    }

    @Test
    fun `negated conditional block shown when player lacks item`() {
        val result = TextResolver.resolve(
            "{if:!has:torch}It is dark.{/if}",
            emptyInventory, emptyStats, emptyRep
        )
        assertEquals("It is dark.", result)
    }

    @Test
    fun `negated conditional block hidden when player has item`() {
        val result = TextResolver.resolve(
            "{if:!has:torch}It is dark.{/if}",
            setOf("torch"), emptyStats, emptyRep
        )
        assertEquals("", result)
    }

    // --- Nested tokens in conditional blocks ---

    @Test
    fun `tokens inside conditional blocks are resolved`() {
        val result = TextResolver.resolve(
            "{if:has:key}Your {item:key} glows.{/if}",
            setOf("key"), emptyStats, emptyRep
        )
        assertEquals("Your Key glows.", result)
    }

    // --- Nested conditional blocks ---

    private val nested = "{if:has:map}You unfold the map.{if:has:compass} The compass agrees.{/if} Onward.{/if}"

    @Test
    fun `nested block shows inner content only when both items held`() {
        val result = TextResolver.resolve(nested, setOf("map", "compass"), emptyStats, emptyRep)
        assertEquals("You unfold the map. The compass agrees. Onward.", result)
    }

    @Test
    fun `nested block hides inner content when only outer item held`() {
        val result = TextResolver.resolve(nested, setOf("map"), emptyStats, emptyRep)
        assertEquals("You unfold the map. Onward.", result)
    }

    @Test
    fun `nested block hides everything when outer item missing`() {
        // Includes text after the inner {/if}: with naive pairing the outer
        // open tag pairs with the inner close and " Onward." leaks through
        val result = TextResolver.resolve(nested, setOf("compass"), emptyStats, emptyRep)
        assertEquals("", result)
    }

    // --- Threshold token edge cases ---

    @Test
    fun `negative rep threshold selects branches correctly`() {
        val token = "{rep:guard:-1:They draw weapons.|They let you pass.}"
        assertEquals("They draw weapons.",
            TextResolver.resolve(token, emptyInventory, emptyStats, mapOf("guard" to -2)))
        assertEquals("They let you pass.",
            TextResolver.resolve(token, emptyInventory, emptyStats, mapOf("guard" to -1)))
    }

    @Test
    fun `empty below branch renders nothing under threshold`() {
        val token = "{stat:wisdom:3:|The runes make sense to you.}"
        assertEquals("",
            TextResolver.resolve(token, emptyInventory, mapOf("wisdom" to 1), emptyRep))
        assertEquals("The runes make sense to you.",
            TextResolver.resolve(token, emptyInventory, mapOf("wisdom" to 3), emptyRep))
    }

    @Test
    fun `empty above branch renders nothing at threshold`() {
        val token = "{stat:wisdom:3:You have much to learn.|}"
        assertEquals("You have much to learn.",
            TextResolver.resolve(token, emptyInventory, mapOf("wisdom" to 1), emptyRep))
        assertEquals("",
            TextResolver.resolve(token, emptyInventory, mapOf("wisdom" to 5), emptyRep))
    }

    @Test
    fun `malformed threshold token is stripped instead of rendering zero`() {
        // Missing the below|above separator: not a valid conditional, and it
        // must not be misread as a value token that prints "0" mid-sentence
        val result = TextResolver.resolve(
            "The elder speaks.{stat:wisdom:3 nonsense} Farewell.",
            emptyInventory, mapOf("wisdom" to 5), emptyRep
        )
        assertEquals("The elder speaks. Farewell.", result)
    }

    // --- Multiple tokens ---

    @Test
    fun `multiple tokens in one string all resolve`() {
        val result = TextResolver.resolve(
            "STR {stat:strength}, WIS {stat:wisdom}. Guard rep: {rep:guard}.",
            emptyInventory, mapOf("strength" to 3, "wisdom" to 5), mapOf("guard" to 1)
        )
        assertEquals("STR 3, WIS 5. Guard rep: 1.", result)
    }

    // --- Unknown tokens ---

    @Test
    fun `unknown tokens are stripped`() {
        val result = TextResolver.resolve(
            "Hello {unknown:foo} world.",
            emptyInventory, emptyStats, emptyRep
        )
        assertEquals("Hello  world.", result)
    }

    // --- formatItemName ---

    @Test
    fun `formatItemName converts underscores to title case`() {
        assertEquals("Holy Key", TextResolver.formatItemName("holy_key"))
        assertEquals("Infernal Mark", TextResolver.formatItemName("infernal_mark"))
        assertEquals("Sword", TextResolver.formatItemName("sword"))
    }
}
