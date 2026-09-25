package com.eraser.recovery.data.local.dao

import androidx.room.*
import com.eraser.recovery.data.local.entity.FlashcardEntity
import com.eraser.recovery.data.local.entity.MessageCategory
import kotlinx.coroutines.flow.Flow

/**
 * Flashcard DAO
 * 
 * Data Access Object for flashcard operations.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Use Flow for reactive queries
 * - Support filtering by category and effectiveness
 * - Enable weighted random selection
 */
@Dao
interface FlashcardDao {
    
    @Query("SELECT * FROM flashcards WHERE isActive = 1")
    fun getAllActive(): Flow<List<FlashcardEntity>>
    
    @Query("SELECT * FROM flashcards WHERE isActive = 1")
    suspend fun getAllActiveList(): List<FlashcardEntity>
    
    @Query("SELECT * FROM flashcards WHERE flashcardId = :flashcardId LIMIT 1")
    suspend fun getByFlashcardId(flashcardId: String): FlashcardEntity?
    
    @Query("SELECT * FROM flashcards WHERE messageCategory = :category AND isActive = 1")
    fun getByCategory(category: MessageCategory): Flow<List<FlashcardEntity>>
    
    @Query("SELECT * FROM flashcards WHERE isActive = 1 ORDER BY effectivenessScore DESC LIMIT :limit")
    suspend fun getTopEffective(limit: Int): List<FlashcardEntity>
    
    @Query("SELECT * FROM flashcards WHERE isActive = 1 ORDER BY timesShown ASC LIMIT :limit")
    suspend fun getLeastShown(limit: Int): List<FlashcardEntity>
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE isActive = 1")
    suspend fun getActiveCount(): Int
    
    @Query("SELECT AVG(effectivenessScore) FROM flashcards WHERE isActive = 1")
    suspend fun getAverageEffectiveness(): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flashcard: FlashcardEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flashcards: List<FlashcardEntity>)
    
    @Update
    suspend fun update(flashcard: FlashcardEntity)
    
    @Delete
    suspend fun delete(flashcard: FlashcardEntity)
    
    @Query("DELETE FROM flashcards")
    suspend fun deleteAll()
    
    /**
     * Update flashcard statistics after showing
     */
    @Query("""
        UPDATE flashcards 
        SET timesShown = timesShown + 1,
            lastShownAt = :timestamp
        WHERE flashcardId = :flashcardId
    """)
    suspend fun incrementShown(flashcardId: String, timestamp: Long)
    
    /**
     * Update flashcard statistics after completion
     */
    @Query("""
        UPDATE flashcards 
        SET timesCompleted = timesCompleted + 1,
            averageCompletionTime = :completionTime
        WHERE flashcardId = :flashcardId
    """)
    suspend fun incrementCompleted(flashcardId: String, completionTime: Long)
    
    /**
     * Update flashcard statistics after skipping
     */
    @Query("""
        UPDATE flashcards 
        SET timesSkipped = timesSkipped + 1
        WHERE flashcardId = :flashcardId
    """)
    suspend fun incrementSkipped(flashcardId: String)
    
    /**
     * Update effectiveness score
     */
    @Query("""
        UPDATE flashcards 
        SET effectivenessScore = :score
        WHERE flashcardId = :flashcardId
    """)
    suspend fun updateEffectivenessScore(flashcardId: String, score: Double)
}

