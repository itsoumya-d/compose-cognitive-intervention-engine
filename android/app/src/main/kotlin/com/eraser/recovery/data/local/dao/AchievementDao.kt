package com.eraser.recovery.data.local.dao

import androidx.room.*
import com.eraser.recovery.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Achievement DAO
 * 
 * Data Access Object for achievement operations.
 */
@Dao
interface AchievementDao {
    
    @Query("SELECT * FROM achievements ORDER BY milestone ASC")
    fun getAll(): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY earnedDate DESC")
    fun getUnlocked(): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE isUnlocked = 0 ORDER BY milestone ASC")
    fun getLocked(): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE achievementId = :achievementId LIMIT 1")
    suspend fun getByAchievementId(achievementId: String): AchievementEntity?
    
    @Query("SELECT * FROM achievements WHERE milestone = :milestone LIMIT 1")
    suspend fun getByMilestone(milestone: Int): AchievementEntity?
    
    @Query("SELECT * FROM achievements WHERE isSpecial = 1")
    fun getSpecial(): Flow<List<AchievementEntity>>
    
    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int
    
    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getTotalCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)
    
    @Update
    suspend fun update(achievement: AchievementEntity)
    
    @Delete
    suspend fun delete(achievement: AchievementEntity)
    
    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
    
    /**
     * Unlock an achievement
     */
    @Query("""
        UPDATE achievements 
        SET isUnlocked = 1,
            earnedDate = :earnedDate
        WHERE achievementId = :achievementId
    """)
    suspend fun unlock(achievementId: String, earnedDate: Long)
    
    /**
     * Get next locked achievement by milestone
     */
    @Query("""
        SELECT * FROM achievements 
        WHERE isUnlocked = 0 
        ORDER BY milestone ASC 
        LIMIT 1
    """)
    suspend fun getNextToUnlock(): AchievementEntity?
}

