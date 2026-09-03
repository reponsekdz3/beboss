package com.example.domain

import com.example.data.dao.RedeemedVoucherDao
import com.example.data.dao.ShopProfileDao
import com.example.data.model.RedeemedVoucher
import com.example.data.model.ShopProfile
import com.example.util.OfflineSubscriptionManager
import com.example.util.PaymentProcessingResult
import com.example.util.SubscriptionSecurityManager
import com.example.util.VoucherValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Domain Manager handling Subscriptions, Cryptographic Voucher Validation,
 * Anti-Replay Ledger, and Mobile Money Payment Processing.
 */
class SubscriptionManager(
    private val shopProfileDao: ShopProfileDao,
    private val redeemedVoucherDao: RedeemedVoucherDao
) {
    val redeemedVouchers: Flow<List<RedeemedVoucher>> = redeemedVoucherDao.getAllRedeemedVouchers()

    /**
     * Validates and redeems an activation voucher securely.
     * Prevents replay attacks by checking and recording voucher fingerprints.
     */
    suspend fun redeemVoucher(
        voucherCode: String,
        shopProfile: ShopProfile,
        branchCount: Int,
        workerCount: Int
    ): VoucherValidationResult = withContext(Dispatchers.IO) {
        val redeemedHashes = redeemedVoucherDao.getAllRedeemedHashes().toSet()

        val validation = SubscriptionSecurityManager.verifyVoucherToken(
            voucherInput = voucherCode,
            shopProfile = shopProfile,
            branchCount = branchCount,
            workerCount = workerCount,
            alreadyRedeemedHashes = redeemedHashes
        )

        if (validation.isValid) {
            val voucherFingerprint = SubscriptionSecurityManager.computeVoucherFingerprint(voucherCode)
            val redeemedRecord = RedeemedVoucher(
                voucherCode = voucherCode.trim().uppercase(),
                voucherHash = voucherFingerprint,
                planName = validation.planName,
                daysAdded = validation.daysToAdd,
                verifiedAmount = validation.verifiedAmount,
                redeemedAt = System.currentTimeMillis()
            )
            redeemedVoucherDao.insertRedeemedVoucher(redeemedRecord)

            // Extend shop profile subscription
            val currentExpiry = shopProfile.subscriptionExpiresAt
            val now = System.currentTimeMillis()
            val baseTime = if (currentExpiry > now) currentExpiry else now
            val newExpiry = baseTime + (validation.daysToAdd.toLong() * 24 * 60 * 60 * 1000L)

            val updatedProfile = shopProfile.copy(
                subscriptionStatus = "ACTIVE",
                subscriptionExpiresAt = newExpiry,
                lastPaymentRef = voucherCode.trim().uppercase(),
                lastPaymentAmount = validation.verifiedAmount,
                lastPaymentDate = now,
                updatedAt = now
            )
            shopProfileDao.insertOrUpdateProfile(updatedProfile)
        }

        validation
    }

    /**
     * Records and activates direct mobile money payment.
     */
    suspend fun recordDirectMoMoPayment(
        shopProfile: ShopProfile,
        payerPhone: String,
        provider: String,
        branchCount: Int,
        workerCount: Int,
        durationMonths: Int,
        smsTransactionReference: String? = null,
        alreadyUsedRefs: Set<String> = emptySet()
    ): PaymentProcessingResult = withContext(Dispatchers.IO) {
        val result = OfflineSubscriptionManager.processDirectMoMoPayment(
            shopProfile = shopProfile,
            payerPhone = payerPhone,
            provider = provider,
            branchCount = branchCount,
            workerCount = workerCount,
            durationMonths = durationMonths,
            smsTransactionReference = smsTransactionReference,
            alreadyUsedRefs = alreadyUsedRefs
        )

        if (result.isSuccess) {
            val currentExpiry = shopProfile.subscriptionExpiresAt
            val now = System.currentTimeMillis()
            val baseTime = if (currentExpiry > now) currentExpiry else now
            val newExpiry = baseTime + (result.planDays.toLong() * 24 * 60 * 60 * 1000L)

            val updatedProfile = shopProfile.copy(
                subscriptionStatus = "ACTIVE",
                subscriptionExpiresAt = newExpiry,
                lastPaymentRef = result.transactionRef,
                lastPaymentAmount = result.amountPaid,
                lastPaymentDate = now,
                updatedAt = now
            )
            shopProfileDao.insertOrUpdateProfile(updatedProfile)
        }

        result
    }
}
