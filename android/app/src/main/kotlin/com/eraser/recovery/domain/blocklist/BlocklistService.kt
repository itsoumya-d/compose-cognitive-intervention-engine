package com.eraser.recovery.domain.blocklist

import android.content.Context
import android.util.Log
import com.eraser.recovery.domain.vpn.VpnManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Blocklist Service
 * 
 * Manages adult content blocklist loading and updates.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family:
 * - Load blocklist from compressed JSON for efficiency
 * - Use HashSet for O(1) domain lookup
 * - Support dynamic updates without VPN restart
 * - Whitelist for false positives
 * - Statistics tracking
 * 
 * Blocklist Sources:
 * - adult_domains.json.gz: 156,000+ adult domains (compressed)
 * - default_whitelist.json: Safe domains that should never be blocked
 * 
 * IMPORTANT: Only adult/pornographic websites should be blocked.
 * Do NOT block gambling, drugs, social media, etc.
 */
@Singleton
class BlocklistService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vpnManager: VpnManager
) {
    
    companion object {
        private const val TAG = "BlocklistService"
        private const val BLOCKLIST_FILE = "blocklists/adult_domains.json.gz"
        private const val WHITELIST_FILE = "blocklists/default_whitelist.json"
    }
    
    private val blockedDomains = mutableSetOf<String>()
    private val whitelistedDomains = mutableSetOf<String>()
    private var isLoaded = false
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Load blocklist from assets
     * This should be called on app startup
     * 
     * @return true if loaded successfully, false otherwise
     */
    suspend fun loadBlocklist(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) {
            Log.d(TAG, "Blocklist already loaded")
            return@withContext true
        }
        
        try {
            Log.i(TAG, "Loading blocklist from assets...")
            val startTime = System.currentTimeMillis()
            
            // Load blocked domains
            val blocked = loadBlockedDomains()
            blockedDomains.clear()
            blockedDomains.addAll(blocked)
            
            // Load whitelisted domains
            val whitelisted = loadWhitelistedDomains()
            whitelistedDomains.clear()
            whitelistedDomains.addAll(whitelisted)
            
            val loadTime = System.currentTimeMillis() - startTime
            
            Log.i(TAG, "Blocklist loaded successfully:")
            Log.i(TAG, "  - Blocked domains: ${blockedDomains.size}")
            Log.i(TAG, "  - Whitelisted domains: ${whitelistedDomains.size}")
            Log.i(TAG, "  - Load time: ${loadTime}ms")
            
            isLoaded = true
            
            // Update VPN service with blocklist
            updateVpnBlocklist()
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading blocklist", e)
            return@withContext false
        }
    }
    
    /**
     * Reload blocklist from assets
     * Useful for updating blocklist without restarting app
     */
    suspend fun reloadBlocklist(): Boolean {
        isLoaded = false
        return loadBlocklist()
    }
    
    /**
     * Add domain to blocklist
     * 
     * @param domain Domain to block (e.g., "example.com")
     */
    suspend fun addBlockedDomain(domain: String) {
        blockedDomains.add(domain.lowercase())
        updateVpnBlocklist()
        Log.d(TAG, "Added domain to blocklist: $domain")
    }
    
    /**
     * Remove domain from blocklist
     * 
     * @param domain Domain to unblock (e.g., "example.com")
     */
    suspend fun removeBlockedDomain(domain: String) {
        blockedDomains.remove(domain.lowercase())
        updateVpnBlocklist()
        Log.d(TAG, "Removed domain from blocklist: $domain")
    }
    
    /**
     * Add domain to whitelist
     * Whitelisted domains will never be blocked
     * 
     * @param domain Domain to whitelist (e.g., "google.com")
     */
    suspend fun addWhitelistedDomain(domain: String) {
        whitelistedDomains.add(domain.lowercase())
        updateVpnBlocklist()
        Log.d(TAG, "Added domain to whitelist: $domain")
    }
    
    /**
     * Remove domain from whitelist
     * 
     * @param domain Domain to remove from whitelist
     */
    suspend fun removeWhitelistedDomain(domain: String) {
        whitelistedDomains.remove(domain.lowercase())
        updateVpnBlocklist()
        Log.d(TAG, "Removed domain from whitelist: $domain")
    }
    
    /**
     * Check if domain should be blocked
     *
     * Implements multiple matching strategies:
     * 1. Exact match: "example.com" matches "example.com"
     * 2. Parent domain match: "sub.example.com" matches if "example.com" is blocked
     * 3. Wildcard patterns: "*.example.com" matches any subdomain
     * 4. Keyword matching: Domains containing blocked keywords
     *
     * @param domain Domain to check (e.g., "example.com" or "sub.example.com")
     * @return true if domain should be blocked, false otherwise
     */
    fun shouldBlock(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()

        // 1. Check whitelist first (highest priority)
        if (isWhitelisted(normalizedDomain)) {
            return false
        }

        // 2. Exact match
        if (blockedDomains.contains(normalizedDomain)) {
            return true
        }

        // 3. Parent domain matching
        // Example: "sub.example.com" should be blocked if "example.com" is blocked
        if (matchesParentDomain(normalizedDomain)) {
            return true
        }

        // 4. Wildcard pattern matching
        // Example: "*.example.com" matches "sub.example.com"
        if (matchesWildcardPattern(normalizedDomain)) {
            return true
        }

        // 5. Keyword matching (optional, for additional safety)
        // Example: Domains containing explicit keywords
        if (matchesKeyword(normalizedDomain)) {
            return true
        }

        return false
    }

    /**
     * Check if domain is blocked (legacy method, kept for compatibility)
     *
     * @param domain Domain to check (e.g., "example.com")
     * @return true if domain is blocked, false otherwise
     */
    @Deprecated("Use shouldBlock() instead for comprehensive matching")
    fun isDomainBlocked(domain: String): Boolean {
        return shouldBlock(domain)
    }
    
    /**
     * Get blocklist statistics
     * 
     * @return Map with statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "blockedDomainsCount" to blockedDomains.size,
            "whitelistedDomainsCount" to whitelistedDomains.size,
            "isLoaded" to isLoaded
        )
    }
    
    /**
     * Get all blocked domains
     * Warning: This returns a large list (156,000+ domains)
     */
    fun getBlockedDomains(): Set<String> {
        return blockedDomains.toSet()
    }
    
    /**
     * Get all whitelisted domains
     */
    fun getWhitelistedDomains(): Set<String> {
        return whitelistedDomains.toSet()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun loadBlockedDomains(): Set<String> {
        val domains = mutableSetOf<String>()
        
        try {
            // Load from compressed JSON file
            context.assets.open(BLOCKLIST_FILE).use { inputStream ->
                GZIPInputStream(inputStream).use { gzipStream ->
                    BufferedReader(InputStreamReader(gzipStream)).use { reader ->
                        val json = reader.readText()
                        val jsonArray = JSONArray(json)
                        
                        for (i in 0 until jsonArray.length()) {
                            val domain = jsonArray.getString(i).lowercase()
                            domains.add(domain)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading blocked domains", e)
            
            // Fallback: Try uncompressed file
            try {
                context.assets.open("blocklists/adult_domains.json").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val json = reader.readText()
                        val jsonArray = JSONArray(json)
                        
                        for (i in 0 until jsonArray.length()) {
                            val domain = jsonArray.getString(i).lowercase()
                            domains.add(domain)
                        }
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Error loading uncompressed blocked domains", e2)
            }
        }
        
        return domains
    }
    
    private fun loadWhitelistedDomains(): Set<String> {
        val domains = mutableSetOf<String>()
        
        try {
            context.assets.open(WHITELIST_FILE).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = reader.readText()
                    val jsonArray = JSONArray(json)
                    
                    for (i in 0 until jsonArray.length()) {
                        val domain = jsonArray.getString(i).lowercase()
                        domains.add(domain)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading whitelisted domains", e)
        }
        
        return domains
    }
    
    private fun updateVpnBlocklist() {
        try {
            vpnManager.updateBlocklist(
                blockedDomains = blockedDomains.toList(),
                whitelistedDomains = whitelistedDomains.toList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating VPN blocklist", e)
        }
    }

    /**
     * Check if domain is whitelisted
     * Also checks parent domains
     *
     * Example: "sub.google.com" is whitelisted if "google.com" is whitelisted
     */
    private fun isWhitelisted(domain: String): Boolean {
        // Exact match
        if (whitelistedDomains.contains(domain)) {
            return true
        }

        // Check parent domains
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
     * Check if domain matches a parent domain in blocklist
     *
     * Example: "sub.example.com" matches if "example.com" is blocked
     *
     * Algorithm:
     * 1. Split domain into parts: ["sub", "example", "com"]
     * 2. Check each parent domain: "example.com", "com"
     * 3. Return true if any parent is blocked
     */
    private fun matchesParentDomain(domain: String): Boolean {
        val parts = domain.split(".")

        // Check each parent domain
        // Example: "a.b.example.com" -> check "b.example.com", "example.com", "com"
        for (i in 1 until parts.size) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parentDomain)) {
                return true
            }
        }

        return false
    }

    /**
     * Check if domain matches a wildcard pattern in blocklist
     *
     * Example: "*.example.com" matches "sub.example.com", "a.b.example.com", etc.
     *
     * Algorithm:
     * 1. Look for patterns like "*.example.com" in blocklist
     * 2. Check if domain ends with ".example.com"
     * 3. Return true if match found
     */
    private fun matchesWildcardPattern(domain: String): Boolean {
        // Check for wildcard patterns in blocklist
        for (blockedDomain in blockedDomains) {
            if (blockedDomain.startsWith("*.")) {
                val pattern = blockedDomain.substring(2) // Remove "*."
                if (domain.endsWith(".$pattern") || domain == pattern) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Check if domain contains blocked keywords
     *
     * This is an additional safety layer for catching domains with explicit keywords.
     *
     * IMPORTANT: Only adult/pornographic keywords should be checked.
     * Do NOT block gambling, drugs, social media, etc.
     *
     * Example keywords: "porn", "xxx", "adult", "sex", etc.
     *
     * Note: This is a conservative list to avoid false positives.
     * Most blocking should rely on the domain blocklist.
     */
    private fun matchesKeyword(domain: String): Boolean {
        // Conservative keyword list for adult content only
        val adultKeywords = listOf(
            "porn", "xxx", "adult", "sex", "nude", "naked",
            "erotic", "hentai", "xvideos", "pornhub", "xhamster",
            "redtube", "youporn", "tube8", "spankwire", "keezmovies"
        )

        // Check if domain contains any adult keywords
        for (keyword in adultKeywords) {
            if (domain.contains(keyword)) {
                return true
            }
        }

        return false
    }
}

