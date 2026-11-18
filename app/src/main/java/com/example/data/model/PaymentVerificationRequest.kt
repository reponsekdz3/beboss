package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "payment_verification_requests")
data class PaymentVerificationRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val shopName: String = "",
    val ownerUsername: String = "",
    val ownerPhone: String = "",
    val deviceChallenge: String = "",
    val provider: String = "MTN MoMo",
    val ussdCode: String = "",
    val payerPhone: String = "",
    val amountRwf: Int = 5000,
    val durationMonths: Int = 1,
    val branchCount: Int = 1,
    val workerCount: Int = 1,
    val telcoReference: String = "",
    val status: String = "PENDING_VERIFICATION", // PENDING_VERIFICATION, APPROVED, REJECTED, REDEEMED
    val generatedUnlockCode: String = "",
    val syncedToSupabase: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
