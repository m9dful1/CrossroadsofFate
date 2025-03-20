package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity

@Dao
interface ScenarioDao {

    @Query("SELECT * FROM scenarios WHERE id = :id LIMIT 1")
    suspend fun getScenarioById(id: String): ScenarioEntity?

    @Query("SELECT COUNT(*) FROM scenarios")
    suspend fun getScenarioCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarios(scenarios: List<ScenarioEntity>)
}