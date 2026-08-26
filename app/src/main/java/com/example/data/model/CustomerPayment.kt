package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customer_payments")
data class CustomerPayment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val previousDebt: Double = 0.0,
    val remainingDebt: Double = 0.0,
    val paymentMethod: String = "Cash", // Cash, Mobile Money (MoMo), Bank, Card
    val notes: String = "",
    val receiptNumber: String = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
    val paymentDate: Long = System.currentTimeMillis(),
    val recordedBy: String = "Shop Owner",
    val branchId: String = "main_branch",
    val branchName: String = "Main Branch"
)
