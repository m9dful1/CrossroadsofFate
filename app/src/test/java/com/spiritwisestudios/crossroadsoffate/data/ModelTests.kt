package com.spiritwisestudios.crossroadsoffate.data

import com.google.gson.GsonBuilder
import com.spiritwisestudios.crossroadsoffate.data.models.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ScenarioEntityTest {
    @Test
    fun testDecisionDeserialization() {
        // Test JSON to Decision conversion
        val jsonString = """
            {
                "text": "Test decision",
                "fallbackText": "Test fallback",
                "condition": {
                    "requiredItem": "key",
                    "removeOnUse": true
                },
                "leadsTo": {
                    "type": "simple",
                    "scenarioId": "next_scenario"
                }
            }
        """.trimIndent()
        
        val gson = GsonBuilder()
            .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
            .create()
            
        val decision = gson.fromJson(jsonString, Decision::class.java)
        
        assertEquals("Test decision", decision.text)
        assertEquals("Test fallback", decision.fallbackText)
        assertEquals("key", decision.condition?.requiredItem)
        assertTrue(decision.condition?.removeOnUse == true)
        assertTrue(decision.leadsTo is LeadsTo.Simple)
        assertEquals("next_scenario", (decision.leadsTo as LeadsTo.Simple).scenarioId)
    }
    
    @Test
    fun testConditionalDecisionDeserialization() {
        // Test JSON to conditional Decision conversion
        val jsonString = """
            {
                "text": "Test decision",
                "fallbackText": null,
                "condition": {
                    "requiredItem": "key",
                    "removeOnUse": false
                },
                "leadsTo": {
                    "type": "conditional",
                    "ifConditionMet": "success_scenario",
                    "ifConditionNotMet": "failure_scenario"
                }
            }
        """.trimIndent()
        
        val gson = GsonBuilder()
            .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
            .create()
            
        val decision = gson.fromJson(jsonString, Decision::class.java)
        
        assertEquals("Test decision", decision.text)
        assertNull(decision.fallbackText)
        assertEquals("key", decision.condition?.requiredItem)
        assertFalse(decision.condition?.removeOnUse == true)
        assertTrue(decision.leadsTo is LeadsTo.Conditional)
        
        val conditional = decision.leadsTo as LeadsTo.Conditional
        assertEquals("success_scenario", conditional.ifConditionMet)
        assertEquals("failure_scenario", conditional.ifConditionNotMet)
    }
}

class PlayerProgressTest {
    @Test
    fun testQuestConverters() {
        // Test quest list serialization/deserialization
        val converters = QuestConverters()
        
        val quests = listOf(
            Quest(
                id = "quest1",
                title = "Test Quest",
                description = "Test description",
                objectives = listOf(
                    QuestObjective(
                        id = "obj1",
                        description = "Test objective",
                        isCompleted = true
                    )
                ),
                isCompleted = false
            )
        )
        
        val json = converters.fromQuests(quests)
        val restored = converters.toQuests(json)
        
        assertEquals(1, restored.size)
        assertEquals("quest1", restored[0].id)
        assertEquals("Test Quest", restored[0].title)
        assertEquals(1, restored[0].objectives.size)
        assertEquals("obj1", restored[0].objectives[0].id)
        assertTrue(restored[0].objectives[0].isCompleted)
    }
    
    @Test
    fun testInventoryConverters() {
        // Test inventory serialization/deserialization
        val converters = InventoryConverters()
        
        val inventory = listOf("item1", "item2", "item3")
        
        val json = converters.fromInventory(inventory)
        val restored = converters.toInventory(json)
        
        assertEquals(3, restored.size)
        assertTrue(restored.contains("item1"))
        assertTrue(restored.contains("item2"))
        assertTrue(restored.contains("item3"))
    }
    
    @Test
    fun testVisitedLocationsConverter() {
        // Test visited locations serialization/deserialization
        val converters = VisitedLocationsConverter()
        
        val locations = setOf("location1", "location2")
        
        val json = converters.fromSet(locations)
        val restored = converters.toSet(json)
        
        assertEquals(2, restored.size)
        assertTrue(restored.contains("location1"))
        assertTrue(restored.contains("location2"))
    }
} 