package com.eraser.recovery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Blocked Attempt Entity
 * 
 * Tracks content blocking events.
 * Migrated from Flutter app's BlockedAttemptModel.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Log all blocking attempts for analytics
 * - Track intervention completion
 * - Support history and reporting features
 */
@Entity(tableName = "blocked_attempts")
data class BlockedAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Blocking Details
    val timestamp: LocalDateTime,
    val url: String,
    val domain: String,
    
    // Intervention Details
    val interventionCompleted: Boolean = false,
    val interventionType: String? = null,
    val interventionDuration: Int? = null, // in seconds
    
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Get formatted timestamp
     */
    fun getFormattedTimestamp(): String {
        return timestamp.toString()
    }
    
    /**
     * Check if intervention was completed
     */
    fun wasInterventionCompleted(): Boolean {
        return interventionCompleted
    }
    
    /**
     * Get intervention duration in minutes
     */
    fun getInterventionDurationMinutes(): Int? {
        return interventionDuration?.let { it / 60 }
    }
}

