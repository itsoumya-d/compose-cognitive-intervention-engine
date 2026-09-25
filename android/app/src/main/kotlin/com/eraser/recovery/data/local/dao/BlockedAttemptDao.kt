package com.eraser.recovery.data.local.dao

import androidx.room.*
import com.eraser.recovery.data.local.entity.BlockedAttemptEntity
import kotlinx.coroutines.flow.Flow

/**
 * Blocked Attempt DAO
 * 
 * Data Access Object for blocked attempt operations.
 */
@Dao
interface BlockedAttemptDao {
    
    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BlockedAttemptEntity>>
    
    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<BlockedAttemptEntity>>
    
    @Query("SELECT * FROM blocked_attempts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BlockedAttemptEntity?
    
    @Query("SELECT * FROM blocked_attempts WHERE domain = :domain ORDER BY timestamp DESC")
    fun getByDomain(domain: String): Flow<List<BlockedAttemptEntity>>
    
    @Query("SELECT COUNT(*) FROM blocked_attempts")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE timestamp >= :startTime")
    suspend fun getCountSince(startTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE interventionCompleted = 1")
    suspend fun getCompletedCount(): Int
    
    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getCountBetween(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE DATE(timestamp / 1000, 'unixepoch', 'localtime') = :date")
    suspend fun getCountByDate(date: String): Int

    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE DATE(timestamp / 1000, 'unixepoch', 'localtime') >= :startDate AND DATE(timestamp / 1000, 'unixepoch', 'localtime') <= :endDate")
    suspend fun getCountBetweenDates(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM blocked_attempts")
    suspend fun getTotalCount(): Int

    @Query("SELECT domain, COUNT(*) as count FROM blocked_attempts GROUP BY domain ORDER BY count DESC LIMIT :limit")
    suspend fun getTopBlockedDomains(limit: Int): List<DomainCount>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedAttempt: BlockedAttemptEntity): Long
    
    @Update
    suspend fun update(blockedAttempt: BlockedAttemptEntity)
    
    @Delete
    suspend fun delete(blockedAttempt: BlockedAttemptEntity)
    
    @Query("DELETE FROM blocked_attempts")
    suspend fun deleteAll()
    
    @Query("DELETE FROM blocked_attempts WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}

/**
 * Domain count result for statistics
 */
data class DomainCount(
    val domain: String,
    val count: Int
)

