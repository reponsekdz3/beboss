package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "branches")
data class Branch(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "Main Branch",
    val code: String = "BR-01",
    val address: String = "Kigali, Rwanda",
    val phone: String = "+250 788 000 000",
    val managerUserId: String = "",
    val managerName: String = "",
    val isMainBranch: Boolean = false,
    val isActive: Boolean = true,
    val colorHex: String = "#FF6B1A",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
