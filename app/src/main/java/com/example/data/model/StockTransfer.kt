package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "stock_transfers")
data class StockTransfer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transferNumber: String = "TRF-${System.currentTimeMillis() % 100000}",
    val productId: String,
    val productName: String,
    val fromBranchId: String,
    val fromBranchName: String,
    val toBranchId: String,
    val toBranchName: String,
    val quantity: Double,
    val unit: String = "pcs",
    val transferDate: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED", // COMPLETED, IN_TRANSIT, CANCELLED
    val notes: String = "",
    val transferredBy: String = "Store Operator",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
