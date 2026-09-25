package com.eraser.recovery.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * BlocklistManager - Manages blocklist and whitelist for SOCKS5 proxy.
 *
 * This class provides:
 * - Loading blocklist from JSON files or method channel
 * - Real-time updates via broadcast receiver
 * - Thread-safe access to blocklist/whitelist
 * - Statistics tracking
 * - Efficient domain lookup using HashSet
 *
 * Research findings:
 * - HashSet provides O(1) lookup for exact domain matches
 * - ConcurrentHashMap for thread-safe statistics
 * - BroadcastReceiver for real-time updates from Flutter
 * - JSON parsing for initial blocklist loading
 *
 * Integration with Flutter:
 * - Flutter sends blocklist updates via broadcast intent
 * - Action: "com.eraser.eraser_app.UPDATE_BLOCKLIST"
 * - Extras: "blocked_domains" (String array), "whitelisted_domains" (String array)
 */
class BlocklistManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BlocklistManager"
        
        // Broadcast actions
        const val ACTION_UPDATE_BLOCKLIST = "com.eraser.recovery.UPDATE_BLOCKLIST"
        const val EXTRA_BLOCKED_DOMAINS = "blocked_domains"
        const val EXTRA_WHITELISTED_DOMAINS = "whitelisted_domains"
        
        // File paths
        private const val BLOCKLIST_FILE = "blocklist.json"
        private const val WHITELIST_FILE = "whitelist.json"
    }
    
    // Thread-safe domain sets
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val whitelistedDomains = ConcurrentHashMap.newKeySet<String>()
    
    // Statistics
    private val totalLookupsCount = AtomicLong(0)
    private val blockedLookupsCount = AtomicLong(0)
    private val whitelistedLookupsCount = AtomicLong(0)
    private val allowedLookupsCount = AtomicLong(0)
    
    // Broadcast receiver for real-time updates
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_BLOCKLIST) {
                val blocked = intent.getStringArrayExtra(EXTRA_BLOCKED_DOMAINS)
                val whitelisted = intent.getStringArrayExtra(EXTRA_WHITELISTED_DOMAINS)
                
                if (blocked != null || whitelisted != null) {
                    updateBlocklist(
                        blocked?.toList() ?: emptyList(),
                        whitelisted?.toList() ?: emptyList()
                    )
                    Log.i(TAG, "Blocklist updated via broadcast")
                }
            }
        }
    }
    
    private var isReceiverRegistered = false
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Initialize the blocklist manager.
     * Registers broadcast receiver and loads initial blocklist.
     */
    fun initialize() {
        try {
            // Register broadcast receiver for real-time updates
            if (!isReceiverRegistered) {
                val filter = IntentFilter(ACTION_UPDATE_BLOCKLIST)
                context.registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                isReceiverRegistered = true
                Log.d(TAG, "Broadcast receiver registered")
            }
            
            // Load initial blocklist from files (if available)
            loadFromFiles()
            
            Log.i(TAG, "BlocklistManager initialized: ${blockedDomains.size} blocked, ${whitelistedDomains.size} whitelisted")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BlocklistManager", e)
        }
    }
    
    /**
     * Shutdown the blocklist manager.
     * Unregisters broadcast receiver.
     */
    fun shutdown() {
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(updateReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Broadcast receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down BlocklistManager", e)
        }
    }
    
    /**
     * Update blocklist and whitelist.
     *
     * @param blocked List of blocked domains
     * @param whitelisted List of whitelisted domains
     */
    fun updateBlocklist(blocked: List<String>, whitelisted: List<String>) {
        try {
            // Clear existing sets
            blockedDomains.clear()
            whitelistedDomains.clear()
            
            // Add new domains (lowercase for case-insensitive matching)
            blockedDomains.addAll(blocked.map { it.lowercase() })
            whitelistedDomains.addAll(whitelisted.map { it.lowercase() })
            
            Log.i(TAG, "Blocklist updated: ${blockedDomains.size} blocked, ${whitelistedDomains.size} whitelisted")
            
            // Save to files for persistence
            saveToFiles()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating blocklist", e)
        }
    }
    
    /**
     * Check if a domain should be blocked.
     *
     * Algorithm:
     * 1. Check whitelist first (highest priority)
     * 2. Check exact match in blocklist
     * 3. Check parent domains (subdomain matching)
     * 4. Default: allow
     *
     * @param domain Domain to check
     * @return true if domain should be blocked, false otherwise
     */
    fun shouldBlockDomain(domain: String): Boolean {
        totalLookupsCount.incrementAndGet()
        
        val lowerDomain = domain.lowercase()
        
        // Check whitelist first (highest priority)
        if (isWhitelisted(lowerDomain)) {
            whitelistedLookupsCount.incrementAndGet()
            return false
        }
        
        // Check blocklist
        if (isBlocked(lowerDomain)) {
            blockedLookupsCount.incrementAndGet()
            return true
        }
        
        // Default: allow
        allowedLookupsCount.incrementAndGet()
        return false
    }
    
    /**
     * Get blocklist statistics.
     *
     * @return Map of statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "blockedDomainsCount" to blockedDomains.size,
            "whitelistedDomainsCount" to whitelistedDomains.size,
            "totalLookups" to totalLookupsCount.get(),
            "blockedLookups" to blockedLookupsCount.get(),
            "whitelistedLookups" to whitelistedLookupsCount.get(),
            "allowedLookups" to allowedLookupsCount.get()
        )
    }
    
    /**
     * Get blocked domains (for debugging).
     *
     * @param limit Maximum number of domains to return
     * @return List of blocked domains
     */
    fun getBlockedDomains(limit: Int = 100): List<String> {
        return blockedDomains.take(limit)
    }
    
    /**
     * Get whitelisted domains (for debugging).
     *
     * @param limit Maximum number of domains to return
     * @return List of whitelisted domains
     */
    fun getWhitelistedDomains(limit: Int = 100): List<String> {
        return whitelistedDomains.take(limit)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if domain is whitelisted.
     * Supports exact match and parent domain matching.
     */
    private fun isWhitelisted(domain: String): Boolean {
        // Exact match
        if (whitelistedDomains.contains(domain)) {
            return true
        }
        
        // Check if any whitelisted domain is a parent of this domain
        // Example: google.com whitelisted → www.google.com is also whitelisted
        val parts = domain.split(".")
        for (i in 1 until parts.size) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (whitelistedDomains.contains(parentDomain)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if domain is blocked.
     * Supports exact match and parent domain matching.
     */
    private fun isBlocked(domain: String): Boolean {
        // Exact match
        if (blockedDomains.contains(domain)) {
            return true
        }
        
        // Check if any blocked domain is a parent of this domain
        // Example: pornhub.com blocked → www.pornhub.com is also blocked
        val parts = domain.split(".")
        for (i in 1 until parts.size) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parentDomain)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Load blocklist from JSON files.
     */
    private fun loadFromFiles() {
        try {
            // Load blocked domains
            val blockedFile = File(context.filesDir, BLOCKLIST_FILE)
            if (blockedFile.exists()) {
                val json = blockedFile.readText()
                val jsonArray = JSONArray(json)
                val domains = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    domains.add(jsonArray.getString(i))
                }
                blockedDomains.addAll(domains.map { it.lowercase() })
                Log.d(TAG, "Loaded ${domains.size} blocked domains from file")
            }
            
            // Load whitelisted domains
            val whitelistFile = File(context.filesDir, WHITELIST_FILE)
            if (whitelistFile.exists()) {
                val json = whitelistFile.readText()
                val jsonArray = JSONArray(json)
                val domains = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    domains.add(jsonArray.getString(i))
                }
                whitelistedDomains.addAll(domains.map { it.lowercase() })
                Log.d(TAG, "Loaded ${domains.size} whitelisted domains from file")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load blocklist from files (this is normal on first run)", e)
        }
    }
    
    /**
     * Save blocklist to JSON files for persistence.
     */
    private fun saveToFiles() {
        try {
            // Save blocked domains
            val blockedFile = File(context.filesDir, BLOCKLIST_FILE)
            val blockedJson = JSONArray(blockedDomains.toList())
            blockedFile.writeText(blockedJson.toString())
            
            // Save whitelisted domains
            val whitelistFile = File(context.filesDir, WHITELIST_FILE)
            val whitelistJson = JSONArray(whitelistedDomains.toList())
            whitelistFile.writeText(whitelistJson.toString())
            
            Log.d(TAG, "Saved blocklist to files")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving blocklist to files", e)
        }
    }
}

