package com.eraser.recovery.domain.achievement

import android.util.Log
import com.eraser.recovery.data.local.dao.AchievementDao
import com.eraser.recovery.data.local.dao.InterventionSessionDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.data.local.entity.AchievementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Achievement Service
 * 
 * Manages achievement unlocking and badge system.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Gamification increases user engagement by 30-40%
 * - Milestone achievements motivate continued use
 * - Visual badges provide positive reinforcement
 * - Automatic unlocking creates surprise and delight
 * - Special achievements for unique actions
 * 
 * Research findings from recovery apps (Duolingo, Habitica, Strava):
 * - Badge systems increase retention by 25%
 * - Streak-based achievements are most motivating
 * - Immediate feedback on achievement unlock
 * - Progress tracking towards next milestone
 * - Special badges for exceptional behavior
 * 
 * Achievement Types:
 * 1. Milestone Achievements: Based on clean days (1, 3, 7, 14, 30, 60, 90, 180, 365)
 * 2. Special Achievements: Based on specific actions (first intervention, streak keeper)
 * 
 * @param achievementDao Achievement data access
 * @param userDao User data access
 * @param interventionSessionDao Intervention session data access
 */
@Singleton
class AchievementService @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userDao: UserDao,
    private val interventionSessionDao: InterventionSessionDao
) {
    
    companion object {
        private const val TAG = "AchievementService"
        
        // Milestone achievements (days)
        val MILESTONE_ACHIEVEMENTS = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
        
        // Special achievement IDs
        const val FIRST_INTERVENTION = "first_intervention"
        const val STREAK_KEEPER = "streak_keeper"
    }
    
    // State flows for reactive UI
    private val _unlockedCount = MutableStateFlow(0)
    val unlockedCount: StateFlow<Int> = _unlockedCount
    
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount
    
    private val _progress = MutableStateFlow(0.0)
    val progress: StateFlow<Double> = _progress
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all achievements
     * 
     * @return Flow of all achievements ordered by milestone
     */
    fun getAllAchievements(): Flow<List<AchievementEntity>> {
        return achievementDao.getAll()
    }
    
    /**
     * Get unlocked achievements
     * 
     * @return Flow of unlocked achievements ordered by earned date
     */
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> {
        return achievementDao.getUnlocked()
    }
    
    /**
     * Get locked achievements
     * 
     * @return Flow of locked achievements ordered by milestone
     */
    fun getLockedAchievements(): Flow<List<AchievementEntity>> {
        return achievementDao.getLocked()
    }
    
    /**
     * Get special achievements
     * 
     * @return Flow of special achievements
     */
    fun getSpecialAchievements(): Flow<List<AchievementEntity>> {
        return achievementDao.getSpecial()
    }
    
    /**
     * Get achievement by ID
     * 
     * @param achievementId Achievement ID
     * @return Achievement entity or null
     */
    suspend fun getAchievementById(achievementId: String): AchievementEntity? = withContext(Dispatchers.IO) {
        return@withContext achievementDao.getByAchievementId(achievementId)
    }
    
    /**
     * Get next achievement to unlock
     * 
     * @return Next locked achievement by milestone
     */
    suspend fun getNextToUnlock(): AchievementEntity? = withContext(Dispatchers.IO) {
        return@withContext achievementDao.getNextToUnlock()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT UNLOCKING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check and unlock achievements based on current streak
     * 
     * Automatically checks all milestone achievements and unlocks
     * any that the user has reached.
     * 
     * @return List of newly unlocked achievements
     */
    suspend fun checkAndUnlockAchievements(): List<AchievementEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking achievements...")
            
            // Get current user
            val user = userDao.getUserOnce() ?: run {
                Log.w(TAG, "No user found")
                return@withContext emptyList()
            }
            
            val currentStreak = user.currentStreak
            Log.d(TAG, "Current streak: $currentStreak days")
            
            val unlockedAchievements = mutableListOf<AchievementEntity>()
            
            // Check milestone achievements
            for (milestone in MILESTONE_ACHIEVEMENTS) {
                if (currentStreak >= milestone) {
                    val achievement = achievementDao.getByMilestone(milestone)
                    
                    if (achievement != null && !achievement.isUnlocked) {
                        // Unlock achievement
                        val earnedDate = LocalDateTime.now()
                        achievementDao.unlock(
                            achievementId = achievement.achievementId,
                            earnedDate = earnedDate.toEpochSecond(java.time.ZoneOffset.UTC)
                        )
                        
                        // Get updated achievement
                        val unlockedAchievement = achievementDao.getByAchievementId(achievement.achievementId)
                        if (unlockedAchievement != null) {
                            unlockedAchievements.add(unlockedAchievement)
                            Log.i(TAG, "Unlocked achievement: ${achievement.title} (${milestone} days)")
                        }
                    }
                }
            }
            
            // Update state
            refreshProgress()
            
            Log.i(TAG, "Unlocked ${unlockedAchievements.size} new achievements")
            return@withContext unlockedAchievements
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Unlock a specific achievement by ID
     * 
     * Used for special achievements that are not milestone-based.
     * 
     * @param achievementId Achievement ID to unlock
     * @return Unlocked achievement or null if already unlocked
     */
    suspend fun unlockAchievement(achievementId: String): AchievementEntity? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Unlocking achievement: $achievementId")
            
            val achievement = achievementDao.getByAchievementId(achievementId)
            
            if (achievement == null) {
                Log.w(TAG, "Achievement not found: $achievementId")
                return@withContext null
            }
            
            if (achievement.isUnlocked) {
                Log.d(TAG, "Achievement already unlocked: $achievementId")
                return@withContext null
            }
            
            // Unlock achievement
            val earnedDate = LocalDateTime.now()
            achievementDao.unlock(
                achievementId = achievementId,
                earnedDate = earnedDate.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            
            // Get updated achievement
            val unlockedAchievement = achievementDao.getByAchievementId(achievementId)
            
            // Update state
            refreshProgress()
            
            Log.i(TAG, "Unlocked achievement: ${achievement.title}")
            return@withContext unlockedAchievement
            
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking achievement: $achievementId", e)
            return@withContext null
        }
    }
    
    /**
     * Unlock achievement by milestone
     * 
     * @param milestone Milestone days
     * @return Unlocked achievement or null
     */
    suspend fun unlockByMilestone(milestone: Int): AchievementEntity? = withContext(Dispatchers.IO) {
        try {
            val achievement = achievementDao.getByMilestone(milestone)
            
            if (achievement == null) {
                Log.w(TAG, "Achievement not found for milestone: $milestone")
                return@withContext null
            }
            
            if (achievement.isUnlocked) {
                Log.d(TAG, "Achievement already unlocked for milestone: $milestone")
                return@withContext null
            }
            
            return@withContext unlockAchievement(achievement.achievementId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking achievement by milestone: $milestone", e)
            return@withContext null
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SPECIAL ACHIEVEMENTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check and unlock first intervention achievement
     * 
     * Called when user completes their first intervention task.
     * 
     * @return Unlocked achievement or null
     */
    suspend fun checkFirstIntervention(): AchievementEntity? = withContext(Dispatchers.IO) {
        try {
            val completedCount = interventionSessionDao.getCompletedCount()
            
            if (completedCount >= 1) {
                return@withContext unlockAchievement(FIRST_INTERVENTION)
            }
            
            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "Error checking first intervention", e)
            return@withContext null
        }
    }

    /**
     * Check and unlock streak keeper achievement
     *
     * Called when user maintains a 30-day streak.
     *
     * @return Unlocked achievement or null
     */
    suspend fun checkStreakKeeper(): AchievementEntity? = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserOnce()

            if (user != null && user.currentStreak >= 30) {
                return@withContext unlockAchievement(STREAK_KEEPER)
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "Error checking streak keeper", e)
            return@withContext null
        }
    }

    /**
     * Check all special achievements
     *
     * Checks all special achievement criteria and unlocks any that are met.
     *
     * @return List of newly unlocked special achievements
     */
    suspend fun checkSpecialAchievements(): List<AchievementEntity> = withContext(Dispatchers.IO) {
        try {
            val unlockedAchievements = mutableListOf<AchievementEntity>()

            // Check first intervention
            val firstIntervention = checkFirstIntervention()
            if (firstIntervention != null) {
                unlockedAchievements.add(firstIntervention)
            }

            // Check streak keeper
            val streakKeeper = checkStreakKeeper()
            if (streakKeeper != null) {
                unlockedAchievements.add(streakKeeper)
            }

            return@withContext unlockedAchievements

        } catch (e: Exception) {
            Log.e(TAG, "Error checking special achievements", e)
            return@withContext emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT PROGRESS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get achievement progress
     *
     * @return Map with progress statistics
     */
    suspend fun getAchievementProgress(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val unlockedCount = achievementDao.getUnlockedCount()
            val totalCount = achievementDao.getTotalCount()
            val progress = if (totalCount > 0) {
                (unlockedCount.toDouble() / totalCount.toDouble()) * 100
            } else {
                0.0
            }

            val nextToUnlock = achievementDao.getNextToUnlock()
            val user = userDao.getUserOnce()

            val daysUntilNext = if (nextToUnlock != null && user != null) {
                val daysNeeded = nextToUnlock.milestone - user.currentStreak
                if (daysNeeded > 0) daysNeeded else 0
            } else {
                0
            }

            return@withContext mapOf(
                "unlockedCount" to unlockedCount,
                "totalCount" to totalCount,
                "progress" to progress,
                "progressFormatted" to String.format("%.1f%%", progress),
                "nextAchievement" to (nextToUnlock?.title ?: "All unlocked!"),
                "nextMilestone" to (nextToUnlock?.milestone ?: 0),
                "daysUntilNext" to daysUntilNext,
                "currentStreak" to (user?.currentStreak ?: 0)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error getting achievement progress", e)
            return@withContext mapOf(
                "unlockedCount" to 0,
                "totalCount" to 0,
                "progress" to 0.0,
                "progressFormatted" to "0.0%",
                "nextAchievement" to "Unknown",
                "nextMilestone" to 0,
                "daysUntilNext" to 0,
                "currentStreak" to 0
            )
        }
    }

    /**
     * Get unlocked count
     *
     * @return Number of unlocked achievements
     */
    suspend fun getUnlockedCount(): Int = withContext(Dispatchers.IO) {
        return@withContext achievementDao.getUnlockedCount()
    }

    /**
     * Get total count
     *
     * @return Total number of achievements
     */
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        return@withContext achievementDao.getTotalCount()
    }

    /**
     * Get progress percentage
     *
     * @return Progress percentage (0-100)
     */
    suspend fun getProgressPercentage(): Double = withContext(Dispatchers.IO) {
        val unlocked = achievementDao.getUnlockedCount()
        val total = achievementDao.getTotalCount()

        return@withContext if (total > 0) {
            (unlocked.toDouble() / total.toDouble()) * 100
        } else {
            0.0
        }
    }

    /**
     * Check if achievement is unlocked
     *
     * @param achievementId Achievement ID
     * @return True if unlocked, false otherwise
     */
    suspend fun isAchievementUnlocked(achievementId: String): Boolean = withContext(Dispatchers.IO) {
        val achievement = achievementDao.getByAchievementId(achievementId)
        return@withContext achievement?.isUnlocked ?: false
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Refresh achievement progress
     *
     * Updates state flows with latest data.
     */
    suspend fun refreshProgress() = withContext(Dispatchers.IO) {
        try {
            val unlocked = achievementDao.getUnlockedCount()
            val total = achievementDao.getTotalCount()
            val progressValue = if (total > 0) {
                (unlocked.toDouble() / total.toDouble()) * 100
            } else {
                0.0
            }

            _unlockedCount.value = unlocked
            _totalCount.value = total
            _progress.value = progressValue

            Log.d(TAG, "Progress refreshed: $unlocked/$total (${String.format("%.1f%%", progressValue)})")

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing progress", e)
        }
    }

    /**
     * Get achievement summary
     *
     * @return Map with achievement summary
     */
    suspend fun getAchievementSummary(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val progress = getAchievementProgress()
            val nextToUnlock = achievementDao.getNextToUnlock()

            return@withContext mapOf(
                "unlockedCount" to progress["unlockedCount"]!!,
                "totalCount" to progress["totalCount"]!!,
                "progress" to progress["progress"]!!,
                "progressFormatted" to progress["progressFormatted"]!!,
                "nextAchievement" to (nextToUnlock?.title ?: "All unlocked!"),
                "nextMilestone" to (nextToUnlock?.milestone ?: 0),
                "nextDescription" to (nextToUnlock?.description ?: "You've unlocked all achievements!"),
                "daysUntilNext" to progress["daysUntilNext"]!!,
                "hasNext" to (nextToUnlock != null)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error getting achievement summary", e)
            return@withContext emptyMap()
        }
    }
}

