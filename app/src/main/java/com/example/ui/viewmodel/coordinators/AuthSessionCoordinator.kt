package com.example.ui.viewmodel.coordinators

import android.app.Application
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BeBossRepository
import com.example.util.AppLanguage
import com.example.util.AuthRateLimiter
import com.example.util.Localization
import com.example.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthSessionCoordinator(
    private val application: Application,
    private val prefs: SharedPreferences,
    private val db: AppDatabase,
    private val repository: BeBossRepository,
    private val scope: CoroutineScope,
    private val language: StateFlow<AppLanguage>,
    private val emitSnackbar: suspend (String) -> Unit
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Terminal starts locked until cryptographic PIN, password, or strong hardware biometric succeeds.
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isRegistrationNeeded = MutableStateFlow(false)
    val isRegistrationNeeded: StateFlow<Boolean> = _isRegistrationNeeded.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun checkAuthenticationAndInit() {
        scope.launch {
            val savedUserId = prefs.getString("active_user_id", null)
            val savedUser = if (!savedUserId.isNullOrBlank()) {
                db.userDao().getUserById(savedUserId)?.takeIf { it.isActive }
            } else {
                null
            }

            val count = db.userDao().getUserCount()
            if (count == 0) {
                _currentUser.value = null
                _isRegistrationNeeded.value = true
                _isLocked.value = true
                return@launch
            }

            val preselected = savedUser ?: db.userDao().getAllActiveUsersList().firstOrNull()
            _currentUser.value = preselected
            _isRegistrationNeeded.value = false
            // Always require explicit user authentication on cold launch
            _isLocked.value = true
        }
    }

    fun registerNewShopAndOwner(
        shopName: String,
        currencyCode: String,
        currencySymbol: String,
        address: String,
        phone: String,
        ownerFullName: String,
        username: String,
        pin: String,
        password: String
    ) {
        scope.launch {
            try {
                val cleanPin = pin.trim()
                val cleanPassword = password.trim()
                val pinCheck = SecurityUtils.validatePinStrength(cleanPin)
                if (!pinCheck.isValid) {
                    _authError.value = pinCheck.message
                    return@launch
                }
                val passCheck = SecurityUtils.validatePasswordStrength(cleanPassword)
                if (!passCheck.isValid) {
                    _authError.value = passCheck.message
                    return@launch
                }

                val now = com.example.util.ClockGuard.effectiveNowMs()
                val profile = ShopProfile(
                    id = 1L,
                    name = ownerFullName.trim(),
                    phone = phone.trim(),
                    email = "${username.trim().lowercase()}@beboss.rw",
                    shopName = shopName.trim(),
                    address = address.trim().ifBlank { "Kigali, Rwanda" },
                    currencyCode = currencyCode.trim(),
                    currencySymbol = currencySymbol.trim(),
                    subscriptionStatus = ShopProfile.STATUS_TRIAL,
                    monthlyFeeRwf = 5000,
                    subscriptionExpiresAt = now + ShopProfile.FREE_TRIAL_DURATION_MS,
                    trialStartedAt = now,
                    lastPaymentRef = "FREE-TRIAL-4D",
                    lastPaymentAmount = 0,
                    receiptFooter = Localization.get("default_receipt_footer", language.value)
                )
                repository.updateShopProfile(profile)

                val ownerUser = User(
                    id = UUID.randomUUID().toString(),
                    name = ownerFullName.trim(),
                    username = username.trim().lowercase(),
                    phone = phone.trim(),
                    pinHash = SecurityUtils.hashPin(cleanPin),
                    password = SecurityUtils.hashPassword(cleanPassword),
                    role = UserRole.OWNER,
                    profileColorHex = "#FF6B1A",
                    canSellPOS = true,
                    canApplyDiscounts = true,
                    canManageInventory = true,
                    canViewCostAndProfit = true,
                    canViewAnalytics = true,
                    canManageCustomers = true,
                    canCollectDebt = true,
                    canDeleteRecords = true,
                    canExportReports = true,
                    canManageCollaborators = true,
                    canManageShopSettings = true,
                    canExportImportData = true
                )
                repository.saveUser(ownerUser)

                val mainBranch = com.example.data.model.Branch(
                    id = "main_branch",
                    name = "${shopName.trim()} (HQ)",
                    code = "HQ-01",
                    address = address.trim().ifBlank { "Kigali, Rwanda" },
                    phone = phone.trim(),
                    managerName = ownerFullName.trim(),
                    isMainBranch = true,
                    colorHex = "#FF6B1A"
                )
                repository.saveBranch(mainBranch)
                repository.deleteBranch("branch_kimironko")
                repository.deleteBranch("branch_nyabugogo")
                repository.recordLocalAudit(
                    actorName = ownerUser.name,
                    actorRole = ownerUser.role.name,
                    action = "SHOP_REGISTERED",
                    targetId = ownerUser.username,
                    details = "Initialized shop '${profile.shopName}' on ${ShopProfile.FREE_TRIAL_DAYS}-Day Free Trial with PBKDF2 owner credentials"
                )

                prefs.edit().putString("active_user_id", ownerUser.id).apply()
                _currentUser.value = ownerUser
                _isRegistrationNeeded.value = false
                _isLocked.value = false
                _authError.value = null
                emitSnackbar("${shopName.trim()} — ${Localization.get("reg_success_trial", language.value)}")
            } catch (e: Exception) {
                _authError.value = "Registration failed: ${e.message}"
            }
        }
    }

    fun loginWithPin(pin: String): Boolean {
        val rateKey = _currentUser.value?.id?.let { "pin:$it" } ?: "pin:global"
        val check = AuthRateLimiter.checkAllowed(rateKey, application)
        if (!check.allowed) {
            _authError.value = check.message
            scope.launch {
                repository.recordLocalAudit(
                    actorName = _currentUser.value?.username ?: "Unknown",
                    actorRole = "SECURITY",
                    action = "AUTH_RATE_LIMITED",
                    targetId = rateKey,
                    details = check.message
                )
            }
            return false
        }

        scope.launch {
            val user = repository.getUserByPin(pin.trim())
            if (user != null) {
                AuthRateLimiter.recordSuccess(rateKey, application)
                AuthRateLimiter.recordSuccess("pin:${user.id}", application)
                _currentUser.value = user
                _isLocked.value = false
                _authError.value = null
                prefs.edit().putString("active_user_id", user.id).apply()
                repository.updateLastLogin(user.id)
                repository.recordLocalAudit(
                    actorName = user.name,
                    actorRole = user.role.name,
                    action = "LOGIN_PIN_SUCCESS",
                    targetId = user.username,
                    details = "Unlocked terminal via PBKDF2 PIN verification"
                )
                val welcome = "${Localization.get("welcome_back_prefix", language.value)}, ${user.name}!"
                emitSnackbar(welcome)
            } else {
                val failResult = AuthRateLimiter.recordFailure(rateKey, application)
                _authError.value = if (!failResult.allowed) {
                    failResult.message
                } else {
                    val remainingBeforeLockout = (3 - failResult.failedAttempts).coerceAtLeast(0)
                    val attemptsNote = if (remainingBeforeLockout > 0) " ($remainingBeforeLockout)" else ""
                    "${Localization.get("incorrect_pin", language.value)}$attemptsNote"
                }
                repository.recordLocalAudit(
                    actorName = _currentUser.value?.username ?: "Unknown",
                    actorRole = "SECURITY",
                    action = "LOGIN_PIN_FAILED",
                    targetId = rateKey,
                    details = _authError.value.orEmpty()
                )
            }
        }
        return true
    }

    fun loginWithCredentials(username: String, pinOrPass: String): Boolean {
        val cleanUsername = username.trim().lowercase()
        val rateKey = "cred:$cleanUsername"
        val check = AuthRateLimiter.checkAllowed(rateKey, application)
        if (!check.allowed) {
            _authError.value = check.message
            scope.launch {
                repository.recordLocalAudit(
                    actorName = cleanUsername.ifBlank { "Unknown" },
                    actorRole = "SECURITY",
                    action = "AUTH_RATE_LIMITED",
                    targetId = rateKey,
                    details = check.message
                )
            }
            return false
        }

        scope.launch {
            val user = repository.getUserByUsername(cleanUsername)
            if (user != null && !user.isActive) {
                _authError.value = Localization.get("account_suspended_err", language.value)
                repository.recordLocalAudit(
                    actorName = user.username,
                    actorRole = user.role.name,
                    action = "LOGIN_SUSPENDED_BLOCKED",
                    targetId = user.id,
                    details = "Suspended user attempted credential login"
                )
                return@launch
            }
            val secret = pinOrPass.trim()
            val isValid = user != null && user.isActive && (
                SecurityUtils.verifyPin(secret, user.pinHash) ||
                    SecurityUtils.verifyPassword(secret, user.password)
                )
            if (user != null && isValid) {
                AuthRateLimiter.recordSuccess(rateKey, application)
                _currentUser.value = user
                _isLocked.value = false
                _authError.value = null
                prefs.edit().putString("active_user_id", user.id).apply()
                repository.updateLastLogin(user.id)
                repository.recordLocalAudit(
                    actorName = user.name,
                    actorRole = user.role.name,
                    action = "LOGIN_CREDENTIALS_SUCCESS",
                    targetId = user.username,
                    details = "Authenticated with PBKDF2 password/PIN"
                )
                val welcome = "${Localization.get("logged_in_as_prefix", language.value)} ${user.name} (${user.role.displayName})"
                emitSnackbar(welcome)
            } else {
                val failResult = AuthRateLimiter.recordFailure(rateKey, application)
                _authError.value = if (!failResult.allowed) {
                    failResult.message
                } else {
                    Localization.get("invalid_credentials", language.value)
                }
                repository.recordLocalAudit(
                    actorName = cleanUsername.ifBlank { "Unknown" },
                    actorRole = "SECURITY",
                    action = "LOGIN_CREDENTIALS_FAILED",
                    targetId = rateKey,
                    details = _authError.value.orEmpty()
                )
            }
        }
        return true
    }

    fun unlockAppWithPin(pin: String): Boolean {
        val active = _currentUser.value
        val rateKey = active?.id?.let { "pin:$it" } ?: "pin:global"
        val check = AuthRateLimiter.checkAllowed(rateKey, application)
        if (!check.allowed) {
            _authError.value = check.message
            return false
        }

        val cleanPin = pin.trim()
        if (active != null && active.isActive && SecurityUtils.verifyPin(cleanPin, active.pinHash)) {
            AuthRateLimiter.recordSuccess(rateKey, application)
            _isLocked.value = false
            _authError.value = null
            prefs.edit().putString("active_user_id", active.id).apply()
            scope.launch {
                repository.updateLastLogin(active.id)
                repository.recordLocalAudit(
                    actorName = active.name,
                    actorRole = active.role.name,
                    action = "UNLOCK_PIN_SUCCESS",
                    targetId = active.username,
                    details = "Unlocked active session via PIN"
                )
            }
            return true
        }
        return loginWithPin(cleanPin)
    }

    fun unlockWithBiometric(verifiedByHardware: Boolean) {
        val active = _currentUser.value
        if (!verifiedByHardware || active == null || !active.isActive) {
            _isLocked.value = true
            _authError.value = Localization.get("biometric_required_err", language.value)
            scope.launch {
                repository.recordLocalAudit(
                    actorName = active?.username ?: "Unknown",
                    actorRole = "SECURITY",
                    action = "UNLOCK_BIOMETRIC_REJECTED",
                    targetId = active?.id ?: "",
                    details = "Biometric unlock rejected (verified=$verifiedByHardware, active=${active != null})"
                )
            }
            return
        }

        AuthRateLimiter.recordSuccess("pin:${active.id}", application)
        AuthRateLimiter.recordSuccess("pin:global", application)
        _isLocked.value = false
        _authError.value = null
        prefs.edit().putString("active_user_id", active.id).apply()
        scope.launch {
            repository.updateLastLogin(active.id)
            repository.recordLocalAudit(
                actorName = active.name,
                actorRole = active.role.name,
                action = "UNLOCK_BIOMETRIC_SUCCESS",
                targetId = active.username,
                details = "Unlocked via BIOMETRIC_STRONG CryptoObject"
            )
            emitSnackbar("${Localization.get("unlocked_biometric_prefix", language.value)}: ${active.name}")
        }
    }

    fun reportBiometricError(message: String) {
        _isLocked.value = true
        _authError.value = message
    }

    fun reportBiometricCancelled() {
        _isLocked.value = true
    }

    fun selectLockedOperator(user: User) {
        _currentUser.value = user
        _isLocked.value = true
        _authError.value = null
    }

    fun lockApp() {
        _isLocked.value = true
        _authError.value = null
    }

    fun logoutUser() {
        val previous = _currentUser.value
        prefs.edit().remove("active_user_id").apply()
        _currentUser.value = null
        _isLocked.value = true
        _authError.value = null
        scope.launch {
            if (previous != null) {
                repository.recordLocalAudit(
                    actorName = previous.name,
                    actorRole = previous.role.name,
                    action = "LOGOUT",
                    targetId = previous.username,
                    details = "Operator logged out"
                )
            }
            emitSnackbar(Localization.get("logged_out_success", language.value))
        }
    }

    fun switchUser(user: User) {
        _currentUser.value = user
        _isLocked.value = true
        _authError.value = null
        scope.launch {
            emitSnackbar("${user.name} — ${Localization.get("selected_operator_pin_prompt", language.value)}")
        }
    }

    fun showRegistrationScreen() {
        _isRegistrationNeeded.value = true
    }

    fun cancelRegistration() {
        _isRegistrationNeeded.value = false
    }

    fun clearAuthError() {
        _authError.value = null
    }
}
