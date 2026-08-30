package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "General",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val quantityInStock: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val unit: String = "pcs", // pcs, kg, litre, pack, box, bottle
    val barcode: String = "",
    val branchId: String = "main_branch",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = quantityInStock <= lowStockThreshold

    val isOutOfStock: Boolean
        get() = quantityInStock <= 0.0

    val profitMarginAmount: Double
        get() = sellingPrice - costPrice

    val profitMarginPercent: Double
        get() = if (costPrice > 0) ((sellingPrice - costPrice) / costPrice) * 100.0 else 0.0

    val currentStockCostValue: Double
        get() = quantityInStock * costPrice

    val currentStockRetailValue: Double
        get() = quantityInStock * sellingPrice
}
