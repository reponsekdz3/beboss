package com.example.util

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.Product
import com.example.data.model.ShopProfile
import com.example.data.model.UserRole
import com.example.data.repository.BranchMergeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.crypto.Mac
import kotlin.random.Random

enum class CloudSyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

data class CloudSyncReport(
    val state: CloudSyncState,
    val itemsPushed: Int = 0,
    val itemsPulled: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val message: String = "",
    val branchesCount: Int = 0,
    val productsCount: Int = 0,
    val salesCount: Int = 0,
    val debtsCount: Int = 0,
    val conflictsResolved: Int = 0,
    val latencyMs: Long = 0L
)

data class EndpointPingResult(
    val reachable: Boolean,
    val latencyMs: Long,
    val statusCode: Int,
    val message: String
)

data class SyncAuditLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "BRANCH_AUTO_SYNC", "CLOUD_PUSH", "P2P_WIFI", "IMPORT_MERGE", "CONFLICT"
    val status: String, // "SUCCESS", "FAILED", "OFFLINE", "RESOLVED"
    val summary: String,
    val latencyMs: Long = 0L,
    val payloadSizeKb: Double = 0.0
)

class CloudSyncManager(private val database: AppDatabase) {

    private val TAG = "CloudSyncManager"

    private val _syncAuditLogs = MutableStateFlow<List<SyncAuditLog>>(emptyList())
    val syncAuditLogs: StateFlow<List<SyncAuditLog>> = _syncAuditLogs.asStateFlow()

    fun addAuditLog(type: String, status: String, summary: String, latencyMs: Long = 0L, payloadSizeKb: Double = 0.0) {
        val newLog = SyncAuditLog(
            type = type,
            status = status,
            summary = summary,
            latencyMs = latencyMs,
            payloadSizeKb = payloadSizeKb
        )
        _syncAuditLogs.value = listOf(newLog) + _syncAuditLogs.value.take(49)
    }

    suspend fun testEndpointConnection(endpoint: String): EndpointPingResult = withContext(Dispatchers.IO) {
        val cleanUrl = endpoint.trim()
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            return@withContext EndpointPingResult(
                reachable = false,
                latencyMs = 0L,
                statusCode = 0,
                message = "Invalid URL protocol (must start with https:// or http://)"
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val pingUrl = if (cleanUrl.endsWith("/")) "${cleanUrl}health" else "$cleanUrl/health"
            val url = URL(pingUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - startTime
            EndpointPingResult(
                reachable = true,
                latencyMs = latency,
                statusCode = code,
                message = if (code in 200..299) "Server online (${latency}ms)" else "Server reachable (HTTP $code)"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            EndpointPingResult(
                reachable = false,
                latencyMs = latency,
                statusCode = 0,
                message = "Unreachable: ${e.localizedMessage ?: "Connection timed out"}"
            )
        }
    }

    /**
     * Synchronous 2-Way Multi-Branch Cloud Sync Engine:
     * 1. Encodes all offline-first Room data (branches, products, customers, payments, sales, purchases, stock transfers, staff)
     *    into a compressed HMAC-signed `BBSHARE1:` snapshot and pushes + pulls peer branch snapshots via Supabase `branch_sync_snapshots`.
     * 2. Updates `shop_accounts` licensing metadata in Supabase.
     * 3. Optionally syncs to any custom `serverEndpoint` configured by the business.
     * 4. Resolves cross-branch edits cleanly via Last-Write-Wins (`mergeRemoteSummary`).
     */
    suspend fun syncAllDataToCloud(
        serverEndpoint: String? = null,
        activeBranchId: String = "main_branch",
        deviceId: String = "DEV-LOCAL",
        mergeRemoteSummary: (suspend (ShopImportSummary) -> BranchMergeResult)? = null
    ): CloudSyncReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val now = System.currentTimeMillis()
            val branchDao = database.branchDao()
            val productDao = database.productDao()
            val customerDao = database.customerDao()
            val customerPaymentDao = database.customerPaymentDao()
            val saleDao = database.saleDao()
            val purchaseDao = database.purchaseDao()
            val stockTransferDao = database.stockTransferDao()
            val userDao = database.userDao()
            val shopProfileDao = database.shopProfileDao()
            val syncQueueDao = database.syncQueueDao()

            val branches = branchDao.getAllActiveBranchesList()
            val products = productDao.getAllProductsList()
            val customers = customerDao.getAllCustomersList()
            val payments = customerPaymentDao.getAllPaymentsList()
            val sales = saleDao.getAllSalesList()
            val saleItems = saleDao.getAllSaleItemsList()
            val purchases = purchaseDao.getAllPurchasesList()
            val transfers = stockTransferDao.getAllTransfersList()
            val users = userDao.getAllActiveUsersList()
            val profile = shopProfileDao.getShopProfileDirect() ?: ShopProfile()

            val ownerUser = users.firstOrNull { it.role == UserRole.OWNER } ?: users.firstOrNull()
            val activeBranch = branches.firstOrNull { it.id == activeBranchId }
                ?: branches.firstOrNull { it.isMainBranch }
                ?: branches.firstOrNull()

            val exportJson = ShopDataTransferManager.createExportJson(
                profile = profile,
                users = users,
                products = products,
                customers = customers,
                sales = sales,
                saleItems = saleItems,
                payments = payments,
                exportedByUser = ownerUser,
                branches = branches,
                stockTransfers = transfers,
                purchases = purchases,
                branchIdFilter = null
            )
            val shareToken = ShopDataTransferManager.createCompressedSyncToken(exportJson)
            val payloadKb = shareToken.toByteArray(Charsets.UTF_8).size / 1024.0
            val totalPushedCount = branches.size + products.size + customers.size + payments.size +
                sales.size + purchases.size + transfers.size

            var itemsPulledCount = 0
            var conflictsResolvedCount = 0
            var supabaseSyncOk = false
            var customServerSyncOk = false

            // 1. Primary Cross-Branch Internet Sync via Supabase `branch_sync_snapshots`
            if (SupabaseAdminClient.isConfigured()) {
                val ownerUsername = ownerUser?.username ?: "owner"
                val ownerPhone = ownerUser?.phone?.ifBlank { profile.phone } ?: profile.phone
                val shopSyncKey = SupabaseAdminClient.computeShopSyncKey(ownerUsername, ownerPhone)
                val recordsSummary = "${branches.size} branches • ${products.size} items • ${sales.size} sales • ${customers.size} clients"

                val pushedSnapshot = SupabaseAdminClient.upsertBranchSyncSnapshot(
                    shopSyncKey = shopSyncKey,
                    deviceId = deviceId,
                    branchId = activeBranch?.id ?: activeBranchId,
                    branchName = activeBranch?.name ?: "Main Store",
                    shopName = profile.shopName,
                    ownerUsername = ownerUsername,
                    payloadToken = shareToken,
                    recordsSummary = recordsSummary
                )

                SupabaseAdminClient.upsertShopAccount(
                    shopProfile = profile,
                    ownerUser = ownerUser,
                    branchCount = branches.size,
                    workerCount = users.size
                )

                val peerSnapshots = SupabaseAdminClient.fetchShopBranchSnapshots(
                    shopSyncKey = shopSyncKey,
                    excludingDeviceId = deviceId
                )

                for (snap in peerSnapshots) {
                    try {
                        val decoded = ShopDataTransferManager.parseShopPackageString(snap.payloadToken)
                        if (mergeRemoteSummary != null) {
                            val mergeRes = mergeRemoteSummary(decoded)
                            itemsPulledCount += mergeRes.totalMergedCount
                            conflictsResolvedCount += mergeRes.conflictsResolved
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipped corrupt branch snapshot ${snap.id}: ${e.message}")
                    }
                }
                supabaseSyncOk = pushedSnapshot
            }

            // 2. Optional Custom Business Relay Server (if explicitly set beyond default)
            val endpoint = serverEndpoint ?: profile.backendServerUrl
            val hasCustomEndpoint = !endpoint.isNullOrBlank() &&
                endpoint != "https://api.beboss.app/v1" &&
                (endpoint.startsWith("http://") || endpoint.startsWith("https://"))

            if (hasCustomEndpoint) {
                val cleanEp = endpoint!!.trim().removeSuffix("/")
                val payloadBytes = exportJson.toString().toByteArray(Charsets.UTF_8)
                val hmacKey = SubscriptionSecurityManager.getOrCreateIsolatedHmacKey(profile)
                val mac = Mac.getInstance("HmacSHA256").apply { init(hmacKey) }
                val hmacSignature = SecurityUtils.bytesToHex(mac.doFinal(payloadBytes))

                for (attempt in 1..2) {
                    try {
                        val url = URL("$cleanEp/sync/cloud-push")
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                            setRequestProperty("X-Shop-Id", profile.id.toString())
                            setRequestProperty("X-Shop-Device", SubscriptionSecurityManager.computeDeviceChallenge(profile))
                            setRequestProperty("X-Shop-Signature", hmacSignature)
                            doOutput = true
                            connectTimeout = 8000
                            readTimeout = 10000
                        }
                        conn.outputStream.use { os -> os.write(payloadBytes, 0, payloadBytes.size) }
                        val respCode = conn.responseCode
                        if (respCode in 200..299) {
                            customServerSyncOk = true
                            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                            if (responseText.isNotBlank()) {
                                val respJson = JSONObject(responseText)
                                if (respJson.has("remoteProducts")) {
                                    val rProducts = respJson.getJSONArray("remoteProducts")
                                    for (i in 0 until rProducts.length()) {
                                        val rp = rProducts.getJSONObject(i)
                                        val pId = rp.optString("id", "").ifBlank { UUID.randomUUID().toString() }
                                        val remoteUpdated = rp.optLong("updatedAt", 0L)
                                        val existingProd = productDao.getProductById(pId)
                                        if (existingProd == null || remoteUpdated > existingProd.updatedAt) {
                                            productDao.insertProduct(
                                                Product(
                                                    id = pId,
                                                    name = rp.optString("name", "Product"),
                                                    category = rp.optString("category", "General"),
                                                    costPrice = rp.optDouble("costPrice", 0.0),
                                                    sellingPrice = rp.optDouble("sellingPrice", 0.0),
                                                    quantityInStock = rp.optDouble("quantityInStock", 0.0),
                                                    unit = rp.optString("unit", "pcs"),
                                                    barcode = rp.optString("barcode", ""),
                                                    branchId = rp.optString("branchId", "main_branch").ifBlank { "main_branch" },
                                                    updatedAt = remoteUpdated
                                                )
                                            )
                                            itemsPulledCount++
                                            if (existingProd != null) conflictsResolvedCount++
                                        }
                                    }
                                }
                            }
                            break
                        } else if (respCode in 500..599) {
                            delay(400L * attempt + Random.nextLong(50, 200))
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Custom server sync attempt $attempt failed: ${e.message}")
                    }
                }
            }

            val anyCloudSuccess = supabaseSyncOk || customServerSyncOk
            if (!SupabaseAdminClient.isConfigured() && !hasCustomEndpoint) {
                val latency = System.currentTimeMillis() - startTime
                addAuditLog(
                    type = "BRANCH_AUTO_SYNC",
                    status = "OFFLINE",
                    summary = "Offline mode: Supabase secrets not configured; data persisted safely in local SQLite.",
                    latencyMs = latency,
                    payloadSizeKb = payloadKb
                )
                return@withContext CloudSyncReport(
                    state = CloudSyncState.SYNCED,
                    itemsPushed = totalPushedCount,
                    itemsPulled = 0,
                    lastSyncTimestamp = now,
                    message = "Local SQLite Snapshot Ready (${branches.size} branch(es), $totalPushedCount records). Configure SUPABASE_URL in Secrets for multi-device auto-sync.",
                    branchesCount = branches.size,
                    productsCount = products.size,
                    salesCount = sales.size,
                    debtsCount = customers.count { it.debtBalance > 0 },
                    latencyMs = latency
                )
            }

            if (!anyCloudSuccess) {
                val latency = System.currentTimeMillis() - startTime
                addAuditLog(
                    type = "BRANCH_AUTO_SYNC",
                    status = "FAILED",
                    summary = "Cloud branch sync queued — waiting for reachable connection.",
                    latencyMs = latency
                )
                return@withContext CloudSyncReport(
                    state = CloudSyncState.ERROR,
                    itemsPushed = 0,
                    itemsPulled = 0,
                    lastSyncTimestamp = profile.lastSyncedAt,
                    message = "Branch auto-sync retry queued (network/Supabase unreachable).",
                    branchesCount = branches.size,
                    productsCount = products.size,
                    salesCount = sales.size,
                    debtsCount = customers.count { it.debtBalance > 0 },
                    latencyMs = latency
                )
            }

            val pendingQueue = syncQueueDao.getPendingQueueDirect()
            if (pendingQueue.isNotEmpty()) {
                syncQueueDao.markAllSynced(pendingQueue.map { it.id }, now)
            }
            val unsyncedSales = saleDao.getUnsyncedSales()
            for (s in unsyncedSales) {
                saleDao.markSaleSynced(s.id, now)
            }

            shopProfileDao.updateLastSyncedAt(now)
            val latency = System.currentTimeMillis() - startTime

            val summaryText = "Branch Auto-Sync: pushed $totalPushedCount records (${"%.1f".format(payloadKb)} KB), merged $itemsPulledCount updates, resolved $conflictsResolvedCount conflicts"
            addAuditLog(
                type = "BRANCH_AUTO_SYNC",
                status = "SUCCESS",
                summary = summaryText,
                latencyMs = latency,
                payloadSizeKb = payloadKb
            )

            CloudSyncReport(
                state = CloudSyncState.SYNCED,
                itemsPushed = totalPushedCount,
                itemsPulled = itemsPulledCount,
                lastSyncTimestamp = now,
                message = summaryText,
                branchesCount = branches.size,
                productsCount = products.size,
                salesCount = sales.size,
                debtsCount = customers.count { it.debtBalance > 0 },
                conflictsResolved = conflictsResolvedCount,
                latencyMs = latency
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Cloud branch sync failed", e)
            addAuditLog(
                type = "BRANCH_AUTO_SYNC",
                status = "FAILED",
                summary = "Error: ${e.localizedMessage ?: "Unknown"}",
                latencyMs = latency
            )
            CloudSyncReport(
                state = CloudSyncState.ERROR,
                message = "Branch sync error: ${e.localizedMessage ?: "Unknown"}",
                latencyMs = latency
            )
        }
    }
}
