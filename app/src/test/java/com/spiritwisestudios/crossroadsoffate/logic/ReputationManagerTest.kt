package com.spiritwisestudios.crossroadsoffate.logic

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ReputationManagerTest {

    private lateinit var reputationManager: ReputationManager

    @Before
    fun setup() {
        reputationManager = ReputationManager()
    }

    @Test
    fun initialize_setsReputationCorrectly() = runBlocking {
        val savedReputation = mapOf("guard" to 5, "merchant" to -2)
        reputationManager.initialize(savedReputation)
        val rep = reputationManager.reputation.first()
        assertEquals(5, rep["guard"])
        assertEquals(-2, rep["merchant"])
        // Defaults still present for unset factions
        assertEquals(0, rep["scholar"])
        assertEquals(0, rep["underworld"])
    }

    @Test
    fun initialize_withEmptyMap_usesDefaults() = runBlocking {
        reputationManager.initialize(emptyMap())
        val rep = reputationManager.reputation.first()
        assertEquals(ReputationManager.DEFAULT_REPUTATION, rep)
    }

    @Test
    fun adjustReputation_incrementsExisting() {
        reputationManager.initialize(emptyMap())
        reputationManager.adjustReputation("guard", 3)
        assertEquals(3, reputationManager.getReputation("guard"))
    }

    @Test
    fun adjustReputation_handlesNegativeAmount() {
        reputationManager.initialize(mapOf("guard" to 2))
        reputationManager.adjustReputation("guard", -3)
        assertEquals(-1, reputationManager.getReputation("guard"))
    }

    @Test
    fun adjustReputation_handlesNewFaction() {
        reputationManager.initialize(emptyMap())
        reputationManager.adjustReputation("thieves_guild", 5)
        assertEquals(5, reputationManager.getReputation("thieves_guild"))
    }

    @Test
    fun getReputation_returnsZeroForUnknownFaction() {
        reputationManager.initialize(emptyMap())
        assertEquals(0, reputationManager.getReputation("nonexistent"))
    }

    @Test
    fun getReputationMap_returnsCurrentState() {
        reputationManager.initialize(mapOf("scholar" to 7))
        val map = reputationManager.getReputationMap()
        assertEquals(7, map["scholar"])
        assertEquals(0, map["guard"])
        assertEquals(0, map["merchant"])
        assertEquals(0, map["underworld"])
    }

    @Test
    fun initialize_calledTwice_replacesState() {
        reputationManager.initialize(mapOf("guard" to 10))
        reputationManager.initialize(mapOf("merchant" to 5))
        assertEquals(5, reputationManager.getReputation("merchant"))
        assertEquals(0, reputationManager.getReputation("guard")) // reset to default
    }
}
