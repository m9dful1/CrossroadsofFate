package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spiritwisestudios.crossroadsoffate.data.models.InteractiveMapLocation

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
     * Get a specific location by ID
     */
    @Query("SELECT * FROM interactive_map_locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): InteractiveMapLocation?

    /**
     * Insert multiple interactive map locations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<InteractiveMapLocation>)

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
}
