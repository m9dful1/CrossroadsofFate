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
     * Insert multiple interactive map locations.
     * REPLACE lets the app re-seed the table from assets on every launch;
     * per-save state (visited locations) lives in PlayerProgress, never here.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<InteractiveMapLocation>)
}
