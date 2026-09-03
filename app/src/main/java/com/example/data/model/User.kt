package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class UserRole(val displayName: String, val badgeColor: String) {
    OWNER("Shop Owner (Full Access)", "#FF6B1A"),
    MANAGER("Store Manager (Inventory & Ops)", "#2563EB"),
    CASHIER("Cashier (POS Checkout)", "#10B981")
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val username: String,
    val email: String = "",
    val phone: String = "",
    val pinHash: String = "", // Secure PBKDF2 hash of fast PIN
    val password: String = "",
    val role: UserRole = UserRole.OWNER,
    val assignedBranchId: String = "", // Empty means all branches (Boss / Owner)
    val assignedBranchName: String = "All Branches",
    val profileColorHex: String = "#FF6B1A",
    val isActive: Boolean = true,
    // Granular Collaborator Permissions
    val canSellPOS: Boolean = true,
    val canApplyDiscounts: Boolean = true,
    val canManageInventory: Boolean = true,
    val canViewCostAndProfit: Boolean = true,
    val canViewAnalytics: Boolean = true,
    val canManageCustomers: Boolean = true,
    val canCollectDebt: Boolean = true,
    val canDeleteRecords: Boolean = true,
    val canExportReports: Boolean = true,
    val canManageCollaborators: Boolean = false,
    val canManageShopSettings: Boolean = false,
    val canExportImportData: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun defaultForRole(
            name: String,
            username: String,
            role: UserRole,
            pin: String = "",
            phone: String = ""
        ): User {
            return when (role) {
                UserRole.OWNER -> User(
                    name = name,
                    username = username,
                    phone = phone,
                    pinHash = pin,
                    role = UserRole.OWNER,
                    profileColorHex = "#FF6B1A",
                    canSellPOS = true,
                    canApplyDiscounts = true,
                    canManageInventory = true,
                    canViewCostAndProfit = true,
                    canViewAnalytics = true,
                    canManageCustomers = true,
                    canCollectDebt = true,
                    canDeleteRecords = true,
                    canExportReports = true,
                    canManageCollaborators = true,
                    canManageShopSettings = true,
                    canExportImportData = true
                )
                UserRole.MANAGER -> User(
                    name = name,
                    username = username,
                    phone = phone,
                    pinHash = pin,
                    role = UserRole.MANAGER,
                    profileColorHex = "#2563EB",
                    canSellPOS = true,
                    canApplyDiscounts = true,
                    canManageInventory = true,
                    canViewCostAndProfit = true,
                    canViewAnalytics = true,
                    canManageCustomers = true,
                    canCollectDebt = true,
                    canDeleteRecords = false,
                    canExportReports = true,
                    canManageCollaborators = false,
                    canManageShopSettings = false,
                    canExportImportData = false
                )
                UserRole.CASHIER -> User(
                    name = name,
                    username = username,
                    phone = phone,
                    pinHash = pin,
                    role = UserRole.CASHIER,
                    profileColorHex = "#10B981",
                    canSellPOS = true,
                    canApplyDiscounts = false,
                    canManageInventory = false,
                    canViewCostAndProfit = false,
                    canViewAnalytics = false,
                    canManageCustomers = true,
                    canCollectDebt = false,
                    canDeleteRecords = false,
                    canExportReports = false,
                    canManageCollaborators = false,
                    canManageShopSettings = false,
                    canExportImportData = false
                )
            }
        }
    }
}
