package com.eraser.recovery.domain.journey

import android.util.Log
import com.eraser.recovery.data.local.dao.AchievementDao
import com.eraser.recovery.data.local.dao.DailyLogDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.data.local.entity.AchievementEntity
import com.eraser.recovery.data.local.entity.DailyLogEntity
import com.eraser.recovery.data.local.entity.UserEntity
import com.eraser.recovery.domain.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Journey Service
 * 
 * Manages user's recovery journey lifecycle and progress tracking.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Journey start/stop with VPN integration
 * - Streak calculation (current, longest, total clean days)
 * - Milestone tracking (1, 3, 7, 14, 30, 60, 90, 180, 365 days)
 * - Achievement unlocking based on progress
 * - Daily check-in system
 * - Progress statistics
 * 
 * Key Features:
 * - Start/stop journey with VPN lifecycle
 * - Calculate streaks from daily logs
 * - Track milestones and unlock achievements
 * - Provide journey statistics
 * - Real-time progress updates via Flow
 */
@Singleton
class JourneyService @Inject constructor(
    private val userDao: UserDao,
    private val dailyLogDao: DailyLogDao,
    private val achievementDao: AchievementDao,
    private val vpnManager: VpnManager
) {
    
    companion object {
        private const val TAG = "JourneyService"
        
        // Milestone days for achievements
        val MILESTONES = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
    }
    
    // State flows for reactive UI
    private val _isJourneyActive = MutableStateFlow(false)
    val isJourneyActive: StateFlow<Boolean> = _isJourneyActive
    
    private val _journeyElapsedDays = MutableStateFlow(0L)
    val journeyElapsedDays: StateFlow<Long> = _journeyElapsedDays
    
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // JOURNEY LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Start the recovery journey
     * 
     * Steps:
     * 1. Get or create user
     * 2. Check if journey is already active
     * 3. Request VPN permission
     * 4. Start VPN service
     * 5. Update user status
     * 6. Update state flows
     * 
     * @return Result with success or error message
     */
    suspend fun startJourney(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting recovery journey...")
            
            // Get or create user
            var user = userDao.getUserOnce()
            if (user == null) {
                Log.i(TAG, "No user found, creating initial user")
                user = UserEntity(
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
            
            // Check if journey is already active
            if (user.isJourneyActive) {
                Log.w(TAG, "Journey is already active")
                return@withContext Result.failure(Exception("Journey is already active"))
            }
            
            // Start VPN service
            val vpnStarted = vpnManager.startVpn()
            if (!vpnStarted) {
                Log.e(TAG, "Failed to start VPN")
                return@withContext Result.failure(
                    Exception("Failed to start VPN")
                )
            }
            
            // Update user status
            val now = LocalDateTime.now()
            userDao.updateJourneyStatus(
                isActive = true,
                startTime = now.toEpochSecond(java.time.ZoneOffset.UTC),
                stopTime = null,
                updatedAt = now.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            
            userDao.updateVpnStatus(
                isEnabled = true,
                updatedAt = now.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            
            // Update state flows
            _isJourneyActive.value = true
            _journeyElapsedDays.value = 0
            
            Log.i(TAG, "Journey started successfully")
            return@withContext Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting journey", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Stop the recovery journey
     * 
     * Steps:
     * 1. Stop VPN service
     * 2. Update user status
     * 3. Update state flows
     * 
     * @return Result with success or error message
     */
    suspend fun stopJourney(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Stopping recovery journey...")
            
            // Stop VPN service
            vpnManager.stopVpn()
            
            // Update user status
            val now = LocalDateTime.now()
            userDao.updateJourneyStatus(
                isActive = false,
                startTime = null,
                stopTime = now.toEpochSecond(java.time.ZoneOffset.UTC),
                updatedAt = now.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            
            userDao.updateVpnStatus(
                isEnabled = false,
                updatedAt = now.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            
            // Update state flows
            _isJourneyActive.value = false
            
            Log.i(TAG, "Journey stopped successfully")
            return@withContext Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping journey", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Toggle journey (start if stopped, stop if started)
     */
    suspend fun toggleJourney(): Result<Unit> {
        val user = userDao.getUserOnce()
        return if (user?.isJourneyActive == true) {
            stopJourney()
        } else {
            startJourney()
        }
    }
    
    /**
     * Get journey elapsed days
     * 
     * @return Number of days since journey start
     */
    suspend fun getElapsedDays(): Long = withContext(Dispatchers.IO) {
        val user = userDao.getUserOnce() ?: return@withContext 0L
        
        if (!user.isJourneyActive || user.journeyStartTime == null) {
            return@withContext 0L
        }
        
        val days = ChronoUnit.DAYS.between(user.journeyStartTime, LocalDateTime.now())
        _journeyElapsedDays.value = days
        return@withContext days
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STREAK CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Update user streak based on daily logs
     * 
     * Algorithm:
     * 1. Get all daily logs ordered by date
     * 2. Calculate current streak (consecutive clean days from today backwards)
     * 3. Calculate longest streak (maximum consecutive clean days in history)
     * 4. Calculate total clean days (count of all clean days)
     * 5. Update user entity
     * 
     * Current Streak Logic:
     * - Start from today
     * - Count backwards while days are consecutive and clean
     * - Stop at first non-clean day or gap
     * 
     * Longest Streak Logic:
     * - Iterate through all logs
     * - Track consecutive clean days
     * - Keep maximum streak found
     */
    suspend fun updateStreak(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating streak...")
            
            val user = userDao.getUserOnce() ?: return@withContext Result.failure(
                Exception("User not found")
            )
            
            // Get all daily logs ordered by date
            val logs = dailyLogDao.getBetweenDates(
                startDate = 0,
                endDate = Long.MAX_VALUE
            ).sortedBy { it.date }
            
            if (logs.isEmpty()) {
                Log.d(TAG, "No daily logs found, resetting streaks")
                userDao.updateStreak(
                    currentStreak = 0,
                    longestStreak = 0,
                    lastCheckInDate = 0,
                    updatedAt = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC)
                )
                _currentStreak.value = 0
                return@withContext Result.success(Unit)
            }
            
            // Calculate current streak
            var currentStreak = 0
            val today = LocalDate.now()
            
            // Start from most recent log and count backwards
            for (i in logs.indices.reversed()) {
                val log = logs[i]
                val logDate = log.date
                val expectedDate = today.minusDays(currentStreak.toLong())

                if (logDate == expectedDate && log.wasClean) {
                    currentStreak++
                } else {
                    break
                }
            }

            // Calculate longest streak
            var longestStreak = 0
            var tempStreak = 0
            var lastDate: LocalDate? = null

            for (log in logs) {
                val logDate = log.date
                
                if (log.wasClean) {
                    if (lastDate == null) {
                        tempStreak = 1
                    } else {
                        val daysDiff = ChronoUnit.DAYS.between(lastDate, logDate)
                        if (daysDiff == 1L) {
                            tempStreak++
                        } else {
                            longestStreak = maxOf(longestStreak, tempStreak)
                            tempStreak = 1
                        }
                    }
                    lastDate = logDate
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 0
                    lastDate = null
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak)
            
            // Calculate total clean days
            val totalCleanDays = logs.count { it.wasClean }
            
            // Update user
            val lastCheckInDate = logs.lastOrNull()?.date?.toEpochDay() ?: 0
            userDao.updateStreak(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                lastCheckInDate = lastCheckInDate,
                updatedAt = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC)
            )
            
            _currentStreak.value = currentStreak
            
            Log.i(TAG, "Streak updated: current=$currentStreak, longest=$longestStreak, total=$totalCleanDays")
            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get current streak
     */
    suspend fun getCurrentStreak(): Int = withContext(Dispatchers.IO) {
        val user = userDao.getUserOnce()
        val streak = user?.currentStreak ?: 0
        _currentStreak.value = streak
        return@withContext streak
    }

    /**
     * Get longest streak
     */
    suspend fun getLongestStreak(): Int = withContext(Dispatchers.IO) {
        val user = userDao.getUserOnce()
        return@withContext user?.longestStreak ?: 0
    }

    /**
     * Get total clean days
     */
    suspend fun getTotalCleanDays(): Int = withContext(Dispatchers.IO) {
        val user = userDao.getUserOnce()
        return@withContext user?.totalDaysClean ?: 0
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MILESTONE TRACKING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Check and unlock achievements based on current streak
     *
     * Milestones: 1, 3, 7, 14, 30, 60, 90, 180, 365 days
     *
     * @return List of newly unlocked achievements
     */
    suspend fun checkAndUnlockAchievements(): Result<List<AchievementEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking achievements...")

            val currentStreak = getCurrentStreak()
            val unlockedAchievements = mutableListOf<AchievementEntity>()

            for (milestone in MILESTONES) {
                if (currentStreak >= milestone) {
                    val achievement = achievementDao.getByMilestone(milestone)

                    if (achievement != null && !achievement.isUnlocked) {
                        // Unlock achievement
                        val now = LocalDateTime.now()
                        achievementDao.unlock(
                            achievementId = achievement.achievementId,
                            earnedDate = now.toEpochSecond(java.time.ZoneOffset.UTC)
                        )

                        // Add to unlocked list
                        unlockedAchievements.add(
                            achievement.copy(
                                isUnlocked = true,
                                earnedDate = now
                            )
                        )

                        Log.i(TAG, "Achievement unlocked: ${achievement.title} (${milestone} days)")
                    }
                }
            }

            return@withContext Result.success(unlockedAchievements)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get next milestone to reach
     *
     * @return Next milestone days, or null if all milestones reached
     */
    suspend fun getNextMilestone(): Int? = withContext(Dispatchers.IO) {
        val currentStreak = getCurrentStreak()
        return@withContext MILESTONES.firstOrNull { it > currentStreak }
    }

    /**
     * Get progress to next milestone (0.0 to 1.0)
     *
     * @return Progress percentage, or 1.0 if all milestones reached
     */
    suspend fun getProgressToNextMilestone(): Double = withContext(Dispatchers.IO) {
        val currentStreak = getCurrentStreak()
        val nextMilestone = getNextMilestone()

        if (nextMilestone == null) {
            return@withContext 1.0 // All milestones reached
        }

        // Find previous milestone
        val previousMilestone = MILESTONES.lastOrNull { it <= currentStreak } ?: 0

        // Calculate progress
        val range = nextMilestone - previousMilestone
        val progress = currentStreak - previousMilestone

        return@withContext progress.toDouble() / range.toDouble()
    }

    /**
     * Get days until next milestone
     *
     * @return Days remaining, or 0 if all milestones reached
     */
    suspend fun getDaysUntilNextMilestone(): Int = withContext(Dispatchers.IO) {
        val currentStreak = getCurrentStreak()
        val nextMilestone = getNextMilestone()

        if (nextMilestone == null) {
            return@withContext 0
        }

        return@withContext maxOf(0, nextMilestone - currentStreak)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // JOURNEY STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get comprehensive journey statistics
     *
     * @return Map with all journey statistics
     */
    suspend fun getJourneyStatistics(): Map<String, Any> = withContext(Dispatchers.IO) {
        val user = userDao.getUserOnce()
        val elapsedDays = getElapsedDays()
        val currentStreak = getCurrentStreak()
        val longestStreak = getLongestStreak()
        val totalCleanDays = getTotalCleanDays()
        val nextMilestone = getNextMilestone()
        val progressToNext = getProgressToNextMilestone()
        val daysUntilNext = getDaysUntilNextMilestone()
        val unlockedCount = achievementDao.getUnlockedCount()
        val totalAchievements = achievementDao.getTotalCount()

        return@withContext mapOf(
            "isJourneyActive" to (user?.isJourneyActive ?: false),
            "journeyElapsedDays" to elapsedDays,
            "currentStreak" to currentStreak,
            "longestStreak" to longestStreak,
            "totalCleanDays" to totalCleanDays,
            "nextMilestone" to (nextMilestone ?: 0),
            "progressToNextMilestone" to progressToNext,
            "daysUntilNextMilestone" to daysUntilNext,
            "unlockedAchievements" to unlockedCount,
            "totalAchievements" to totalAchievements,
            "achievementProgress" to (unlockedCount.toDouble() / totalAchievements.toDouble()),
            "startDate" to (user?.startDate?.toString() ?: ""),
            "lastCheckInDate" to (user?.lastCheckInDate?.toString() ?: ""),
            "hasCheckedInToday" to (user?.hasCheckedInToday() ?: false),
            "isStreakAtRisk" to (user?.isStreakAtRisk() ?: false)
        )
    }

    /**
     * Initialize journey service
     *
     * Call this on app startup to:
     * 1. Load journey state
     * 2. Update state flows
     * 3. Resume VPN if journey was active
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing journey service...")

            val user = userDao.getUserOnce()

            if (user != null) {
                // Update state flows
                _isJourneyActive.value = user.isJourneyActive
                _currentStreak.value = user.currentStreak

                if (user.isJourneyActive) {
                    Log.i(TAG, "Journey was active, updating elapsed days...")
                    getElapsedDays()

                    // Verify VPN is still active
                    // Note: VPN state is managed by VpnManager
                }
            }

            Log.i(TAG, "Journey service initialized")
            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing journey service", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get user Flow for reactive UI
     */
    fun getUserFlow(): Flow<UserEntity?> {
        return userDao.getUser()
    }

    /**
     * Reset journey (start over)
     *
     * WARNING: This will reset all progress!
     */
    suspend fun resetJourney(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "Resetting journey...")

            // Stop journey if active
            if (_isJourneyActive.value) {
                stopJourney()
            }

            // Reset user
            val now = LocalDateTime.now()
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
                createdAt = now,
                updatedAt = now
            )
            userDao.insert(user)

            // Clear daily logs
            dailyLogDao.deleteAll()

            // Reset state flows
            _isJourneyActive.value = false
            _journeyElapsedDays.value = 0
            _currentStreak.value = 0

            Log.i(TAG, "Journey reset successfully")
            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error resetting journey", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Check in today as clean
     *
     * @param mood User's mood rating (1-5)
     * @param notes Optional reflection notes
     * @return Result with success or error
     */
    suspend fun checkInToday(mood: Int, notes: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val todayEpoch = today.toEpochDay()

            // Check if already checked in today
            val existingLog = dailyLogDao.getByDate(todayEpoch)

            if (existingLog != null) {
                // Update existing log
                val updatedLog = existingLog.copy(
                    wasClean = true,
                    moodRating = mood,
                    notes = notes
                )
                dailyLogDao.update(updatedLog)
            } else {
                // Create new log
                val newLog = DailyLogEntity(
                    date = today,
                    wasClean = true,
                    moodRating = mood,
                    notes = notes
                )
                dailyLogDao.insert(newLog)
            }

            // Update streak
            updateStreak()

            Log.i(TAG, "Checked in today as clean")
            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking in today", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Record a relapse
     *
     * @param mood User's mood rating (1-5)
     * @param notes Optional reflection notes
     * @return Result with success or error
     */
    suspend fun recordRelapse(mood: Int, notes: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val todayEpoch = today.toEpochDay()

            // Check if already logged today
            val existingLog = dailyLogDao.getByDate(todayEpoch)

            if (existingLog != null) {
                // Update existing log
                val updatedLog = existingLog.copy(
                    wasClean = false,
                    moodRating = mood,
                    notes = notes
                )
                dailyLogDao.update(updatedLog)
            } else {
                // Create new log
                val newLog = DailyLogEntity(
                    date = today,
                    wasClean = false,
                    moodRating = mood,
                    notes = notes
                )
                dailyLogDao.insert(newLog)
            }

            // Update streak (will reset to 0)
            updateStreak()

            Log.i(TAG, "Recorded relapse")
            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error recording relapse", e)
            return@withContext Result.failure(e)
        }
    }
}

