package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.ShopProfile
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

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

/**
 * Supabase PostgREST + GoTrue Auth Client for:
 * 1. Authenticated Admin Licensing Operations (`shop_accounts`, `payment_requests`, `unlock_codes`, `admin_profiles`, `admin_audit_logs`)
 * 2. Synchronous Cross-Branch Internet DataShare Relay (`branch_sync_snapshots`)
 *
 * All POS, Inventory, Sales, Customers, Purchases, and Debts remain 100% offline-first in Room SQLite.
 */
object SupabaseAdminClient {

    private const val TAG = "SupabaseAdminClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var activeAdminSession: SupabaseAdminSession? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
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
        return try {
            BuildConfig.SUPABASE_ADMIN_EMAIL.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()
        return url.startsWith("https://") &&
            !url.contains("your-project-id") &&
            key.isNotBlank() &&
            !key.contains("your_supabase_anon_key")
    }

    fun getCurrentAdminSession(): SupabaseAdminSession? = activeAdminSession

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

    private fun buildRestRequest(path: String, useAdminJwtIfAvailable: Boolean = false): Request.Builder {
        val url = "${getSupabaseUrl()}/rest/v1/$path"
        val key = getSupabaseAnonKey()
        val bearer = if (useAdminJwtIfAvailable && activeAdminSession != null) {
            activeAdminSession!!.accessToken
        } else {
            key
        }
        return Request.Builder()
            .url(url)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $bearer")
            .addHeader("Content-Type", "application/json")
    }

    // -------------------------------------------------------------------------
    // REAL SUPABASE ADMIN AUTHENTICATION (GoTrue Email/Password JWT)
    // -------------------------------------------------------------------------
    suspend fun signInAdmin(email: String, password: String): Result<SupabaseAdminSession> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."))
        }
        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase(Locale.ROOT))
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
                val session = parseAuthSession(json, email)
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
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."))
        }
        try {
            val cleanName = fullName.trim().ifBlank { email.substringBefore("@") }
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase(Locale.ROOT))
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
            val req = buildRestRequest("branch_sync_snapshots?on_conflict=id")
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
            val req = buildRestRequest("branch_sync_snapshots?shop_sync_key=eq.$encKey&order=updated_at.asc&limit=30")
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

            val req = buildRestRequest("shop_accounts?on_conflict=id")
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

            val req = buildRestRequest("payment_requests?on_conflict=id")
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
                val cleanUser = URLEncoder.encode(username.trim().lowercase(Locale.ROOT), "UTF-8")
                val path = "payment_requests?owner_username=eq.$cleanUser&status=eq.APPROVED&order=updated_at.desc&limit=10"
                val req = buildRestRequest(path).get().build()

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
            val cleanCode = URLEncoder.encode(unlockCode.trim().uppercase(Locale.ROOT), "UTF-8")
            val req = buildRestRequest("unlock_codes?unlock_code=eq.$cleanCode&limit=1").get().build()
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
                val patchReq = buildRestRequest("unlock_codes?id=eq.${URLEncoder.encode(matched!!.id, "UTF-8")}")
                    .patch(patchBody.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(patchReq).execute().close()

                val cleanUser = URLEncoder.encode(username.trim().lowercase(Locale.ROOT), "UTF-8")
                val prPatch = JSONObject().apply {
                    put("status", "REDEEMED")
                    put("updated_at", now)
                }
                val prReq = buildRestRequest("payment_requests?generated_unlock_code=eq.$cleanCode&owner_username=eq.$cleanUser")
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
            val req = buildRestRequest("shop_accounts?order=updated_at.desc&limit=100", useAdminJwtIfAvailable = true)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).map { idx ->
                    val o = arr.getJSONObject(idx)
                    SupabaseShopAccountDto(
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllShopAccounts error: ${e.message}")
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
        val tierCode = when {
            request.durationMonths >= 12 -> "ANNUAL"
            request.branchCount >= 3 -> "MULTI"
            request.branchCount == 2 -> "DUAL"
            else -> "SINGLE"
        }
        val days = (request.durationMonths.coerceAtLeast(1)) * 30
        val generatedCode = SubscriptionSecurityManager.generateAccountBoundUnlockCode(
            username = request.ownerUsername,
            registeredPhone = request.ownerPhone,
            tierCode = tierCode,
            days = days
        )

        if (!isConfigured() || activeAdminSession == null) {
            return@withContext Pair(false, generatedCode)
        }

        try {
            val now = System.currentTimeMillis()
            val codeId = UUID.randomUUID().toString()
            val operatorLabel = activeAdminSession?.let { "${it.fullName} (${it.email})" } ?: adminName
            val codePayload = JSONObject().apply {
                put("id", codeId)
                put("unlock_code", generatedCode)
                put("shop_username", request.ownerUsername.trim().lowercase(Locale.ROOT))
                put("shop_phone", request.ownerPhone.trim())
                put("device_challenge", request.deviceChallenge)
                put("tier_code", tierCode)
                put("plan_days", days)
                put("amount_rwf", request.amountRwf)
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
                logAdminAudit(
                    action = "APPROVE_PAYMENT",
                    targetId = request.ownerUsername,
                    details = "Approved ${request.amountRwf} FRw (${request.provider}) -> $generatedCode"
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
                logAdminAudit("ISSUE_CODE", username, "Issued $code (+$days d, $amountRwf FRw)")
            }
            Pair(ok, code)
        } catch (e: Exception) {
            Log.e(TAG, "issueManualUnlockCode error: ${e.message}")
            Pair(false, code)
        }
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
}
