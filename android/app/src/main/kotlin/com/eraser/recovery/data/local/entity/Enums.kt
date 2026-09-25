package com.eraser.recovery.data.local.entity

/**
 * Enums for Database Entities
 * 
 * Migrated from Flutter app's Isar models
 */

/**
 * Task Type for Flashcards
 */
enum class TaskType {
    PHYSICAL,
    MINDFULNESS,
    SOCIAL,
    CREATIVE,
    REFLECTION
}

/**
 * Difficulty Level for Flashcards
 */
enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD
}

/**
 * Message Category for Flashcards
 */
enum class MessageCategory {
    GOAL_ORIENTED,
    CONSEQUENCE_AWARENESS,
    EMPOWERMENT,
    MINDFULNESS,
    REDIRECTION
}

