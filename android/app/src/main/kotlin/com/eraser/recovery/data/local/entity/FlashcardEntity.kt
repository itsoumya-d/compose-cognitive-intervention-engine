package com.eraser.recovery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Flashcard Entity
 * 
 * Stores motivational flashcards with intervention tasks.
 * Migrated from Flutter app's FlashcardModel.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Track effectiveness metrics for adaptive intervention
 * - Store completion statistics for analytics
 * - Support weighted random selection based on effectiveness
 */
@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Core Fields
    val flashcardId: String,
    val frontMessage: String,
    val backTask: String,
    val taskType: TaskType,
    val taskDuration: Int, // in minutes
    val difficultyLevel: DifficultyLevel,
    val messageCategory: MessageCategory,
    
    // Analytics Fields
    val timesShown: Int = 0,
    val timesCompleted: Int = 0,
    val timesSkipped: Int = 0,
    val effectivenessScore: Double = 0.0,
    val averageCompletionTime: Long = 0, // in seconds
    
    // Status Fields
    val isActive: Boolean = true,
    val lastShownAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Calculate effectiveness score based on completion rate
     * Score = (completionRate * 70) + ((1 - skipRate) * 30)
     */
    fun calculateEffectivenessScore(): Double {
        if (timesShown == 0) return 0.0
        
        val completionRate = timesCompleted.toDouble() / timesShown
        val skipRate = timesSkipped.toDouble() / timesShown
        
        return (completionRate * 70) + ((1 - skipRate) * 30)
    }
    
    /**
     * Get completion rate as percentage
     */
    fun getCompletionRate(): Double {
        if (timesShown == 0) return 0.0
        return (timesCompleted.toDouble() / timesShown) * 100
    }
    
    /**
     * Get skip rate as percentage
     */
    fun getSkipRate(): Double {
        if (timesShown == 0) return 0.0
        return (timesSkipped.toDouble() / timesShown) * 100
    }
}

