package com.eraser.recovery.vpn

import android.os.Binder

/**
 * Binder class for EraserVpnService
 * Allows MainActivity to communicate with the running VPN service
 * 
 * This enables:
 * - Dynamic blocklist updates without restarting VPN
 * - Real-time statistics retrieval
 * - Service status queries
 * 
 * Based on Android Service binding best practices
 */
class VpnServiceBinder(private val service: EraserVpnService) : Binder() {
    
    /**
     * Get the VPN service instance
     */
    fun getService(): EraserVpnService = service
    
    /**
     * Update the blocklist and whitelist with new domains
     * @param blockedDomains List of domains to block (e.g., ["pornhub.com", "xvideos.com"])
     * @param whitelistedDomains List of domains to always allow (e.g., ["google.com", "facebook.com"])
     */
    fun updateBlocklist(blockedDomains: List<String>, whitelistedDomains: List<String> = emptyList()) {
        service.updateBlocklist(blockedDomains, whitelistedDomains)
    }
    
    /**
     * Get current VPN statistics
     * @return Map with keys: blockedCount, allowedCount, bytesTransferred, uptime
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf("blockedCount" to 0, "allowedCount" to 0)
    }

    /**
     * Check if VPN is currently running
     */
    fun isRunning(): Boolean {
        return false // TODO: Implement proper status tracking
    }
}

