package com.eraser.recovery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eraser.recovery.data.local.dao.*
import com.eraser.recovery.data.local.entity.*

/**
 * Eraser Database
 * 
 * Main Room database for the Eraser app.
 * Migrated from Flutter app's Isar database.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Use Room for type-safe database access
 * - Export schema for version control
 * - Support database migrations
 * - Enable WAL mode for better performance
 * 
 * Database Version: 1
 */
@Database(
    entities = [
        FlashcardEntity::class,
        UserEntity::class,
        BlockedAttemptEntity::class,
        AchievementEntity::class,
        DailyLogEntity::class,
        InterventionSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class EraserDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun flashcardDao(): FlashcardDao
    abstract fun userDao(): UserDao
    abstract fun blockedAttemptDao(): BlockedAttemptDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun interventionSessionDao(): InterventionSessionDao
    
    companion object {
        const val DATABASE_NAME = "eraser_database.db"
    }
}

