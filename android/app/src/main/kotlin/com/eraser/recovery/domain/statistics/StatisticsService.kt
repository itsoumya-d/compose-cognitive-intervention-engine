package com.eraser.recovery.domain.statistics

import android.util.Log
import com.eraser.recovery.data.local.dao.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Statistics Service
 * 
 * Provides analytics and reporting for the recovery journey.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Comprehensive dashboard with key metrics
 * - Blocked attempts tracking and trends
 * - Intervention completion rates
 * - Time-based analytics (daily, weekly, monthly)
 * - Chart data for visualization (line charts, bar charts, pie charts)
 * - Top blocked domains
 * - Progress trends over time
 * 
 * Key Features:
 * - Aggregate blocked attempts
 * - Calculate intervention completion rates
 * - Generate chart data for Vico library
 * - Track trends over time
 * - Provide dashboard statistics
 */
@Singleton
class StatisticsService @Inject constructor(
    private val userDao: UserDao,
    private val blockedAttemptDao: BlockedAttemptDao,
    private val interventionSessionDao: InterventionSessionDao,
    private val dailyLogDao: DailyLogDao,
    private val achievementDao: AchievementDao,
    private val flashcardDao: FlashcardDao
) {
    
    companion object {
        private const val TAG = "StatisticsService"
    }
    
    // State flows for reactive UI
    private val _totalBlockedAttempts = MutableStateFlow(0)
    val totalBlockedAttempts: StateFlow<Int> = _totalBlockedAttempts
    
    private val _interventionCompletionRate = MutableStateFlow(0.0)
    val interventionCompletionRate: StateFlow<Double> = _interventionCompletionRate
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DASHBOARD STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get comprehensive dashboard statistics
     * 
     * @return Map with all dashboard metrics
     */
    suspend fun getDashboardStatistics(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting dashboard statistics...")
            
            val user = userDao.getUserOnce()
            
            // Blocked attempts
            val totalBlocked = blockedAttemptDao.getCount()
            val blockedToday = getBlockedAttemptsToday()
            val blockedThisWeek = getBlockedAttemptsThisWeek()
            val blockedThisMonth = getBlockedAttemptsThisMonth()
            
            // Interventions
            val totalInterventions = interventionSessionDao.getCount()
            val completedInterventions = interventionSessionDao.getCompletedCount()
            val skippedInterventions = interventionSessionDao.getSkippedCount()
            val completionRate = if (totalInterventions > 0) {
                (completedInterventions.toDouble() / totalInterventions.toDouble()) * 100
            } else {
                0.0
            }
            val avgCompletionTime = interventionSessionDao.getAverageCompletionTime() ?: 0.0
            
            // Journey stats
            val currentStreak = user?.currentStreak ?: 0
            val longestStreak = user?.longestStreak ?: 0
            val totalCleanDays = user?.totalDaysClean ?: 0
            
            // Achievements
            val unlockedAchievements = achievementDao.getUnlockedCount()
            val totalAchievements = achievementDao.getTotalCount()
            
            // Daily logs
            val totalCheckIns = dailyLogDao.getCount()
            val avgMoodRating = dailyLogDao.getAverageMoodRating() ?: 0.0
            
            // Update state flows
            _totalBlockedAttempts.value = totalBlocked
            _interventionCompletionRate.value = completionRate
            
            return@withContext mapOf(
                // Blocked attempts
                "totalBlockedAttempts" to totalBlocked,
                "blockedToday" to blockedToday,
                "blockedThisWeek" to blockedThisWeek,
                "blockedThisMonth" to blockedThisMonth,
                
                // Interventions
                "totalInterventions" to totalInterventions,
                "completedInterventions" to completedInterventions,
                "skippedInterventions" to skippedInterventions,
                "interventionCompletionRate" to completionRate,
                "averageCompletionTime" to avgCompletionTime,
                
                // Journey
                "currentStreak" to currentStreak,
                "longestStreak" to longestStreak,
                "totalCleanDays" to totalCleanDays,
                
                // Achievements
                "unlockedAchievements" to unlockedAchievements,
                "totalAchievements" to totalAchievements,
                "achievementProgress" to if (totalAchievements > 0) {
                    (unlockedAchievements.toDouble() / totalAchievements.toDouble()) * 100
                } else {
                    0.0
                },
                
                // Daily logs
                "totalCheckIns" to totalCheckIns,
                "averageMoodRating" to avgMoodRating,
                
                // Calculated metrics
                "interventionSuccessRate" to completionRate,
                "averageBlocksPerDay" to if (totalCleanDays > 0) {
                    totalBlocked.toDouble() / totalCleanDays.toDouble()
                } else {
                    0.0
                }
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dashboard statistics", e)
            return@withContext emptyMap()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BLOCKED ATTEMPTS ANALYTICS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get blocked attempts today
     */
    suspend fun getBlockedAttemptsToday(): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.atTime(23, 59, 59)
        
        return@withContext blockedAttemptDao.getCountBetween(
            startTime = startOfDay.toEpochSecond(java.time.ZoneOffset.UTC),
            endTime = endOfDay.toEpochSecond(java.time.ZoneOffset.UTC)
        )
    }
    
    /**
     * Get blocked attempts this week
     */
    suspend fun getBlockedAttemptsThisWeek(): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startTime = startOfWeek.atStartOfDay()
        val endTime = LocalDateTime.now()
        
        return@withContext blockedAttemptDao.getCountBetween(
            startTime = startTime.toEpochSecond(java.time.ZoneOffset.UTC),
            endTime = endTime.toEpochSecond(java.time.ZoneOffset.UTC)
        )
    }
    
    /**
     * Get blocked attempts this month
     */
    suspend fun getBlockedAttemptsThisMonth(): Int = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        val startTime = startOfMonth.atStartOfDay()
        val endTime = LocalDateTime.now()
        
        return@withContext blockedAttemptDao.getCountBetween(
            startTime = startTime.toEpochSecond(java.time.ZoneOffset.UTC),
            endTime = endTime.toEpochSecond(java.time.ZoneOffset.UTC)
        )
    }
    
    /**
     * Get top blocked domains
     * 
     * @param limit Number of top domains to return
     * @return List of domain counts
     */
    suspend fun getTopBlockedDomains(limit: Int = 10): List<DomainCount> = withContext(Dispatchers.IO) {
        return@withContext blockedAttemptDao.getTopBlockedDomains(limit)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INTERVENTION ANALYTICS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get intervention completion rate
     * 
     * @return Completion rate as percentage (0-100)
     */
    suspend fun getInterventionCompletionRate(): Double = withContext(Dispatchers.IO) {
        val total = interventionSessionDao.getCount()
        val completed = interventionSessionDao.getCompletedCount()
        
        val rate = if (total > 0) {
            (completed.toDouble() / total.toDouble()) * 100
        } else {
            0.0
        }
        
        _interventionCompletionRate.value = rate
        return@withContext rate
    }
    
    /**
     * Get intervention skip rate
     * 
     * @return Skip rate as percentage (0-100)
     */
    suspend fun getInterventionSkipRate(): Double = withContext(Dispatchers.IO) {
        val total = interventionSessionDao.getCount()
        val skipped = interventionSessionDao.getSkippedCount()
        
        return@withContext if (total > 0) {
            (skipped.toDouble() / total.toDouble()) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Get average intervention completion time
     * 
     * @return Average time in seconds
     */
    suspend fun getAverageInterventionTime(): Double = withContext(Dispatchers.IO) {
        return@withContext interventionSessionDao.getAverageCompletionTime() ?: 0.0
    }
    
    /**
     * Get intervention statistics
     * 
     * @return Map with intervention metrics
     */
    suspend fun getInterventionStatistics(): Map<String, Any> = withContext(Dispatchers.IO) {
        val total = interventionSessionDao.getCount()
        val completed = interventionSessionDao.getCompletedCount()
        val skipped = interventionSessionDao.getSkippedCount()
        val completionRate = getInterventionCompletionRate()
        val skipRate = getInterventionSkipRate()
        val avgTime = getAverageInterventionTime()
        
        return@withContext mapOf(
            "totalInterventions" to total,
            "completedInterventions" to completed,
            "skippedInterventions" to skipped,
            "completionRate" to completionRate,
            "skipRate" to skipRate,
            "averageCompletionTime" to avgTime,
            "completionRateFormatted" to String.format("%.1f%%", completionRate),
            "skipRateFormatted" to String.format("%.1f%%", skipRate),
            "averageTimeFormatted" to formatTime(avgTime.toLong())
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CHART DATA GENERATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get blocked attempts chart data for last N days
     *
     * @param days Number of days to include
     * @return List of ChartDataPoint for line/bar chart
     */
    suspend fun getBlockedAttemptsChartData(days: Int = 7): List<ChartDataPoint> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val chartData = mutableListOf<ChartDataPoint>()

            for (i in (days - 1) downTo 0) {
                val date = today.minusDays(i.toLong())
                val startTime = date.atStartOfDay()
                val endTime = date.atTime(23, 59, 59)

                val count = blockedAttemptDao.getCountBetween(
                    startTime = startTime.toEpochSecond(java.time.ZoneOffset.UTC),
                    endTime = endTime.toEpochSecond(java.time.ZoneOffset.UTC)
                )

                chartData.add(
                    ChartDataPoint(
                        label = formatDateLabel(date, days),
                        value = count.toFloat(),
                        date = date
                    )
                )
            }

            return@withContext chartData

        } catch (e: Exception) {
            Log.e(TAG, "Error getting blocked attempts chart data", e)
            return@withContext emptyList()
        }
    }

    /**
     * Get intervention completion chart data for last N days
     *
     * @param days Number of days to include
     * @return List of ChartDataPoint for line/bar chart
     */
    suspend fun getInterventionCompletionChartData(days: Int = 7): List<ChartDataPoint> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val chartData = mutableListOf<ChartDataPoint>()

            for (i in (days - 1) downTo 0) {
                val date = today.minusDays(i.toLong())
                val startTime = date.atStartOfDay()
                val endTime = date.atTime(23, 59, 59)

                val sessions = interventionSessionDao.getBetweenDates(
                    startTime = startTime.toEpochSecond(java.time.ZoneOffset.UTC),
                    endTime = endTime.toEpochSecond(java.time.ZoneOffset.UTC)
                )

                val completed = sessions.count { it.taskCompleted }
                val total = sessions.size
                val rate = if (total > 0) {
                    (completed.toFloat() / total.toFloat()) * 100
                } else {
                    0f
                }

                chartData.add(
                    ChartDataPoint(
                        label = formatDateLabel(date, days),
                        value = rate,
                        date = date
                    )
                )
            }

            return@withContext chartData

        } catch (e: Exception) {
            Log.e(TAG, "Error getting intervention completion chart data", e)
            return@withContext emptyList()
        }
    }

    /**
     * Get streak progress chart data for last N days
     *
     * @param days Number of days to include
     * @return List of ChartDataPoint for line chart
     */
    suspend fun getStreakProgressChartData(days: Int = 30): List<ChartDataPoint> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val chartData = mutableListOf<ChartDataPoint>()

            // Get all daily logs
            val logs = dailyLogDao.getBetweenDates(
                startDate = today.minusDays(days.toLong()).toEpochDay(),
                endDate = today.toEpochDay()
            )

            val logMap = logs.associateBy { it.date }

            var currentStreak = 0
            for (i in (days - 1) downTo 0) {
                val date = today.minusDays(i.toLong())
                val log = logMap[date]

                if (log != null && log.wasClean) {
                    currentStreak++
                } else {
                    currentStreak = 0
                }

                chartData.add(
                    ChartDataPoint(
                        label = formatDateLabel(date, days),
                        value = currentStreak.toFloat(),
                        date = date
                    )
                )
            }

            return@withContext chartData

        } catch (e: Exception) {
            Log.e(TAG, "Error getting streak progress chart data", e)
            return@withContext emptyList()
        }
    }

    /**
     * Get mood rating chart data for last N days
     *
     * @param days Number of days to include
     * @return List of ChartDataPoint for line chart
     */
    suspend fun getMoodRatingChartData(days: Int = 30): List<ChartDataPoint> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val chartData = mutableListOf<ChartDataPoint>()

            val logs = dailyLogDao.getBetweenDates(
                startDate = today.minusDays(days.toLong()).toEpochDay(),
                endDate = today.toEpochDay()
            )

            val logMap = logs.associateBy { it.date }

            for (i in (days - 1) downTo 0) {
                val date = today.minusDays(i.toLong())
                val log = logMap[date]
                val rating = log?.moodRating?.toFloat() ?: 0f

                chartData.add(
                    ChartDataPoint(
                        label = formatDateLabel(date, days),
                        value = rating,
                        date = date
                    )
                )
            }

            return@withContext chartData

        } catch (e: Exception) {
            Log.e(TAG, "Error getting mood rating chart data", e)
            return@withContext emptyList()
        }
    }

    /**
     * Get top domains pie chart data
     *
     * @param limit Number of top domains
     * @return List of PieChartDataPoint
     */
    suspend fun getTopDomainsPieChartData(limit: Int = 5): List<PieChartDataPoint> = withContext(Dispatchers.IO) {
        try {
            val topDomains = getTopBlockedDomains(limit)
            val total = topDomains.sumOf { it.count }

            return@withContext topDomains.map { domainCount ->
                val percentage = if (total > 0) {
                    (domainCount.count.toFloat() / total.toFloat()) * 100
                } else {
                    0f
                }

                PieChartDataPoint(
                    label = domainCount.domain,
                    value = domainCount.count.toFloat(),
                    percentage = percentage
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting top domains pie chart data", e)
            return@withContext emptyList()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TREND ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get blocked attempts trend
     *
     * Compares current period with previous period
     *
     * @param days Number of days in period
     * @return TrendData with change percentage
     */
    suspend fun getBlockedAttemptsTrend(days: Int = 7): TrendData = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()

            // Current period
            val currentStart = today.minusDays(days.toLong() - 1).atStartOfDay()
            val currentEnd = today.atTime(23, 59, 59)
            val currentCount = blockedAttemptDao.getCountBetween(
                startTime = currentStart.toEpochSecond(java.time.ZoneOffset.UTC),
                endTime = currentEnd.toEpochSecond(java.time.ZoneOffset.UTC)
            )

            // Previous period
            val previousStart = today.minusDays((days * 2).toLong() - 1).atStartOfDay()
            val previousEnd = today.minusDays(days.toLong()).atTime(23, 59, 59)
            val previousCount = blockedAttemptDao.getCountBetween(
                startTime = previousStart.toEpochSecond(java.time.ZoneOffset.UTC),
                endTime = previousEnd.toEpochSecond(java.time.ZoneOffset.UTC)
            )

            val changePercentage = if (previousCount > 0) {
                ((currentCount - previousCount).toDouble() / previousCount.toDouble()) * 100
            } else if (currentCount > 0) {
                100.0
            } else {
                0.0
            }

            return@withContext TrendData(
                currentValue = currentCount,
                previousValue = previousCount,
                changePercentage = changePercentage,
                isIncreasing = currentCount > previousCount,
                isImproving = currentCount < previousCount // Lower blocks = improvement
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error getting blocked attempts trend", e)
            return@withContext TrendData(0, 0, 0.0, false, false)
        }
    }

    /**
     * Get intervention completion trend
     *
     * @param days Number of days in period
     * @return TrendData with change percentage
     */
    suspend fun getInterventionCompletionTrend(days: Int = 7): TrendData = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()

            // Current period
            val currentStart = today.minusDays(days.toLong() - 1).atStartOfDay()
            val currentEnd = today.atTime(23, 59, 59)
            val currentSessions = interventionSessionDao.getBetweenDates(
                startTime = currentStart.toEpochSecond(java.time.ZoneOffset.UTC),
                endTime = currentEnd.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            val currentRate = if (currentSessions.isNotEmpty()) {
                (currentSessions.count { it.taskCompleted }.toDouble() / currentSessions.size.toDouble()) * 100
            } else {
                0.0
            }

            // Previous period
            val previousStart = today.minusDays((days * 2).toLong() - 1).atStartOfDay()
            val previousEnd = today.minusDays(days.toLong()).atTime(23, 59, 59)
            val previousSessions = interventionSessionDao.getBetweenDates(
                startTime = previousStart.toEpochSecond(java.time.ZoneOffset.UTC),
                endTime = previousEnd.toEpochSecond(java.time.ZoneOffset.UTC)
            )
            val previousRate = if (previousSessions.isNotEmpty()) {
                (previousSessions.count { it.taskCompleted }.toDouble() / previousSessions.size.toDouble()) * 100
            } else {
                0.0
            }

            val changePercentage = if (previousRate > 0) {
                ((currentRate - previousRate) / previousRate) * 100
            } else if (currentRate > 0) {
                100.0
            } else {
                0.0
            }

            return@withContext TrendData(
                currentValue = currentRate.toInt(),
                previousValue = previousRate.toInt(),
                changePercentage = changePercentage,
                isIncreasing = currentRate > previousRate,
                isImproving = currentRate > previousRate // Higher completion = improvement
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error getting intervention completion trend", e)
            return@withContext TrendData(0, 0, 0.0, false, false)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Format date label for chart
     *
     * @param date Date to format
     * @param totalDays Total days in chart (affects format)
     * @return Formatted label
     */
    private fun formatDateLabel(date: LocalDate, totalDays: Int): String {
        return when {
            totalDays <= 7 -> {
                // Show day of week for weekly view
                date.dayOfWeek.toString().substring(0, 3)
            }
            totalDays <= 31 -> {
                // Show day number for monthly view
                date.dayOfMonth.toString()
            }
            else -> {
                // Show month/day for longer periods
                "${date.monthValue}/${date.dayOfMonth}"
            }
        }
    }

    /**
     * Format time in seconds to human-readable string
     *
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    private fun formatTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                "${minutes}m ${secs}s"
            }
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
        }
    }

    /**
     * Get weekly summary
     *
     * @return Map with weekly statistics
     */
    suspend fun getWeeklySummary(): Map<String, Any> = withContext(Dispatchers.IO) {
        val blockedThisWeek = getBlockedAttemptsThisWeek()
        val blockedTrend = getBlockedAttemptsTrend(7)
        val interventionRate = getInterventionCompletionRate()
        val interventionTrend = getInterventionCompletionTrend(7)

        return@withContext mapOf(
            "blockedAttempts" to blockedThisWeek,
            "blockedTrend" to blockedTrend.changePercentage,
            "blockedTrendImproving" to blockedTrend.isImproving,
            "interventionCompletionRate" to interventionRate,
            "interventionTrend" to interventionTrend.changePercentage,
            "interventionTrendImproving" to interventionTrend.isImproving
        )
    }

    /**
     * Get monthly summary
     *
     * @return Map with monthly statistics
     */
    suspend fun getMonthlySummary(): Map<String, Any> = withContext(Dispatchers.IO) {
        val blockedThisMonth = getBlockedAttemptsThisMonth()
        val blockedTrend = getBlockedAttemptsTrend(30)
        val interventionRate = getInterventionCompletionRate()
        val interventionTrend = getInterventionCompletionTrend(30)

        return@withContext mapOf(
            "blockedAttempts" to blockedThisMonth,
            "blockedTrend" to blockedTrend.changePercentage,
            "blockedTrendImproving" to blockedTrend.isImproving,
            "interventionCompletionRate" to interventionRate,
            "interventionTrend" to interventionTrend.changePercentage,
            "interventionTrendImproving" to interventionTrend.isImproving
        )
    }

    /**
     * Refresh all statistics
     *
     * Updates all state flows with latest data
     */
    suspend fun refreshStatistics() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Refreshing statistics...")

            val totalBlocked = blockedAttemptDao.getCount()
            val completionRate = getInterventionCompletionRate()

            _totalBlockedAttempts.value = totalBlocked
            _interventionCompletionRate.value = completionRate

            Log.i(TAG, "Statistics refreshed: blocked=$totalBlocked, completion=$completionRate%")

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing statistics", e)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Chart data point for line/bar charts
 *
 * Used with Vico library for visualization
 */
data class ChartDataPoint(
    val label: String,
    val value: Float,
    val date: LocalDate
)

/**
 * Pie chart data point
 *
 * Used for pie/donut charts
 */
data class PieChartDataPoint(
    val label: String,
    val value: Float,
    val percentage: Float
)

/**
 * Trend data for comparing periods
 *
 * Shows change over time with improvement indicator
 */
data class TrendData(
    val currentValue: Int,
    val previousValue: Int,
    val changePercentage: Double,
    val isIncreasing: Boolean,
    val isImproving: Boolean
) {
    /**
     * Get formatted change string
     *
     * @return String like "+15.5%" or "-10.2%"
     */
    fun getFormattedChange(): String {
        val sign = if (changePercentage >= 0) "+" else ""
        return "$sign${String.format("%.1f", changePercentage)}%"
    }

    /**
     * Get trend indicator
     *
     * @return "↑" for increasing, "↓" for decreasing, "→" for no change
     */
    fun getTrendIndicator(): String {
        return when {
            changePercentage > 0 -> "↑"
            changePercentage < 0 -> "↓"
            else -> "→"
        }
    }
}

