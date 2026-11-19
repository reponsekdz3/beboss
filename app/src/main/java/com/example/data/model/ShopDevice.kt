package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a hardware terminal paired to a single BeBoss shop.
 * The shop owner's primary terminal is included; every added device with a staff operator
 * adds +2,000 FRw/month to the shop's subscription and automatically syncs all shop data.
 */
@Entity(
    tableName = "shop_devices",
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["joinCode"])
    ]
)
data class ShopDevice(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val shopSyncKey: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val joinCode: String = "",
    val assignedUserId: String = "",
    val assignedUsername: String = "",
    @ColumnInfo(name = "assignedFullName") val assignedUserName: String = "",
    val role: String = "CASHIER",
    val branchId: String = "main_branch",
    val branchName: String = "Main Store",
    val isOwnerPrimary: Boolean = false,
    val isActive: Boolean = true,
    val monthlyFeeAddonRwf: Int = 2000,
    val lastSyncedAt: Long = 0L,
    val syncedToSupabase: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
