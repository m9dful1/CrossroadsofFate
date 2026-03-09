package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.*
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for InteractiveMapLocation entities.
 * Provides database operations for the enhanced map system.
 */
@Dao
interface InteractiveMapLocationDao {

    /**
     * Get all interactive map locations
     */
    @Query("SELECT * FROM interactive_map_locations")
    suspend fun getAllLocations(): List<InteractiveMapLocation>

    /**
     * Get all interactive map locations as a Flow for reactive updates
     */
    @Query("SELECT * FROM interactive_map_locations")
    fun getAllLocationsFlow(): Flow<List<InteractiveMapLocation>>

    /**
     * Get a specific location by ID
     */
    @Query("SELECT * FROM interactive_map_locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): InteractiveMapLocation?

    /**
     * Get all visited locations
     */
    @Query("SELECT * FROM interactive_map_locations WHERE isVisited = 1")
    suspend fun getVisitedLocations(): List<InteractiveMapLocation>

    /**
     * Get all discovered locations (locations that can be seen on the map)
     */
    @Query("SELECT * FROM interactive_map_locations WHERE isVisited = 1 OR discoveryConditions = '[]'")
    suspend fun getDiscoveredLocations(): List<InteractiveMapLocation>

    /**
     * Get locations connected to a specific location
     */
    @Query("SELECT * FROM interactive_map_locations WHERE id IN (:connectionIds)")
    suspend fun getConnectedLocations(connectionIds: List<String>): List<InteractiveMapLocation>

    /**
     * Insert a new interactive map location
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: InteractiveMapLocation)

    /**
     * Insert multiple interactive map locations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<InteractiveMapLocation>)

    /**
     * Update an existing location
     */
    @Update
    suspend fun updateLocation(location: InteractiveMapLocation)

    /**
     * Update location visit status
     */
    @Query("UPDATE interactive_map_locations SET isVisited = :isVisited WHERE id = :locationId")
    suspend fun updateVisitStatus(locationId: String, isVisited: Boolean)

    /**
     * Update location activities (when activities are completed or changed)
     */
    @Query("UPDATE interactive_map_locations SET availableActivities = :activities WHERE id = :locationId")
    suspend fun updateLocationActivities(locationId: String, activities: String)

    /**
     * Delete a location
     */
    @Delete
    suspend fun deleteLocation(location: InteractiveMapLocation)

    /**
     * Delete all locations
     */
    @Query("DELETE FROM interactive_map_locations")
    suspend fun deleteAllLocations()

    /**
     * Get locations with incomplete activities
     */
    @Query("SELECT * FROM interactive_map_locations WHERE availableActivities LIKE '%\"isCompleted\":false%'")
    suspend fun getLocationsWithIncompleteActivities(): List<InteractiveMapLocation>

    /**
     * Search locations by name or description
     */
    @Query("SELECT * FROM interactive_map_locations WHERE name LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%'")
    suspend fun searchLocations(searchTerm: String): List<InteractiveMapLocation>
} 