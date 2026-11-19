package com.example.util

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.Product
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BranchMergeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import kotlin.math.min
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
    val type: String,
    val status: String,
    val summary: String,
    val latencyMs: Long = 0L,
    val payloadSizeKb: Double = 0.0
)

/**
 * Multi-branch Cloud Sync & Durable Outbox Worker Engine:
 * - Processes SyncQueueDao due items with exponential backoff + jitter + DEAD_LETTER ceiling.
 * - Uses chunked delta exports (`ShopDataTransferManager.exportChunkedTokens`) to avoid HTTP size bloat.
 * - Resolves cross-branch merges via `ConflictResolutionEngine`.
 */
class CloudSyncManager(private val database: AppDatabase) {

    private val TAG = "CloudSyncManager"
    private val MAX_QUEUE_RETRIES = 6

    private val pingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                        .build(),
                    ConnectionSpec.CLEARTEXT
                )
            )
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

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

    private fun isSafeEndpointUrl(endpoint: String): Boolean {
        val clean = endpoint.trim()
        if (clean.startsWith("https://", ignoreCase = true)) return true
        if (!clean.startsWith("http://", ignoreCase = true)) return false
        val host = clean.removePrefix("http://").substringBefore("/").substringBefore(":").lowercase()
        return host == "localhost" ||
            host == "127.0.0.1" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.") ||
            Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\.").containsMatchIn(host)
    }

    suspend fun testEndpointConnection(endpoint: String): EndpointPingResult = withContext(Dispatchers.IO) {
        val cleanUrl = endpoint.trim()
        if (!isSafeEndpointUrl(cleanUrl)) {
            return@withContext EndpointPingResult(
                reachable = false,
                latencyMs = 0L,
                statusCode = 0,
                message = "HTTPS required for remote servers (http:// allowed only on private LAN)."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val pingUrl = if (cleanUrl.endsWith("/")) "${cleanUrl}health" else "$cleanUrl/health"
            val req = Request.Builder().url(pingUrl).get().build()
            val code = pingClient.newCall(req).execute().use { it.code }
            val latency = System.currentTimeMillis() - startTime
            EndpointPingResult(
                reachable = true,
                latencyMs = latency,
                statusCode = code,
                message = if (code in 200..299) "Server online (${latency}ms)" else "Server reachable (HTTP $code)"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            BeBossLogger.w(TAG, "Endpoint health ping failed", e)
            EndpointPingResult(
                reachable = false,
                latencyMs = latency,
                statusCode = 0,
                message = "Unreachable: ${e.localizedMessage ?: "Connection timed out"}"
            )
        }
    }

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
            val users = userDao.getAllUsersDirect()
            val activeUsers = users.filter { it.isActive }
            var profile = shopProfileDao.getShopProfileDirect() ?: ShopProfile()

            val ownerUser = activeUsers.firstOrNull { it.role == UserRole.OWNER }
                ?: users.firstOrNull { it.role == UserRole.OWNER }
                ?: users.firstOrNull()
            val activeBranch = branches.firstOrNull { it.id == activeBranchId }
                ?: branches.firstOrNull { it.isMainBranch }
                ?: branches.firstOrNull()

            val tombstones = database.syncTombstoneDao().getAllTombstonesList()

            // Process due outbox items
            val dueQueueItems = syncQueueDao.getDueQueueItems(nowMs = now, limit = 250)

            val chunkedTokens = ShopDataTransferManager.exportChunkedTokens(
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
                sinceUpdatedAt = 0L, // Full authoritative state per snapshot slot so any branch joining cold catches up safely, while chunked <=250 rows/token
                maxRecordsPerChunk = 250,
                tombstones = tombstones
            )
            val payloadKb = chunkedTokens.sumOf { it.toByteArray(Charsets.UTF_8).size } / 1024.0
            val totalPushedCount = branches.size + products.size + customers.size + payments.size +
                sales.size + purchases.size + transfers.size

            var itemsPulledCount = 0
            var conflictsResolvedCount = 0
            var supabaseSyncOk = false
            var customServerSyncOk = false

            // 1. Primary Cross-Branch Internet Sync via Supabase
            if (SupabaseAdminClient.isConfigured()) {
                val ownerUsername = ownerUser?.username ?: "owner"
                val ownerPhone = ownerUser?.phone?.ifBlank { profile.phone } ?: profile.phone
                val shopSyncKey = SupabaseAdminClient.computeShopSyncKey(ownerUsername, ownerPhone)
                val recordsSummary = "${branches.size} branches • ${products.size} items • ${sales.size} sales • ${customers.size} clients (${chunkedTokens.size} chunk(s))"

                val appCfg = SupabaseAdminClient.fetchPublicAppConfig()
                if (appCfg != null) {
                    OfflineSubscriptionManager.saveDynamicPaymentConfig(null, appCfg)
                    if (appCfg.adminWhatsAppNumber.isNotBlank() && appCfg.adminWhatsAppNumber != profile.adminWhatsAppNumber) {
                        profile = profile.copy(adminWhatsAppNumber = appCfg.adminWhatsAppNumber)
                        shopProfileDao.insertOrUpdateProfile(profile)
                    }
                }

                val remoteShop = SupabaseAdminClient.fetchShopAccountById(shopSyncKey)
                if (remoteShop != null) {
                    ClockGuard.observeTrustedEpoch(remoteShop.updatedAt)
                    val isDirectUnlockUpdate = remoteShop.autoUnlockedAt > profile.lastAppliedCloudUnlockAt
                    val isRemoteExtension = remoteShop.subscriptionStatus == ShopProfile.STATUS_ACTIVE &&
                        remoteShop.subscriptionExpiresAt > profile.subscriptionExpiresAt + 60_000L
                    val isRemoteLock = (remoteShop.subscriptionStatus == ShopProfile.STATUS_SUSPENDED ||
                        remoteShop.subscriptionStatus == ShopProfile.STATUS_EXPIRED) &&
                        remoteShop.updatedAt > profile.updatedAt &&
                        remoteShop.autoUnlockedAt <= profile.lastAppliedCloudUnlockAt

                    if (isDirectUnlockUpdate || isRemoteExtension || isRemoteLock || remoteShop.updatedAt > profile.updatedAt) {
                        val updatedProf = profile.copy(
                            subscriptionStatus = if (isDirectUnlockUpdate || isRemoteExtension) ShopProfile.STATUS_ACTIVE else remoteShop.subscriptionStatus,
                            subscriptionExpiresAt = maxOf(profile.subscriptionExpiresAt, remoteShop.subscriptionExpiresAt).let {
                                if (isRemoteLock) remoteShop.subscriptionExpiresAt else it
                            },
                            deviceCountLimit = maxOf(profile.deviceCountLimit, remoteShop.deviceLimit),
                            adminWhatsAppNumber = remoteShop.adminWhatsApp.ifBlank { profile.adminWhatsAppNumber },
                            autoUnlockedAt = maxOf(profile.autoUnlockedAt, remoteShop.autoUnlockedAt),
                            lastAppliedCloudUnlockAt = if (isDirectUnlockUpdate) remoteShop.autoUnlockedAt else profile.lastAppliedCloudUnlockAt,
                            lastPaymentRef = if (isDirectUnlockUpdate) "ADMIN-DIRECT-UNLOCK" else profile.lastPaymentRef,
                            updatedAt = maxOf(profile.updatedAt, remoteShop.updatedAt)
                        )
                        shopProfileDao.insertOrUpdateProfile(updatedProf)
                        profile = updatedProf
                        itemsPulledCount++
                    }
                }

                val shopDeviceDao = database.shopDeviceDao()
                val remoteDevices = SupabaseAdminClient.fetchShopDevicesForShop(shopSyncKey)
                for (rd in remoteDevices) {
                    val localDev = shopDeviceDao.getDeviceById(rd.id)
                    if (localDev == null || rd.updatedAt > localDev.updatedAt) {
                        shopDeviceDao.insertOrUpdate(
                            com.example.data.model.ShopDevice(
                                id = rd.id,
                                shopSyncKey = rd.shopSyncKey,
                                deviceId = rd.deviceId,
                                deviceName = rd.deviceName,
                                joinCode = rd.joinCode,
                                assignedUserId = rd.assignedUserId,
                                assignedUsername = rd.assignedUsername,
                                assignedUserName = rd.assignedUserName,
                                role = rd.role,
                                branchId = rd.branchId,
                                branchName = rd.branchName,
                                isOwnerPrimary = rd.isOwnerPrimary,
                                isActive = rd.isActive,
                                monthlyFeeAddonRwf = rd.monthlyFeeAddonRwf,
                                lastSyncedAt = rd.lastSyncedAt,
                                syncedToSupabase = true,
                                createdAt = rd.createdAt,
                                updatedAt = rd.updatedAt
                            )
                        )
                        itemsPulledCount++
                    }
                }
                val localDevices = shopDeviceDao.getAllDevicesList()
                val activeDeviceCount = localDevices.count { it.isActive }.coerceAtLeast(1)
                for (ld in localDevices) {
                    if (!ld.syncedToSupabase || ld.deviceId == deviceId) {
                        val toPush = if (ld.deviceId == deviceId) ld.copy(lastSyncedAt = now, syncedToSupabase = true) else ld.copy(syncedToSupabase = true)
                        if (SupabaseAdminClient.upsertShopDevice(shopSyncKey, profile.shopName, toPush)) {
                            shopDeviceDao.insertOrUpdate(toPush)
                        }
                    }
                }

                val remoteUsers = SupabaseAdminClient.fetchShopUsersForShop(shopSyncKey)
                for (ru in remoteUsers) {
                    val localUser = userDao.getUserById(ru.userId) ?: continue
                    if (ru.updatedAt > localUser.updatedAt) {
                        val parsedRole = try { UserRole.valueOf(ru.role) } catch (_: Exception) { localUser.role }
                        val roleTemplate = User.defaultForRole(
                            name = localUser.name,
                            username = localUser.username,
                            role = parsedRole,
                            pin = localUser.pinHash,
                            phone = localUser.phone
                        )
                        val updatedLocal = if (parsedRole != localUser.role) {
                            roleTemplate.copy(
                                id = localUser.id,
                                email = localUser.email,
                                password = localUser.password,
                                assignedBranchId = ru.assignedBranchId.ifBlank { localUser.assignedBranchId },
                                assignedBranchName = ru.assignedBranchName.ifBlank { localUser.assignedBranchName },
                                isActive = ru.isActive,
                                lastLoginAt = localUser.lastLoginAt,
                                createdAt = localUser.createdAt,
                                updatedAt = ru.updatedAt
                            )
                        } else {
                            localUser.copy(
                                isActive = ru.isActive,
                                assignedBranchId = ru.assignedBranchId.ifBlank { localUser.assignedBranchId },
                                assignedBranchName = ru.assignedBranchName.ifBlank { localUser.assignedBranchName },
                                updatedAt = ru.updatedAt
                            )
                        }
                        userDao.updateUser(updatedLocal)
                        itemsPulledCount++
                    }
                }

                val latestUsers = userDao.getAllUsersDirect()
                var allChunksUploaded = chunkedTokens.isNotEmpty()
                chunkedTokens.forEachIndexed { chunkIdx, token ->
                    val chunkDeviceId = if (chunkIdx == 0) deviceId else "${deviceId}#c$chunkIdx"
                    val ok = retryWithBackoff(maxAttempts = 3) {
                        SupabaseAdminClient.upsertBranchSyncSnapshot(
                            shopSyncKey = shopSyncKey,
                            deviceId = chunkDeviceId,
                            branchId = activeBranch?.id ?: activeBranchId,
                            branchName = activeBranch?.name ?: "Main Store",
                            shopName = profile.shopName,
                            ownerUsername = ownerUsername,
                            payloadToken = token,
                            recordsSummary = recordsSummary
                        )
                    }
                    if (!ok) allChunksUploaded = false
                }

                SupabaseAdminClient.upsertShopAccount(
                    shopProfile = profile,
                    ownerUser = ownerUser,
                    branchCount = branches.size,
                    workerCount = latestUsers.count { it.isActive }.coerceAtLeast(1),
                    deviceCount = activeDeviceCount
                )
                SupabaseAdminClient.upsertShopUsers(
                    shopSyncKey = shopSyncKey,
                    shopName = profile.shopName,
                    users = latestUsers
                )

                val peerSnapshots = SupabaseAdminClient.fetchShopBranchSnapshots(
                    shopSyncKey = shopSyncKey,
                    excludingDeviceId = deviceId
                )

                for (snap in peerSnapshots) {
                    if (snap.deviceId.startsWith("${deviceId}#c")) continue
                    try {
                        val decoded = ShopDataTransferManager.parseShopPackageString(snap.payloadToken)
                        val mergeRes = mergeRemoteSummary?.invoke(decoded)
                            ?: ConflictResolutionEngine.mergePackage(database, decoded, "CLOUD:${snap.deviceId}")
                        itemsPulledCount += mergeRes.totalMergedCount
                        conflictsResolvedCount += mergeRes.conflictsResolved
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipped corrupt branch snapshot ${snap.id}: ${e.message}")
                    }
                }
                supabaseSyncOk = allChunksUploaded
            }

            // 2. Optional Custom Business Relay Server
            val endpoint = serverEndpoint ?: profile.backendServerUrl
            val hasCustomEndpoint = !endpoint.isNullOrBlank() &&
                endpoint != "https://api.beboss.app/v1" &&
                isSafeEndpointUrl(endpoint)

            if (hasCustomEndpoint) {
                val cleanEp = endpoint!!.trim().removeSuffix("/")
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
                    tombstones = tombstones
                )
                val payloadBytes = exportJson.toString().toByteArray(Charsets.UTF_8)
                val hmacKey = SubscriptionSecurityManager.getOrCreateIsolatedHmacKey(profile)
                val mac = Mac.getInstance("HmacSHA256").apply { init(hmacKey) }
                val hmacSignature = SecurityUtils.bytesToHex(mac.doFinal(payloadBytes))

                for (attempt in 1..3) {
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
                                                    costPrice = rp.optDouble("costPrice", 0.0).coerceAtLeast(0.0),
                                                    sellingPrice = rp.optDouble("sellingPrice", 0.0).coerceAtLeast(0.0),
                                                    quantityInStock = rp.optDouble("quantityInStock", 0.0).coerceAtLeast(0.0),
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
                            delay(400L * (1 shl (attempt - 1)) + Random.nextLong(50, 250))
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Custom server sync attempt $attempt failed: ${e.message}")
                        if (attempt < 3) {
                            delay(400L * (1 shl (attempt - 1)) + Random.nextLong(50, 250))
                        }
                    }
                }
            }

            if (!SupabaseAdminClient.isConfigured() && !hasCustomEndpoint) {
                val latency = System.currentTimeMillis() - startTime
                addAuditLog(
                    type = "BRANCH_AUTO_SYNC",
                    status = "OFFLINE",
                    summary = "Offline mode: Supabase secrets not configured; data persisted in encrypted SQLite.",
                    latencyMs = latency,
                    payloadSizeKb = payloadKb
                )
                return@withContext CloudSyncReport(
                    state = CloudSyncState.SYNCED,
                    itemsPushed = totalPushedCount,
                    itemsPulled = 0,
                    lastSyncTimestamp = now,
                    message = "Local Snapshot Ready (${branches.size} branch(es), $totalPushedCount records). Configure SUPABASE_URL in Secrets for multi-device auto-sync.",
                    branchesCount = branches.size,
                    productsCount = products.size,
                    salesCount = sales.size,
                    debtsCount = customers.count { it.debtBalance > 0 },
                    latencyMs = latency
                )
            }

            val anyCloudSuccess = supabaseSyncOk || customServerSyncOk
            if (!anyCloudSuccess) {
                // Record exponential backoff on queued outbox items
                for (qItem in dueQueueItems) {
                    val nextRetry = qItem.retryCount + 1
                    val isDead = nextRetry >= MAX_QUEUE_RETRIES
                    val backoffMs = min(3_600_000L, 5_000L * (1L shl nextRetry.coerceAtMost(8))) + Random.nextLong(200, 1000)
                    syncQueueDao.recordAttemptFailure(
                        id = qItem.id,
                        retryCount = nextRetry,
                        nextAttemptAt = now + backoffMs,
                        lastError = "Cloud unreachable on attempt #$nextRetry",
                        status = if (isDead) "DEAD_LETTER" else "PENDING"
                    )
                }

                val latency = System.currentTimeMillis() - startTime
                addAuditLog(
                    type = "BRANCH_AUTO_SYNC",
                    status = "FAILED",
                    summary = "Outbox retry scheduled with backoff (${dueQueueItems.size} item(s)).",
                    latencyMs = latency
                )
                return@withContext CloudSyncReport(
                    state = CloudSyncState.ERROR,
                    itemsPushed = 0,
                    itemsPulled = 0,
                    lastSyncTimestamp = profile.lastSyncedAt,
                    message = "Branch sync retry scheduled with exponential backoff.",
                    branchesCount = branches.size,
                    productsCount = products.size,
                    salesCount = sales.size,
                    debtsCount = customers.count { it.debtBalance > 0 },
                    latencyMs = latency
                )
            }

            if (dueQueueItems.isNotEmpty()) {
                syncQueueDao.markAllSynced(dueQueueItems.map { it.id }, now)
            }
            val unsyncedSales = saleDao.getUnsyncedSales()
            for (s in unsyncedSales) {
                saleDao.markSaleSynced(s.id, now)
            }

            shopProfileDao.updateLastSyncedAt(now)
            val latency = System.currentTimeMillis() - startTime

            val summaryText = "Branch Sync: pushed $totalPushedCount records (${"%.1f".format(payloadKb)} KB in ${chunkedTokens.size} chunk(s)), merged $itemsPulledCount updates, resolved $conflictsResolvedCount conflicts"
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

    private suspend fun retryWithBackoff(
        maxAttempts: Int = 3,
        block: suspend () -> Boolean
    ): Boolean {
        for (attempt in 1..maxAttempts) {
            if (block()) return true
            if (attempt < maxAttempts) {
                val jitter = Random.nextLong(80L, 250L)
                delay(350L * (1 shl (attempt - 1)) + jitter)
            }
        }
        return false
    }
}
