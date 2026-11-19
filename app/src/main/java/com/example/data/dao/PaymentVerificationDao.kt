package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentVerificationRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentVerificationDao {

    @Query("SELECT * FROM payment_verification_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<PaymentVerificationRequest>>

    @Query("SELECT * FROM payment_verification_requests ORDER BY createdAt DESC")
    suspend fun getAllRequestsList(): List<PaymentVerificationRequest>

    @Query("SELECT * FROM payment_verification_requests WHERE syncedToSupabase = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedRequests(): List<PaymentVerificationRequest>

    @Query("SELECT * FROM payment_verification_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): PaymentVerificationRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(request: PaymentVerificationRequest)

    @Query("UPDATE payment_verification_requests SET syncedToSupabase = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE payment_verification_requests SET status = :status, generatedUnlockCode = :unlockCode, approvalMode = :approvalMode, syncedToSupabase = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCloudApproval(
        id: String,
        status: String,
        unlockCode: String,
        approvalMode: String = "",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM payment_verification_requests")
    suspend fun clearAll()
}
