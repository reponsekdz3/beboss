package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "local_audit_logs",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["action"])
    ]
)
data class LocalAuditLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val actorName: String,
    val actorRole: String,
    val action: String, // "AUTH_LOCKED", "ADMIN_APPROVE", "DATASHARE_MERGE", "VOUCHER_REDEEM", "DB_INTEGRITY", etc.
    val targetId: String = "",
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
