package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class CustomerCategory(val displayName: String) {
    REGULAR("Regular Customer"),
    WHOLESALE("Wholesaler / Trader"),
    VIP_CREDIT("VIP Credit Partner"),
    RETAIL("Walk-in Shopper")
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "Kigali",
    val taxIdOrNin: String = "", // National ID or Tax TIN
    val category: String = CustomerCategory.REGULAR.displayName,
    val creditLimit: Double = 500000.0,
    val notes: String = "",
    val debtBalance: Double = 0.0, // Positive value means customer owes shop
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
