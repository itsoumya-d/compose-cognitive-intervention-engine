package com.eraser.recovery

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Eraser Application Class
 * 
 * Main application entry point for the Eraser app.
 * Initializes Hilt dependency injection, notification channels, and WorkManager.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Initialize notification channels on app start
 * - Set up WorkManager for background tasks
 * - Configure Hilt for dependency injection
 * - Keep initialization lightweight for fast app startup
 */
@HiltAndroidApp
class EraserApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Create notification channels
        createNotificationChannels()
        
        // Initialize WorkManager (handled by Hilt)
        // Background tasks will be scheduled by services
    }

    /**
     * Create notification channels for Android O+
     * 
     * Channels:
     * - VPN Service: Foreground service notification
     * - Achievements: Badge unlock notifications
     * - Reminders: Daily check-in and streak reminders
     * - Blocking: Content blocking notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // VPN Service Channel (High priority - always visible)
            val vpnChannel = NotificationChannel(
                CHANNEL_VPN_SERVICE,
                "VPN Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when VPN protection is active"
                setShowBadge(false)
            }
            
            // Achievement Channel (Default priority)
            val achievementChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for unlocked achievements and milestones"
                setShowBadge(true)
            }
            
            // Reminder Channel (Default priority)
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily check-in and streak reminders"
                setShowBadge(true)
            }
            
            // Blocking Channel (Low priority)
            val blockingChannel = NotificationChannel(
                CHANNEL_BLOCKING,
                "Content Blocking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications about blocked content"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(vpnChannel, achievementChannel, reminderChannel, blockingChannel)
            )
        }
    }

    companion object {
        // Notification Channel IDs
        const val CHANNEL_VPN_SERVICE = "vpn_service"
        const val CHANNEL_ACHIEVEMENTS = "achievements"
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_BLOCKING = "blocking"
    }
}

