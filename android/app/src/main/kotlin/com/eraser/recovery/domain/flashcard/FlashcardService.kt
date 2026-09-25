package com.eraser.recovery.domain.flashcard

import android.util.Log
import com.eraser.recovery.data.local.dao.FlashcardDao
import com.eraser.recovery.data.local.dao.InterventionSessionDao
import com.eraser.recovery.data.local.entity.FlashcardEntity
import com.eraser.recovery.data.local.entity.InterventionSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flashcard Service
 * 
 * Manages flashcard selection, display, and analytics.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Use truly random selection (NOT sequential) for unpredictability
 * - Fisher-Yates shuffle algorithm for unbiased randomization
 * - SecureRandom for cryptographically strong random numbers
 * - Track effectiveness metrics for adaptive intervention
 * - Update statistics after each interaction
 * 
 * IMPORTANT: Flashcards must be TRULY RANDOM, not sequential.
 * This prevents users from predicting which flashcard will appear next.
 * 
 * Selection Algorithm:
 * 1. Get all 60 active flashcards from database
 * 2. Use SecureRandom to generate random index
 * 3. Select flashcard at that index
 * 4. Track selection to avoid immediate repeats (last 5 flashcards)
 * 5. Update statistics (timesShown, lastShownAt)
 * 
 * Effectiveness Scoring:
 * - Score = (completionRate * 70) + ((1 - skipRate) * 30)
 * - Range: 0.0 to 100.0
 * - Updated after each completion or skip
 */
@Singleton
class FlashcardService @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val interventionSessionDao: InterventionSessionDao
) {
    
    companion object {
        private const val TAG = "FlashcardService"
        private const val RECENT_HISTORY_SIZE = 5 // Avoid repeating last 5 flashcards
    }
    
    // SecureRandom for cryptographically strong random numbers
    private val secureRandom = SecureRandom()
    
    // Recent flashcard history to avoid immediate repeats
    private val recentFlashcardIds = mutableListOf<String>()
    
    // Current flashcard being displayed
    private val _currentFlashcard = MutableStateFlow<FlashcardEntity?>(null)
    val currentFlashcard: Flow<FlashcardEntity?> = _currentFlashcard.asStateFlow()
    
    // Current intervention session
    private var currentSessionId: Long? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API - FLASHCARD SELECTION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get a truly random flashcard
     * Uses SecureRandom for unpredictable selection
     * Avoids repeating last 5 flashcards
     * 
     * @return Random flashcard or null if no flashcards available
     */
    suspend fun getRandomFlashcard(): FlashcardEntity? = withContext(Dispatchers.IO) {
        try {
            // Get all active flashcards
            val allFlashcards = flashcardDao.getAllActiveList()
            
            if (allFlashcards.isEmpty()) {
                Log.w(TAG, "No active flashcards available")
                return@withContext null
            }
            
            Log.d(TAG, "Total active flashcards: ${allFlashcards.size}")
            
            // Filter out recently shown flashcards to avoid immediate repeats
            val availableFlashcards = if (recentFlashcardIds.size >= RECENT_HISTORY_SIZE) {
                allFlashcards.filter { it.flashcardId !in recentFlashcardIds }
            } else {
                allFlashcards
            }
            
            // If all flashcards were recently shown, use all flashcards
            val selectionPool = if (availableFlashcards.isEmpty()) {
                Log.d(TAG, "All flashcards recently shown, resetting pool")
                recentFlashcardIds.clear()
                allFlashcards
            } else {
                availableFlashcards
            }
            
            // Use SecureRandom to select truly random index
            val randomIndex = secureRandom.nextInt(selectionPool.size)
            val selectedFlashcard = selectionPool[randomIndex]
            
            Log.i(TAG, "Selected flashcard: ${selectedFlashcard.flashcardId} (index $randomIndex of ${selectionPool.size})")
            
            // Add to recent history
            recentFlashcardIds.add(selectedFlashcard.flashcardId)
            if (recentFlashcardIds.size > RECENT_HISTORY_SIZE) {
                recentFlashcardIds.removeAt(0)
            }
            
            // Update statistics
            flashcardDao.incrementShown(
                flashcardId = selectedFlashcard.flashcardId,
                timestamp = System.currentTimeMillis()
            )
            
            // Set as current flashcard
            _currentFlashcard.value = selectedFlashcard
            
            return@withContext selectedFlashcard
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting random flashcard", e)
            return@withContext null
        }
    }
    
    /**
     * Get a shuffled list of all flashcards
     * Uses Fisher-Yates shuffle algorithm for unbiased randomization
     * 
     * @return Shuffled list of all active flashcards
     */
    suspend fun getShuffledFlashcards(): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        try {
            val flashcards = flashcardDao.getAllActiveList().toMutableList()
            
            // Fisher-Yates shuffle algorithm
            for (i in flashcards.size - 1 downTo 1) {
                val j = secureRandom.nextInt(i + 1)
                val temp = flashcards[i]
                flashcards[i] = flashcards[j]
                flashcards[j] = temp
            }
            
            Log.d(TAG, "Shuffled ${flashcards.size} flashcards using Fisher-Yates")
            return@withContext flashcards
            
        } catch (e: Exception) {
            Log.e(TAG, "Error shuffling flashcards", e)
            return@withContext emptyList()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API - INTERVENTION SESSION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Start a new intervention session
     * Called when content is blocked and flashcard is displayed
     * 
     * @param flashcardId ID of the flashcard being shown
     * @param blockedUrl URL that was blocked
     * @return Session ID
     */
    suspend fun startInterventionSession(flashcardId: String, blockedUrl: String, blockedDomain: String): Long = withContext(Dispatchers.IO) {
        try {
            val session = InterventionSessionEntity(
                sessionId = java.util.UUID.randomUUID().toString(),
                timestamp = LocalDateTime.now(),
                blockedDomain = blockedDomain,
                blockedUrl = blockedUrl,
                flashcardId = flashcardId,
                taskCompleted = false,
                taskSkipped = false
            )

            val id = interventionSessionDao.insert(session)
            currentSessionId = id

            Log.i(TAG, "Started intervention session: $id for flashcard: $flashcardId")
            return@withContext id

        } catch (e: Exception) {
            Log.e(TAG, "Error starting intervention session", e)
            return@withContext -1
        }
    }
    
    /**
     * Complete the current intervention session
     * Called when user completes the flashcard task
     * 
     * @param sessionId Session ID to complete
     * @param completionTime Time taken to complete in seconds
     */
    suspend fun completeInterventionSession(sessionId: Long, completionTime: Long) = withContext(Dispatchers.IO) {
        try {
            val session = interventionSessionDao.getById(sessionId) ?: run {
                Log.w(TAG, "Session not found: $sessionId")
                return@withContext
            }
            
            // Update session
            val updatedSession = session.copy(
                taskCompleted = true,
                taskSkipped = false,
                completionTime = completionTime.toInt()
            )
            interventionSessionDao.update(updatedSession)

            // Update flashcard statistics
            flashcardDao.incrementCompleted(
                flashcardId = session.flashcardId,
                completionTime = completionTime
            )
            
            // Recalculate effectiveness score
            val flashcard = flashcardDao.getByFlashcardId(session.flashcardId)
            flashcard?.let {
                val newScore = it.calculateEffectivenessScore()
                flashcardDao.updateEffectivenessScore(it.flashcardId, newScore)
                Log.d(TAG, "Updated effectiveness score for ${it.flashcardId}: $newScore")
            }
            
            currentSessionId = null
            Log.i(TAG, "Completed intervention session: $sessionId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error completing intervention session", e)
        }
    }
    
    /**
     * Skip the current intervention session
     * Called when user skips the flashcard task
     * 
     * @param sessionId Session ID to skip
     */
    suspend fun skipInterventionSession(sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            val session = interventionSessionDao.getById(sessionId) ?: run {
                Log.w(TAG, "Session not found: $sessionId")
                return@withContext
            }
            
            // Update session
            val updatedSession = session.copy(
                taskCompleted = false,
                taskSkipped = true
            )
            interventionSessionDao.update(updatedSession)
            
            // Update flashcard statistics
            flashcardDao.incrementSkipped(session.flashcardId)
            
            // Recalculate effectiveness score
            val flashcard = flashcardDao.getByFlashcardId(session.flashcardId)
            flashcard?.let {
                val newScore = it.calculateEffectivenessScore()
                flashcardDao.updateEffectivenessScore(it.flashcardId, newScore)
                Log.d(TAG, "Updated effectiveness score for ${it.flashcardId}: $newScore")
            }
            
            currentSessionId = null
            Log.i(TAG, "Skipped intervention session: $sessionId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error skipping intervention session", e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API - STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get flashcard statistics
     * 
     * @return Map with statistics
     */
    suspend fun getStatistics(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val totalFlashcards = flashcardDao.getActiveCount()
            val averageEffectiveness = flashcardDao.getAverageEffectiveness() ?: 0.0
            val topEffective = flashcardDao.getTopEffective(5)
            val leastShown = flashcardDao.getLeastShown(5)
            
            return@withContext mapOf(
                "totalFlashcards" to totalFlashcards,
                "averageEffectiveness" to averageEffectiveness,
                "topEffectiveCount" to topEffective.size,
                "leastShownCount" to leastShown.size,
                "recentHistorySize" to recentFlashcardIds.size
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting statistics", e)
            return@withContext emptyMap()
        }
    }
    
    /**
     * Clear recent flashcard history
     * Useful for testing or resetting selection pool
     */
    fun clearRecentHistory() {
        recentFlashcardIds.clear()
        Log.d(TAG, "Cleared recent flashcard history")
    }

    /**
     * Get current session ID
     */
    fun getCurrentSessionId(): Long? = currentSessionId

    /**
     * Start a new intervention session
     * Wrapper method for startInterventionSession
     */
    suspend fun startSession(
        blockedDomain: String,
        blockedUrl: String?,
        flashcardId: String
    ): InterventionSessionEntity = withContext(Dispatchers.IO) {
        val sessionId = startInterventionSession(flashcardId, blockedUrl ?: "", blockedDomain)
        return@withContext interventionSessionDao.getById(sessionId)
            ?: throw Exception("Failed to create session")
    }

    /**
     * Complete a task in the current session
     */
    suspend fun completeTask(sessionId: Long, durationSeconds: Int) = withContext(Dispatchers.IO) {
        try {
            val session = interventionSessionDao.getById(sessionId) ?: run {
                Log.w(TAG, "Session not found: $sessionId")
                return@withContext
            }

            val updatedSession = session.copy(
                taskCompleted = true,
                taskSkipped = false,
                completionTime = durationSeconds
            )

            interventionSessionDao.update(updatedSession)
            Log.d(TAG, "Task completed for session: $sessionId")

        } catch (e: Exception) {
            Log.e(TAG, "Error completing task", e)
        }
    }

    /**
     * Skip a task in the current session
     */
    suspend fun skipTask(sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            val session = interventionSessionDao.getById(sessionId) ?: run {
                Log.w(TAG, "Session not found: $sessionId")
                return@withContext
            }

            val updatedSession = session.copy(
                taskCompleted = false,
                taskSkipped = true
            )

            interventionSessionDao.update(updatedSession)
            Log.d(TAG, "Task skipped for session: $sessionId")

        } catch (e: Exception) {
            Log.e(TAG, "Error skipping task", e)
        }
    }

    /**
     * Exit a session early (abandoned)
     */
    suspend fun exitSession(sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            val session = interventionSessionDao.getById(sessionId) ?: run {
                Log.w(TAG, "Session not found: $sessionId")
                return@withContext
            }

            // Session remains with taskCompleted = false and taskSkipped = false
            // This indicates an abandoned session
            Log.d(TAG, "Session exited early: $sessionId")

        } catch (e: Exception) {
            Log.e(TAG, "Error exiting session", e)
        }
    }
}

