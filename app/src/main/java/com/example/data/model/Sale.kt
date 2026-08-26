package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer",
    val saleDate: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val discountAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, MOMO, CARD, DEBT
    val paymentStatus: String = "PAID", // PAID, PARTIAL, UNPAID
    val amountPaid: Double = 0.0,
    val notes: String = "",
    val receiptNumber: String = "",
    val branchId: String = "main_branch",
    val branchName: String = "Main Store",
    val cashierId: String = "",
    val cashierName: String = "Shop Operator",
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productId: String,
    val productName: String,
    val category: String = "General",
    val unit: String = "pcs",
    val quantitySold: Double = 1.0,
    val costPriceAtSale: Double = 0.0,
    val unitPriceAtSale: Double = 0.0,
    val subtotal: Double = 0.0,
    val profit: Double = 0.0,
    val branchId: String = "main_branch"
)

data class SaleWithItems(
    val sale: Sale,
    val items: List<SaleItem>
)
