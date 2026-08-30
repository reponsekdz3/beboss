package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopProfile(
    @PrimaryKey val id: Long = 1L,
    val name: String = "Shop Owner",
    val phone: String = "+250 788 000 000",
    val email: String = "shop@beboss.rw",
    val shopName: String = "BeBoss Market & Store",
    val address: String = "Kigali, Rwanda",
    val currencyCode: String = "RWF",
    val currencySymbol: String = "FRw",
    val taxRate: Double = 0.0,
    val tinNumber: String = "",
    val receiptFooter: String = "Murakoze cyane! Thank you for your business!",
    val receiptHeader: String = "BEBOSS SMART RETAIL",
    val dailySalesTarget: Double = 150000.0,
    val monthlyRevenueTarget: Double = 4500000.0,
    val targetMarginPercent: Double = 25.0,
    val cashRegisterFloat: Double = 20000.0,
    val receiptPaperWidthMm: Int = 58,
    val showLogoOnReceipt: Boolean = true,
    val showTaxOnReceipt: Boolean = true,
    val showBarcodeOnReceipt: Boolean = true,
    val autoPrintReceiptOnSale: Boolean = false,
    val soundFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val lowStockThresholdDefault: Double = 5.0,
    val isOnlineSyncEnabled: Boolean = true,
    val backendServerUrl: String = "https://api.beboss.app/v1",
    val lastSyncedAt: Long = 0L,
    val subscriptionStatus: String = "ACTIVE", // ACTIVE, TRIAL, EXPIRED
    val monthlyFeeRwf: Int = 5000,
    val subscriptionExpiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // Default 30 days
    val trialStartedAt: Long = System.currentTimeMillis(),
    val lastPaymentRef: String = "INIT-TRIAL-30D",
    val lastPaymentAmount: Int = 5000,
    val lastPaymentDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val ownerName: String
        get() = name

    val isSubscriptionActive: Boolean
        get() = subscriptionStatus != "EXPIRED" && System.currentTimeMillis() <= subscriptionExpiresAt

    val daysRemaining: Int
        get() {
            val diff = subscriptionExpiresAt - System.currentTimeMillis()
            return if (diff > 0) ((diff / (24 * 60 * 60 * 1000)).toInt() + 1) else 0
        }

    val isGracePeriod: Boolean
        get() = !isSubscriptionActive && (System.currentTimeMillis() - subscriptionExpiresAt < (3L * 24 * 60 * 60 * 1000))
}
