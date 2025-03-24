package com.spiritwisestudios.crossroadsoffate.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spiritwisestudios.crossroadsoffate.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room database class for the game.
 * Handles persistence of game scenarios and player progress.
 */
@Database(
    entities = [ScenarioEntity::class, PlayerProgress::class], // Define database entities
    version = 6,                                               // Database version for migrations
    exportSchema = false                                       // Don't export database schema
)
@TypeConverters(
    Converters::class,              // For scenario-related conversions
    InventoryConverters::class,     // For player inventory conversions
    QuestConverters::class,         // For quest data conversions
    VisitedLocationsConverter::class // For visited locations conversions
)
abstract class GameDatabase : RoomDatabase() {
    // Data Access Objects (DAOs) for database operations
    abstract fun scenarioDao(): ScenarioDao
    abstract fun playerProgressDao(): PlayerProgressDao

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
        .addTypeConverter(VisitedLocationsConverter()) // Add custom type converter
        .addCallback(object : Callback() {
            // Enable foreign key support when database is created
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        })
        .build()

        /**
         * Clears all data from the database and deletes the database file.
         * Used for resetting game state or handling critical errors.
         *
         * @param context Application context
         * @throws IllegalStateException if database clearing fails
         */
        suspend fun clearDatabase(context: Context) = withContext(Dispatchers.IO) {
            try {
                INSTANCE?.apply {
                    clearAllTables()  // Clear all database tables
                    close()          // Close database connection
                }
                context.deleteDatabase("game_database") // Delete database file
                INSTANCE = null      // Clear singleton instance
            } catch (e: Exception) {
                throw IllegalStateException("Failed to clear database: ${e.message}")
            }
        }
    }
}