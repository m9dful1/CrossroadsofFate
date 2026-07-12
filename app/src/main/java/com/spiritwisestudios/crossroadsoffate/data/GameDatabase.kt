package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*

/**
 * Room database class for the game.
 * Handles persistence of game scenarios and player progress.
 */
@Database(
    entities = [ScenarioEntity::class, PlayerProgress::class, InteractiveMapLocation::class], // Define database entities
    version = 12,                                               // Database version for migrations
    exportSchema = false                                       // Don't export database schema
)
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {
    // Data Access Objects (DAOs) for database operations
    abstract fun scenarioDao(): ScenarioDao
    abstract fun playerProgressDao(): PlayerProgressDao
    abstract fun interactiveMapLocationDao(): InteractiveMapLocationDao

    companion object {
        // Singleton instance of the database
        @Volatile
        private var INSTANCE: GameDatabase? = null
        // Lock object for thread safety
        private val LOCK = Any()

        /**
         * Gets the database instance, creating it if it doesn't exist.
         * Uses Double-Checked Locking pattern for thread safety.
         *
         * @param context Application context
         * @return GameDatabase instance
         */
        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(LOCK) {
                INSTANCE ?: createDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Creates a new database instance.
         *
         * @param context Application context
         * @return New GameDatabase instance
         */
        private fun createDatabase(context: Context) = Room.databaseBuilder(
            context,
            GameDatabase::class.java,
            "game_database"
        )
        .fallbackToDestructiveMigration()         // Recreate database if migration fails
        .addTypeConverter(Converters())           // Provide the type converter instance
        .addCallback(object : Callback() {
            // Enable foreign key support when database is created
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        })
        .build()
    }
}