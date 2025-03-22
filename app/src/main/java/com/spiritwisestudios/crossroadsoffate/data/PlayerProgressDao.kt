package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.*
import com.spiritwisestudios.crossroadsoffate.data.models.PlayerProgress

@Dao
interface PlayerProgressDao {
    @Query("SELECT * FROM player_progress WHERE playerId = :playerId LIMIT 1")
    suspend fun getPlayerProgress(playerId: String): PlayerProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProgress(playerProgress: PlayerProgress): Long

    @Update
    suspend fun updatePlayerProgress(playerProgress: PlayerProgress)

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