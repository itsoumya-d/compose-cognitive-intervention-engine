package com.eraser.recovery.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

class EraserVpnService : VpnService() {
    
    companion object {
        private const val TAG = "EraserVpnService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_UPDATE_BLOCKLIST = "UPDATE_BLOCKLIST"
        const val EXTRA_BLOCKED_DOMAINS = "blocked_domains"
        const val EXTRA_WHITELISTED_DOMAINS = "whitelisted_domains"
        const val CHANNEL_ID = "EraserVPN"
        const val NOTIFICATION_ID = 1
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_PREFIX_LENGTH = 24
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_ROUTE_PREFIX = 0
        private const val VPN_DNS_PRIMARY = "8.8.8.8"
        private const val VPN_DNS_SECONDARY = "8.8.4.4"
        private const val VPN_MTU = 1500
        private const val VPN_SESSION_NAME = "Eraser VPN"
        private const val SOCKS5_PORT = 1080
        private const val SOCKET_MARK = 438

        // Track service instance for statistics
        @Volatile
        private var instance: EraserVpnService? = null

        /**
         * Get VPN statistics.
         * Returns null if service is not running.
         */
        fun getStatistics(): Map<String, Any>? {
            return instance?.getStats()
        }

        /**
         * Check if VPN service is running.
         */
        fun isServiceRunning(): Boolean {
            return instance?.isRunning == true
        }
    }
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var socks5ProxyServer: Socks5ProxyServer? = null
    private var isRunning = false
    private var startTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "EraserVpnService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "Received ACTION_START")
                startVpn()
            }
            ACTION_STOP -> {
                Log.i(TAG, "Received ACTION_STOP")
                stopVpn()
            }
            ACTION_UPDATE_BLOCKLIST -> {
                Log.i(TAG, "Received ACTION_UPDATE_BLOCKLIST")
                val blocked = intent.getStringArrayExtra(EXTRA_BLOCKED_DOMAINS)
                val whitelisted = intent.getStringArrayExtra(EXTRA_WHITELISTED_DOMAINS)
                updateBlocklist(blocked?.toList() ?: emptyList(), whitelisted?.toList() ?: emptyList())
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "EraserVpnService destroyed")
        stopVpn()
    }
    
    private fun startVpn() {
        if (isRunning) {
            Log.w(TAG, "VPN already running")
            return
        }
        
        try {
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            Log.i(TAG, "Starting VPN service...")
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            
            Log.i(TAG, "Step 1/4: Starting SOCKS5 proxy server...")
            socks5ProxyServer = Socks5ProxyServer(this, SOCKS5_PORT)
            if (!socks5ProxyServer!!.start()) {
                throw Exception("Failed to start SOCKS5 proxy server")
            }
            Log.i(TAG, "✅ SOCKS5 proxy server started on 127.0.0.1:$SOCKS5_PORT")
            
            Log.i(TAG, "Step 2/4: Creating VPN interface...")
            vpnInterface = createVpnInterface()
            if (vpnInterface == null) {
                throw Exception("Failed to create VPN interface")
            }
            Log.i(TAG, "✅ VPN interface created (fd=${vpnInterface!!.fd})")
            
            Log.i(TAG, "Step 3/4: Starting tun2socks...")
            val tunFd = vpnInterface!!.fd
            if (!Tun2SocksManager.start(this, tunFd, SOCKS5_PORT, SOCKET_MARK)) {
                throw Exception("Failed to start tun2socks")
            }
            Log.i(TAG, "✅ Tun2socks started (TUN fd=$tunFd → SOCKS5 127.0.0.1:$SOCKS5_PORT)")
            
            Log.i(TAG, "Step 4/4: Starting foreground service...")
            startForeground(NOTIFICATION_ID, createNotification())
            Log.i(TAG, "✅ Foreground service started")
            
            isRunning = true
            startTime = System.currentTimeMillis()

            Log.i(TAG, "═══════════════════════════════════════════════════════")
            Log.i(TAG, "✅ VPN SERVICE STARTED SUCCESSFULLY")
            Log.i(TAG, "═══════════════════════════════════════════════════════")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start VPN service", e)
            cleanupResources()
            isRunning = false
        }
    }
    
    private fun stopVpn() {
        if (!isRunning) {
            Log.w(TAG, "VPN not running")
            return
        }
        
        Log.i(TAG, "Stopping VPN service...")
        isRunning = false
        cleanupResources()
        Log.i(TAG, "✅ VPN service stopped")
    }
    
    private fun cleanupResources() {
        try {
            Tun2SocksManager.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tun2socks", e)
        }
        
        try {
            socks5ProxyServer?.stop()
            socks5ProxyServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SOCKS5 proxy", e)
        }
        
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        
        try {
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground service", e)
        }
    }
    
    private fun createVpnInterface(): ParcelFileDescriptor? {
        return Builder()
            .setSession(VPN_SESSION_NAME)
            .addAddress(VPN_ADDRESS, VPN_PREFIX_LENGTH)
            .addRoute(VPN_ROUTE, VPN_ROUTE_PREFIX)
            .addDnsServer(VPN_DNS_PRIMARY)
            .addDnsServer(VPN_DNS_SECONDARY)
            .setMtu(VPN_MTU)
            .setBlocking(false)
            .establish()
    }

    internal fun updateBlocklist(blocked: List<String>, whitelisted: List<String>) {
        socks5ProxyServer?.updateBlocklist(blocked.toSet(), whitelisted.toSet())
        Log.i(TAG, "Blocklist updated: ${blocked.size} blocked, ${whitelisted.size} whitelisted")
    }

    /**
     * Get VPN statistics.
     *
     * Phase 6.1: Added for real-time status indicators
     *
     * @return Map with VPN statistics including:
     *   - blockedRequests: Number of blocked requests
     *   - allowedRequests: Number of allowed requests
     *   - totalConnections: Total connections handled
     *   - activeConnections: Currently active connections
     *   - blockedDomainsCount: Number of domains in blocklist
     *   - whitelistedDomainsCount: Number of domains in whitelist
     *   - bytesTransferred: Total bytes transferred (from Tun2SocksManager)
     *   - uptime: Service uptime in milliseconds
     */
    private fun getStats(): Map<String, Any> {
        val proxyStats = socks5ProxyServer?.getStats() ?: emptyMap()
        val tun2socksStats = Tun2SocksManager.getStats()
        val uptime = if (isRunning) System.currentTimeMillis() - startTime else 0

        return mapOf(
            "blockedRequests" to (proxyStats["blockedRequests"] ?: 0),
            "allowedRequests" to (proxyStats["allowedRequests"] ?: 0),
            "totalConnections" to (proxyStats["totalConnections"] ?: 0),
            "activeConnections" to (proxyStats["activeConnections"] ?: 0),
            "blockedDomainsCount" to (proxyStats["blockedDomainsCount"] ?: 0),
            "whitelistedDomainsCount" to (proxyStats["whitelistedDomainsCount"] ?: 0),
            "bytesTransferred" to (tun2socksStats?.txBytes?.plus(tun2socksStats.rxBytes) ?: 0),
            "uptime" to uptime
        )
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "Eraser VPN", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Eraser Protection Active")
            .setContentText("Adult content blocking enabled")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
