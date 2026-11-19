package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.ShopProfile
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.TlsVersion
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

data class SupabaseAdminSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val fullName: String,
    val expiresAt: Long
)

data class SupabaseShopAccountDto(
    val id: String,
    val shopName: String,
    val ownerName: String,
    val ownerUsername: String,
    val ownerPhone: String,
    val deviceChallenge: String,
    val branchCount: Int,
    val workerCount: Int,
    val monthlyFeeRwf: Int,
    val subscriptionStatus: String,
    val subscriptionExpiresAt: Long,
    val updatedAt: Long
)

data class SupabasePaymentRequestDto(
    val id: String,
    val shopName: String,
    val ownerUsername: String,
    val ownerPhone: String,
    val deviceChallenge: String,
    val provider: String,
    val ussdCode: String,
    val payerPhone: String,
    val amountRwf: Int,
    val durationMonths: Int,
    val branchCount: Int,
    val workerCount: Int,
    val telcoReference: String,
    val status: String,
    val generatedUnlockCode: String,
    val verifiedByAdmin: String,
    val rejectionReason: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

data class SupabaseUnlockCodeDto(
    val id: String,
    val unlockCode: String,
    val shopUsername: String,
    val shopPhone: String,
    val deviceChallenge: String,
    val tierCode: String,
    val planDays: Int,
    val amountRwf: Int,
    val status: String,
    val issuedByAdmin: String,
    val issuedAt: Long,
    val redeemedAt: Long?
)

data class SupabaseBranchSnapshotDto(
    val id: String,
    val shopSyncKey: String,
    val deviceId: String,
    val branchId: String,
    val branchName: String,
    val shopName: String,
    val ownerUsername: String,
    val payloadToken: String,
    val recordsSummary: String,
    val updatedAt: Long
)

data class SupabaseShopUserDto(
    val id: String,
    val shopSyncKey: String,
    val shopName: String,
    val userId: String,
    val fullName: String,
    val username: String,
    val phone: String,
    val role: String,
    val assignedBranchId: String,
    val assignedBranchName: String,
    val isActive: Boolean,
    val updatedAt: Long
)

data class SupabaseAuditLogDto(
    val id: String,
    val adminEmail: String,
    val action: String,
    val targetId: String,
    val details: String,
    val createdAt: Long
)

data class SupabaseSyncResult(
    val success: Boolean,
    val message: String,
    val approvedCodesFound: List<SupabasePaymentRequestDto> = emptyList()
)

data class BatchApprovalResult(
    val approvedCount: Int,
    val failedCount: Int,
    val issuedCodes: List<Pair<String, String>> = emptyList()
)

/**
 * Supabase PostgREST + GoTrue Auth Client for:
 * 1. Authenticated Admin Licensing Operations (`shop_accounts`, `payment_requests`, `unlock_codes`, `admin_profiles`, `admin_audit_logs`)
 * 2. Synchronous Cross-Branch Internet DataShare Relay (`branch_sync_snapshots`)
 *
 * All POS, Inventory, Sales, Customers, Purchases, and Debts remain 100% offline-first in Room SQLite.
 */
object SupabaseAdminClient {

    private const val TAG = "SupabaseAdminClient"
    const val MASTER_ADMIN_EMAIL = "reponsekdz@gmail.com"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var activeAdminSession: SupabaseAdminSession? = null

    private val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .build()

    fun buildCertificatePinner(
        supabaseUrl: String = getSupabaseUrl(),
        pinsCsv: String = BuildConfig.SUPABASE_CERT_PINS.ifBlank {
            System.getProperty("beboss.supabase.certPins").orEmpty()
        }
    ): CertificatePinner? {
        val rawPins = pinsCsv.split(',').map { it.trim() }.filter { it.startsWith("sha256/") }
        if (rawPins.isEmpty()) return null
        val host = try {
            URL(supabaseUrl).host.takeIf { it.isNotBlank() } ?: return null
        } catch (_: Throwable) {
            return null
        }
        val builder = CertificatePinner.Builder()
        rawPins.forEach { pin -> builder.add(host, pin) }
        return builder.build()
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionSpecs(listOf(modernTlsSpec))
            .followRedirects(false)
            .followSslRedirects(false)
            .apply {
                buildCertificatePinner()?.let { certificatePinner(it) }
            }
            .hostnameVerifier { hostname, session ->
                HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
            }
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun getSupabaseUrl(): String {
        return try {
            BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
        } catch (_: Throwable) {
            ""
        }
    }

    fun getSupabaseAnonKey(): String {
        return try {
            BuildConfig.SUPABASE_ANON_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun getDefaultAdminEmail(): String {
        val envEmail = try {
            BuildConfig.SUPABASE_ADMIN_EMAIL.trim().lowercase(Locale.ROOT)
        } catch (_: Throwable) {
            ""
        }
        return if (envEmail == MASTER_ADMIN_EMAIL) envEmail else MASTER_ADMIN_EMAIL
    }

    fun isAllowedMasterAdminEmail(email: String): Boolean {
        return email.trim().lowercase(Locale.ROOT) == MASTER_ADMIN_EMAIL
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.startsWith("https://") &&
            !url.contains("your-project-id") &&
            key.isNotBlank() &&
            !key.contains("your_supabase_anon_key")
    }

    fun getCurrentAdminSession(): SupabaseAdminSession? {
        val current = activeAdminSession ?: return null
        if (System.currentTimeMillis() >= current.expiresAt && current.refreshToken.isBlank()) {
            activeAdminSession = null
            return null
        }
        return current
    }

    @Synchronized
    fun ensureValidAdminSession(): SupabaseAdminSession? {
        val session = activeAdminSession ?: return null
        val now = System.currentTimeMillis()
        if (now < session.expiresAt - 60_000L) {
            return session
        }
        if (session.refreshToken.isBlank() || !isConfigured()) {
            activeAdminSession = null
            return null
        }
        return try {
            val payload = JSONObject().apply { put("refresh_token", session.refreshToken) }
            val req = Request.Builder()
                .url("${getSupabaseUrl()}/auth/v1/token?grant_type=refresh_token")
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    activeAdminSession = null
                    return null
                }
                val raw = resp.body?.string().orEmpty()
                val refreshed = parseAuthSession(JSONObject(raw), session.email, session.fullName)
                if (refreshed != null && isAllowedMasterAdminEmail(refreshed.email)) {
                    activeAdminSession = refreshed
                    refreshed
                } else {
                    activeAdminSession = null
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Admin JWT refresh failed: ${e.message}")
            activeAdminSession = null
            null
        }
    }

    fun computeShopSyncKey(ownerUsername: String, phone: String): String {
        val user = ownerUsername.trim().lowercase(Locale.ROOT).ifBlank { "owner" }
        val phone9 = SubscriptionSecurityManager.normalizePhoneLast9(phone).ifBlank { "000000000" }
        return "${user}_${phone9}"
    }

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("beboss_sync_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("device_sync_id", null)
        if (!existing.isNullOrBlank()) return existing
        val created = "DEV-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}"
        prefs.edit().putString("device_sync_id", created).apply()
        return created
    }

    private fun buildRestRequest(
        path: String,
        useAdminJwtIfAvailable: Boolean = false,
        tenantSyncKey: String? = null
    ): Request.Builder {
        val url = "${getSupabaseUrl()}/rest/v1/$path"
        val key = getSupabaseAnonKey()
        val adminToken = if (useAdminJwtIfAvailable) ensureValidAdminSession()?.accessToken else null
        val bearer = adminToken ?: key
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $bearer")
            .addHeader("Content-Type", "application/json")
        if (!tenantSyncKey.isNullOrBlank()) {
            builder.addHeader("X-Shop-Sync-Key", tenantSyncKey.trim())
        }
        return builder
    }

    // -------------------------------------------------------------------------
    // REAL SUPABASE ADMIN AUTHENTICATION (GoTrue Email/Password JWT)
    // -------------------------------------------------------------------------
    suspend fun signInAdmin(email: String, password: String): Result<SupabaseAdminSession> = withContext(Dispatchers.IO) {
        if (!isAllowedMasterAdminEmail(email)) {
            return@withContext Result.failure(
                IllegalStateException("Access restricted to single master administrator ($MASTER_ADMIN_EMAIL).")
            )
        }
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."))
        }
        try {
            val payload = JSONObject().apply {
                put("email", MASTER_ADMIN_EMAIL)
                put("password", password)
            }
            val req = Request.Builder()
                .url("${getSupabaseUrl()}/auth/v1/token?grant_type=password")
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val json = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
                if (!resp.isSuccessful) {
                    val msg = json.optString("error_description")
                        .ifBlank { json.optString("msg") }
                        .ifBlank { json.optString("message") }
                        .ifBlank { "Sign in failed (HTTP ${resp.code})" }
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                val session = parseAuthSession(json, MASTER_ADMIN_EMAIL)
                    ?: return@withContext Result.failure(IllegalStateException("Missing access token in Supabase response."))
                ensureAdminProfile(session)
                activeAdminSession = session
                logAdminAudit("ADMIN_LOGIN", session.email, "Signed in via Android Admin Console")
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpAdmin(email: String, password: String, fullName: String): Result<SupabaseAdminSession> = withContext(Dispatchers.IO) {
        if (!isAllowedMasterAdminEmail(email)) {
            return@withContext Result.failure(
                IllegalStateException("Only the designated master admin ($MASTER_ADMIN_EMAIL) can register in BeBoss Admin.")
            )
        }
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."))
        }
        try {
            val cleanName = fullName.trim().ifBlank { "BeBoss Master Admin" }
            val payload = JSONObject().apply {
                put("email", MASTER_ADMIN_EMAIL)
                put("password", password)
                put("data", JSONObject().apply { put("full_name", cleanName) })
            }
            val req = Request.Builder()
                .url("${getSupabaseUrl()}/auth/v1/signup")
                .addHeader("apikey", getSupabaseAnonKey())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val json = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
                if (!resp.isSuccessful) {
                    val msg = json.optString("error_description")
                        .ifBlank { json.optString("msg") }
                        .ifBlank { json.optString("message") }
                        .ifBlank { "Registration failed (HTTP ${resp.code})" }
                    return@withContext Result.failure(IllegalStateException(msg))
                }
                val session = parseAuthSession(json, email, cleanName)
                    ?: return@withContext Result.failure(IllegalStateException("Account created. Verify email in Supabase if confirmation is enabled, then Sign In."))
                ensureAdminProfile(session)
                activeAdminSession = session
                logAdminAudit("ADMIN_SIGNUP", session.email, "Registered admin operator profile")
                Result.success(session)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOutAdmin() = withContext(Dispatchers.IO) {
        val current = activeAdminSession
        activeAdminSession = null
        if (current != null && isConfigured()) {
            try {
                val req = Request.Builder()
                    .url("${getSupabaseUrl()}/auth/v1/logout")
                    .addHeader("apikey", getSupabaseAnonKey())
                    .addHeader("Authorization", "Bearer ${current.accessToken}")
                    .post("{}".toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().close()
            } catch (_: Exception) {}
        }
    }

    private fun parseAuthSession(json: JSONObject, fallbackEmail: String, fallbackName: String = "BeBoss Admin"): SupabaseAdminSession? {
        val sessionObj = json.optJSONObject("session") ?: json
        val accessToken = sessionObj.optString("access_token")
        if (accessToken.isBlank()) return null
        val refreshToken = sessionObj.optString("refresh_token")
        val expiresIn = sessionObj.optLong("expires_in", 3600L)
        val userObj = json.optJSONObject("user") ?: sessionObj.optJSONObject("user") ?: JSONObject()
        val userId = userObj.optString("id")
        val email = userObj.optString("email").ifBlank { fallbackEmail.trim().lowercase(Locale.ROOT) }
        val metaName = userObj.optJSONObject("user_metadata")?.optString("full_name").orEmpty()
        return SupabaseAdminSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            fullName = metaName.ifBlank { fallbackName },
            expiresAt = System.currentTimeMillis() + expiresIn * 1000L
        )
    }

    private fun ensureAdminProfile(session: SupabaseAdminSession) {
        if (!isAllowedMasterAdminEmail(session.email)) {
            throw IllegalStateException("Unauthorized admin identity: ${session.email}")
        }
        if (session.userId.isBlank()) return
        val checkReq = Request.Builder()
            .url("${getSupabaseUrl()}/rest/v1/admin_profiles?user_id=eq.${URLEncoder.encode(session.userId, "UTF-8")}&limit=1")
            .addHeader("apikey", getSupabaseAnonKey())
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .get()
            .build()
        client.newCall(checkReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                if (arr.length() > 0) {
                    val row = arr.getJSONObject(0)
                    if (!row.optBoolean("is_active", true)) {
                        throw IllegalStateException("This admin operator account is deactivated.")
                    }
                    return
                }
            }
        }

        val payload = JSONObject().apply {
            put("user_id", session.userId)
            put("email", session.email)
            put("full_name", session.fullName)
            put("role", "SUPER_ADMIN")
            put("is_active", true)
            put("created_at", System.currentTimeMillis())
        }
        val insReq = Request.Builder()
            .url("${getSupabaseUrl()}/rest/v1/admin_profiles")
            .addHeader("apikey", getSupabaseAnonKey())
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        client.newCall(insReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Admin profile authorization rejected (${resp.code}). Confirm your user in public.admin_profiles.")
            }
        }
    }

    // -------------------------------------------------------------------------
    // SYNCHRONOUS MULTI-BRANCH AUTO-SYNC SNAPSHOTS (`branch_sync_snapshots`)
    // -------------------------------------------------------------------------
    suspend fun upsertBranchSyncSnapshot(
        shopSyncKey: String,
        deviceId: String,
        branchId: String,
        branchName: String,
        shopName: String,
        ownerUsername: String,
        payloadToken: String,
        recordsSummary: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || payloadToken.isBlank()) return@withContext false
        try {
            val now = System.currentTimeMillis()
            val snapshotId = "${shopSyncKey}::${deviceId}"
            val body = JSONObject().apply {
                put("id", snapshotId)
                put("shop_sync_key", shopSyncKey)
                put("device_id", deviceId)
                put("branch_id", branchId)
                put("branch_name", branchName)
                put("shop_name", shopName)
                put("owner_username", ownerUsername.trim().lowercase(Locale.ROOT))
                put("payload_token", payloadToken)
                put("records_summary", recordsSummary)
                put("updated_at", now)
            }
            val req = buildRestRequest("branch_sync_snapshots?on_conflict=id", tenantSyncKey = shopSyncKey)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "upsertBranchSyncSnapshot HTTP ${resp.code}: ${resp.body?.string()}")
                }
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "upsertBranchSyncSnapshot error: ${e.message}")
            false
        }
    }

    suspend fun fetchShopBranchSnapshots(
        shopSyncKey: String,
        excludingDeviceId: String = ""
    ): List<SupabaseBranchSnapshotDto> = withContext(Dispatchers.IO) {
        if (!isConfigured() || shopSyncKey.isBlank()) return@withContext emptyList()
        try {
            val encKey = URLEncoder.encode(shopSyncKey, "UTF-8")
            val req = buildRestRequest(
                "branch_sync_snapshots?shop_sync_key=eq.$encKey&order=updated_at.asc&limit=30",
                tenantSyncKey = shopSyncKey
            )
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                val list = mutableListOf<SupabaseBranchSnapshotDto>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val devId = o.optString("device_id")
                    if (excludingDeviceId.isNotBlank() && devId == excludingDeviceId) continue
                    list.add(
                        SupabaseBranchSnapshotDto(
                            id = o.optString("id"),
                            shopSyncKey = o.optString("shop_sync_key"),
                            deviceId = devId,
                            branchId = o.optString("branch_id"),
                            branchName = o.optString("branch_name"),
                            shopName = o.optString("shop_name"),
                            ownerUsername = o.optString("owner_username"),
                            payloadToken = o.optString("payload_token"),
                            recordsSummary = o.optString("records_summary"),
                            updatedAt = o.optLong("updated_at", 0L)
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchShopBranchSnapshots error: ${e.message}")
            emptyList()
        }
    }

    // -------------------------------------------------------------------------
    // SHOP ACCOUNTS & SUBSCRIPTION LICENSING
    // -------------------------------------------------------------------------
    suspend fun upsertShopAccount(
        shopProfile: ShopProfile,
        ownerUser: User?,
        branchCount: Int,
        workerCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val username = ownerUser?.username?.trim()?.lowercase(Locale.ROOT).orEmpty().ifBlank { "owner" }
            val rawPhone = ownerUser?.phone?.ifBlank { shopProfile.phone } ?: shopProfile.phone
            val accountId = computeShopSyncKey(username, rawPhone)
            val deviceCode = SubscriptionSecurityManager.computeDeviceChallenge(shopProfile)
            val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(branchCount, workerCount, 1)

            val payload = JSONObject().apply {
                put("id", accountId)
                put("shop_name", shopProfile.shopName)
                put("owner_name", ownerUser?.name?.ifBlank { shopProfile.name } ?: shopProfile.name)
                put("owner_username", username)
                put("owner_phone", rawPhone)
                put("device_challenge", deviceCode)
                put("branch_count", branchCount.coerceAtLeast(1))
                put("worker_count", workerCount.coerceAtLeast(1))
                put("monthly_fee_rwf", pricing.monthlySubtotal)
                put("subscription_status", if (shopProfile.isSubscriptionActive) "ACTIVE" else "EXPIRED")
                put("subscription_expires_at", shopProfile.subscriptionExpiresAt)
                put("updated_at", System.currentTimeMillis())
            }

            val req = buildRestRequest("shop_accounts?on_conflict=id", tenantSyncKey = accountId)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "upsertShopAccount HTTP ${resp.code}: ${resp.body?.string()}")
                }
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "upsertShopAccount error: ${e.message}")
            false
        }
    }

    suspend fun fetchShopAccountById(accountId: String): SupabaseShopAccountDto? = withContext(Dispatchers.IO) {
        if (!isConfigured() || accountId.isBlank()) return@withContext null
        try {
            val enc = URLEncoder.encode(accountId, "UTF-8")
            val req = buildRestRequest("shop_accounts?id=eq.$enc&limit=1", tenantSyncKey = accountId).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                if (arr.length() == 0) return@withContext null
                parseShopAccountDto(arr.getJSONObject(0))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchShopAccountById error: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------------------
    // SHOP USERS DIRECTORY SYNC (`shop_users`) — Metadata only (No PIN/Password)
    // -------------------------------------------------------------------------
    suspend fun upsertShopUsers(
        shopSyncKey: String,
        shopName: String,
        users: List<User>
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || shopSyncKey.isBlank() || users.isEmpty()) return@withContext false
        try {
            val arr = JSONArray()
            for (u in users) {
                arr.put(
                    JSONObject().apply {
                        put("id", "${shopSyncKey}::${u.id}")
                        put("shop_sync_key", shopSyncKey)
                        put("shop_name", shopName)
                        put("user_id", u.id)
                        put("full_name", u.name)
                        put("username", u.username.trim().lowercase(Locale.ROOT))
                        put("phone", u.phone)
                        put("role", u.role.name)
                        put("assigned_branch_id", u.assignedBranchId)
                        put("assigned_branch_name", u.assignedBranchName)
                        put("is_active", u.isActive)
                        put("updated_at", u.updatedAt)
                    }
                )
            }
            val req = buildRestRequest("shop_users?on_conflict=id", tenantSyncKey = shopSyncKey)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(arr.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "upsertShopUsers HTTP ${resp.code}: ${resp.body?.string()}")
                }
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "upsertShopUsers error: ${e.message}")
            false
        }
    }

    suspend fun fetchShopUsersForShop(shopSyncKey: String): List<SupabaseShopUserDto> = withContext(Dispatchers.IO) {
        if (!isConfigured() || shopSyncKey.isBlank()) return@withContext emptyList()
        try {
            val enc = URLEncoder.encode(shopSyncKey, "UTF-8")
            val req = buildRestRequest("shop_users?shop_sync_key=eq.$enc&limit=200", tenantSyncKey = shopSyncKey).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { parseShopUserDto(arr.getJSONObject(it)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchShopUsersForShop error: ${e.message}")
            emptyList()
        }
    }

    suspend fun submitPaymentRequest(request: PaymentVerificationRequest): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val payload = JSONObject().apply {
                put("id", request.id)
                put("shop_name", request.shopName)
                put("owner_username", request.ownerUsername.trim().lowercase(Locale.ROOT))
                put("owner_phone", request.ownerPhone.trim())
                put("device_challenge", request.deviceChallenge)
                put("provider", request.provider)
                put("ussd_code", request.ussdCode)
                put("payer_phone", request.payerPhone)
                put("amount_rwf", request.amountRwf)
                put("duration_months", request.durationMonths)
                put("branch_count", request.branchCount)
                put("worker_count", request.workerCount)
                put("telco_reference", request.telcoReference)
                put("status", request.status)
                put("generated_unlock_code", request.generatedUnlockCode)
                put("created_at", request.createdAt)
                put("updated_at", request.updatedAt)
            }

            val shopSyncKey = computeShopSyncKey(request.ownerUsername, request.ownerPhone)
            val req = buildRestRequest("payment_requests?on_conflict=id", tenantSyncKey = shopSyncKey)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "submitPaymentRequest HTTP ${resp.code}: ${resp.body?.string()}")
                }
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "submitPaymentRequest error: ${e.message}")
            false
        }
    }

    suspend fun fetchApprovedRequestsForUser(username: String, phone: String): List<SupabasePaymentRequestDto> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) return@withContext emptyList()
            try {
                val shopSyncKey = computeShopSyncKey(username, phone)
                val cleanUser = URLEncoder.encode(username.trim().lowercase(Locale.ROOT), "UTF-8")
                val path = "payment_requests?owner_username=eq.$cleanUser&status=eq.APPROVED&order=updated_at.desc&limit=10"
                val req = buildRestRequest(path, tenantSyncKey = shopSyncKey).get().build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                    val list = mutableListOf<SupabasePaymentRequestDto>()
                    val phone9 = SubscriptionSecurityManager.normalizePhoneLast9(phone)
                    for (i in 0 until arr.length()) {
                        val dto = parsePaymentRequestDto(arr.getJSONObject(i))
                        val dtoPhone9 = SubscriptionSecurityManager.normalizePhoneLast9(dto.ownerPhone)
                        if (phone9.isBlank() || dtoPhone9 == phone9) {
                            list.add(dto)
                        }
                    }
                    list
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchApprovedRequestsForUser error: ${e.message}")
                emptyList()
            }
        }

    suspend fun verifyAndMarkCodeRedeemedInCloud(
        unlockCode: String,
        username: String,
        phone: String
    ): SupabaseUnlockCodeDto? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        try {
            val shopSyncKey = computeShopSyncKey(username, phone)
            val cleanCode = URLEncoder.encode(unlockCode.trim().uppercase(Locale.ROOT), "UTF-8")
            val req = buildRestRequest("unlock_codes?unlock_code=eq.$cleanCode&limit=1", tenantSyncKey = shopSyncKey).get().build()
            var matched: SupabaseUnlockCodeDto? = null
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                    if (arr.length() > 0) {
                        matched = parseUnlockCodeDto(arr.getJSONObject(0))
                    }
                }
            }

            if (matched != null && matched!!.status == "ISSUED") {
                val now = System.currentTimeMillis()
                val patchBody = JSONObject().apply {
                    put("status", "REDEEMED")
                    put("redeemed_at", now)
                }
                val patchReq = buildRestRequest(
                    "unlock_codes?id=eq.${URLEncoder.encode(matched!!.id, "UTF-8")}",
                    tenantSyncKey = shopSyncKey
                )
                    .patch(patchBody.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(patchReq).execute().close()

                val cleanUser = URLEncoder.encode(username.trim().lowercase(Locale.ROOT), "UTF-8")
                val prPatch = JSONObject().apply {
                    put("status", "REDEEMED")
                    put("updated_at", now)
                }
                val prReq = buildRestRequest(
                    "payment_requests?generated_unlock_code=eq.$cleanCode&owner_username=eq.$cleanUser",
                    tenantSyncKey = shopSyncKey
                )
                    .patch(prPatch.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(prReq).execute().close()
            }
            matched
        } catch (e: Exception) {
            Log.e(TAG, "verifyAndMarkCodeRedeemedInCloud error: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------------------
    // AUTHENTICATED ADMIN CONSOLE OPERATIONS
    // -------------------------------------------------------------------------
    suspend fun fetchAllPaymentRequests(): List<SupabasePaymentRequestDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val req = buildRestRequest("payment_requests?order=created_at.desc&limit=100", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { parsePaymentRequestDto(arr.getJSONObject(it)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllPaymentRequests error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAllShopAccounts(): List<SupabaseShopAccountDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val req = buildRestRequest("shop_accounts?order=updated_at.desc&limit=150", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { parseShopAccountDto(arr.getJSONObject(it)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllShopAccounts error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAllShopUsers(): List<SupabaseShopUserDto> = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext emptyList()
        try {
            val req = buildRestRequest("shop_users?order=updated_at.desc&limit=250", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { parseShopUserDto(arr.getJSONObject(it)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllShopUsers error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAllBranchSnapshots(): List<SupabaseBranchSnapshotDto> = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext emptyList()
        try {
            val req = buildRestRequest("branch_sync_snapshots?order=updated_at.desc&limit=150", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { idx ->
                    val o = arr.getJSONObject(idx)
                    SupabaseBranchSnapshotDto(
                        id = o.optString("id"),
                        shopSyncKey = o.optString("shop_sync_key"),
                        deviceId = o.optString("device_id"),
                        branchId = o.optString("branch_id"),
                        branchName = o.optString("branch_name"),
                        shopName = o.optString("shop_name"),
                        ownerUsername = o.optString("owner_username"),
                        payloadToken = o.optString("payload_token"),
                        recordsSummary = o.optString("records_summary"),
                        updatedAt = o.optLong("updated_at", 0L)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllBranchSnapshots error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAllUnlockCodes(): List<SupabaseUnlockCodeDto> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        try {
            val req = buildRestRequest("unlock_codes?order=issued_at.desc&limit=100", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { parseUnlockCodeDto(arr.getJSONObject(it)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllUnlockCodes error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchAdminAuditLogs(): List<SupabaseAuditLogDto> = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext emptyList()
        try {
            val req = buildRestRequest("admin_audit_logs?order=created_at.desc&limit=60", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { idx ->
                    val o = arr.getJSONObject(idx)
                    SupabaseAuditLogDto(
                        id = o.optString("id"),
                        adminEmail = o.optString("admin_email"),
                        action = o.optString("action"),
                        targetId = o.optString("target_id"),
                        details = o.optString("details"),
                        createdAt = o.optLong("created_at", 0L)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAdminAuditLogs error: ${e.message}")
            emptyList()
        }
    }

    suspend fun approvePaymentAndIssueCode(
        request: SupabasePaymentRequestDto,
        adminName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val session = ensureValidAdminSession()
        if (!isConfigured() || session == null) {
            return@withContext Pair(false, "Admin authentication expired or unavailable.")
        }
        val tierCode = when {
            request.durationMonths >= 12 -> "ANNUAL"
            request.branchCount >= 3 -> "MULTI"
            request.branchCount == 2 -> "DUAL"
            else -> "SINGLE"
        }
        val days = (request.durationMonths.coerceAtLeast(1)) * 30
        val generatedCode = try {
            SubscriptionSecurityManager.generateAccountBoundUnlockCode(
                username = request.ownerUsername,
                registeredPhone = request.ownerPhone,
                tierCode = tierCode,
                days = days
            )
        } catch (e: Exception) {
            return@withContext Pair(false, e.message ?: "Unable to bind unlock code.")
        }

        try {
            val now = System.currentTimeMillis()
            val codeId = UUID.randomUUID().toString()
            val operatorLabel = activeAdminSession?.let { "${it.fullName} (${it.email})" } ?: adminName
            val recomputedBreakdown = OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = request.branchCount.coerceAtLeast(1),
                workerCount = request.workerCount.coerceAtLeast(1),
                durationMonths = request.durationMonths.coerceAtLeast(1)
            )
            val verifiedAmountRwf = recomputedBreakdown.totalPayable
            val codePayload = JSONObject().apply {
                put("id", codeId)
                put("unlock_code", generatedCode)
                put("shop_username", request.ownerUsername.trim().lowercase(Locale.ROOT))
                put("shop_phone", request.ownerPhone.trim())
                put("device_challenge", request.deviceChallenge)
                put("tier_code", tierCode)
                put("plan_days", days)
                put("amount_rwf", verifiedAmountRwf)
                put("status", "ISSUED")
                put("issued_by_admin", operatorLabel)
                put("issued_at", now)
            }
            val insertCodeReq = buildRestRequest("unlock_codes", useAdminJwtIfAvailable = true)
                .addHeader("Prefer", "return=minimal")
                .post(codePayload.toString().toRequestBody(jsonMediaType))
                .build()
            val codeOk = client.newCall(insertCodeReq).execute().use { it.isSuccessful }

            val prPatch = JSONObject().apply {
                put("status", "APPROVED")
                put("amount_rwf", verifiedAmountRwf)
                put("generated_unlock_code", generatedCode)
                put("verified_by_admin", operatorLabel)
                put("updated_at", now)
            }
            val patchPrReq = buildRestRequest(
                "payment_requests?id=eq.${URLEncoder.encode(request.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(prPatch.toString().toRequestBody(jsonMediaType)).build()
            val prOk = client.newCall(patchPrReq).execute().use { it.isSuccessful }

            if (codeOk && prOk) {
                val shopKey = computeShopSyncKey(request.ownerUsername, request.ownerPhone)
                extendShopExpiryInternal(shopKey, days)
                val varianceNote = if (request.amountRwf != verifiedAmountRwf) {
                    " [recomputed from ${request.amountRwf} FRw]"
                } else ""
                logAdminAudit(
                    action = "APPROVE_PAYMENT",
                    targetId = request.ownerUsername,
                    details = "Approved $verifiedAmountRwf FRw$varianceNote (${request.provider}) -> $generatedCode (+${days}d)"
                )
            }
            Pair(codeOk && prOk, generatedCode)
        } catch (e: Exception) {
            Log.e(TAG, "approvePaymentAndIssueCode error: ${e.message}")
            Pair(false, generatedCode)
        }
    }

    suspend fun rejectPaymentRequest(
        request: SupabasePaymentRequestDto,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val operatorLabel = "${activeAdminSession!!.fullName} (${activeAdminSession!!.email})"
            val cleanReason = reason.trim().ifBlank { "Payment reference could not be confirmed by Admin" }
            val prPatch = JSONObject().apply {
                put("status", "REJECTED")
                put("rejection_reason", cleanReason)
                put("verified_by_admin", operatorLabel)
                put("updated_at", System.currentTimeMillis())
            }
            val req = buildRestRequest(
                "payment_requests?id=eq.${URLEncoder.encode(request.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(prPatch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("REJECT_PAYMENT", request.ownerUsername, cleanReason)
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "rejectPaymentRequest error: ${e.message}")
            false
        }
    }

    suspend fun revokeUnlockCode(codeDto: SupabaseUnlockCodeDto): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val patch = JSONObject().apply { put("status", "REVOKED") }
            val req = buildRestRequest(
                "unlock_codes?id=eq.${URLEncoder.encode(codeDto.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(patch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("REVOKE_CODE", codeDto.unlockCode, "Revoked code for @${codeDto.shopUsername}")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "revokeUnlockCode error: ${e.message}")
            false
        }
    }

    suspend fun issueManualUnlockCode(
        username: String,
        registeredPhone: String,
        deviceChallenge: String,
        tierCode: String,
        days: Int,
        amountRwf: Int,
        adminName: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val code = SubscriptionSecurityManager.generateAccountBoundUnlockCode(
            username = username,
            registeredPhone = registeredPhone,
            tierCode = tierCode,
            days = days
        )
        if (!isConfigured() || activeAdminSession == null) return@withContext Pair(false, code)

        try {
            val operatorLabel = activeAdminSession?.let { "${it.fullName} (${it.email})" } ?: adminName
            val payload = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("unlock_code", code)
                put("shop_username", username.trim().lowercase(Locale.ROOT))
                put("shop_phone", registeredPhone.trim())
                put("device_challenge", deviceChallenge)
                put("tier_code", tierCode)
                put("plan_days", days)
                put("amount_rwf", amountRwf)
                put("status", "ISSUED")
                put("issued_by_admin", operatorLabel)
                put("issued_at", System.currentTimeMillis())
            }
            val req = buildRestRequest("unlock_codes", useAdminJwtIfAvailable = true)
                .addHeader("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                val shopKey = computeShopSyncKey(username, registeredPhone)
                extendShopExpiryInternal(shopKey, days)
                logAdminAudit("ISSUE_CODE", username, "Issued $code (+$days d, $amountRwf FRw)")
            }
            Pair(ok, code)
        } catch (e: Exception) {
            Log.e(TAG, "issueManualUnlockCode error: ${e.message}")
            Pair(false, code)
        }
    }

    suspend fun adminExtendShopSubscription(
        shop: SupabaseShopAccountDto,
        addDays: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val now = System.currentTimeMillis()
            val base = maxOf(shop.subscriptionExpiresAt, now)
            val newExpiry = base + addDays.coerceAtLeast(1) * 86_400_000L
            val patch = JSONObject().apply {
                put("subscription_status", "ACTIVE")
                put("subscription_expires_at", newExpiry)
                put("updated_at", now)
            }
            val req = buildRestRequest(
                "shop_accounts?id=eq.${URLEncoder.encode(shop.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(patch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("EXTEND_SHOP", "@${shop.ownerUsername}", "Extended ${shop.shopName} license by +$addDays days")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminExtendShopSubscription error: ${e.message}")
            false
        }
    }

    suspend fun adminSetShopStatus(
        shop: SupabaseShopAccountDto,
        newStatus: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val now = System.currentTimeMillis()
            val normalized = newStatus.trim().uppercase(Locale.ROOT)
            val isLocking = normalized == ShopProfile.STATUS_EXPIRED || normalized == ShopProfile.STATUS_SUSPENDED
            val patch = JSONObject().apply {
                put("subscription_status", normalized)
                if (isLocking) {
                    put("subscription_expires_at", now)
                }
                put("updated_at", now)
            }
            val req = buildRestRequest(
                "shop_accounts?id=eq.${URLEncoder.encode(shop.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(patch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("SET_SHOP_STATUS", "@${shop.ownerUsername}", "${shop.shopName} -> $normalized")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminSetShopStatus error: ${e.message}")
            false
        }
    }

    suspend fun adminResetShopTrial(
        shop: SupabaseShopAccountDto,
        trialDays: Int = ShopProfile.FREE_TRIAL_DAYS
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val now = System.currentTimeMillis()
            val safeDays = trialDays.coerceIn(1, 14)
            val newExpiry = now + safeDays * 86_400_000L
            val patch = JSONObject().apply {
                put("subscription_status", ShopProfile.STATUS_TRIAL)
                put("subscription_expires_at", newExpiry)
                put("updated_at", now)
            }
            val req = buildRestRequest(
                "shop_accounts?id=eq.${URLEncoder.encode(shop.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(patch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("RESET_TRIAL", "@${shop.ownerUsername}", "Reset ${shop.shopName} to ${safeDays}d TRIAL")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminResetShopTrial error: ${e.message}")
            false
        }
    }

    suspend fun approvePaymentBatch(
        requests: List<SupabasePaymentRequestDto>,
        adminName: String
    ): BatchApprovalResult = withContext(Dispatchers.IO) {
        if (!isConfigured() || ensureValidAdminSession() == null) {
            return@withContext BatchApprovalResult(approvedCount = 0, failedCount = requests.size)
        }
        var approved = 0
        var failed = 0
        val issued = mutableListOf<Pair<String, String>>()
        for (req in requests.filter { it.status == "PENDING_VERIFICATION" }) {
            val (ok, code) = approvePaymentAndIssueCode(req, adminName)
            if (ok) {
                approved++
                issued.add(req.ownerUsername to code)
            } else {
                failed++
            }
        }
        if (approved > 0) {
            logAdminAudit(
                action = "BATCH_APPROVE_PAYMENTS",
                targetId = "count:$approved",
                details = "Batch approved $approved pending payment request(s), failed: $failed"
            )
        }
        BatchApprovalResult(approvedCount = approved, failedCount = failed, issuedCodes = issued)
    }

    suspend fun adminUpdateShopLimits(
        shop: SupabaseShopAccountDto,
        branchCount: Int,
        workerCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || ensureValidAdminSession() == null) return@withContext false
        try {
            val safeBranches = branchCount.coerceAtLeast(1)
            val safeWorkers = workerCount.coerceAtLeast(1)
            val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(safeBranches, safeWorkers, 1)
            val now = System.currentTimeMillis()
            val patch = JSONObject().apply {
                put("branch_count", safeBranches)
                put("worker_count", safeWorkers)
                put("monthly_fee_rwf", pricing.monthlySubtotal)
                put("updated_at", now)
            }
            val req = buildRestRequest(
                "shop_accounts?id=eq.${URLEncoder.encode(shop.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(patch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit(
                    "UPDATE_SHOP_LIMITS",
                    "@${shop.ownerUsername}",
                    "${shop.shopName}: branches=$safeBranches, workers=$safeWorkers, fee=${pricing.monthlySubtotal} FRw/mo"
                )
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminUpdateShopLimits error: ${e.message}")
            false
        }
    }

    suspend fun adminDeleteShopAccount(shop: SupabaseShopAccountDto): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val req = buildRestRequest(
                "shop_accounts?id=eq.${URLEncoder.encode(shop.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).delete().build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("DELETE_SHOP", "@${shop.ownerUsername}", "Deleted shop record ${shop.shopName}")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminDeleteShopAccount error: ${e.message}")
            false
        }
    }

    suspend fun adminUpdateShopUser(
        user: SupabaseShopUserDto,
        newRole: String? = null,
        isActive: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val now = System.currentTimeMillis()
            val patch = JSONObject().apply {
                if (newRole != null) put("role", newRole)
                if (isActive != null) put("is_active", isActive)
                put("updated_at", now)
            }
            val req = buildRestRequest(
                "shop_users?id=eq.${URLEncoder.encode(user.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).patch(patch.toString().toRequestBody(jsonMediaType)).build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                val changes = buildList {
                    if (newRole != null) add("role=$newRole")
                    if (isActive != null) add("active=$isActive")
                }.joinToString(", ")
                logAdminAudit("UPDATE_USER", "@${user.username}", "Updated ${user.fullName} (${user.shopName}): $changes")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminUpdateShopUser error: ${e.message}")
            false
        }
    }

    suspend fun adminDeleteShopUser(user: SupabaseShopUserDto): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val req = buildRestRequest(
                "shop_users?id=eq.${URLEncoder.encode(user.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).delete().build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("DELETE_USER", "@${user.username}", "Removed user ${user.fullName} from ${user.shopName}")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminDeleteShopUser error: ${e.message}")
            false
        }
    }

    suspend fun adminDeleteBranchSnapshot(snapshot: SupabaseBranchSnapshotDto): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || activeAdminSession == null) return@withContext false
        try {
            val req = buildRestRequest(
                "branch_sync_snapshots?id=eq.${URLEncoder.encode(snapshot.id, "UTF-8")}",
                useAdminJwtIfAvailable = true
            ).delete().build()
            val ok = client.newCall(req).execute().use { it.isSuccessful }
            if (ok) {
                logAdminAudit("DELETE_SNAPSHOT", snapshot.deviceId, "Removed branch snapshot ${snapshot.branchName} (${snapshot.shopName})")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "adminDeleteBranchSnapshot error: ${e.message}")
            false
        }
    }

    private fun extendShopExpiryInternal(shopId: String, addDays: Int) {
        try {
            val enc = URLEncoder.encode(shopId, "UTF-8")
            val getReq = buildRestRequest("shop_accounts?id=eq.$enc&limit=1", useAdminJwtIfAvailable = true).get().build()
            var existingExpiry = 0L
            var exists = false
            client.newCall(getReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                    if (arr.length() > 0) {
                        exists = true
                        existingExpiry = arr.getJSONObject(0).optLong("subscription_expires_at", 0L)
                    }
                }
            }
            if (!exists) return
            val now = System.currentTimeMillis()
            val newExpiry = maxOf(existingExpiry, now) + addDays.coerceAtLeast(1) * 86_400_000L
            val patch = JSONObject().apply {
                put("subscription_status", "ACTIVE")
                put("subscription_expires_at", newExpiry)
                put("updated_at", now)
            }
            val patchReq = buildRestRequest("shop_accounts?id=eq.$enc", useAdminJwtIfAvailable = true)
                .patch(patch.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(patchReq).execute().close()
        } catch (_: Exception) {}
    }

    private fun logAdminAudit(action: String, targetId: String, details: String) {
        val session = activeAdminSession ?: return
        try {
            val payload = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("admin_email", session.email)
                put("action", action)
                put("target_id", targetId)
                put("details", details)
                put("created_at", System.currentTimeMillis())
            }
            val req = buildRestRequest("admin_audit_logs", useAdminJwtIfAvailable = true)
                .addHeader("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(req).execute().close()
        } catch (_: Exception) {}
    }

    private fun parsePaymentRequestDto(o: JSONObject): SupabasePaymentRequestDto {
        return SupabasePaymentRequestDto(
            id = o.optString("id"),
            shopName = o.optString("shop_name"),
            ownerUsername = o.optString("owner_username"),
            ownerPhone = o.optString("owner_phone"),
            deviceChallenge = o.optString("device_challenge"),
            provider = o.optString("provider"),
            ussdCode = o.optString("ussd_code"),
            payerPhone = o.optString("payer_phone"),
            amountRwf = o.optInt("amount_rwf", 0),
            durationMonths = o.optInt("duration_months", 1),
            branchCount = o.optInt("branch_count", 1),
            workerCount = o.optInt("worker_count", 1),
            telcoReference = o.optString("telco_reference"),
            status = o.optString("status", "PENDING_VERIFICATION"),
            generatedUnlockCode = o.optString("generated_unlock_code"),
            verifiedByAdmin = o.optString("verified_by_admin"),
            rejectionReason = o.optString("rejection_reason"),
            createdAt = o.optLong("created_at", 0L),
            updatedAt = o.optLong("updated_at", 0L)
        )
    }

    private fun parseUnlockCodeDto(o: JSONObject): SupabaseUnlockCodeDto {
        return SupabaseUnlockCodeDto(
            id = o.optString("id"),
            unlockCode = o.optString("unlock_code"),
            shopUsername = o.optString("shop_username"),
            shopPhone = o.optString("shop_phone"),
            deviceChallenge = o.optString("device_challenge"),
            tierCode = o.optString("tier_code"),
            planDays = o.optInt("plan_days", 30),
            amountRwf = o.optInt("amount_rwf", 5000),
            status = o.optString("status", "ISSUED"),
            issuedByAdmin = o.optString("issued_by_admin"),
            issuedAt = o.optLong("issued_at", 0L),
            redeemedAt = if (o.isNull("redeemed_at")) null else o.optLong("redeemed_at")
        )
    }

    private fun parseShopAccountDto(o: JSONObject): SupabaseShopAccountDto {
        return SupabaseShopAccountDto(
            id = o.optString("id"),
            shopName = o.optString("shop_name"),
            ownerName = o.optString("owner_name"),
            ownerUsername = o.optString("owner_username"),
            ownerPhone = o.optString("owner_phone"),
            deviceChallenge = o.optString("device_challenge"),
            branchCount = o.optInt("branch_count", 1),
            workerCount = o.optInt("worker_count", 1),
            monthlyFeeRwf = o.optInt("monthly_fee_rwf", 5000),
            subscriptionStatus = o.optString("subscription_status", "ACTIVE"),
            subscriptionExpiresAt = o.optLong("subscription_expires_at", 0L),
            updatedAt = o.optLong("updated_at", 0L)
        )
    }

    private fun parseShopUserDto(o: JSONObject): SupabaseShopUserDto {
        return SupabaseShopUserDto(
            id = o.optString("id"),
            shopSyncKey = o.optString("shop_sync_key"),
            shopName = o.optString("shop_name"),
            userId = o.optString("user_id"),
            fullName = o.optString("full_name"),
            username = o.optString("username"),
            phone = o.optString("phone"),
            role = o.optString("role", "CASHIER"),
            assignedBranchId = o.optString("assigned_branch_id"),
            assignedBranchName = o.optString("assigned_branch_name", "All Branches"),
            isActive = o.optBoolean("is_active", true),
            updatedAt = o.optLong("updated_at", 0L)
        )
    }
}
