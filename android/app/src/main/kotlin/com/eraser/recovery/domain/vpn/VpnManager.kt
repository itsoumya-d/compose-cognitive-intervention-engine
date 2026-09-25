package com.eraser.recovery.domain.vpn

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.vpn.EraserVpnService
import com.eraser.recovery.vpn.VpnServiceBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VPN Manager
 * 
 * Manages VPN service lifecycle and provides UI-friendly API.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family:
 * - VPN permission must be requested before starting service
 * - Service binding for real-time communication
 * - State management with Flow for reactive UI
 * - Automatic reconnection on network changes
 * - Statistics tracking for user feedback
 * 
 * Responsibilities:
 * 1. Request VPN permission
 * 2. Start/stop VPN service
 * 3. Update blocklist dynamically
 * 4. Provide VPN status to UI
 * 5. Retrieve statistics
 * 6. Handle service binding
 */
@Singleton
class VpnManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) {
    
    companion object {
        private const val TAG = "VpnManager"
        const val VPN_PERMISSION_REQUEST_CODE = 100
    }
    
    // VPN service binding
    private var vpnService: EraserVpnService? = null
    private var serviceBinder: VpnServiceBinder? = null
    private var isBound = false
    
    // VPN state
    private val _isVpnEnabled = MutableStateFlow(false)
    val isVpnEnabled: Flow<Boolean> = _isVpnEnabled.asStateFlow()
    
    private val _isVpnConnecting = MutableStateFlow(false)
    val isVpnConnecting: Flow<Boolean> = _isVpnConnecting.asStateFlow()
    
    private val _vpnError = MutableStateFlow<String?>(null)
    val vpnError: Flow<String?> = _vpnError.asStateFlow()
    
    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "VPN service connected")
            serviceBinder = service as? VpnServiceBinder
            vpnService = serviceBinder?.getService()
            isBound = true
            updateVpnStatus()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "VPN service disconnected")
            serviceBinder = null
            vpnService = null
            isBound = false
            _isVpnEnabled.value = false
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Request VPN permission from user
     * Must be called from an Activity
     * 
     * @param activity The activity to request permission from
     * @return Intent to start for result, or null if permission already granted
     */
    fun requestVpnPermission(activity: Activity): Intent? {
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            Log.d(TAG, "VPN permission required")
        } else {
            Log.d(TAG, "VPN permission already granted")
        }
        return intent
    }
    
    /**
     * Check if VPN permission is granted
     */
    fun hasVpnPermission(): Boolean {
        val intent = VpnService.prepare(context)
        return intent == null
    }
    
    /**
     * Start VPN service
     * Requires VPN permission to be granted first
     * 
     * @return true if service started successfully, false otherwise
     */
    suspend fun startVpn(): Boolean {
        if (!hasVpnPermission()) {
            Log.e(TAG, "Cannot start VPN: permission not granted")
            _vpnError.value = "VPN permission not granted"
            return false
        }
        
        try {
            Log.i(TAG, "Starting VPN service...")
            _isVpnConnecting.value = true
            _vpnError.value = null
            
            // Start VPN service
            val intent = Intent(context, EraserVpnService::class.java).apply {
                action = EraserVpnService.ACTION_START
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            // Bind to service for communication
            bindToService()

            // Update database
            val now = java.time.LocalDateTime.now()
            userDao.updateVpnStatus(true, now.toEpochSecond(java.time.ZoneOffset.UTC))

            _isVpnEnabled.value = true
            _isVpnConnecting.value = false
            
            Log.i(TAG, "VPN service started successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN service", e)
            _vpnError.value = "Failed to start VPN: ${e.message}"
            _isVpnConnecting.value = false
            return false
        }
    }
    
    /**
     * Stop VPN service
     */
    suspend fun stopVpn() {
        try {
            Log.i(TAG, "Stopping VPN service...")
            
            // Unbind from service
            unbindFromService()
            
            // Stop VPN service
            val intent = Intent(context, EraserVpnService::class.java).apply {
                action = EraserVpnService.ACTION_STOP
            }
            context.startService(intent)

            // Update database
            val now = java.time.LocalDateTime.now()
            userDao.updateVpnStatus(false, now.toEpochSecond(java.time.ZoneOffset.UTC))

            _isVpnEnabled.value = false
            _vpnError.value = null
            
            Log.i(TAG, "VPN service stopped successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN service", e)
            _vpnError.value = "Failed to stop VPN: ${e.message}"
        }
    }
    
    /**
     * Toggle VPN on/off
     */
    suspend fun toggleVpn(): Boolean {
        return if (_isVpnEnabled.value) {
            stopVpn()
            false
        } else {
            startVpn()
        }
    }
    
    /**
     * Update blocklist with new domains
     * 
     * @param blockedDomains List of domains to block
     * @param whitelistedDomains List of domains to always allow
     */
    fun updateBlocklist(blockedDomains: List<String>, whitelistedDomains: List<String> = emptyList()) {
        try {
            Log.d(TAG, "Updating blocklist: ${blockedDomains.size} blocked, ${whitelistedDomains.size} whitelisted")
            
            if (isBound && serviceBinder != null) {
                // Update via service binder
                serviceBinder?.updateBlocklist(blockedDomains, whitelistedDomains)
            } else {
                // Update via intent
                val intent = Intent(context, EraserVpnService::class.java).apply {
                    action = EraserVpnService.ACTION_UPDATE_BLOCKLIST
                    putExtra(EraserVpnService.EXTRA_BLOCKED_DOMAINS, blockedDomains.toTypedArray())
                    putExtra(EraserVpnService.EXTRA_WHITELISTED_DOMAINS, whitelistedDomains.toTypedArray())
                }
                context.startService(intent)
            }
            
            Log.d(TAG, "Blocklist updated successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating blocklist", e)
            _vpnError.value = "Failed to update blocklist: ${e.message}"
        }
    }
    
    /**
     * Get VPN statistics
     * 
     * @return Map with statistics or null if service not running
     */
    fun getStatistics(): Map<String, Any>? {
        return if (isBound && serviceBinder != null) {
            serviceBinder?.getStatistics()
        } else {
            EraserVpnService.getStatistics()
        }
    }
    
    /**
     * Check if VPN service is running
     */
    fun isServiceRunning(): Boolean {
        return EraserVpnService.isServiceRunning()
    }
    
    /**
     * Clear VPN error
     */
    fun clearError() {
        _vpnError.value = null
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun bindToService() {
        if (isBound) {
            Log.d(TAG, "Already bound to service")
            return
        }
        
        try {
            val intent = Intent(context, EraserVpnService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "Binding to VPN service...")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to service", e)
        }
    }
    
    private fun unbindFromService() {
        if (!isBound) {
            return
        }
        
        try {
            context.unbindService(serviceConnection)
            isBound = false
            serviceBinder = null
            vpnService = null
            Log.d(TAG, "Unbound from VPN service")
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding from service", e)
        }
    }
    
    private fun updateVpnStatus() {
        val isRunning = serviceBinder?.isRunning() ?: false
        _isVpnEnabled.value = isRunning
        Log.d(TAG, "VPN status updated: $isRunning")
    }
}

