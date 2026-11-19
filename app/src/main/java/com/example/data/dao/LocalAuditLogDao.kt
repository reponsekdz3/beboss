package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LocalAuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAuditLogDao {
    @Query("SELECT * FROM local_audit_logs ORDER BY createdAt DESC LIMIT 120")
    fun getRecentLogs(): Flow<List<LocalAuditLog>>

    @Query("SELECT * FROM local_audit_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentLogsDirect(limit: Int = 60): List<LocalAuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LocalAuditLog)

    @Query("DELETE FROM local_audit_logs WHERE createdAt < :olderThanMs")
    suspend fun purgeOlderThan(olderThanMs: Long)
}
