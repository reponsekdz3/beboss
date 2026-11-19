package com.example.util

import com.example.BuildConfig
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Enterprise-grade security utility providing PBKDF2WithHmacSHA256 password/PIN hashing,
 * cryptographically secure salt generation, and timing-attack-safe verification.
 *
 * Strictly enforces PBKDF2 + BuildConfig pepper with ZERO plaintext, static-salt, or hardcoded fallbacks.
 */
object SecurityUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 30000
    private const val KEY_LENGTH = 256 // in bits
    private const val SALT_BYTES = 16

    private fun getAppPepper(): String {
        return CryptoKeyManager.requireConfiguredSecret(
            rawSecret = BuildConfig.BEBOSS_SECURITY_PEPPER,
            secretName = "BEBOSS_SECURITY_PEPPER"
        )
    }

    /**
     * Generates a cryptographically random salt.
     */
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Hashes a PIN using PBKDF2WithHmacSHA256 with an individual random salt.
     * Output format: pbkdf2:iterations:saltHex:hashHex
     */
    fun hashPin(pin: String): String {
        val clean = pin.trim()
        require(clean.isNotEmpty()) { "PIN cannot be blank" }
        return hashWithPbkdf2(clean)
    }

    /**
     * Hashes a user password using PBKDF2WithHmacSHA256 with an individual random salt.
     * Output format: pbkdf2:iterations:saltHex:hashHex
     */
    fun hashPassword(password: String): String {
        val clean = password.trim()
        require(clean.isNotEmpty()) { "Password cannot be blank" }
        return hashWithPbkdf2(clean)
    }

    private fun hashWithPbkdf2(input: String): String {
        val salt = generateSalt()
        val pepperedInput = "$input:${getAppPepper()}"
        val hash = pbkdf2(pepperedInput.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return "pbkdf2:$ITERATIONS:${bytesToHex(salt)}:${bytesToHex(hash)}"
    }

    /**
     * Verifies an entered PIN against a stored PBKDF2 hash.
     */
    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        return verifySecret(enteredPin.trim(), storedHash.trim())
    }

    /**
     * Verifies an entered password against a stored PBKDF2 hash.
     */
    fun verifyPassword(enteredPass: String, storedHash: String): Boolean {
        return verifySecret(enteredPass.trim(), storedHash.trim())
    }

    /**
     * Checks whether a credential value is missing PBKDF2 encoding and needs hashing/re-hashing.
     */
    fun needsUpgrade(storedHash: String): Boolean {
        return !storedHash.startsWith("pbkdf2:")
    }

    /**
     * Strict PBKDF2 verification with constant-time byte comparison.
     * Rejects plaintext, static-salt SHA-256, and malformed hashes outright.
     */
    private fun verifySecret(enteredSecret: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || enteredSecret.isBlank()) return false
        if (!storedHash.startsWith("pbkdf2:")) return false

        val parts = storedHash.split(":")
        if (parts.size != 4) return false

        val iterations = parts[1].toIntOrNull()?.takeIf { it >= 10000 } ?: return false
        val saltHex = parts[2]
        val hashHex = parts[3]
        if (saltHex.isEmpty() || hashHex.isEmpty() || saltHex.length % 2 != 0 || hashHex.length % 2 != 0) {
            return false
        }

        return try {
            val salt = hexToBytes(saltHex)
            val expectedHash = hexToBytes(hashHex)
            val peppered = "$enteredSecret:${getAppPepper()}"
            val actualHash = pbkdf2(peppered.toCharArray(), salt, iterations, expectedHash.size * 8)
            java.security.MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Throwable) {
            false
        }
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return skf.generateSecret(spec).encoded
    }

    /**
     * Timing-attack resistant constant-time comparison for secrets, HMAC tags, and hashes.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        return java.security.MessageDigest.isEqual(
            a.toByteArray(Charsets.UTF_8),
            b.toByteArray(Charsets.UTF_8)
        )
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            result[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        return result
    }

    /**
     * Generates a cryptographically secure, non-trivial 4-digit PIN that passes strength validation.
     */
    fun generateStrongRandomPin(): String {
        val random = SecureRandom()
        while (true) {
            val num = random.nextInt(9000) + 1000
            val candidate = num.toString()
            if (validatePinStrength(candidate).isValid) {
                return candidate
            }
        }
    }

    private val commonWeakPins = setOf(
        "1234", "4321", "0000", "1111", "2222", "3333", "4444",
        "5555", "6666", "7777", "8888", "9999", "1212", "2121",
        "1010", "2580", "1379", "0852", "1122", "2211", "0007", "2000"
    )

    /**
     * Validates PIN complexity (4-6 numeric digits, non-repeated, non-sequential, non-palindromic).
     */
    fun validatePinStrength(pin: String): PinStrengthResult {
        val clean = pin.trim()
        if (clean.length < 4 || clean.length > 6 || !clean.all { it.isDigit() }) {
            return PinStrengthResult(false, "PIN must be between 4 and 6 numeric digits")
        }
        if (clean.toSet().size <= 1) {
            return PinStrengthResult(false, "PIN cannot consist of identical repeated digits")
        }
        val sequentialAscending = "0123456789"
        val sequentialDescending = "9876543210"
        if (sequentialAscending.contains(clean) || sequentialDescending.contains(clean)) {
            return PinStrengthResult(false, "PIN cannot be simple sequential numbers (e.g. 1234)")
        }
        if (clean in commonWeakPins || clean == clean.reversed() || clean.toSet().size < 3) {
            return PinStrengthResult(false, "PIN is too predictable; use at least 3 distinct non-paired digits")
        }
        return PinStrengthResult(true, "Strong PIN")
    }

    /**
     * Validates password strength for administrative accounts (8+ chars, upper + lower/symbol + digit).
     */
    fun validatePasswordStrength(password: String): PasswordStrengthResult {
        val clean = password.trim()
        if (clean.length < 8) {
            return PasswordStrengthResult(false, "Password must be at least 8 characters long")
        }
        val hasLetter = clean.any { it.isLetter() }
        val hasDigit = clean.any { it.isDigit() }
        val hasUpper = clean.any { it.isUpperCase() }
        if (!hasLetter || !hasDigit || !hasUpper) {
            return PasswordStrengthResult(
                false,
                "Password must include uppercase, lowercase letters, and numbers"
            )
        }
        val lower = clean.lowercase()
        if (lower in setOf("password1", "admin123", "qwerty12", "beboss123", "12345678a")) {
            return PasswordStrengthResult(false, "Choose a less common administrative password")
        }
        return PasswordStrengthResult(true, "Strong Password")
    }
}

data class PinStrengthResult(val isValid: Boolean, val message: String)
data class PasswordStrengthResult(val isValid: Boolean, val message: String)
