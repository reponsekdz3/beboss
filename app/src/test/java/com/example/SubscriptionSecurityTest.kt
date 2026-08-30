package com.example

import com.example.data.model.ShopProfile
import com.example.util.SubscriptionSecurityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionSecurityTest {

    private val sampleShop = ShopProfile(
        id = 1,
        name = "Jean Paul",
        shopName = "BeBoss Kigali Store",
        phone = "+250 788 123 456"
    )

    @Test
    fun testValidCryptographicVoucherVerification() {
        val voucher = SubscriptionSecurityManager.generateCryptographicVoucher(
            shopProfile = sampleShop,
            tierCode = "SINGLE",
            days = 30,
            validityWindowDays = 7
        )

        val result = SubscriptionSecurityManager.verifyVoucherToken(
            voucherInput = voucher,
            shopProfile = sampleShop,
            branchCount = 1,
            workerCount = 1
        )

        assertTrue(result.isValid)
        assertTrue(result.daysToAdd >= 30)
    }

    @Test
    fun testTamperedVoucherSignatureRejected() {
        val validVoucher = SubscriptionSecurityManager.generateCryptographicVoucher(
            shopProfile = sampleShop,
            tierCode = "SINGLE",
            days = 30,
            validityWindowDays = 7
        )

        // Modify signature character
        val tampered = validVoucher.dropLast(2) + "99"

        val result = SubscriptionSecurityManager.verifyVoucherToken(
            voucherInput = tampered,
            shopProfile = sampleShop,
            branchCount = 1,
            workerCount = 1
        )

        assertFalse(result.isValid)
    }

    @Test
    fun testAntiReplayRejection() {
        val voucher = SubscriptionSecurityManager.generateCryptographicVoucher(
            shopProfile = sampleShop,
            tierCode = "SINGLE",
            days = 30,
            validityWindowDays = 7
        )

        val fingerprint = SubscriptionSecurityManager.computeVoucherFingerprint(voucher)
        val redeemedSet = setOf(fingerprint)

        // Second redemption attempt with same token must be blocked
        val result = SubscriptionSecurityManager.verifyVoucherToken(
            voucherInput = voucher,
            shopProfile = sampleShop,
            branchCount = 1,
            workerCount = 1,
            alreadyRedeemedHashes = redeemedSet
        )

        assertFalse(result.isValid)
        assertTrue(result.message.contains("Anti-Replay"))
    }

    @Test
    fun testDeviceChallengeBinding() {
        val otherShop = ShopProfile(
            id = 2,
            name = "Aline",
            shopName = "Nyabugogo Branch",
            phone = "+250 788 999 888"
        )

        // Generate voucher specifically for sampleShop
        val voucherForSample = SubscriptionSecurityManager.generateCryptographicVoucher(
            shopProfile = sampleShop,
            tierCode = "SINGLE",
            days = 30
        )

        // Attempt to apply to otherShop
        val result = SubscriptionSecurityManager.verifyVoucherToken(
            voucherInput = voucherForSample,
            shopProfile = otherShop,
            branchCount = 1,
            workerCount = 1
        )

        assertFalse(result.isValid)
    }
}
