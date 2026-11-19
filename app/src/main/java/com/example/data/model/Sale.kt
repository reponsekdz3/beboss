package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["branchId"]),
        Index(value = ["saleDate"]),
        Index(value = ["synced"])
    ]
)
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
    val updatedAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 1L,
    val hlcTimestamp: Long = 0L,
    val lastModifiedDeviceId: String = ""
)

@Entity(
    tableName = "sale_items",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
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

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val customPrice: Double? = null
) {
    val effectiveUnitPrice: Double
        get() = customPrice ?: product.sellingPrice

    val subtotal: Double
        get() = effectiveUnitPrice * quantity

    val cost: Double
        get() = product.costPrice * quantity

    val profit: Double
        get() = subtotal - cost
}
