package com.spiritwisestudios.crossroadsoffate.logic

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class StatsManagerTest {

    private lateinit var statsManager: StatsManager

    @Before
    fun setup() {
        statsManager = StatsManager()
    }

    @Test
    fun initialize_setsStatsCorrectly() = runBlocking {
        val savedStats = mapOf("strength" to 5, "wisdom" to 3)
        statsManager.initialize(savedStats)
        val stats = statsManager.stats.first()
        assertEquals(5, stats["strength"])
        assertEquals(3, stats["wisdom"])
        // Defaults still present for unset stats
        assertEquals(1, stats["charisma"])
        assertEquals(1, stats["cunning"])
    }

    @Test
    fun initialize_withEmptyMap_usesDefaults() = runBlocking {
        statsManager.initialize(emptyMap())
        val stats = statsManager.stats.first()
        assertEquals(StatsManager.DEFAULT_STATS, stats)
    }

    @Test
    fun addStat_incrementsExistingStat() {
        statsManager.initialize(emptyMap())
        statsManager.addStat("strength", 2)
        assertEquals(3, statsManager.getStat("strength")) // 1 default + 2
    }

    @Test
    fun addStat_handlesNewStat() {
        statsManager.initialize(emptyMap())
        statsManager.addStat("luck", 5)
        assertEquals(5, statsManager.getStat("luck"))
    }

    @Test
    fun getStat_returnsZeroForUnknownStat() {
        statsManager.initialize(emptyMap())
        assertEquals(0, statsManager.getStat("nonexistent"))
    }

    @Test
    fun meetsRequirement_returnsCorrectBoolean() {
        statsManager.initialize(mapOf("strength" to 5))
        assertTrue(statsManager.meetsRequirement("strength", 3))
        assertTrue(statsManager.meetsRequirement("strength", 5))
        assertFalse(statsManager.meetsRequirement("strength", 6))
        assertFalse(statsManager.meetsRequirement("nonexistent", 1))
    }

    @Test
    fun getStatsMap_returnsCurrentState() {
        statsManager.initialize(mapOf("cunning" to 7))
        val map = statsManager.getStatsMap()
        assertEquals(7, map["cunning"])
        assertEquals(1, map["strength"])
        assertEquals(1, map["charisma"])
        assertEquals(1, map["wisdom"])
    }

    @Test
    fun addStat_withNegativeAmount_clampsToZero() {
        statsManager.initialize(mapOf("strength" to 2))
        statsManager.addStat("strength", -5)
        assertEquals(0, statsManager.getStat("strength"))
    }

    @Test
    fun addStat_withZeroAmount_doesNotChange() {
        statsManager.initialize(emptyMap())
        statsManager.addStat("strength", 0)
        assertEquals(1, statsManager.getStat("strength"))
    }

    @Test
    fun initialize_calledTwice_replacesState() {
        statsManager.initialize(mapOf("strength" to 10))
        statsManager.initialize(mapOf("wisdom" to 5))
        assertEquals(5, statsManager.getStat("wisdom"))
        assertEquals(1, statsManager.getStat("strength")) // reset to default, not 10
    }
}
