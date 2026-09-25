package com.eraser.recovery.data.local.dao

import androidx.room.*
import com.eraser.recovery.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Daily Log DAO
 * 
 * Data Access Object for daily log operations.
 */
@Dao
interface DailyLogDao {
    
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAll(): Flow<List<DailyLogEntity>>
    
    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<DailyLogEntity>>
    
    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: Long): DailyLogEntity?
    
    @Query("SELECT * FROM daily_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: Long, endDate: Long): List<DailyLogEntity>
    
    @Query("SELECT * FROM daily_logs WHERE wasClean = 1 ORDER BY date DESC")
    fun getCleanDays(): Flow<List<DailyLogEntity>>
    
    @Query("SELECT * FROM daily_logs WHERE wasClean = 0 ORDER BY date DESC")
    fun getRelapseDays(): Flow<List<DailyLogEntity>>

    @Query("SELECT COUNT(*) FROM daily_logs")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM daily_logs WHERE wasClean = 1")
    suspend fun getCleanDaysCount(): Int
    
    @Query("SELECT COUNT(*) FROM daily_logs WHERE wasClean = 0")
    suspend fun getRelapseDaysCount(): Int
    
    @Query("SELECT AVG(moodRating) FROM daily_logs WHERE moodRating IS NOT NULL")
    suspend fun getAverageMoodRating(): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyLog: DailyLogEntity): Long
    
    @Update
    suspend fun update(dailyLog: DailyLogEntity)
    
    @Delete
    suspend fun delete(dailyLog: DailyLogEntity)
    
    @Query("DELETE FROM daily_logs")
    suspend fun deleteAll()
    
    @Query("DELETE FROM daily_logs WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: Long)
}

