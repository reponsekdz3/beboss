package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "redeemed_vouchers")
data class RedeemedVoucher(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val voucherCode: String,
    val voucherHash: String,
    val planName: String,
    val daysAdded: Int,
    val verifiedAmount: Int,
    val redeemedAt: Long = System.currentTimeMillis()
)
