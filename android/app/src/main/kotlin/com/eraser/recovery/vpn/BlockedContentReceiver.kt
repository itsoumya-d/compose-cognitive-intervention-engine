package com.eraser.recovery.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for handling blocked content events
 * Launches the app and navigates to Safe Page when content is blocked
 */
class BlockedContentReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BlockedContentReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.eraser.CONTENT_BLOCKED") {
            return
        }
        
        val domain = intent.getStringExtra("domain") ?: return
        val url = intent.getStringExtra("url")
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        
        Log.d(TAG, "Received blocked content event for domain: $domain")
        
        // Launch app and navigate to Safe Page
        launchSafePage(context, domain, url)
    }
    
    private fun launchSafePage(context: Context, domain: String, url: String?) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            
            if (launchIntent != null) {
                launchIntent.action = "com.eraser.SHOW_SAFE_PAGE"
                launchIntent.putExtra("blockedDomain", domain)
                launchIntent.putExtra("blockedUrl", url)
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched Safe Page for domain: $domain")
            } else {
                Log.e(TAG, "Could not get launch intent for package")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Safe Page", e)
        }
    }
}

