package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tableName: String, // "products", "sales", "customers", "shop_profile"
    val recordId: String,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val payloadJson: String,
    val deviceId: String = "android-offline-device-01",
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val status: String = "PENDING" // "PENDING", "SYNCED", "FAILED"
)
