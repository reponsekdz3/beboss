package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.ClockGuard
import com.example.util.HlcClock

/**
 * Durable deletion tombstone so deleted records (products, customers, branches, sales, users)
 * do NOT resurrect during offline/online multi-branch merges or LWW sync.
 */
@Entity(
    tableName = "sync_tombstones",
    indices = [
        Index(value = ["tableName", "deletedAt"])
    ]
)
data class SyncTombstone(
    @PrimaryKey val id: String,
    val tableName: String,
    val recordId: String,
    val deletedAt: Long = ClockGuard.effectiveNowMs(),
    val hlcTimestamp: Long = HlcClock.tick(),
    val deletedByDeviceId: String = ""
) {
    companion object {
        fun keyFor(tableName: String, recordId: String): String = "$tableName:$recordId"
    }
}
