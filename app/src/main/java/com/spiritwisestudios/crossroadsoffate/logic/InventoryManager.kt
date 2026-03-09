package com.spiritwisestudios.crossroadsoffate.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the player's inventory, including adding, removing, and checking for items.
 * This class encapsulates all inventory-related business logic.
 */
class InventoryManager {
    private val _inventory = MutableStateFlow<Set<String>>(emptySet())
    val inventory: StateFlow<Set<String>> = _inventory.asStateFlow()

    /**
     * Initializes the inventory with a list of items, typically from saved game progress.
     * @param initialInventory The initial list of items.
     */
    fun initialize(initialInventory: List<String>) {
        _inventory.value = initialInventory.toSet()
    }

    /**
     * Adds an item to the inventory if it doesn't already exist.
     * @param item The item to add.
     */
    fun addItem(item: String) {
        if (item.isNotBlank()) {
            _inventory.value = _inventory.value + item
        }
    }

    /**
     * Removes an item from the inventory.
     * @param item The item to remove.
     */
    fun removeItem(item: String) {
        _inventory.value = _inventory.value - item
    }

    /**
     * Checks if the inventory contains a specific item.
     * @param item The item to check for.
     * @return True if the item exists, false otherwise.
     */
    fun hasItem(item: String): Boolean {
        return item in _inventory.value
    }

    /**
     * Returns the current inventory as a List.
     * @return A list of items in the inventory.
     */
    fun getInventoryList(): List<String> {
        return _inventory.value.toList()
    }

    /**
     * Returns the current inventory as a Set.
     * @return A set of items in the inventory.
     */
    fun getInventorySet(): Set<String> {
        return _inventory.value
    }
} 