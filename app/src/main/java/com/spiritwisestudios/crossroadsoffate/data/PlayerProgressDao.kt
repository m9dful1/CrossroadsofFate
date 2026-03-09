package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spiritwisestudios.crossroadsoffate.data.models.PlayerProgress

/**
 * Data Access Object (DAO) for player progress related database operations.
 * Provides methods to read and write player progress data.
 */
@Dao
interface PlayerProgressDao {
    /**
     * Retrieves player progress for a specific player ID.
     *
     * @param playerId The unique identifier of the player
     * @return PlayerProgress object if found, null otherwise
     */
    @Query("SELECT * FROM player_progress WHERE playerId = :playerId LIMIT 1")
    suspend fun getPlayerProgress(playerId: String): PlayerProgress?

    /**
     * Inserts or replaces player progress.
     * If a record with the same playerId exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: PlayerProgress)

    /**
     * Deletes all player progress records from the database.
     * Used for resetting game state.
     */
    @Query("DELETE FROM player_progress")
    suspend fun deleteAll()
}