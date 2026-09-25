package com.eraser.recovery.data.local.dao

import androidx.room.*
import com.eraser.recovery.data.local.entity.InterventionSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Intervention Session DAO
 * 
 * Data Access Object for intervention session operations.
 */
@Dao
interface InterventionSessionDao {
    
    @Query("SELECT * FROM intervention_sessions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<InterventionSessionEntity>>
    
    @Query("SELECT * FROM intervention_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<InterventionSessionEntity>>

    @Query("SELECT * FROM intervention_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InterventionSessionEntity?

    @Query("SELECT * FROM intervention_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): InterventionSessionEntity?
    
    @Query("SELECT * FROM intervention_sessions WHERE flashcardId = :flashcardId ORDER BY timestamp DESC")
    fun getByFlashcardId(flashcardId: String): Flow<List<InterventionSessionEntity>>
    
    @Query("SELECT COUNT(*) FROM intervention_sessions")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM intervention_sessions WHERE taskCompleted = 1")
    suspend fun getCompletedCount(): Int
    
    @Query("SELECT COUNT(*) FROM intervention_sessions WHERE taskSkipped = 1")
    suspend fun getSkippedCount(): Int
    
    @Query("SELECT AVG(completionTime) FROM intervention_sessions WHERE taskCompleted = 1")
    suspend fun getAverageCompletionTime(): Double?
    
    @Query("SELECT * FROM intervention_sessions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getBetweenDates(startTime: Long, endTime: Long): List<InterventionSessionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: InterventionSessionEntity): Long
    
    @Update
    suspend fun update(session: InterventionSessionEntity)
    
    @Delete
    suspend fun delete(session: InterventionSessionEntity)
    
    @Query("DELETE FROM intervention_sessions")
    suspend fun deleteAll()
    
    @Query("DELETE FROM intervention_sessions WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}

