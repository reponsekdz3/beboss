package com.example.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object SecurityUtils {

    private const val SALT = "BeBoss_ShopSecure_Salt_2026"

    /**
     * Hashes a 4-digit PIN using SHA-256 with a secure application salt.
     */
    fun hashPin(pin: String): String {
        return hashWithSalt(pin.trim(), SALT)
    }

    /**
     * Hashes a user password using SHA-256 with salt.
     */
    fun hashPassword(password: String): String {
        return hashWithSalt(password.trim(), SALT)
    }

    /**
     * Verifies an entered PIN against a stored hash or legacy plain text.
     * If legacy plain text was stored, it returns true and signals compatibility.
     */
    fun verifyPin(enteredPin: String, storedHashOrPlain: String): Boolean {
        if (storedHashOrPlain.isBlank()) return false
        val trimmed = enteredPin.trim()
        val hashed = hashPin(trimmed)

        // Check against secure hash
        if (hashed.equals(storedHashOrPlain, ignoreCase = true)) return true

        // Check against plain text (legacy upgrade compatibility)
        if (storedHashOrPlain == trimmed) return true

        return false
    }

    /**
     * Verifies an entered password against a stored hash or legacy plain text.
     */
    fun verifyPassword(enteredPass: String, storedHashOrPlain: String): Boolean {
        if (storedHashOrPlain.isBlank()) return false
        val trimmed = enteredPass.trim()
        val hashed = hashPassword(trimmed)

        if (hashed.equals(storedHashOrPlain, ignoreCase = true)) return true
        if (storedHashOrPlain == trimmed) return true

        return false
    }

    private fun hashWithSalt(input: String, salt: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val combined = "$salt:$input"
            val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            // Fallback
            input.hashCode().toString()
        }
    }
}
