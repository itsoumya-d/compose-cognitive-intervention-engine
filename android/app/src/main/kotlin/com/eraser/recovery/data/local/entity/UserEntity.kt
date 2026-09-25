package com.eraser.recovery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * User Entity
 * 
 * Represents the app user's recovery journey data.
 * Migrated from Flutter app's UserModel.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Track streak and progress metrics
 * - Support daily check-in functionality
 * - Monitor VPN protection status
 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: Long = 1, // Single user app, always ID 1
    
    // Journey Fields
    val startDate: LocalDate,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDaysClean: Int = 0,
    val lastCheckInDate: LocalDate? = null,
    
    // Status Fields
    val isActive: Boolean = true,
    val isJourneyActive: Boolean = false,
    val journeyStartTime: LocalDateTime? = null,
    val journeyStopTime: LocalDateTime? = null,
    val isVpnEnabled: Boolean = false,
    
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Calculate days since journey start
     */
    fun daysSinceStart(): Long {
        return ChronoUnit.DAYS.between(startDate, LocalDate.now())
    }
    
    /**
     * Check if user has checked in today
     */
    fun hasCheckedInToday(): Boolean {
        return lastCheckInDate == LocalDate.now()
    }
    
    /**
     * Check if streak is at risk (no check-in yesterday)
     */
    fun isStreakAtRisk(): Boolean {
        val yesterday = LocalDate.now().minusDays(1)
        return lastCheckInDate != null && lastCheckInDate!! < yesterday
    }
    
    /**
     * Get journey duration in days
     */
    fun getJourneyDuration(): Long {
        return if (journeyStartTime != null) {
            val endTime = journeyStopTime ?: LocalDateTime.now()
            ChronoUnit.DAYS.between(journeyStartTime, endTime)
        } else {
            0
        }
    }
}

