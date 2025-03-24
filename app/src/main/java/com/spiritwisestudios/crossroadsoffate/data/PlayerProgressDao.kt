package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.*
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
     * Inserts a new player progress record into the database.
     * If a record with the same playerId exists, it will be replaced.
     *
     * @param playerProgress The progress data to insert
     * @return Row ID of the inserted record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProgress(playerProgress: PlayerProgress): Long

    /**
     * Updates an existing player progress record.
     *
     * @param playerProgress The progress data to update
     */
    @Update
    suspend fun updatePlayerProgress(playerProgress: PlayerProgress)

    /**
     * Atomically inserts or updates player progress.
     * If a record exists for the player, it updates it; otherwise creates a new record.
     *
     * @param progress The player progress to insert or update
     */
    @Transaction
    suspend fun insertOrUpdate(progress: PlayerProgress) {
        val existing = getPlayerProgress(progress.playerId)
        if (existing == null) {
            insertPlayerProgress(progress)
        } else {
            updatePlayerProgress(progress)
        }
    }
}