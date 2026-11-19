package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.ClockGuard

@Entity(tableName = "shop_profile")
data class ShopProfile(
    @PrimaryKey val id: Long = 1L,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val shopName: String = "",
    val address: String = "",
    val currencyCode: String = "RWF",
    val currencySymbol: String = "FRw",
    val taxRate: Double = 0.0,
    val tinNumber: String = "",
    val receiptFooter: String = "Thank you for your business!",
    val receiptHeader: String = "",
    val dailySalesTarget: Double = 0.0,
    val monthlyRevenueTarget: Double = 0.0,
    val targetMarginPercent: Double = 25.0,
    val cashRegisterFloat: Double = 0.0,
    val receiptPaperWidthMm: Int = 58,
    val showLogoOnReceipt: Boolean = true,
    val showTaxOnReceipt: Boolean = true,
    val showBarcodeOnReceipt: Boolean = true,
    val autoPrintReceiptOnSale: Boolean = false,
    val soundFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val lowStockThresholdDefault: Double = 5.0,
    val isOnlineSyncEnabled: Boolean = true,
    val backendServerUrl: String = "",
    val lastSyncedAt: Long = 0L,
    val subscriptionStatus: String = STATUS_TRIAL, // TRIAL, ACTIVE, EXPIRED, SUSPENDED
    val monthlyFeeRwf: Int = 5000,
    val subscriptionExpiresAt: Long = System.currentTimeMillis() + FREE_TRIAL_DURATION_MS,
    val trialStartedAt: Long = System.currentTimeMillis(),
    val lastPaymentRef: String = "FREE-TRIAL-4D",
    val lastPaymentAmount: Int = 0,
    val lastPaymentDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 1L,
    val hlcTimestamp: Long = System.currentTimeMillis(),
    val lastModifiedDeviceId: String = ""
) {
    companion object {
        const val FREE_TRIAL_DAYS: Int = 4
        const val FREE_TRIAL_DURATION_MS: Long = FREE_TRIAL_DAYS * 24L * 60L * 60L * 1000L
        const val STATUS_TRIAL: String = "TRIAL"
        const val STATUS_ACTIVE: String = "ACTIVE"
        const val STATUS_EXPIRED: String = "EXPIRED"
        const val STATUS_SUSPENDED: String = "SUSPENDED"
    }

    val ownerName: String
        get() = name

    val isSuspended: Boolean
        get() = subscriptionStatus.equals(STATUS_SUSPENDED, ignoreCase = true)

    val isFreeTrial: Boolean
        get() = subscriptionStatus.equals(STATUS_TRIAL, ignoreCase = true) &&
            ClockGuard.effectiveNowMs() < subscriptionExpiresAt

    val isPaidActive: Boolean
        get() = subscriptionStatus.equals(STATUS_ACTIVE, ignoreCase = true) &&
            ClockGuard.effectiveNowMs() < subscriptionExpiresAt

    /**
     * Strict 4-day free trial & paid license gate.
     * Enforced via ClockGuard anti-rollback monotonic clock; blocks access immediately
     * when 4-day trial or paid cycle expires or when remotely locked/suspended by Master Admin.
     */
    val isSubscriptionActive: Boolean
        get() {
            if (isSuspended || subscriptionStatus.equals(STATUS_EXPIRED, ignoreCase = true)) {
                return false
            }
            return ClockGuard.effectiveNowMs() < subscriptionExpiresAt
        }

    /**
     * Disabled: zero post-expiry grace bypass — app locks immediately after 4-day trial ends.
     */
    val isGracePeriod: Boolean
        get() = false

    val millisRemaining: Long
        get() = (subscriptionExpiresAt - ClockGuard.effectiveNowMs()).coerceAtLeast(0L)

    val hoursRemaining: Int
        get() = (millisRemaining / (60L * 60L * 1000L)).toInt()

    val daysRemaining: Int
        get() {
            val diff = millisRemaining
            return if (diff <= 0L) 0 else ((diff + 86_399_999L) / 86_400_000L).toInt()
        }

    val trialProgressFraction: Float
        get() = if (!isFreeTrial) 1f else (millisRemaining.toFloat() / FREE_TRIAL_DURATION_MS.toFloat()).coerceIn(0f, 1f)
}
