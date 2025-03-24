package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity

/**
 * Data Access Object (DAO) for handling scenario database operations.
 * Provides methods to query and insert scenarios in the database.
 */
@Dao
interface ScenarioDao {

    /**
     * Retrieves a specific scenario from the database by its ID.
     *
     * @param id The unique identifier of the scenario to retrieve
     * @return The scenario if found, null otherwise
     */
    @Query("SELECT * FROM scenarios WHERE id = :id LIMIT 1")
    suspend fun getScenarioById(id: String): ScenarioEntity?

    /**
     * Gets the total number of scenarios stored in the database.
     *
     * @return The count of scenarios in the database
     */
    @Query("SELECT COUNT(*) FROM scenarios")
    suspend fun getScenarioCount(): Int

    /**
     * Inserts or updates a list of scenarios in the database.
     * If a scenario with the same ID exists, it will be replaced.
     *
     * @param scenarios List of scenarios to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scenarios: List<ScenarioEntity>)
}