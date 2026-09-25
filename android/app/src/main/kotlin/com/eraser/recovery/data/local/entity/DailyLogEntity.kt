package com.eraser.recovery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Daily Log Entity
 * 
 * Tracks daily check-ins and user status.
 * Migrated from Flutter app's DailyLogModel.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Daily check-ins improve accountability
 * - Mood tracking helps identify patterns
 * - Trigger logging supports relapse prevention
 */
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Log Details
    val date: LocalDate,
    val wasClean: Boolean = true,
    val notes: String? = null,
    val moodRating: Int? = null, // 1-5 scale
    val triggers: String? = null, // Comma-separated list
    val blockedAttempts: Int? = null,
    
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Get formatted date
     */
    fun getFormattedDate(): String {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
    
    /**
     * Check if this log is for today
     */
    fun isToday(): Boolean {
        return date == LocalDate.now()
    }
    
    /**
     * Get status text
     */
    fun getStatusText(): String {
        return if (wasClean) "Clean" else "Relapse"
    }
    
    /**
     * Get mood text
     */
    fun getMoodText(): String {
        return when (moodRating) {
            1 -> "Very Bad"
            2 -> "Bad"
            3 -> "Neutral"
            4 -> "Good"
            5 -> "Very Good"
            else -> "Not Rated"
        }
    }
    
    /**
     * Get triggers as list
     */
    fun getTriggersList(): List<String> {
        return triggers?.split(",")?.map { it.trim() } ?: emptyList()
    }
    
    /**
     * Check if has notes
     */
    fun hasNotes(): Boolean {
        return !notes.isNullOrBlank()
    }
}

