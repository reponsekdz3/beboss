package com.example.util

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.ShopProfile
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class LocalServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class DiscoveredBranchHub(
    val id: String,
    val shopName: String,
    val branchName: String,
    val ipAddress: String,
    val port: Int,
    val productsCount: Int,
    val salesCount: Int,
    val branchesCount: Int,
    val lastSeenAt: Long = System.currentTimeMillis()
)

data class BranchPeerSyncEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val peerName: String,
    val peerIp: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LocalSyncClientResult(
    val success: Boolean,
    val message: String,
    val pushedSalesCount: Int = 0,
    val pulledProductsCount: Int = 0,
    val pulledBranchesCount: Int = 0,
    val latencyMs: Long = 0L
)

class LocalNetworkSyncServer(
    private val context: Context,
    private val database: AppDatabase
) {
    private val TAG = "LocalSyncServer"
    val port = 8989
    private val discoveryUdpPort = 8990
    private val nsdServiceType = "_beboss._tcp."
    private val packetHmacSecret = "BeBoss_LAN_P2P_Mesh_Integrity_2026"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var beaconBroadcastJob: Job? = null
    private var discoveryListenJob: Job? = null
    private var udpListenSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _serverStatus = MutableStateFlow(LocalServerStatus.STOPPED)
    val serverStatus: StateFlow<LocalServerStatus> = _serverStatus.asStateFlow()

    private val _serverIpAddress = MutableStateFlow<String?>(null)
    val serverIpAddress: StateFlow<String?> = _serverIpAddress.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _lastReceivedPacketSummary = MutableStateFlow<String?>(null)
    val lastReceivedPacketSummary: StateFlow<String?> = _lastReceivedPacketSummary.asStateFlow()

    private val _discoveredHubs = MutableStateFlow<List<DiscoveredBranchHub>>(emptyList())
    val discoveredHubs: StateFlow<List<DiscoveredBranchHub>> = _discoveredHubs.asStateFlow()

    private val _isDiscoveringHubs = MutableStateFlow(false)
    val isDiscoveringHubs: StateFlow<Boolean> = _isDiscoveringHubs.asStateFlow()

    private val _peerSyncEvents = MutableStateFlow<List<BranchPeerSyncEvent>>(emptyList())
    val peerSyncEvents: StateFlow<List<BranchPeerSyncEvent>> = _peerSyncEvents.asStateFlow()

    fun getDeviceLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP", e)
        }
        return "127.0.0.1"
    }

    fun startServer(): Boolean {
        if (_serverStatus.value == LocalServerStatus.RUNNING) return true
        _serverStatus.value = LocalServerStatus.STARTING

        return try {
            val ip = getDeviceLocalIpAddress()
            _serverIpAddress.value = ip
            serverSocket = ServerSocket(port)

            _serverStatus.value = LocalServerStatus.RUNNING
            Log.i(TAG, "Local Hub Server running at http://$ip:$port")

            serverJob = scope.launch {
                while (_serverStatus.value == LocalServerStatus.RUNNING) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch { handleClientSocket(socket) }
                    } catch (e: Exception) {
                        if (_serverStatus.value == LocalServerStatus.RUNNING) {
                            Log.e(TAG, "Server socket accept error", e)
                        }
                    }
                }
            }

            startBeaconBroadcaster(ip)
            registerNsdService()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local sync server on port $port", e)
            _serverStatus.value = LocalServerStatus.ERROR
            false
        }
    }

    fun stopServer() {
        try {
            _serverStatus.value = LocalServerStatus.STOPPED
            beaconBroadcastJob?.cancel()
            beaconBroadcastJob = null
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
            unregisterNsdService()
            _serverIpAddress.value = null
            _connectedClientsCount.value = 0
            Log.i(TAG, "Local Hub Server stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    /**
     * Starts active LAN discovery (UDP broadcast listener + NsdManager mDNS) to auto-detect
     * BeBoss Master Hubs on the same WiFi or Phone Hotspot.
     */
    fun startDiscovery() {
        if (_isDiscoveringHubs.value) return
        _isDiscoveringHubs.value = true
        acquireMulticastLock()

        discoveryListenJob = scope.launch {
            try {
                udpListenSocket = DatagramSocket(discoveryUdpPort).apply {
                    broadcast = true
                    soTimeout = 3000
                }
                val buf = ByteArray(2048)
                val myIp = getDeviceLocalIpAddress()

                while (isActive && _isDiscoveringHubs.value) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        udpListenSocket?.receive(packet)
                        val senderIp = packet.address?.hostAddress.orEmpty()
                        if (senderIp.isNotBlank() && senderIp != myIp && senderIp != "127.0.0.1") {
                            val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                            parseBeaconAndAddHub(msg, senderIp)
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // Prune stale hubs older than 20s
                        val cutoff = System.currentTimeMillis() - 20_000L
                        _discoveredHubs.value = _discoveredHubs.value.filter { it.lastSeenAt >= cutoff }
                    } catch (e: Exception) {
                        if (_isDiscoveringHubs.value) Log.w(TAG, "UDP listen: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP discovery bind failed: ${e.message}")
            } finally {
                try { udpListenSocket?.close() } catch (_: Exception) {}
                udpListenSocket = null
            }
        }

        startNsdDiscovery()
    }

    fun stopDiscovery() {
        _isDiscoveringHubs.value = false
        discoveryListenJob?.cancel()
        discoveryListenJob = null
        try { udpListenSocket?.close() } catch (_: Exception) {}
        udpListenSocket = null
        stopNsdDiscovery()
        releaseMulticastLock()
    }

    private fun startBeaconBroadcaster(serverIp: String) {
        beaconBroadcastJob?.cancel()
        beaconBroadcastJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply { broadcast = true }
                while (isActive && _serverStatus.value == LocalServerStatus.RUNNING) {
                    try {
                        val profile = database.shopProfileDao().getShopProfileDirect() ?: ShopProfile()
                        val mainBranch = database.branchDao().getMainBranch()
                        val pCount = database.productDao().getProductCountDirect()
                        val sCount = database.saleDao().getSaleCount()
                        val bCount = database.branchDao().getBranchCount()

                        val beacon = JSONObject().apply {
                            put("magic", "BEBOSS_HUB_V2")
                            put("id", "${profile.id}_${serverIp}")
                            put("shopName", profile.shopName)
                            put("branchName", mainBranch?.name ?: "HQ Hub")
                            put("ip", serverIp)
                            put("port", port)
                            put("productsCount", pCount)
                            put("salesCount", sCount)
                            put("branchesCount", bCount)
                        }.toString().toByteArray(StandardCharsets.UTF_8)

                        val broadcastAddr = InetAddress.getByName("255.255.255.255")
                        socket.send(DatagramPacket(beacon, beacon.size, broadcastAddr, discoveryUdpPort))
                    } catch (e: Exception) {
                        Log.w(TAG, "Beacon send warning: ${e.message}")
                    }
                    delay(3000L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Beacon broadcaster error: ${e.message}")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    private fun parseBeaconAndAddHub(raw: String, fallbackIp: String) {
        try {
            val obj = JSONObject(raw)
            if (obj.optString("magic") != "BEBOSS_HUB_V2") return
            val ip = obj.optString("ip").ifBlank { fallbackIp }
            val hub = DiscoveredBranchHub(
                id = obj.optString("id").ifBlank { ip },
                shopName = obj.optString("shopName", "BeBoss Terminal"),
                branchName = obj.optString("branchName", "Master Hub"),
                ipAddress = ip,
                port = obj.optInt("port", port),
                productsCount = obj.optInt("productsCount", 0),
                salesCount = obj.optInt("salesCount", 0),
                branchesCount = obj.optInt("branchesCount", 1),
                lastSeenAt = System.currentTimeMillis()
            )
            val current = _discoveredHubs.value.filterNot { it.ipAddress == hub.ipAddress }.toMutableList()
            current.add(0, hub)
            _discoveredHubs.value = current
        } catch (_: Exception) {}
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            if (multicastLock == null) {
                multicastLock = wifi.createMulticastLock("BeBossLanDiscovery").apply {
                    setReferenceCounted(false)
                }
            }
            if (multicastLock?.isHeld == false) multicastLock?.acquire()
        } catch (_: Exception) {}
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        } catch (_: Exception) {}
    }

    private fun registerNsdService() {
        try {
            nsdManager = (context.getSystemService(Context.NSD_SERVICE) as? NsdManager) ?: return
            val info = NsdServiceInfo().apply {
                serviceName = "BeBossBranchHub"
                serviceType = nsdServiceType
                port = this@LocalNetworkSyncServer.port
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.i(TAG, "NSD service registered: ${serviceInfo.serviceName}")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            registrationListener = listener
            nsdManager?.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD registration skipped: ${e.message}")
        }
    }

    private fun unregisterNsdService() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {}
        registrationListener = null
    }

    private fun startNsdDiscovery() {
        try {
            if (nsdManager == null) nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            val manager = nsdManager ?: return
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}
                override fun onServiceFound(service: NsdServiceInfo) {
                    if (!service.serviceType.contains("_beboss")) return
                    try {
                        @Suppress("DEPRECATION")
                        manager.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                @Suppress("DEPRECATION")
                                val host = serviceInfo.host?.hostAddress ?: return
                                val myIp = getDeviceLocalIpAddress()
                                if (host != myIp && host != "127.0.0.1") {
                                    scope.launch { probeHubInfo(host, serviceInfo.port) }
                                }
                            }
                        })
                    } catch (_: Exception) {}
                }
                override fun onServiceLost(service: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            discoveryListener = listener
            manager.discoverServices(nsdServiceType, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD discovery skipped: ${e.message}")
        }
    }

    private fun stopNsdDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        discoveryListener = null
    }

    suspend fun probeHubInfo(ip: String, targetPort: Int = port) = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("http://$ip:$targetPort/api/info").openConnection() as HttpURLConnection).apply {
                connectTimeout = 2000
                readTimeout = 2000
            }
            if (conn.responseCode in 200..299) {
                val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val hub = DiscoveredBranchHub(
                    id = "${obj.optString("hubId")}_$ip",
                    shopName = obj.optString("hubName", "BeBoss Terminal"),
                    branchName = obj.optString("branchName", "Master Hub"),
                    ipAddress = ip,
                    port = targetPort,
                    productsCount = obj.optInt("productsCount", 0),
                    salesCount = obj.optInt("salesCount", 0),
                    branchesCount = obj.optInt("branchesCount", 1),
                    lastSeenAt = System.currentTimeMillis()
                )
                val current = _discoveredHubs.value.filterNot { it.ipAddress == ip }.toMutableList()
                current.add(0, hub)
                _discoveredHubs.value = current
            }
        } catch (_: Exception) {}
    }

    private suspend fun handleClientSocket(socket: Socket) = withContext(Dispatchers.IO) {
        _connectedClientsCount.value = (_connectedClientsCount.value + 1).coerceAtLeast(1)
        val remoteIp = socket.inetAddress?.hostAddress ?: "Peer"
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val path = parts[1]

            var contentLength = 0
            var receivedSig = ""
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrBlank()) {
                when {
                    headerLine.startsWith("Content-Length:", ignoreCase = true) -> {
                        contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    headerLine.startsWith("X-BeBoss-Signature:", ignoreCase = true) -> {
                        receivedSig = headerLine.substringAfter(":").trim()
                    }
                }
                headerLine = reader.readLine()
            }

            when {
                path == "/api/info" && method == "GET" -> {
                    val profile = database.shopProfileDao().getShopProfileDirect() ?: ShopProfile()
                    val mainBranch = database.branchDao().getMainBranch()
                    val productsCount = database.productDao().getAllProductsList().size
                    val salesCount = database.saleDao().getAllSalesList().size
                    val branchesCount = database.branchDao().getAllActiveBranchesList().size

                    val json = JSONObject().apply {
                        put("status", "ONLINE")
                        put("hubName", profile.shopName)
                        put("branchName", mainBranch?.name ?: "HQ Hub")
                        put("hubId", profile.id)
                        put("currency", profile.currencyCode)
                        put("productsCount", productsCount)
                        put("salesCount", salesCount)
                        put("branchesCount", branchesCount)
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendJsonResponse(out, 200, json.toString())
                }

                path == "/api/sync/pull" && method == "GET" -> {
                    val fullPackage = buildFullMeshPackage(null)
                    val bodyStr = fullPackage.toString()
                    sendJsonResponse(out, 200, bodyStr, computePacketSignature(bodyStr))
                }

                path == "/api/sync/push" && method == "POST" -> {
                    val bodyBuilder = StringBuilder()
                    if (contentLength > 0) {
                        val buffer = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val r = reader.read(buffer, read, contentLength - read)
                            if (r <= 0) break
                            read += r
                        }
                        bodyBuilder.append(buffer, 0, read)
                    }

                    val body = bodyBuilder.toString()
                    if (body.isNotBlank()) {
                        if (receivedSig.isNotBlank() && !receivedSig.equals(computePacketSignature(body), ignoreCase = true)) {
                            sendJsonResponse(out, 403, """{"error":"HMAC integrity check failed"}""")
                            return@withContext
                        }
                        val parsed = ShopDataTransferManager.parseShopPackageString(body)
                        mergePackageIntoDatabase(parsed)

                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val summaryMsg = "Merged ${parsed.sales.size} sales, ${parsed.products.size} products & ${parsed.branches.size} branches from ${parsed.exportedBy}"
                        _lastReceivedPacketSummary.value = "$summaryMsg ($time)"
                        val currentEvents = _peerSyncEvents.value.toMutableList()
                        currentEvents.add(0, BranchPeerSyncEvent(peerName = parsed.exportedBy, peerIp = remoteIp, summary = summaryMsg))
                        _peerSyncEvents.value = currentEvents.take(25)

                        val resp = JSONObject().apply {
                            put("success", true)
                            put("message", summaryMsg)
                            put("receivedAt", System.currentTimeMillis())
                        }
                        sendJsonResponse(out, 200, resp.toString())
                    } else {
                        sendJsonResponse(out, 400, """{"error":"Empty request body"}""")
                    }
                }

                else -> {
                    sendJsonResponse(out, 404, """{"error":"Not Found"}""")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client socket handler error", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
            _connectedClientsCount.value = (_connectedClientsCount.value - 1).coerceAtLeast(0)
        }
    }

    private suspend fun buildFullMeshPackage(currentUser: User?): JSONObject {
        val profile = database.shopProfileDao().getShopProfileDirect() ?: ShopProfile()
        return ShopDataTransferManager.createExportJson(
            profile = profile,
            users = database.userDao().getAllActiveUsersList(),
            products = database.productDao().getAllProductsList(),
            customers = database.customerDao().getAllCustomersList(),
            sales = database.saleDao().getAllSalesList(),
            saleItems = database.saleDao().getAllSaleItemsList(),
            payments = database.customerPaymentDao().getAllPaymentsList(),
            exportedByUser = currentUser,
            branches = database.branchDao().getAllActiveBranchesList(),
            stockTransfers = database.stockTransferDao().getAllTransfersList(),
            purchases = database.purchaseDao().getAllPurchasesList()
        )
    }

    /**
     * Last-Write-Wins merge for products, customers, branches, sales, payments, purchases & stock transfers.
     */
    private suspend fun mergePackageIntoDatabase(parsed: ShopImportSummary) {
        if (parsed.branches.isNotEmpty()) {
            val existingMap = database.branchDao().getAllActiveBranchesList().associateBy { it.id }
            val merged = parsed.branches.mapNotNull { incoming ->
                val cur = existingMap[incoming.id]
                if (cur == null || incoming.updatedAt >= cur.updatedAt) incoming else null
            }
            if (merged.isNotEmpty()) database.branchDao().insertAllBranches(merged)
        }
        if (parsed.products.isNotEmpty()) {
            val existingMap = database.productDao().getAllProductsList().associateBy { it.id }
            val merged = parsed.products.mapNotNull { incoming ->
                val cur = existingMap[incoming.id]
                if (cur == null || incoming.updatedAt >= cur.updatedAt) incoming else null
            }
            if (merged.isNotEmpty()) database.productDao().insertAll(merged)
        }
        if (parsed.customers.isNotEmpty()) {
            val existingMap = database.customerDao().getAllCustomersList().associateBy { it.id }
            val merged = parsed.customers.mapNotNull { incoming ->
                val cur = existingMap[incoming.id]
                if (cur == null || incoming.updatedAt >= cur.updatedAt) incoming else null
            }
            if (merged.isNotEmpty()) database.customerDao().insertAll(merged)
        }
        if (parsed.sales.isNotEmpty()) database.saleDao().insertSales(parsed.sales)
        if (parsed.saleItems.isNotEmpty()) database.saleDao().insertSaleItems(parsed.saleItems)
        if (parsed.payments.isNotEmpty()) database.customerPaymentDao().insertPayments(parsed.payments)
        if (parsed.purchases.isNotEmpty()) database.purchaseDao().insertPurchases(parsed.purchases)
        if (parsed.stockTransfers.isNotEmpty()) database.stockTransferDao().insertTransfers(parsed.stockTransfers)
        if (parsed.users.isNotEmpty()) {
            val existingUsers = database.userDao().getAllActiveUsersList().associateBy { it.id }
            val mergedUsers = parsed.users.mapNotNull { incoming ->
                val cur = existingUsers[incoming.id]
                if (cur == null || incoming.updatedAt >= cur.updatedAt) incoming else null
            }
            if (mergedUsers.isNotEmpty()) database.userDao().insertAllUsers(mergedUsers)
        }
    }

    private fun sendJsonResponse(out: OutputStream, statusCode: Int, body: String, signature: String? = null) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val sigHeader = if (!signature.isNullOrBlank()) "X-BeBoss-Signature: $signature\r\n" else ""
        val header = "HTTP/1.1 $statusCode OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                sigHeader +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    suspend fun syncWithMasterHub(hubIp: String, currentUser: User?): LocalSyncClientResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val cleanIp = hubIp.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
            val hubUrl = "http://$cleanIp:$port"

            // 1. Push full local branch dataset with HMAC integrity signature
            val unsyncedSales = database.saleDao().getUnsyncedSales()
            val allSales = database.saleDao().getAllSalesList()
            val payloadStr = buildFullMeshPackage(currentUser).toString()
            val sig = computePacketSignature(payloadStr)

            val pushUrl = URL("$hubUrl/api/sync/push")
            val conn = (pushUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-BeBoss-Signature", sig)
                doOutput = true
                connectTimeout = 6000
                readTimeout = 10000
            }

            conn.outputStream.use { os ->
                val input = payloadStr.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val pushCode = conn.responseCode
            if (pushCode in 200..299) {
                val now = System.currentTimeMillis()
                for (s in unsyncedSales) {
                    database.saleDao().markSaleSynced(s.id, now)
                }
            }

            // 2. Pull complete updated branch state from Master Hub
            val pullUrl = URL("$hubUrl/api/sync/pull")
            val pullConn = (pullUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 10000
            }

            var pulledProductsCount = 0
            var pulledBranchesCount = 0
            if (pullConn.responseCode in 200..299) {
                val respText = pullConn.inputStream.bufferedReader().use { it.readText() }
                val summary = ShopDataTransferManager.parseShopPackageString(respText)
                mergePackageIntoDatabase(summary)
                pulledProductsCount = summary.products.size
                pulledBranchesCount = summary.branches.size
            }

            val latency = System.currentTimeMillis() - startTime
            LocalSyncClientResult(
                success = true,
                message = "Branch Mesh Sync Succeeded with $cleanIp ($pulledBranchesCount branches, $pulledProductsCount products)",
                pushedSalesCount = allSales.size,
                pulledProductsCount = pulledProductsCount,
                pulledBranchesCount = pulledBranchesCount,
                latencyMs = latency
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "P2P WiFi Sync Error", e)
            LocalSyncClientResult(
                success = false,
                message = "Cannot connect to Master Hub IP ($hubIp): ${e.localizedMessage ?: "Connection timed out"}",
                latencyMs = latency
            )
        }
    }

    private fun computePacketSignature(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(packetHmacSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return SecurityUtils.bytesToHex(mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))).take(16)
    }
}
