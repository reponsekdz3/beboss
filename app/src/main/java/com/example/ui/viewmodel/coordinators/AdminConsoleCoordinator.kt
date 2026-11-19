package com.example.ui.viewmodel.coordinators

import android.content.Context
import com.example.data.model.Branch
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BeBossRepository
import com.example.util.OfflineSubscriptionManager
import com.example.util.PaymentProcessingResult
import com.example.util.ShopDataTransferManager
import com.example.util.SubscriptionPriceBreakdown
import com.example.util.SubscriptionSecurityManager
import com.example.util.SupabaseAdminClient
import com.example.util.SupabaseAdminSession
import com.example.util.SupabaseAuditLogDto
import com.example.util.SupabaseBranchSnapshotDto
import com.example.util.SupabasePaymentRequestDto
import com.example.util.SupabaseShopAccountDto
import com.example.util.SupabaseShopUserDto
import com.example.util.SupabaseUnlockCodeDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminConsoleCoordinator(
    private val application: android.app.Application,
    private val repository: BeBossRepository,
    private val scope: CoroutineScope,
    private val currentUser: StateFlow<User?>,
    private val allUsers: StateFlow<List<User>>,
    private val branches: StateFlow<List<Branch>>,
    private val shopProfile: StateFlow<ShopProfile>,
    private val emitSnackbar: suspend (String) -> Unit,
    private val triggerCloudSync: () -> Unit
) {
    private val _adminSession = MutableStateFlow<SupabaseAdminSession?>(
        SupabaseAdminClient.getCurrentAdminSession()
    )
    val adminSession: StateFlow<SupabaseAdminSession?> = _adminSession.asStateFlow()

    private val _adminAuthError = MutableStateFlow<String?>(null)
    val adminAuthError: StateFlow<String?> = _adminAuthError.asStateFlow()

    private val _isAdminAuthenticating = MutableStateFlow(false)
    val isAdminAuthenticating: StateFlow<Boolean> = _isAdminAuthenticating.asStateFlow()

    private val _adminPaymentRequests = MutableStateFlow<List<SupabasePaymentRequestDto>>(emptyList())
    val adminPaymentRequests: StateFlow<List<SupabasePaymentRequestDto>> = _adminPaymentRequests.asStateFlow()

    private val _adminShopAccounts = MutableStateFlow<List<SupabaseShopAccountDto>>(emptyList())
    val adminShopAccounts: StateFlow<List<SupabaseShopAccountDto>> = _adminShopAccounts.asStateFlow()

    private val _adminShopUsers = MutableStateFlow<List<SupabaseShopUserDto>>(emptyList())
    val adminShopUsers: StateFlow<List<SupabaseShopUserDto>> = _adminShopUsers.asStateFlow()

    private val _adminBranchSnapshots = MutableStateFlow<List<SupabaseBranchSnapshotDto>>(emptyList())
    val adminBranchSnapshots: StateFlow<List<SupabaseBranchSnapshotDto>> = _adminBranchSnapshots.asStateFlow()

    private val _adminUnlockCodes = MutableStateFlow<List<SupabaseUnlockCodeDto>>(emptyList())
    val adminUnlockCodes: StateFlow<List<SupabaseUnlockCodeDto>> = _adminUnlockCodes.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<SupabaseAuditLogDto>>(emptyList())
    val adminAuditLogs: StateFlow<List<SupabaseAuditLogDto>> = _adminAuditLogs.asStateFlow()

    private val _isAdminLoading = MutableStateFlow(false)
    val isAdminLoading: StateFlow<Boolean> = _isAdminLoading.asStateFlow()

    private val _adminLastSyncedAt = MutableStateFlow(0L)
    val adminLastSyncedAt: StateFlow<Long> = _adminLastSyncedAt.asStateFlow()

    private fun resolveOwnerUser(): User? {
        return allUsers.value.firstOrNull { it.role == UserRole.OWNER }
            ?: currentUser.value
            ?: allUsers.value.firstOrNull()
    }

    fun signInSupabaseAdmin(email: String, password: String) {
        scope.launch {
            _isAdminAuthenticating.value = true
            _adminAuthError.value = null
            val res = SupabaseAdminClient.signInAdmin(email, password)
            _isAdminAuthenticating.value = false
            res.onSuccess { sess ->
                _adminSession.value = sess
                repository.recordLocalAudit(
                    actorName = sess.fullName,
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_SIGN_IN",
                    targetId = sess.email,
                    details = "Signed in to Master Admin Console"
                )
                refreshAdminDashboardData()
                emitSnackbar("Authenticated as Master Admin (${sess.email})")
            }.onFailure { err ->
                _adminAuthError.value = err.message ?: "Admin sign-in failed"
            }
        }
    }

    fun signUpSupabaseAdmin(email: String, password: String, fullName: String) {
        scope.launch {
            _isAdminAuthenticating.value = true
            _adminAuthError.value = null
            val res = SupabaseAdminClient.signUpAdmin(email, password, fullName)
            _isAdminAuthenticating.value = false
            res.onSuccess { sess ->
                _adminSession.value = sess
                repository.recordLocalAudit(
                    actorName = sess.fullName,
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_INITIALIZED",
                    targetId = sess.email,
                    details = "Initialized Master Admin account"
                )
                refreshAdminDashboardData()
                emitSnackbar("Master Admin profile initialized (${sess.email})")
            }.onFailure { err ->
                _adminAuthError.value = err.message ?: "Admin initialization failed"
            }
        }
    }

    fun signOutSupabaseAdmin() {
        scope.launch {
            val prev = _adminSession.value
            SupabaseAdminClient.signOutAdmin()
            _adminSession.value = null
            _adminPaymentRequests.value = emptyList()
            _adminShopAccounts.value = emptyList()
            _adminShopUsers.value = emptyList()
            _adminBranchSnapshots.value = emptyList()
            _adminUnlockCodes.value = emptyList()
            _adminAuditLogs.value = emptyList()
            _adminAuthError.value = null
            if (prev != null) {
                repository.recordLocalAudit(
                    actorName = prev.fullName,
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_SIGN_OUT",
                    targetId = prev.email,
                    details = "Signed out from Master Admin Console"
                )
            }
        }
    }

    fun activateSubscriptionVoucher(code: String) {
        scope.launch {
            val currentProf = shopProfile.value
            val owner = resolveOwnerUser()
            val branchCount = branches.value.size.coerceAtLeast(1)
            val workerCount = allUsers.value.size.coerceAtLeast(1)
            val result = repository.redeemVoucher(
                voucherCode = code,
                shopProfile = currentProf,
                branchCount = branchCount,
                workerCount = workerCount,
                ownerUsername = owner?.username.orEmpty(),
                ownerPhoneOverride = owner?.phone.orEmpty(),
                ownerUser = owner
            )
            repository.recordLocalAudit(
                actorName = owner?.name ?: "Shop Owner",
                actorRole = owner?.role?.name ?: "OWNER",
                action = if (result.isValid) "VOUCHER_ACTIVATED" else "VOUCHER_REJECTED",
                targetId = code.trim().take(24),
                details = result.message
            )
            emitSnackbar(result.message)
            if (result.isValid) {
                syncAdminDataToSupabase(silent = true)
            }
        }
    }

    fun submitUssdPaymentProof(
        provider: String,
        payerPhone: String,
        username: String,
        registeredPhone: String,
        durationMonths: Int,
        branchCount: Int,
        workerCount: Int,
        telcoReference: String = "",
        onSubmitted: (PaymentVerificationRequest, Boolean) -> Unit = { _, _ -> }
    ) {
        scope.launch {
            val profile = shopProfile.value
            val owner = resolveOwnerUser()
            val breakdown = OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths
            )
            val ussd = if (provider.contains("AIRTEL", ignoreCase = true)) breakdown.ussdAirtelCode else breakdown.ussdMtnCode
            val cleanUsername = username.trim().lowercase().ifBlank { owner?.username ?: "owner" }
            val cleanRegPhone = registeredPhone.trim().ifBlank { profile.phone }
            val req = PaymentVerificationRequest(
                shopName = profile.shopName,
                ownerUsername = cleanUsername,
                ownerPhone = cleanRegPhone,
                deviceChallenge = SubscriptionSecurityManager.computeDeviceChallenge(profile),
                provider = provider,
                ussdCode = ussd,
                payerPhone = payerPhone.trim().ifBlank { cleanRegPhone },
                amountRwf = breakdown.totalPayable,
                durationMonths = durationMonths,
                branchCount = branchCount.coerceAtLeast(1),
                workerCount = workerCount.coerceAtLeast(1),
                telcoReference = telcoReference.trim().uppercase(),
                status = "PENDING_VERIFICATION",
                syncedToSupabase = false
            )
            val synced = repository.submitPaymentVerificationRequest(req, profile, owner)
            repository.logSyncEvent(
                type = "SUPABASE_ADMIN",
                status = if (synced) "SUCCESS" else "QUEUED",
                summary = if (synced) "Payment proof synced to Supabase Admin Queue" else "Saved offline — auto-syncs to Supabase when internet returns"
            )
            emitSnackbar(
                if (synced) "Payment verification submitted to Admin Cloud! Send WhatsApp proof now."
                else "Saved offline! Will sync to Supabase automatically when internet returns. Send WhatsApp proof now."
            )
            onSubmitted(req, synced)
        }
    }

    suspend fun syncAdminDataToSupabaseSuspended(silent: Boolean = false) {
        val profile = shopProfile.value
        val owner = resolveOwnerUser()
        val branchCount = branches.value.size.coerceAtLeast(1)
        val workerCount = allUsers.value.size.coerceAtLeast(1)
        val res = repository.syncPendingAdminDataWithSupabase(
            shopProfile = profile,
            ownerUser = owner,
            branchCount = branchCount,
            workerCount = workerCount
        )
        if (!silent || res.approvedCodesFound.isNotEmpty()) {
            emitSnackbar(res.message)
        }
    }

    fun syncAdminDataToSupabase(silent: Boolean = false) {
        scope.launch {
            syncAdminDataToSupabaseSuspended(silent)
        }
    }

    fun checkCloudUnlockApprovals() {
        syncAdminDataToSupabase(silent = false)
    }

    fun refreshAdminDashboardData() {
        if (_adminSession.value == null) return
        scope.launch {
            _isAdminLoading.value = true
            val validSession = SupabaseAdminClient.ensureValidAdminSession()
            _adminSession.value = validSession
            if (validSession == null) {
                _adminAuthError.value = "Admin session expired. Sign in again."
                _isAdminLoading.value = false
                return@launch
            }
            _adminPaymentRequests.value = SupabaseAdminClient.fetchAllPaymentRequests()
            _adminShopAccounts.value = SupabaseAdminClient.fetchAllShopAccounts()
            _adminShopUsers.value = SupabaseAdminClient.fetchAllShopUsers()
            _adminBranchSnapshots.value = SupabaseAdminClient.fetchAllBranchSnapshots()
            _adminUnlockCodes.value = SupabaseAdminClient.fetchAllUnlockCodes()
            _adminAuditLogs.value = SupabaseAdminClient.fetchAdminAuditLogs()
            _adminLastSyncedAt.value = System.currentTimeMillis()
            _isAdminLoading.value = false
        }
    }

    fun adminApproveAndGenerateCode(
        request: SupabasePaymentRequestDto,
        onGenerated: (Boolean, String) -> Unit
    ) {
        scope.launch {
            _isAdminLoading.value = true
            val adminName = _adminSession.value?.fullName ?: currentUser.value?.name ?: "BeBoss Master Admin"
            val (ok, code) = SupabaseAdminClient.approvePaymentAndIssueCode(request, adminName)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = adminName,
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_APPROVE_PAYMENT",
                    targetId = request.ownerUsername,
                    details = "Approved ${request.amountRwf} FRw (${request.durationMonths * 30}d) -> ${code.take(18)}..."
                )
                refreshAdminDashboardData()
                emitSnackbar("Approved & generated unlock code: $code")
            } else {
                emitSnackbar("Admin Supabase approval failed — sign in as Master Admin.")
            }
            _isAdminLoading.value = false
            onGenerated(ok, code)
        }
    }

    fun adminApproveBatchPendingPayments(onDone: (Int, Int) -> Unit = { _, _ -> }) {
        scope.launch {
            _isAdminLoading.value = true
            val pending = _adminPaymentRequests.value.filter { it.status == "PENDING_VERIFICATION" }
            val adminName = _adminSession.value?.fullName ?: "BeBoss Master Admin"
            val res = SupabaseAdminClient.approvePaymentBatch(pending, adminName)
            if (res.approvedCount > 0) {
                repository.recordLocalAudit(
                    actorName = adminName,
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_BATCH_APPROVE",
                    targetId = "batch:${res.approvedCount}",
                    details = "Batch approved ${res.approvedCount} pending payment claims (${res.failedCount} failed)"
                )
                refreshAdminDashboardData()
            }
            _isAdminLoading.value = false
            emitSnackbar("Batch approval: ${res.approvedCount} approved, ${res.failedCount} failed.")
            onDone(res.approvedCount, res.failedCount)
        }
    }

    fun adminRejectPayment(
        request: SupabasePaymentRequestDto,
        reason: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.rejectPaymentRequest(request, reason)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = _adminSession.value?.fullName ?: "MASTER_ADMIN",
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_REJECT_PAYMENT",
                    targetId = request.ownerUsername,
                    details = reason
                )
                refreshAdminDashboardData()
                emitSnackbar("Payment request marked REJECTED.")
            } else {
                emitSnackbar("Could not reject request in Supabase.")
            }
            _isAdminLoading.value = false
            onDone(ok)
        }
    }

    fun adminRevokeCode(codeDto: SupabaseUnlockCodeDto) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.revokeUnlockCode(codeDto)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = _adminSession.value?.fullName ?: "MASTER_ADMIN",
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_REVOKE_CODE",
                    targetId = codeDto.shopUsername,
                    details = "Revoked ${codeDto.unlockCode}"
                )
                refreshAdminDashboardData()
                emitSnackbar("Revoked unlock code ${codeDto.unlockCode}.")
            } else {
                emitSnackbar("Could not revoke code in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminIssueManualCode(
        username: String,
        registeredPhone: String,
        tierCode: String,
        days: Int,
        amountRwf: Int,
        onGenerated: (Boolean, String) -> Unit
    ) {
        scope.launch {
            _isAdminLoading.value = true
            val adminName = _adminSession.value?.fullName ?: currentUser.value?.name ?: "BeBoss Master Admin"
            val deviceCode = SubscriptionSecurityManager.computeDeviceChallenge(shopProfile.value)
            val (ok, code) = SupabaseAdminClient.issueManualUnlockCode(
                username = username,
                registeredPhone = registeredPhone,
                deviceChallenge = deviceCode,
                tierCode = tierCode,
                days = days,
                amountRwf = amountRwf,
                adminName = adminName
            )
            if (ok) {
                repository.recordLocalAudit(
                    actorName = adminName,
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_ISSUE_MANUAL_CODE",
                    targetId = username,
                    details = "$tierCode ($days d, $amountRwf FRw)"
                )
                refreshAdminDashboardData()
            } else {
                emitSnackbar("Failed generating BB2 code — verify secret & Admin session.")
            }
            _isAdminLoading.value = false
            onGenerated(ok, code)
        }
    }

    fun adminExtendShopSubscription(shop: SupabaseShopAccountDto, addDays: Int) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminExtendShopSubscription(shop, addDays)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = _adminSession.value?.fullName ?: "MASTER_ADMIN",
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_EXTEND_SHOP",
                    targetId = shop.ownerUsername,
                    details = "+$addDays days for ${shop.shopName}"
                )
                refreshAdminDashboardData()
                emitSnackbar("Extended ${shop.shopName} license by +$addDays days.")
            } else {
                emitSnackbar("Failed extending shop license in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminSetShopStatus(shop: SupabaseShopAccountDto, status: String) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminSetShopStatus(shop, status)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = _adminSession.value?.fullName ?: "MASTER_ADMIN",
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_SET_SHOP_STATUS",
                    targetId = shop.ownerUsername,
                    details = "${shop.shopName} -> $status"
                )
                refreshAdminDashboardData()
                emitSnackbar("${shop.shopName} set to $status.")
            } else {
                emitSnackbar("Failed updating shop status.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminUpdateShopLimits(shop: SupabaseShopAccountDto, branchCount: Int, workerCount: Int) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminUpdateShopLimits(shop, branchCount, workerCount)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = _adminSession.value?.fullName ?: "MASTER_ADMIN",
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_UPDATE_SHOP_LIMITS",
                    targetId = shop.ownerUsername,
                    details = "${shop.shopName}: $branchCount branches / $workerCount workers"
                )
                refreshAdminDashboardData()
                emitSnackbar("Updated quotas for ${shop.shopName} ($branchCount br / $workerCount workers).")
            } else {
                emitSnackbar("Failed updating shop quotas.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminDeleteShopAccount(shop: SupabaseShopAccountDto) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminDeleteShopAccount(shop)
            if (ok) {
                refreshAdminDashboardData()
                emitSnackbar("Deleted shop record ${shop.shopName}.")
            } else {
                emitSnackbar("Failed deleting shop record.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminUpdateShopUserRole(user: SupabaseShopUserDto, newRole: String) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminUpdateShopUser(user, newRole = newRole)
            if (ok) {
                refreshAdminDashboardData()
                emitSnackbar("@${user.username} role updated to $newRole.")
            } else {
                emitSnackbar("Failed updating user role in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminToggleShopUserActive(user: SupabaseShopUserDto, newActive: Boolean) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminUpdateShopUser(user, isActive = newActive)
            if (ok) {
                refreshAdminDashboardData()
                emitSnackbar("@${user.username} ${if (newActive) "reactivated" else "suspended"}.")
            } else {
                emitSnackbar("Failed updating user status in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminDeleteShopUser(user: SupabaseShopUserDto) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminDeleteShopUser(user)
            if (ok) {
                refreshAdminDashboardData()
                emitSnackbar("Removed user @${user.username} from directory.")
            } else {
                emitSnackbar("Failed deleting user in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminDeleteBranchSnapshot(snapshot: SupabaseBranchSnapshotDto) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminDeleteBranchSnapshot(snapshot)
            if (ok) {
                refreshAdminDashboardData()
                emitSnackbar("Removed branch snapshot (${snapshot.branchName}).")
            } else {
                emitSnackbar("Failed removing branch snapshot.")
            }
            _isAdminLoading.value = false
        }
    }

    fun shareAdminWebPortal(context: Context) {
        try {
            ShopDataTransferManager.exportAndShareAdminWebPortal(context)
        } catch (e: Exception) {
            scope.launch { emitSnackbar("Cannot export Web Portal: ${e.message}") }
        }
    }

    fun adminResetShopTrial(shop: SupabaseShopAccountDto, trialDays: Int = ShopProfile.FREE_TRIAL_DAYS) {
        scope.launch {
            _isAdminLoading.value = true
            val ok = SupabaseAdminClient.adminResetShopTrial(shop, trialDays)
            if (ok) {
                repository.recordLocalAudit(
                    actorName = _adminSession.value?.fullName ?: "MASTER_ADMIN",
                    actorRole = "MASTER_ADMIN",
                    action = "ADMIN_RESET_TRIAL",
                    targetId = shop.ownerUsername,
                    details = "Reset ${shop.shopName} to ${trialDays}-day TRIAL"
                )
                refreshAdminDashboardData()
                emitSnackbar("Reset ${shop.shopName} to $trialDays-day Free Trial.")
            } else {
                emitSnackbar("Failed resetting trial in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun processDirectSubscriptionPayment(
        provider: String,
        phone: String,
        durationMonths: Int,
        userReference: String? = null,
        onResult: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit
    ) {
        scope.launch {
            val currentProf = shopProfile.value
            val owner = resolveOwnerUser()
            val branchCount = branches.value.size.coerceAtLeast(1)
            val workerCount = allUsers.value.size.coerceAtLeast(1)
            val breakdown = OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths
            )
            val pastRefs = setOfNotNull(currentProf.lastPaymentRef.takeIf { it.isNotBlank() })
            val result = OfflineSubscriptionManager.processMoMoPaymentAsync(
                context = application,
                shopProfile = currentProf,
                payerPhone = phone,
                provider = provider,
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths,
                userEnteredReference = userReference,
                alreadyUsedRefs = pastRefs
            )

            if (result.isSuccess) {
                val now = com.example.util.ClockGuard.effectiveNowMs()
                val currentExpiry = if (currentProf.subscriptionExpiresAt > now && !currentProf.isSuspended)
                    currentProf.subscriptionExpiresAt
                else now
                val newExpiry = currentExpiry + (result.planDays.toLong() * 24 * 60 * 60 * 1000L)

                val updatedProf = currentProf.copy(
                    subscriptionStatus = ShopProfile.STATUS_ACTIVE,
                    subscriptionExpiresAt = newExpiry,
                    lastPaymentRef = result.transactionRef,
                    lastPaymentAmount = result.amountPaid,
                    lastPaymentDate = now,
                    monthlyFeeRwf = breakdown.monthlySubtotal,
                    updatedAt = now
                )
                repository.updateShopProfile(updatedProf)
                repository.recordLocalAudit(
                    actorName = owner?.name ?: currentProf.name.ifBlank { "Shop Owner" },
                    actorRole = owner?.role?.name ?: "OWNER",
                    action = "ONLINE_PAYMENT_VERIFIED",
                    targetId = result.transactionRef,
                    details = "$provider verified ${result.amountPaid} FRw (+${result.planDays}d)"
                )
                emitSnackbar(result.message)
                syncAdminDataToSupabase(silent = true)
                triggerCloudSync()
            } else {
                emitSnackbar(result.message)
            }
            onResult(result, breakdown)
        }
    }
}
