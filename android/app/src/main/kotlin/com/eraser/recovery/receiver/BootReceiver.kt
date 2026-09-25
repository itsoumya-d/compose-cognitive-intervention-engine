package com.eraser.recovery.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.eraser.recovery.vpn.EraserVpnService

/**
 * Boot receiver to auto-start VPN service on device boot
 * Ensures protection is always active
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, checking VPN auto-start")

            // Check if VPN was previously enabled
            val prefs = context.getSharedPreferences("eraser_prefs", Context.MODE_PRIVATE)
            val vpnEnabled = prefs.getBoolean("vpn_enabled", false)

            if (vpnEnabled) {
                Log.d(TAG, "Auto-starting VPN service")

                // Start VPN service
                val serviceIntent = Intent(context, EraserVpnService::class.java)
                context.startService(serviceIntent)
            } else {
                Log.d(TAG, "VPN auto-start disabled")
            }
        }
    }
}

