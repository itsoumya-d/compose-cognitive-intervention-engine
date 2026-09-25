package com.eraser.recovery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Intervention Session Entity
 * 
 * Tracks each intervention session when user attempts to access blocked content.
 * Migrated from Flutter app's InterventionSessionModel.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Track intervention effectiveness
 * - Analyze user behavior patterns
 * - Support adaptive intervention strategies
 */
@Entity(tableName = "intervention_sessions")
data class InterventionSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Session Details
    val sessionId: String,
    val timestamp: LocalDateTime,
    val blockedDomain: String,
    val blockedUrl: String? = null,
    
    // Flashcard Details
    val flashcardId: String,
    
    // Task Completion
    val taskCompleted: Boolean = false,
    val taskSkipped: Boolean = false,
    val completionTime: Int? = null, // in seconds
    
    // User Feedback
    val userEmotion: String? = null,
    val userNotes: String? = null,
    
    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Get session outcome
     */
    fun getOutcome(): String {
        return when {
            taskCompleted -> "Completed"
            taskSkipped -> "Skipped"
            else -> "Abandoned"
        }
    }
    
    /**
     * Get completion time in minutes
     */
    fun getCompletionTimeMinutes(): Int? {
        return completionTime?.let { it / 60 }
    }
    
    /**
     * Check if session was successful
     */
    fun wasSuccessful(): Boolean {
        return taskCompleted
    }
}

