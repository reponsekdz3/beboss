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
    val receiptFooter: String = "Murakoze cyane! Thank you for your business!",
    val isOnlineSyncEnabled: Boolean = true,
    val backendServerUrl: String = "https://api.beboss.app/v1",
    val lastSyncedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val ownerName: String
        get() = name
}
