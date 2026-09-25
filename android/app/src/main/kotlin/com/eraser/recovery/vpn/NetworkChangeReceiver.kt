package com.eraser.recovery.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * Network Change Receiver
 * Detects network connectivity changes and triggers VPN reconnection if needed
 */
class NetworkChangeReceiver : BroadcastReceiver() {
    private val TAG = "NetworkChangeReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        Log.d(TAG, "Network change detected: $action")

        // Check if network is available
        if (isNetworkAvailable(context)) {
            Log.d(TAG, "Network is available")
            // VPN reconnection logic can be added here if needed
        } else {
            Log.d(TAG, "Network is not available")
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun triggerReconnection(context: Context) {
        try {
            // Start VPN service with reconnection flag
            val intent = Intent(context, EraserVpnService::class.java).apply {
                putExtra("reconnect", true)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            Log.d(TAG, "Reconnection triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering reconnection", e)
        }
    }
}

