package com.example.util

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.PurchaseRecord
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LocalServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class LocalSyncClientResult(
    val success: Boolean,
    val message: String,
    val pushedSalesCount: Int = 0,
    val pulledProductsCount: Int = 0,
    val latencyMs: Long = 0L
)

class LocalNetworkSyncServer(
    private val context: Context,
    private val database: AppDatabase
) {
    private val TAG = "LocalSyncServer"
    val port = 8989

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _serverStatus = MutableStateFlow(LocalServerStatus.STOPPED)
    val serverStatus: StateFlow<LocalServerStatus> = _serverStatus.asStateFlow()

    private val _serverIpAddress = MutableStateFlow<String?>(null)
    val serverIpAddress: StateFlow<String?> = _serverIpAddress.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _lastReceivedPacketSummary = MutableStateFlow<String?>(null)
    val lastReceivedPacketSummary: StateFlow<String?> = _lastReceivedPacketSummary.asStateFlow()

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
            serverJob?.cancel()
            serverSocket?.close()
            serverSocket = null
            _serverIpAddress.value = null
            _connectedClientsCount.value = 0
            Log.i(TAG, "Local Hub Server stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private suspend fun handleClientSocket(socket: Socket) = withContext(Dispatchers.IO) {
        _connectedClientsCount.value = (_connectedClientsCount.value + 1).coerceAtLeast(1)
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val path = parts[1]

            // Read headers to find Content-Length
            var contentLength = 0
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrBlank()) {
                if (headerLine.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                headerLine = reader.readLine()
            }

            when {
                path == "/api/info" && method == "GET" -> {
                    val profile = database.shopProfileDao().getShopProfileDirect() ?: ShopProfile()
                    val productsCount = database.productDao().getAllProductsList().size
                    val salesCount = database.saleDao().getAllSalesList().size
                    val branchesCount = database.branchDao().getAllActiveBranchesList().size

                    val json = JSONObject().apply {
                        put("status", "ONLINE")
                        put("hubName", profile.shopName)
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
                    // Send full master catalog to client terminal
                    val profile = database.shopProfileDao().getShopProfileDirect() ?: ShopProfile()
                    val products = database.productDao().getAllProductsList()
                    val customers = database.customerDao().getAllCustomersList()
                    val branches = database.branchDao().getAllActiveBranchesList()
                    val users = database.userDao().getAllActiveUsersList()

                    val json = JSONObject().apply {
                        put("shopProfile", JSONObject().apply {
                            put("id", profile.id)
                            put("shopName", profile.shopName)
                            put("phone", profile.phone)
                            put("currencyCode", profile.currencyCode)
                            put("currencySymbol", profile.currencySymbol)
                        })
                        val pArr = JSONArray()
                        products.forEach { p ->
                            pArr.put(JSONObject().apply {
                                put("id", p.id)
                                put("name", p.name)
                                put("category", p.category)
                                put("costPrice", p.costPrice)
                                put("sellingPrice", p.sellingPrice)
                                put("quantityInStock", p.quantityInStock)
                                put("unit", p.unit)
                                put("barcode", p.barcode)
                                put("branchId", p.branchId)
                            })
                        }
                        put("products", pArr)

                        val cArr = JSONArray()
                        customers.forEach { c ->
                            cArr.put(JSONObject().apply {
                                put("id", c.id)
                                put("name", c.name)
                                put("phone", c.phone)
                                put("debtBalance", c.debtBalance)
                                put("creditLimit", c.creditLimit)
                            })
                        }
                        put("customers", cArr)

                        val bArr = JSONArray()
                        branches.forEach { b ->
                            bArr.put(JSONObject().apply {
                                put("id", b.id)
                                put("name", b.name)
                                put("code", b.code)
                                put("isMainBranch", b.isMainBranch)
                            })
                        }
                        put("branches", bArr)
                    }
                    sendJsonResponse(out, 200, json.toString())
                }

                path == "/api/sync/push" && method == "POST" -> {
                    // Receive sales and payments from branch terminal
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
                        val parsed = ShopDataTransferManager.parseShopPackageString(body)
                        // Merge into master database
                        if (parsed.sales.isNotEmpty()) {
                            database.saleDao().insertSales(parsed.sales)
                        }
                        if (parsed.saleItems.isNotEmpty()) {
                            database.saleDao().insertSaleItems(parsed.saleItems)
                        }
                        if (parsed.payments.isNotEmpty()) {
                            database.customerPaymentDao().insertPayments(parsed.payments)
                        }
                        if (parsed.customers.isNotEmpty()) {
                            database.customerDao().insertAll(parsed.customers)
                        }

                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        _lastReceivedPacketSummary.value = "Synced ${parsed.sales.size} sales & ${parsed.payments.size} payments at $time from ${parsed.exportedBy}"

                        val resp = JSONObject().apply {
                            put("success", true)
                            put("message", "Merged ${parsed.sales.size} sales into Master Hub")
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
            try {
                socket.close()
            } catch (_: Exception) {}
            _connectedClientsCount.value = (_connectedClientsCount.value - 1).coerceAtLeast(0)
        }
    }

    private fun sendJsonResponse(out: OutputStream, statusCode: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    /**
     * Client Mode: Connects to a Master Hub Terminal at the given IP:Port over local WiFi/Hotspot.
     */
    suspend fun syncWithMasterHub(hubIp: String, currentUser: User?): LocalSyncClientResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val cleanIp = hubIp.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
            val hubUrl = "http://$cleanIp:$port"

            // 1. Push local unsynced sales and payments to Master Hub
            val profile = database.shopProfileDao().getShopProfileDirect() ?: ShopProfile()
            val unsyncedSales = database.saleDao().getUnsyncedSales()
            val allSales = database.saleDao().getAllSalesList()
            val allSaleItems = database.saleDao().getAllSaleItemsList()
            val payments = database.customerPaymentDao().getAllPaymentsList()
            val customers = database.customerDao().getAllCustomersList()

            val payload = ShopDataTransferManager.createExportJson(
                profile = profile,
                users = database.userDao().getAllActiveUsersList(),
                products = database.productDao().getAllProductsList(),
                customers = customers,
                sales = allSales,
                saleItems = allSaleItems,
                payments = payments,
                exportedByUser = currentUser
            )

            val pushUrl = URL("$hubUrl/api/sync/push")
            val conn = (pushUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 8000
            }

            conn.outputStream.use { os ->
                val input = payload.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val pushCode = conn.responseCode
            if (pushCode in 200..299) {
                // Mark unsynced sales as synced
                val now = System.currentTimeMillis()
                for (s in unsyncedSales) {
                    database.saleDao().markSaleSynced(s.id, now)
                }
            }

            // 2. Pull updated catalog from Master Hub
            val pullUrl = URL("$hubUrl/api/sync/pull")
            val pullConn = (pullUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 8000
            }

            var pulledProductsCount = 0
            if (pullConn.responseCode in 200..299) {
                val respText = pullConn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(respText)
                if (root.has("products")) {
                    val pArr = root.getJSONArray("products")
                    pulledProductsCount = pArr.length()
                    val pulledProducts = mutableListOf<Product>()
                    for (i in 0 until pArr.length()) {
                        val obj = pArr.getJSONObject(i)
                        pulledProducts.add(
                            Product(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                name = obj.optString("name", "Product"),
                                category = obj.optString("category", "General"),
                                costPrice = obj.optDouble("costPrice", 0.0),
                                sellingPrice = obj.optDouble("sellingPrice", 0.0),
                                quantityInStock = obj.optDouble("quantityInStock", 0.0),
                                unit = obj.optString("unit", "pcs"),
                                barcode = obj.optString("barcode", ""),
                                branchId = obj.optString("branchId", "main_branch")
                            )
                        )
                    }
                    if (pulledProducts.isNotEmpty()) {
                        database.productDao().insertAll(pulledProducts)
                    }
                }
            }

            val latency = System.currentTimeMillis() - startTime
            LocalSyncClientResult(
                success = true,
                message = "P2P WiFi Sync Succeeded with Master Hub ($cleanIp)",
                pushedSalesCount = allSales.size,
                pulledProductsCount = pulledProductsCount,
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
}
