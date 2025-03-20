package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.PlayerProgress
import com.spiritwisestudios.crossroadsoffate.data.models.ScenarioEntity
import com.spiritwisestudios.crossroadsoffate.data.models.Converters
import com.spiritwisestudios.crossroadsoffate.data.models.InventoryConverters

@Database(entities = [ScenarioEntity::class, PlayerProgress::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class, InventoryConverters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract fun scenarioDao(): ScenarioDao
    abstract fun playerProgressDao(): PlayerProgressDao

    companion object {
        @Volatile
        internal var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            println("Database created")
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            println("Database opened")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun clearDatabase(context: Context) {
            INSTANCE?.close()
            context.deleteDatabase("game_database")
            INSTANCE = null
            println("Database cleared and instance reset")
        }
    }
}