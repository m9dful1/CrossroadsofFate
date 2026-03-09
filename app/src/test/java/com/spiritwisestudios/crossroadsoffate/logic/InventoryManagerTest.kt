package com.spiritwisestudios.crossroadsoffate.logic

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class InventoryManagerTest {

    private lateinit var inventoryManager: InventoryManager

    @Before
    fun setup() {
        inventoryManager = InventoryManager()
    }

    @Test
    fun initialize_setsInventoryCorrectly() = runBlocking {
        val initialItems = listOf("sword", "potion")
        inventoryManager.initialize(initialItems)
        assertEquals(initialItems.toSet(), inventoryManager.inventory.first())
    }

    @Test
    fun addItem_addsNewItem() = runBlocking {
        inventoryManager.addItem("shield")
        assertTrue(inventoryManager.hasItem("shield"))
        assertEquals(1, inventoryManager.getInventoryList().size)
    }

    @Test
    fun addItem_doesNotAddDuplicate() = runBlocking {
        inventoryManager.addItem("shield")
        inventoryManager.addItem("shield")
        assertEquals(1, inventoryManager.getInventoryList().size)
    }

    @Test
    fun addItem_doesNotAddBlankItem() = runBlocking {
        inventoryManager.addItem("  ")
        assertTrue(inventoryManager.getInventoryList().isEmpty())
    }

    @Test
    fun removeItem_removesExistingItem() = runBlocking {
        inventoryManager.initialize(listOf("sword", "potion"))
        inventoryManager.removeItem("sword")
        assertFalse(inventoryManager.hasItem("sword"))
        assertTrue(inventoryManager.hasItem("potion"))
    }

    @Test
    fun removeItem_doesNothingForNonexistentItem() = runBlocking {
        inventoryManager.initialize(listOf("sword"))
        inventoryManager.removeItem("shield")
        assertEquals(1, inventoryManager.getInventoryList().size)
        assertTrue(inventoryManager.hasItem("sword"))
    }

    @Test
    fun hasItem_returnsCorrectBoolean() {
        inventoryManager.initialize(listOf("key"))
        assertTrue(inventoryManager.hasItem("key"))
        assertFalse(inventoryManager.hasItem("lockpick"))
    }

    @Test
    fun getInventoryList_returnsCorrectList() {
        val initialItems = listOf("apple", "banana", "cherry")
        inventoryManager.initialize(initialItems)
        assertEquals(initialItems.sorted(), inventoryManager.getInventoryList().sorted())
    }
} 