package com.eraser.recovery.data.local

import androidx.room.TypeConverter
import com.eraser.recovery.data.local.entity.DifficultyLevel
import com.eraser.recovery.data.local.entity.MessageCategory
import com.eraser.recovery.data.local.entity.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Type Converters for Room Database
 * 
 * Converts complex types to/from primitive types for database storage.
 * 
 * Research findings:
 * - Store LocalDateTime as epoch milliseconds for efficiency
 * - Store enums as strings for readability and migration safety
 * - Handle nullable types properly
 */
class Converters {
    
    // ========== LocalDateTime Converters ==========
    
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        }
    }
    
    @TypeConverter
    fun dateTimeToTimestamp(dateTime: LocalDateTime?): Long? {
        return dateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }
    
    // ========== LocalDate Converters ==========
    
    @TypeConverter
    fun fromDateTimestamp(value: Long?): LocalDate? {
        return value?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }
    
    // ========== Enum Converters ==========
    
    @TypeConverter
    fun fromTaskType(value: String?): TaskType? {
        return value?.let { TaskType.valueOf(it) }
    }
    
    @TypeConverter
    fun taskTypeToString(taskType: TaskType?): String? {
        return taskType?.name
    }
    
    @TypeConverter
    fun fromDifficultyLevel(value: String?): DifficultyLevel? {
        return value?.let { DifficultyLevel.valueOf(it) }
    }
    
    @TypeConverter
    fun difficultyLevelToString(difficultyLevel: DifficultyLevel?): String? {
        return difficultyLevel?.name
    }
    
    @TypeConverter
    fun fromMessageCategory(value: String?): MessageCategory? {
        return value?.let { MessageCategory.valueOf(it) }
    }
    
    @TypeConverter
    fun messageCategoryToString(messageCategory: MessageCategory?): String? {
        return messageCategory?.name
    }
}

