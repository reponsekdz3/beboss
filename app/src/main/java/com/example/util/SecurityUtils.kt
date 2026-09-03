package com.example.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Enterprise-grade security utility providing PBKDF2WithHmacSHA256 password/PIN hashing,
 * cryptographically secure salt generation, timing-attack-safe comparisons,
 * and transparent automated upgrade from legacy hashes.
 */
object SecurityUtils {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256 // in bits
    private const val SALT_BYTES = 16

    // Secure application pepper for multi-layered defense
    private const val APP_PEPPER = "BeBoss_ShopEnterprise_2026_SecurePepper#RW"

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
        return hashWithPbkdf2(pin.trim())
    }

    /**
     * Hashes a user password using PBKDF2WithHmacSHA256 with an individual random salt.
     * Output format: pbkdf2:iterations:saltHex:hashHex
     */
    fun hashPassword(password: String): String {
        return hashWithPbkdf2(password.trim())
    }

    /**
     * Computes PBKDF2 hash for a given plaintext input and generates a unique random salt.
     */
    private fun hashWithPbkdf2(input: String): String {
        val salt = generateSalt()
        val pepperedInput = "$input:$APP_PEPPER"
        val hash = pbkdf2(pepperedInput.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return "pbkdf2:$ITERATIONS:${bytesToHex(salt)}:${bytesToHex(hash)}"
    }

    /**
     * Verifies an entered PIN against a stored PBKDF2 hash or legacy hash.
     */
    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        return verifySecret(enteredPin.trim(), storedHash.trim())
    }

    /**
     * Verifies an entered password against a stored PBKDF2 hash or legacy hash.
     */
    fun verifyPassword(enteredPass: String, storedHash: String): Boolean {
        return verifySecret(enteredPass.trim(), storedHash.trim())
    }

    /**
     * Checks if a stored hash is using legacy format and needs upgrading to PBKDF2.
     */
    fun needsUpgrade(storedHash: String): Boolean {
        return !storedHash.startsWith("pbkdf2:")
    }

    /**
     * Core verification logic with timing-safe comparison and backward compatibility for legacy hashes.
     */
    private fun verifySecret(enteredSecret: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || enteredSecret.isBlank()) return false

        // 1. Check modern PBKDF2 format
        if (storedHash.startsWith("pbkdf2:")) {
            val parts = storedHash.split(":")
            if (parts.size == 4) {
                val iterations = parts[1].toIntOrNull() ?: ITERATIONS
                val salt = hexToBytes(parts[2])
                val expectedHash = hexToBytes(parts[3])
                val peppered = "$enteredSecret:$APP_PEPPER"
                val actualHash = pbkdf2(peppered.toCharArray(), salt, iterations, expectedHash.size * 8)
                return slowEquals(expectedHash, actualHash)
            }
        }

        // 2. Backward compatibility with legacy SHA-256 with static salt
        val legacySalt = "BeBoss_ShopSecure_Salt_2026"
        val legacyHashed = hashWithLegacySha256(enteredSecret, legacySalt)
        if (legacyHashed.equals(storedHash, ignoreCase = true)) {
            return true
        }

        // 3. Fallback for initial unhashed setup if legacy plain text existed
        if (storedHash == enteredSecret) {
            return true
        }

        return false
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return skf.generateSecret(spec).encoded
    }

    /**
     * Timing-attack resistant byte comparison.
     */
    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }

    private fun hashWithLegacySha256(input: String, salt: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val combined = "$salt:$input"
            val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            input.hashCode().toString()
        }
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

    /**
     * Validates PIN complexity (must be 4-6 digits, not trivially repeated like 0000 or sequential 1234).
     */
    fun validatePinStrength(pin: String): PinStrengthResult {
        val clean = pin.trim()
        if (clean.length < 4 || clean.length > 6 || !clean.all { it.isDigit() }) {
            return PinStrengthResult(false, "PIN must be between 4 and 6 numeric digits")
        }
        if (clean.toSet().size == 1) {
            return PinStrengthResult(false, "PIN cannot consist of identical repeated digits")
        }
        val sequentialAscending = "0123456789"
        val sequentialDescending = "9876543210"
        if (sequentialAscending.contains(clean) || sequentialDescending.contains(clean)) {
            return PinStrengthResult(false, "PIN cannot be simple sequential numbers (e.g. 1234)")
        }
        return PinStrengthResult(true, "Strong PIN")
    }

    /**
     * Validates password strength for administrative accounts.
     */
    fun validatePasswordStrength(password: String): PasswordStrengthResult {
        val clean = password.trim()
        if (clean.length < 6) {
            return PasswordStrengthResult(false, "Password must be at least 6 characters long")
        }
        val hasLetter = clean.any { it.isLetter() }
        val hasDigit = clean.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return PasswordStrengthResult(false, "Password should contain both letters and numbers")
        }
        return PasswordStrengthResult(true, "Strong Password")
    }
}

data class PinStrengthResult(val isValid: Boolean, val message: String)
data class PasswordStrengthResult(val isValid: Boolean, val message: String)
