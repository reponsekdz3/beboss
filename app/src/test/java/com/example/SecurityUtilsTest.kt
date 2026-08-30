package com.example

import com.example.util.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun testPbkdf2PinHashingAndVerification() {
        val pin = "4829"
        val hash1 = SecurityUtils.hashPin(pin)
        val hash2 = SecurityUtils.hashPin(pin)

        // PBKDF2 must use unique random salts for each hash
        assertNotEquals(hash1, hash2)
        assertTrue(hash1.startsWith("pbkdf2:"))
        assertTrue(hash2.startsWith("pbkdf2:"))

        // Verification must succeed for the right PIN
        assertTrue(SecurityUtils.verifyPin(pin, hash1))
        assertTrue(SecurityUtils.verifyPin(pin, hash2))

        // Verification must fail for incorrect PIN
        assertFalse(SecurityUtils.verifyPin("9999", hash1))
        assertFalse(SecurityUtils.verifyPin("1234", hash2))
    }

    @Test
    fun testPasswordHashingAndVerification() {
        val pass = "SuperAdmin2026!#"
        val hash = SecurityUtils.hashPassword(pass)

        assertTrue(hash.startsWith("pbkdf2:"))
        assertTrue(SecurityUtils.verifyPassword(pass, hash))
        assertFalse(SecurityUtils.verifyPassword("WrongPassword", hash))
    }

    @Test
    fun testNeedsUpgradeFlag() {
        val modernHash = SecurityUtils.hashPin("1234")
        val legacySha256 = "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b"
        val plainText = "1234"

        assertFalse(SecurityUtils.needsUpgrade(modernHash))
        assertTrue(SecurityUtils.needsUpgrade(legacySha256))
        assertTrue(SecurityUtils.needsUpgrade(plainText))
    }

    @Test
    fun testPinComplexityValidation() {
        // Trivial repeated digits
        assertFalse(SecurityUtils.validatePinStrength("0000").isValid)
        assertFalse(SecurityUtils.validatePinStrength("1111").isValid)

        // Simple sequential digits
        assertFalse(SecurityUtils.validatePinStrength("1234").isValid)
        assertFalse(SecurityUtils.validatePinStrength("9876").isValid)

        // Too short / Too long / Non-numeric
        assertFalse(SecurityUtils.validatePinStrength("12").isValid)
        assertFalse(SecurityUtils.validatePinStrength("1234567").isValid)
        assertFalse(SecurityUtils.validatePinStrength("abcd").isValid)

        // Strong non-sequential PINs
        assertTrue(SecurityUtils.validatePinStrength("4829").isValid)
        assertTrue(SecurityUtils.validatePinStrength("917382").isValid)
    }

    @Test
    fun testPasswordComplexityValidation() {
        assertFalse(SecurityUtils.validatePasswordStrength("abc").isValid)
        assertFalse(SecurityUtils.validatePasswordStrength("abcdef").isValid)
        assertFalse(SecurityUtils.validatePasswordStrength("123456").isValid)

        assertTrue(SecurityUtils.validatePasswordStrength("Admin2026!").isValid)
    }
}
