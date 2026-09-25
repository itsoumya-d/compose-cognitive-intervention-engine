package com.eraser.recovery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Achievement Entity
 * 
 * Represents milestone badges.
 * Migrated from Flutter app's AchievementModel.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Gamification increases user engagement
 * - Milestone achievements motivate continued use
 * - Visual badges provide positive reinforcement
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Achievement Details
    val achievementId: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val milestone: Int, // days required
    
    // Status Fields
    val earnedDate: LocalDateTime? = null,
    val isUnlocked: Boolean = false,
    val isSpecial: Boolean = false,
    
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if achievement is locked
     */
    fun isLocked(): Boolean {
        return !isUnlocked
    }
    
    /**
     * Get display status
     */
    fun getStatus(): String {
        return if (isUnlocked) "Earned" else "Locked"
    }
    
    /**
     * Get formatted earned date
     */
    fun getFormattedEarnedDate(): String {
        return earnedDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) 
            ?: "Not earned yet"
    }
    
    /**
     * Get milestone description
     */
    fun getMilestoneDescription(): String {
        return when {
            milestone == 1 -> "1 day"
            milestone < 30 -> "$milestone days"
            milestone < 365 -> "${milestone / 30} months"
            else -> "${milestone / 365} year${if (milestone > 365) "s" else ""}"
        }
    }
}

