package com.spiritwisestudios.crossroadsoffate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spiritwisestudios.crossroadsoffate.data.models.PlayerProgress

@Dao
interface PlayerProgressDao {

    @Query("SELECT * FROM player_progress WHERE playerId = :playerId LIMIT 1")
    suspend fun getPlayerProgress(playerId: String): PlayerProgress? // ✅ Return the entity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProgress(playerProgress: PlayerProgress): Long // ✅ Must return Long or List<Long>
}
