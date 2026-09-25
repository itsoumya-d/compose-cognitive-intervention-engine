package com.eraser.recovery.data.local

import com.eraser.recovery.data.local.dao.AchievementDao
import com.eraser.recovery.data.local.dao.FlashcardDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.data.local.entity.*
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database Initializer
 * 
 * Initializes database with default data on first launch.
 * 
 * Research findings:
 * - Populate flashcards on first launch
 * - Create default achievements
 * - Initialize user profile
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val achievementDao: AchievementDao,
    private val userDao: UserDao
) {
    
    /**
     * Initialize database with default data
     */
    suspend fun initialize() {
        // Check if already initialized
        val user = userDao.getUserOnce()
        if (user != null) return
        
        // Initialize user
        initializeUser()

        // Initialize flashcards (60 total: 50 from Flutter app + 10 new)
        initializeFlashcards()
        
        // Initialize achievements
        initializeAchievements()
    }
    
    /**
     * Initialize user profile
     */
    private suspend fun initializeUser() {
        val user = UserEntity(
            id = 1,
            startDate = LocalDate.now(),
            currentStreak = 0,
            longestStreak = 0,
            totalDaysClean = 0,
            lastCheckInDate = null,
            isActive = true,
            isJourneyActive = false,
            journeyStartTime = null,
            journeyStopTime = null,
            isVpnEnabled = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        userDao.insert(user)
    }
    
    /**
     * Initialize flashcards
     * Populates 60 flashcards (50 from Flutter app + 10 new)
     */
    private suspend fun initializeFlashcards() {
        // Check if flashcards already exist
        val count = flashcardDao.getActiveCount()
        if (count > 0) return

        // Get all 60 flashcards and insert them
        val flashcards = FlashcardData.getAllFlashcards()
        flashcardDao.insertAll(flashcards)
    }
    
    /**
     * Initialize default achievements
     * Based on Flutter app's achievement system
     */
    private suspend fun initializeAchievements() {
        // Check if achievements already exist
        val count = achievementDao.getTotalCount()
        if (count > 0) return
        
        val achievements = listOf(
            // Day milestones
            AchievementEntity(
                achievementId = "day_1",
                title = "First Step",
                description = "Complete your first day clean",
                milestone = 1,
                isSpecial = false
            ),
            AchievementEntity(
                achievementId = "day_3",
                title = "Three Days Strong",
                description = "Reach 3 days clean",
                milestone = 3,
                isSpecial = false
            ),
            AchievementEntity(
                achievementId = "day_7",
                title = "One Week Warrior",
                description = "Complete your first week",
                milestone = 7,
                isSpecial = false
            ),
            AchievementEntity(
                achievementId = "day_14",
                title = "Two Week Champion",
                description = "Reach 2 weeks clean",
                milestone = 14,
                isSpecial = false
            ),
            AchievementEntity(
                achievementId = "day_30",
                title = "One Month Milestone",
                description = "Complete your first month",
                milestone = 30,
                isSpecial = false
            ),
            AchievementEntity(
                achievementId = "day_60",
                title = "Two Month Master",
                description = "Reach 60 days clean",
                milestone = 60,
                isSpecial = false
            ),
            AchievementEntity(
                achievementId = "day_90",
                title = "90 Day Legend",
                description = "Complete 90 days clean",
                milestone = 90,
                isSpecial = true
            ),
            AchievementEntity(
                achievementId = "day_180",
                title = "Half Year Hero",
                description = "Reach 6 months clean",
                milestone = 180,
                isSpecial = true
            ),
            AchievementEntity(
                achievementId = "day_365",
                title = "One Year Victory",
                description = "Complete one full year clean",
                milestone = 365,
                isSpecial = true
            ),
            
            // Special achievements
            AchievementEntity(
                achievementId = "first_intervention",
                title = "Intervention Complete",
                description = "Complete your first intervention task",
                milestone = 0,
                isSpecial = true
            ),
            AchievementEntity(
                achievementId = "streak_keeper",
                title = "Streak Keeper",
                description = "Maintain a 30-day streak",
                milestone = 30,
                isSpecial = true
            )
        )
        
        achievementDao.insertAll(achievements)
    }
}

