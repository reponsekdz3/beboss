package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "purchases")
data class PurchaseRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val category: String = "General",
    val supplierName: String = "Local Wholesale Supplier",
    val supplierPhone: String = "",
    val quantityPurchased: Double = 1.0,
    val unitCostPrice: Double = 0.0,
    val totalPurchaseCost: Double = 0.0,
    val sellingPriceAtPurchase: Double = 0.0,
    val paymentStatus: String = "PAID_CASH", // PAID_CASH, PAID_MOMO, PAID_BANK, SUPPLIER_CREDIT
    val invoiceNumber: String = "",
    val branchId: String = "main_branch",
    val branchName: String = "Main Store",
    val purchasedByUserId: String = "",
    val purchasedByName: String = "Shop Manager",
    val notes: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val estimatedGrossProfitPerUnit: Double
        get() = (sellingPriceAtPurchase - unitCostPrice).coerceAtLeast(0.0)

    val estimatedTotalProfit: Double
        get() = estimatedGrossProfitPerUnit * quantityPurchased

    val markupPercent: Double
        get() = if (unitCostPrice > 0) ((sellingPriceAtPurchase - unitCostPrice) / unitCostPrice) * 100.0 else 0.0
}

data class PurchaseSummary(
    val totalPurchasesCount: Int = 0,
    val totalUnitsBought: Double = 0.0,
    val totalExpenditure: Double = 0.0,
    val uniqueSuppliersCount: Int = 0,
    val averagePurchaseCost: Double = 0.0
)
