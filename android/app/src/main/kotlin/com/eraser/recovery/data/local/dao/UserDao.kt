package com.eraser.recovery.data.local.dao

import androidx.room.*
import com.eraser.recovery.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * User DAO
 * 
 * Data Access Object for user operations.
 * Single user app - always uses ID 1.
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM user WHERE id = 1 LIMIT 1")
    fun getUser(): Flow<UserEntity?>
    
    @Query("SELECT * FROM user WHERE id = 1 LIMIT 1")
    suspend fun getUserOnce(): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long
    
    @Update
    suspend fun update(user: UserEntity)
    
    @Delete
    suspend fun delete(user: UserEntity)
    
    @Query("DELETE FROM user")
    suspend fun deleteAll()
    
    /**
     * Update streak information
     */
    @Query("""
        UPDATE user 
        SET currentStreak = :currentStreak,
            longestStreak = :longestStreak,
            lastCheckInDate = :lastCheckInDate,
            updatedAt = :updatedAt
        WHERE id = 1
    """)
    suspend fun updateStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastCheckInDate: Long,
        updatedAt: Long
    )
    
    /**
     * Update VPN status
     */
    @Query("""
        UPDATE user 
        SET isVpnEnabled = :isEnabled,
            updatedAt = :updatedAt
        WHERE id = 1
    """)
    suspend fun updateVpnStatus(isEnabled: Boolean, updatedAt: Long)
    
    /**
     * Update journey status
     */
    @Query("""
        UPDATE user 
        SET isJourneyActive = :isActive,
            journeyStartTime = :startTime,
            journeyStopTime = :stopTime,
            updatedAt = :updatedAt
        WHERE id = 1
    """)
    suspend fun updateJourneyStatus(
        isActive: Boolean,
        startTime: Long?,
        stopTime: Long?,
        updatedAt: Long
    )
    
    /**
     * Increment total days clean
     */
    @Query("""
        UPDATE user 
        SET totalDaysClean = totalDaysClean + 1,
            updatedAt = :updatedAt
        WHERE id = 1
    """)
    suspend fun incrementTotalDaysClean(updatedAt: Long)
}

