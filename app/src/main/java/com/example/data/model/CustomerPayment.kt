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
    val paymentMethod: String = "Cash", // Cash, Mobile Money (MoMo), Bank, Card
    val notes: String = "",
    val receiptNumber: String = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
    val paymentDate: Long = System.currentTimeMillis()
)
