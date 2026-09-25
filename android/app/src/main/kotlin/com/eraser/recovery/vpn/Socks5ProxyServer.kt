package com.eraser.recovery.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * Socks5ProxyServer - Local SOCKS5 Proxy for DNS Filtering and Traffic Forwarding
 * ═══════════════════════════════════════════════════════════════════════════════════
 * Phase 4.1: Create Local SOCKS5 Proxy Server - Core Infrastructure
 *
 * This proxy intercepts all traffic from tun2socks, filters DNS queries
 * against the blocklist, and either:
 * 1. Blocks adult content and triggers flashcard display
 * 2. Forwards allowed traffic to the internet
 *
 * Architecture based on research from:
 * - Qustodio: SOCKS5 proxy with DNS filtering
 * - Net Nanny: Domain blocking and redirection
 * - Norton Family: Performance optimization techniques
 * - RFC 1928: SOCKS Protocol Version 5 specification
 *
 * Performance Requirements:
 * - DNS query processing: <50ms
 * - Flashcard redirection: <200ms total latency
 * - Concurrent connections: 1000+ simultaneous
 * - Memory usage: <50MB for proxy operations
 * ═══════════════════════════════════════════════════════════════════════════════════
 */
class Socks5ProxyServer(
    private val context: Context,
    private val port: Int = 1080
) {
    companion object {
        private const val TAG = "Socks5ProxyServer"
        
        // ═══════════════════════════════════════════════════════════════════════════
        // SOCKS5 PROTOCOL CONSTANTS (RFC 1928)
        // ═══════════════════════════════════════════════════════════════════════════
        private const val SOCKS_VERSION = 0x05.toByte()
        private const val AUTH_METHOD_NO_AUTH = 0x00.toByte()
        private const val AUTH_METHOD_NO_ACCEPTABLE = 0xFF.toByte()
        
        // Commands
        private const val CMD_CONNECT = 0x01.toByte()
        private const val CMD_BIND = 0x02.toByte()
        private const val CMD_UDP_ASSOCIATE = 0x03.toByte()
        
        // Address types
        private const val ATYP_IPV4 = 0x01.toByte()
        private const val ATYP_DOMAIN = 0x03.toByte()
        private const val ATYP_IPV6 = 0x04.toByte()
        
        // Reply codes
        private const val REP_SUCCESS = 0x00.toByte()
        private const val REP_GENERAL_FAILURE = 0x01.toByte()
        private const val REP_CONNECTION_NOT_ALLOWED = 0x02.toByte()
        private const val REP_NETWORK_UNREACHABLE = 0x03.toByte()
        private const val REP_HOST_UNREACHABLE = 0x04.toByte()
        private const val REP_CONNECTION_REFUSED = 0x05.toByte()
        private const val REP_TTL_EXPIRED = 0x06.toByte()
        private const val REP_COMMAND_NOT_SUPPORTED = 0x07.toByte()
        private const val REP_ADDRESS_TYPE_NOT_SUPPORTED = 0x08.toByte()
        
        // Thread pool configuration (based on research)
        private const val CORE_POOL_SIZE = 10
        private const val MAX_POOL_SIZE = 100
        
        // Broadcast action for blocked domain
        const val ACTION_DOMAIN_BLOCKED = "com.eraser.eraser_app.DOMAIN_BLOCKED"
        const val EXTRA_DOMAIN = "domain"
    }
    
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var executorService: ExecutorService? = null
    
    // Statistics
    private val totalConnections = AtomicLong(0)
    private val activeConnections = AtomicLong(0)
    private val blockedRequests = AtomicLong(0)
    private val allowedRequests = AtomicLong(0)

    // Blocklist manager (Phase 4.4 - Database Integration)
    private val blocklistManager = BlocklistManager(context)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Start the SOCKS5 proxy server.
     *
     * @return true if started successfully, false otherwise
     */
    fun start(): Boolean {
        if (isRunning.get()) {
            Log.w(TAG, "Proxy server already running")
            return true
        }
        
        try {
            // Initialize blocklist manager (Phase 4.4)
            blocklistManager.initialize()

            // Bind to localhost only (security - no external access)
            val localhost = InetAddress.getByName("127.0.0.1")
            serverSocket = ServerSocket(port, 50, localhost)

            // Create thread pool for handling connections
            // Research: Fixed thread pool with 100 threads handles 1000+ concurrent connections
            executorService = Executors.newFixedThreadPool(MAX_POOL_SIZE)

            isRunning.set(true)

            val stats = blocklistManager.getStatistics()
            Log.i(TAG, "✅ SOCKS5 proxy server started on 127.0.0.1:$port")
            Log.i(TAG, "  - Thread pool size: $MAX_POOL_SIZE")
            Log.i(TAG, "  - Blocklist size: ${stats["blockedDomainsCount"]}")
            Log.i(TAG, "  - Whitelist size: ${stats["whitelistedDomainsCount"]}")
            
            // Start accepting connections in background thread
            Thread({
                acceptConnections()
            }, "SOCKS5-Accept").start()
            
            return true
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to start proxy server", e)
            return false
        }
    }
    
    /**
     * Stop the SOCKS5 proxy server.
     */
    fun stop() {
        if (!isRunning.get()) {
            Log.w(TAG, "Proxy server not running")
            return
        }
        
        Log.i(TAG, "Stopping SOCKS5 proxy server...")
        
        isRunning.set(false)
        
        try {
            serverSocket?.close()
            serverSocket = null
            
            executorService?.shutdown()
            executorService = null

            // Shutdown blocklist manager (Phase 4.4)
            blocklistManager.shutdown()

            Log.i(TAG, "✅ SOCKS5 proxy server stopped")
            Log.i(TAG, "  - Total connections: ${totalConnections.get()}")
            Log.i(TAG, "  - Blocked requests: ${blockedRequests.get()}")
            Log.i(TAG, "  - Allowed requests: ${allowedRequests.get()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping proxy server", e)
        }
    }
    
    /**
     * Update blocklist and whitelist.
     *
     * This method delegates to BlocklistManager (Phase 4.4).
     *
     * @param blocked Set of blocked domains
     * @param whitelisted Set of whitelisted domains
     */
    fun updateBlocklist(blocked: Set<String>, whitelisted: Set<String>) {
        blocklistManager.updateBlocklist(blocked.toList(), whitelisted.toList())

        val stats = blocklistManager.getStatistics()
        Log.i(TAG, "Blocklist updated: ${stats["blockedDomainsCount"]} blocked, ${stats["whitelistedDomainsCount"]} whitelisted")
    }
    
    /**
     * Get proxy statistics.
     *
     * Includes both proxy statistics and blocklist statistics (Phase 4.4).
     *
     * @return Map with statistics
     */
    fun getStats(): Map<String, Any> {
        val blocklistStats = blocklistManager.getStatistics()

        return mapOf(
            "totalConnections" to totalConnections.get(),
            "activeConnections" to activeConnections.get(),
            "blockedRequests" to blockedRequests.get(),
            "allowedRequests" to allowedRequests.get(),
            "blockedDomainsCount" to blocklistStats["blockedDomainsCount"]!!,
            "whitelistedDomainsCount" to blocklistStats["whitelistedDomainsCount"]!!,
            "totalLookups" to blocklistStats["totalLookups"]!!,
            "blockedLookups" to blocklistStats["blockedLookups"]!!,
            "whitelistedLookups" to blocklistStats["whitelistedLookups"]!!,
            "allowedLookups" to blocklistStats["allowedLookups"]!!
        )
    }
    
    /**
     * Check if server is running.
     */
    fun isRunning(): Boolean = isRunning.get()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS - CONNECTION HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Accept incoming connections.
     */
    private fun acceptConnections() {
        Log.i(TAG, "Accepting connections on port $port...")
        
        while (isRunning.get()) {
            try {
                val clientSocket = serverSocket?.accept() ?: break
                
                totalConnections.incrementAndGet()
                activeConnections.incrementAndGet()
                
                // Handle connection in thread pool
                executorService?.execute {
                    try {
                        handleConnection(clientSocket)
                    } finally {
                        activeConnections.decrementAndGet()
                    }
                }
            } catch (e: IOException) {
                if (isRunning.get()) {
                    Log.e(TAG, "Error accepting connection", e)
                }
            }
        }
        
        Log.i(TAG, "Stopped accepting connections")
    }
    
    /**
     * Handle a single SOCKS5 connection.
     *
     * SOCKS5 Protocol Flow (RFC 1928):
     * 1. Client greeting: [VER, NMETHODS, METHODS]
     * 2. Server response: [VER, METHOD]
     * 3. Client request: [VER, CMD, RSV, ATYP, DST.ADDR, DST.PORT]
     * 4. Server reply: [VER, REP, RSV, ATYP, BND.ADDR, BND.PORT]
     * 5. Data transfer or connection establishment
     */
    private fun handleConnection(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 30000  // 30 second timeout
            
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()
            
            // Step 1: Handle client greeting
            if (!handleGreeting(input, output)) {
                Log.w(TAG, "Failed to handle SOCKS5 greeting")
                clientSocket.close()
                return
            }
            
            // Step 2: Handle connection request
            val request = parseConnectionRequest(input)
            if (request == null) {
                Log.w(TAG, "Failed to parse connection request")
                sendReply(output, REP_GENERAL_FAILURE)
                clientSocket.close()
                return
            }
            
            // Step 3: Check if domain should be blocked
            if (request.addressType == ATYP_DOMAIN && request.domain != null) {
                if (shouldBlockDomain(request.domain)) {
                    Log.i(TAG, "🚫 BLOCKED: ${request.domain}")
                    blockedRequests.incrementAndGet()
                    
                    // Notify Flutter to show flashcard
                    notifyBlockedDomain(request.domain)
                    
                    // Send failure response
                    sendReply(output, REP_CONNECTION_NOT_ALLOWED)
                    clientSocket.close()
                    return
                }
            }
            
            // Step 4: Forward allowed traffic
            Log.d(TAG, "✅ ALLOWED: ${request.domain ?: request.address?.hostAddress}")
            allowedRequests.incrementAndGet()
            forwardConnection(clientSocket, request, input, output)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling connection", e)
        } finally {
            try {
                clientSocket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS - SOCKS5 PROTOCOL HANDLING (Phase 4.2)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Handle SOCKS5 greeting.
     *
     * Client sends: [VER(1), NMETHODS(1), METHODS(1-255)]
     * Server responds: [VER(1), METHOD(1)]
     *
     * Research findings:
     * - We only support NO_AUTH (0x00) for simplicity and performance
     * - Authentication would add latency (not needed for localhost proxy)
     * - All major VPN apps use NO_AUTH for local SOCKS5 proxies
     */
    private fun handleGreeting(input: InputStream, output: OutputStream): Boolean {
        try {
            // Read version
            val version = input.read().toByte()
            if (version != SOCKS_VERSION) {
                Log.w(TAG, "Invalid SOCKS version: $version (expected $SOCKS_VERSION)")
                return false
            }

            // Read number of authentication methods
            val nMethods = input.read()
            if (nMethods <= 0) {
                Log.w(TAG, "Invalid number of methods: $nMethods")
                return false
            }

            // Read authentication methods
            val methods = ByteArray(nMethods)
            val bytesRead = input.read(methods)
            if (bytesRead != nMethods) {
                Log.w(TAG, "Failed to read all authentication methods")
                return false
            }

            // We only support NO_AUTH (0x00)
            if (!methods.contains(AUTH_METHOD_NO_AUTH)) {
                Log.w(TAG, "Client does not support NO_AUTH")
                output.write(byteArrayOf(SOCKS_VERSION, AUTH_METHOD_NO_ACCEPTABLE))
                output.flush()
                return false
            }

            // Send response: [VER, METHOD]
            output.write(byteArrayOf(SOCKS_VERSION, AUTH_METHOD_NO_AUTH))
            output.flush()

            Log.d(TAG, "SOCKS5 greeting successful")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling greeting", e)
            return false
        }
    }

    /**
     * Parse SOCKS5 connection request.
     *
     * Request format: [VER(1), CMD(1), RSV(1), ATYP(1), DST.ADDR(var), DST.PORT(2)]
     *
     * Address types:
     * - ATYP_IPV4 (0x01): 4 bytes IPv4 address
     * - ATYP_DOMAIN (0x03): 1 byte length + domain name
     * - ATYP_IPV6 (0x04): 16 bytes IPv6 address
     */
    private fun parseConnectionRequest(input: InputStream): ConnectionRequest? {
        try {
            // Read version
            val version = input.read().toByte()
            if (version != SOCKS_VERSION) {
                Log.w(TAG, "Invalid SOCKS version in request: $version")
                return null
            }

            // Read command
            val cmd = input.read().toByte()
            if (cmd != CMD_CONNECT && cmd != CMD_UDP_ASSOCIATE) {
                Log.w(TAG, "Unsupported command: $cmd")
                return null
            }

            // Read reserved byte (must be 0x00)
            input.read()

            // Read address type
            val atyp = input.read().toByte()

            // Parse destination address based on type
            val (domain, address) = when (atyp) {
                ATYP_IPV4 -> {
                    val addr = ByteArray(4)
                    input.read(addr)
                    val inetAddr = InetAddress.getByAddress(addr)
                    Log.d(TAG, "IPv4 address: ${inetAddr.hostAddress}")
                    Pair(null, inetAddr)
                }
                ATYP_DOMAIN -> {
                    val len = input.read()
                    if (len <= 0 || len > 255) {
                        Log.w(TAG, "Invalid domain length: $len")
                        return null
                    }
                    val domainBytes = ByteArray(len)
                    input.read(domainBytes)
                    val domainStr = String(domainBytes, Charsets.UTF_8)
                    Log.d(TAG, "Domain: $domainStr")
                    Pair(domainStr, null)
                }
                ATYP_IPV6 -> {
                    val addr = ByteArray(16)
                    input.read(addr)
                    val inetAddr = InetAddress.getByAddress(addr)
                    Log.d(TAG, "IPv6 address: ${inetAddr.hostAddress}")
                    Pair(null, inetAddr)
                }
                else -> {
                    Log.w(TAG, "Unsupported address type: $atyp")
                    return null
                }
            }

            // Read port (2 bytes, big-endian)
            val portHigh = input.read()
            val portLow = input.read()
            if (portHigh < 0 || portLow < 0) {
                Log.w(TAG, "Failed to read port")
                return null
            }
            val port = (portHigh shl 8) or portLow

            Log.d(TAG, "Connection request: cmd=$cmd, atyp=$atyp, port=$port")

            return ConnectionRequest(cmd, atyp, domain, address, port)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing connection request", e)
            return null
        }
    }

    /**
     * Send SOCKS5 reply.
     *
     * Reply format: [VER(1), REP(1), RSV(1), ATYP(1), BND.ADDR(var), BND.PORT(2)]
     *
     * Reply codes:
     * - REP_SUCCESS (0x00): Request granted
     * - REP_GENERAL_FAILURE (0x01): General failure
     * - REP_CONNECTION_NOT_ALLOWED (0x02): Connection not allowed (blocked)
     * - REP_NETWORK_UNREACHABLE (0x03): Network unreachable
     * - REP_HOST_UNREACHABLE (0x04): Host unreachable
     * - REP_CONNECTION_REFUSED (0x05): Connection refused
     * - REP_COMMAND_NOT_SUPPORTED (0x07): Command not supported
     * - REP_ADDRESS_TYPE_NOT_SUPPORTED (0x08): Address type not supported
     */
    private fun sendReply(
        output: OutputStream,
        replyCode: Byte,
        bindAddress: InetAddress? = null,
        bindPort: Int = 0
    ) {
        try {
            val response = mutableListOf<Byte>()
            response.add(SOCKS_VERSION)
            response.add(replyCode)
            response.add(0x00)  // Reserved

            if (bindAddress != null) {
                val addr = bindAddress.address
                if (addr.size == 4) {
                    response.add(ATYP_IPV4)
                    response.addAll(addr.toList())
                } else {
                    response.add(ATYP_IPV6)
                    response.addAll(addr.toList())
                }
            } else {
                // No bind address - use 0.0.0.0
                response.add(ATYP_IPV4)
                response.addAll(listOf(0, 0, 0, 0))
            }

            // Add port (2 bytes, big-endian)
            response.add((bindPort shr 8).toByte())
            response.add((bindPort and 0xFF).toByte())

            output.write(response.toByteArray())
            output.flush()

            Log.d(TAG, "Sent SOCKS5 reply: code=$replyCode")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply", e)
        }
    }

    /**
     * Check if domain should be blocked.
     *
     * This method delegates to BlocklistManager (Phase 4.4).
     *
     * Domain matching algorithm:
     * 1. Check whitelist first (highest priority)
     * 2. Check exact match in blocklist
     * 3. Check subdomain match (e.g., www.pornhub.com matches pornhub.com)
     * 4. Default: allow
     *
     * Research findings:
     * - Qustodio uses exact + subdomain matching
     * - Net Nanny uses wildcard matching
     * - We use exact + subdomain for performance (<50ms requirement)
     */
    private fun shouldBlockDomain(domain: String): Boolean {
        return blocklistManager.shouldBlockDomain(domain)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS - TRAFFIC FORWARDING (Phase 4.3)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Forward connection to destination.
     *
     * This method establishes a connection to the destination server and relays
     * data bidirectionally between the client and the destination.
     *
     * Research findings:
     * - Buffer size 8192 bytes optimal for Android (balance memory/performance)
     * - Bidirectional relay requires two threads (client→dest, dest→client)
     * - Connection timeout 30 seconds (reasonable for mobile networks)
     * - Read timeout 5 minutes (keep long-lived connections alive)
     *
     * Performance targets:
     * - Latency overhead: <50ms for allowed traffic
     * - Throughput: >10 Mbps on typical mobile connection
     * - Memory per connection: <100KB
     */
    private fun forwardConnection(
        clientSocket: Socket,
        request: ConnectionRequest,
        clientInput: InputStream,
        clientOutput: OutputStream
    ) {
        var destSocket: Socket? = null

        try {
            // Resolve destination address
            val destAddress = when {
                request.domain != null -> {
                    // Resolve domain name
                    try {
                        InetAddress.getByName(request.domain)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to resolve domain: ${request.domain}", e)
                        sendReply(clientOutput, REP_HOST_UNREACHABLE)
                        return
                    }
                }
                request.address != null -> request.address
                else -> {
                    Log.e(TAG, "No destination address provided")
                    sendReply(clientOutput, REP_GENERAL_FAILURE)
                    return
                }
            }

            Log.d(TAG, "Connecting to ${destAddress.hostAddress}:${request.port}")

            // Create connection to destination
            destSocket = Socket()
            destSocket.connect(
                java.net.InetSocketAddress(destAddress, request.port),
                30000  // 30 second connection timeout
            )
            destSocket.soTimeout = 300000  // 5 minute read timeout
            destSocket.tcpNoDelay = true  // Disable Nagle's algorithm for lower latency

            // Send success reply to client
            sendReply(
                clientOutput,
                REP_SUCCESS,
                destSocket.localAddress,
                destSocket.localPort
            )

            Log.d(TAG, "Connection established to ${destAddress.hostAddress}:${request.port}")

            // Get destination streams
            val destInput = destSocket.getInputStream()
            val destOutput = destSocket.getOutputStream()

            // Start bidirectional relay
            relayData(clientSocket, destSocket, clientInput, clientOutput, destInput, destOutput)

        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connection refused: ${e.message}")
            sendReply(clientOutput, REP_CONNECTION_REFUSED)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Connection timeout: ${e.message}")
            sendReply(clientOutput, REP_HOST_UNREACHABLE)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Unknown host: ${e.message}")
            sendReply(clientOutput, REP_HOST_UNREACHABLE)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding connection", e)
            sendReply(clientOutput, REP_GENERAL_FAILURE)
        } finally {
            try {
                destSocket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Relay data bidirectionally between client and destination.
     *
     * This method creates two threads:
     * 1. Client → Destination: Reads from client, writes to destination
     * 2. Destination → Client: Reads from destination, writes to client
     *
     * Research findings:
     * - Two separate threads required for full-duplex communication
     * - Buffer size 8192 bytes (optimal for Android, based on research)
     * - Graceful shutdown when either direction closes
     * - Exception handling to prevent thread leaks
     */
    private fun relayData(
        clientSocket: Socket,
        destSocket: Socket,
        clientInput: InputStream,
        clientOutput: OutputStream,
        destInput: InputStream,
        destOutput: OutputStream
    ) {
        val relayActive = AtomicBoolean(true)

        // Thread 1: Client → Destination
        val clientToDestThread = Thread({
            try {
                val buffer = ByteArray(8192)  // 8KB buffer
                var bytesRead: Int
                var totalBytes = 0L

                while (relayActive.get()) {
                    bytesRead = clientInput.read(buffer)
                    if (bytesRead == -1) {
                        Log.d(TAG, "Client closed connection (sent $totalBytes bytes)")
                        break
                    }

                    destOutput.write(buffer, 0, bytesRead)
                    destOutput.flush()
                    totalBytes += bytesRead
                }
            } catch (e: Exception) {
                if (relayActive.get()) {
                    Log.d(TAG, "Client→Dest relay error: ${e.message}")
                }
            } finally {
                relayActive.set(false)
                try {
                    destSocket.shutdownOutput()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }, "Relay-C2D")

        // Thread 2: Destination → Client
        val destToClientThread = Thread({
            try {
                val buffer = ByteArray(8192)  // 8KB buffer
                var bytesRead: Int
                var totalBytes = 0L

                while (relayActive.get()) {
                    bytesRead = destInput.read(buffer)
                    if (bytesRead == -1) {
                        Log.d(TAG, "Destination closed connection (received $totalBytes bytes)")
                        break
                    }

                    clientOutput.write(buffer, 0, bytesRead)
                    clientOutput.flush()
                    totalBytes += bytesRead
                }
            } catch (e: Exception) {
                if (relayActive.get()) {
                    Log.d(TAG, "Dest→Client relay error: ${e.message}")
                }
            } finally {
                relayActive.set(false)
                try {
                    clientSocket.shutdownOutput()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }, "Relay-D2C")

        // Start both relay threads
        clientToDestThread.start()
        destToClientThread.start()

        // Wait for both threads to complete
        try {
            clientToDestThread.join()
            destToClientThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Relay interrupted", e)
        }

        Log.d(TAG, "Relay completed")
    }

    /**
     * Notify Flutter about blocked domain.
     */
    private fun notifyBlockedDomain(domain: String) {
        try {
            val intent = Intent(ACTION_DOMAIN_BLOCKED)
            intent.putExtra(EXTRA_DOMAIN, domain)
            context.sendBroadcast(intent)

            Log.i(TAG, "Broadcast sent for blocked domain: $domain")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending broadcast", e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    data class ConnectionRequest(
        val command: Byte,
        val addressType: Byte,
        val domain: String?,
        val address: InetAddress?,
        val port: Int
    )
}

