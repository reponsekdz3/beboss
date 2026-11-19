package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.BuildConfig
import com.example.data.model.ShopProfile
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Enterprise Cryptographic Voucher & Admin Unlock Code Engine for BeBoss.
 *
 * Strictly zero-fallback design:
 * - No inline hardcoded HMAC secrets (requires BuildConfig.BEBOSS_UNLOCK_SIGNING_SECRET).
 * - No wildcard bypass hashes (ALL000 / ALL00 / empty username candidates removed).
 * - No offline self-approval of unverified MoMo/SMS reference strings.
 */
object SubscriptionSecurityManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS_PREFIX = "BeBoss_Voucher_HW_"
    private const val HMAC_ALGORITHM = "HmacSHA256"

    private val jvmEphemeralKeys = ConcurrentHashMap<Long, SecretKey>()
    private val secureRandom = SecureRandom()

    private fun getCrossPlatformSigningSecret(): String {
        return CryptoKeyManager.requireConfiguredSecret(
            rawSecret = BuildConfig.BEBOSS_UNLOCK_SIGNING_SECRET,
            secretName = "BEBOSS_UNLOCK_SIGNING_SECRET"
        )
    }

    /**
     * Normalizes phone to last 9 digits so +250788123456 and 0788123456 match identically.
     */
    fun normalizePhoneLast9(phone: String): String {
        return phone.filter { it.isDigit() }.takeLast(9)
    }

    /**
     * Computes deterministic 6-character uppercase SHA-256 binding hash for (username, phone).
     * Mirrors WebCrypto SHA-256 in the Admin Web Dashboard.
     */
    fun computeAccountBindingHash(username: String, phone: String): String {
        val normUser = username.trim().lowercase(Locale.ROOT)
        val normPhone = normalizePhoneLast9(phone)
        val seed = "$normUser:$normPhone"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
        return SecurityUtils.bytesToHex(digest).take(6).uppercase(Locale.ROOT)
    }

    /**
     * Generates a Cross-Platform Account-Bound Unlock Code (BB2 format) matching the Web Admin Platform.
     */
    fun generateAccountBoundUnlockCode(
        username: String,
        registeredPhone: String,
        tierCode: String = "SINGLE",
        days: Int = 30,
        validityWindowDays: Int = 14,
        signingSecret: String = getCrossPlatformSigningSecret()
    ): String {
        require(username.trim().isNotEmpty()) { "Owner username is required for BB2 unlock codes." }
        require(normalizePhoneLast9(registeredPhone).isNotEmpty()) { "Registered phone is required for BB2 unlock codes." }
        val cleanSecret = CryptoKeyManager.requireConfiguredSecret(signingSecret, "BEBOSS_UNLOCK_SIGNING_SECRET")
        val cleanTier = tierCode.trim().uppercase(Locale.ROOT).ifBlank { "SINGLE" }
        val acctHash6 = computeAccountBindingHash(username, registeredPhone)
        val expiryEpochDays = (System.currentTimeMillis() / 86_400_000L) + validityWindowDays
        val expiryHex = expiryEpochDays.toString(16).uppercase(Locale.ROOT)
        val payload = "BB2:$cleanTier:$days:$expiryHex:$acctHash6"
        val key = SecretKeySpec(cleanSecret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM)
        val sig8 = computeHmacSignature(payload, key).take(8).uppercase(Locale.ROOT)
        return "BB2-$cleanTier-$days-$expiryHex-$acctHash6-$sig8"
    }

    /**
     * Obtains a hardware-backed SecretKey from AndroidKeyStore.
     * In host JVM unit tests where AndroidKeyStore does not exist, uses a per-shop CSPRNG key
     * (never derived from predictable public profile fields). Fails closed on Android devices if
     * hardware keystore initialization fails.
     */
    fun getOrCreateIsolatedHmacKey(shopProfile: ShopProfile): SecretKey {
        val alias = "$KEYSTORE_ALIAS_PREFIX${shopProfile.id}"
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(alias)) {
                val existing = keyStore.getKey(alias, null) as? SecretKey
                if (existing != null) return existing
            }
            val keyGen = KeyGenerator.getInstance(HMAC_ALGORITHM, ANDROID_KEYSTORE)
            val keySpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).build()
            keyGen.init(keySpec)
            keyGen.generateKey()
        } catch (t: Throwable) {
            if (!CryptoKeyManager.isRobolectricJvm()) {
                throw SecurityException("Hardware AndroidKeyStore HMAC unavailable.", t)
            }
            jvmEphemeralKeys.getOrPut(shopProfile.id) {
                val raw = ByteArray(32).also { secureRandom.nextBytes(it) }
                SecretKeySpec(raw, HMAC_ALGORITHM)
            }
        }
    }

    /**
     * Computes the unique Shop Device Challenge Code.
     */
    fun computeDeviceChallenge(shopProfile: ShopProfile): String {
        val cleanPhone = shopProfile.phone.filter { it.isDigit() }.takeLast(4).ifBlank { "8801" }
        val shopHash = abs(shopProfile.shopName.trim().lowercase(Locale.ROOT).hashCode() % 900 + 100)
        return "BB-$cleanPhone-$shopHash"
    }

    /**
     * Generates a cryptographically valid device-bound voucher (BB format).
     */
    fun generateCryptographicVoucher(
        shopProfile: ShopProfile,
        tierCode: String,
        days: Int = 30,
        validityWindowDays: Int = 14
    ): String {
        val challenge = computeDeviceChallenge(shopProfile)
        val challengeShortHash = (abs(challenge.hashCode()) % 100000).toString().padStart(5, '0')
        val expiryEpochDays = (System.currentTimeMillis() / (24 * 60 * 60 * 1000L)) + validityWindowDays
        val expiryHex = expiryEpochDays.toString(16).uppercase(Locale.ROOT)

        val rawPayload = "$tierCode:$days:$expiryHex:$challengeShortHash"
        val key = getOrCreateIsolatedHmacKey(shopProfile)
        val signature = computeHmacSignature(rawPayload, key).take(8).uppercase(Locale.ROOT)

        return "BB-$tierCode-$days-$expiryHex-$challengeShortHash-$signature"
    }

    /**
     * Returns true when the pasted or typed text matches a complete cryptographic
     * BB2 account-bound unlock code or BB hardware voucher token so the UI can
     * immediately verify and auto-unlock without extra clicks.
     */
    fun isCompleteUnlockCodeFormat(input: String): Boolean {
        val clean = input.trim().uppercase(Locale.ROOT)
        val parts = clean.split("-")
        if (parts.size != 6) return false
        val prefix = parts[0]
        if (prefix != "BB2" && prefix != "BB") return false
        val tierOk = parts[1].length in 2..12 && parts[1].all { it.isLetter() }
        val daysOk = (parts[2].toIntOrNull() ?: 0) > 0
        val expiryOk = parts[3].length in 3..8 && parts[3].all { it in '0'..'9' || it in 'A'..'F' }
        val bindOk = if (prefix == "BB2") {
            parts[4].length == 6 && parts[4].all { it in '0'..'9' || it in 'A'..'F' }
        } else {
            parts[4].length == 5 && parts[4].all { it.isDigit() }
        }
        val sigOk = parts[5].length == 8 && parts[5].all { it in '0'..'9' || it in 'A'..'F' }
        return tierOk && daysOk && expiryOk && bindOk && sigOk
    }

    /**
     * Verifies an Admin Unlock Code (BB2) or hardware-bound voucher (BB).
     * Rejects unverified payment references and wildcard hashes.
     */
    fun verifyVoucherToken(
        voucherInput: String,
        shopProfile: ShopProfile,
        branchCount: Int = 1,
        workerCount: Int = 1,
        alreadyRedeemedHashes: Set<String> = emptySet(),
        ownerUsername: String = "",
        ownerPhoneOverride: String = "",
        signingSecretOverride: String? = null
    ): VoucherValidationResult {
        val cleanCode = voucherInput.trim().uppercase(Locale.ROOT)
        if (cleanCode.isBlank()) {
            return VoucherValidationResult(false, 0, "Please enter an unlock code", "")
        }

        // Anti-Replay Attack Check
        val voucherFingerprint = computeVoucherFingerprint(cleanCode)
        if (alreadyRedeemedHashes.contains(voucherFingerprint)) {
            return VoucherValidationResult(
                isValid = false,
                daysToAdd = 0,
                message = "This unlock code has already been redeemed and cannot be reused (Anti-Replay Protection).",
                planName = ""
            )
        }

        val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(branchCount, workerCount, 1)
        val parts = cleanCode.split("-")

        // 1. Cross-Platform Admin-Issued Unlock Code: BB2-[TIER]-[DAYS]-[EXPIRY_HEX]-[ACCT_HASH6]-[HMAC_SIG8]
        if (parts.size == 6 && parts[0] == "BB2") {
            val tier = parts[1]
            val days = parts[2].toIntOrNull() ?: 0
            val expiryHex = parts[3]
            val receivedAcctHash = parts[4]
            val receivedSignature = parts[5]

            val expiryEpochDays = expiryHex.toLongOrNull(16)
            val currentEpochDays = ClockGuard.effectiveNowMs() / 86_400_000L
            if (expiryEpochDays == null || currentEpochDays > expiryEpochDays) {
                return VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = "This admin unlock code has expired. Please request a fresh code from BeBoss Admin.",
                    planName = ""
                )
            }

            val candidatePhones = listOf(shopProfile.phone, ownerPhoneOverride)
                .map { it.trim() }
                .filter { normalizePhoneLast9(it).isNotEmpty() }
                .distinct()
            val candidateUsernames = listOf(ownerUsername.trim())
                .filter { it.isNotEmpty() }
                .distinct()

            val accountMatches = candidateUsernames.isNotEmpty() &&
                candidatePhones.isNotEmpty() &&
                candidateUsernames.any { u ->
                    candidatePhones.any { p ->
                        computeAccountBindingHash(u, p) == receivedAcctHash
                    }
                }

            if (!accountMatches) {
                return VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = "Account mismatch: this unlock code is bound to a different username/phone ($receivedAcctHash).",
                    planName = ""
                )
            }

            val signingSecret = try {
                if (signingSecretOverride != null) {
                    CryptoKeyManager.requireConfiguredSecret(signingSecretOverride, "BEBOSS_UNLOCK_SIGNING_SECRET")
                } else {
                    getCrossPlatformSigningSecret()
                }
            } catch (e: SecurityException) {
                return VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = e.message ?: "Unlock verification secret not configured.",
                    planName = ""
                )
            }

            val payload = "BB2:$tier:$days:$expiryHex:$receivedAcctHash"
            val key = SecretKeySpec(signingSecret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM)
            val expectedSig = computeHmacSignature(payload, key).take(8).uppercase(Locale.ROOT)

            return if (slowEquals(receivedSignature, expectedSig)) {
                val (planName, verifiedAmt) = mapTierNameAndAmount(tier, branchCount, pricing.totalPayable)
                VoucherValidationResult(
                    isValid = true,
                    daysToAdd = days.coerceAtLeast(1),
                    message = "Admin Unlock Code Verified! Subscription extended by +$days Days.",
                    planName = planName,
                    verifiedAmount = verifiedAmt
                )
            } else {
                VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = "Unlock code cryptographic signature verification failed (Invalid or Tampered Code).",
                    planName = ""
                )
            }
        }

        // 2. Device-Bound Hardware Voucher: BB-[TIER]-[DAYS]-[EXPIRY_HEX]-[CHALLENGE_HASH]-[SIGNATURE]
        if (parts.size == 6 && parts[0] == "BB") {
            val tier = parts[1]
            val days = parts[2].toIntOrNull() ?: 0
            val expiryHex = parts[3]
            val challengeHash = parts[4]
            val receivedSignature = parts[5]

            val expiryEpochDays = expiryHex.toLongOrNull(16)
            val currentEpochDays = ClockGuard.effectiveNowMs() / (24 * 60 * 60 * 1000L)
            if (expiryEpochDays == null || currentEpochDays > expiryEpochDays) {
                return VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = "This voucher has expired or has an invalid expiry header.",
                    planName = ""
                )
            }

            val myChallenge = computeDeviceChallenge(shopProfile)
            val expectedChallengeHash = (abs(myChallenge.hashCode()) % 100000).toString().padStart(5, '0')
            if (challengeHash != expectedChallengeHash) {
                return VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = "This voucher was issued for a different shop device ($challengeHash).",
                    planName = ""
                )
            }

            val payload = "$tier:$days:$expiryHex:$challengeHash"
            val key = try {
                getOrCreateIsolatedHmacKey(shopProfile)
            } catch (e: SecurityException) {
                return VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = e.message ?: "Hardware voucher keystore unavailable.",
                    planName = ""
                )
            }
            val expectedSig = computeHmacSignature(payload, key).take(8).uppercase(Locale.ROOT)

            return if (slowEquals(receivedSignature, expectedSig)) {
                val (planName, verifiedAmt) = mapTierNameAndAmount(tier, branchCount, pricing.totalPayable)
                VoucherValidationResult(
                    isValid = true,
                    daysToAdd = days.coerceAtLeast(1),
                    message = "Cryptographic Voucher Verified & Authenticated! (+$days Days)",
                    planName = planName,
                    verifiedAmount = verifiedAmt
                )
            } else {
                VoucherValidationResult(
                    isValid = false,
                    daysToAdd = 0,
                    message = "Voucher signature verification failed (Tampered or Invalid Code).",
                    planName = ""
                )
            }
        }

        return VoucherValidationResult(
            isValid = false,
            daysToAdd = 0,
            message = "Unrecognized unlock code. Submit your MoMo transaction reference for admin verification to receive a signed BB2-... code.",
            planName = ""
        )
    }

    private fun mapTierNameAndAmount(tier: String, branchCount: Int, calculatedMonthly: Int): Pair<String, Int> {
        return when (tier) {
            "ANNUAL" -> Pair("Annual VIP Merchant Plan", calculatedMonthly * 10)
            "MULTI" -> Pair("Enterprise Multi-Branch ($branchCount Branches)", 20000)
            "DUAL" -> Pair("Dual Branch Plan", 10000)
            else -> Pair("Single Store Standard Plan", 5000)
        }
    }

    fun computeVoucherFingerprint(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(code.trim().uppercase(Locale.ROOT).toByteArray(StandardCharsets.UTF_8))
        return SecurityUtils.bytesToHex(hash)
    }

    private fun computeHmacSignature(data: String, key: SecretKey): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        val hmacBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return SecurityUtils.bytesToHex(hmacBytes)
    }

    fun generateSecureTokenNonce(byteCount: Int = 8): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return SecurityUtils.bytesToHex(bytes).uppercase(Locale.ROOT)
    }

    private fun slowEquals(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        return MessageDigest.isEqual(
            a.uppercase(Locale.ROOT).toByteArray(StandardCharsets.UTF_8),
            b.uppercase(Locale.ROOT).toByteArray(StandardCharsets.UTF_8)
        )
    }
}
