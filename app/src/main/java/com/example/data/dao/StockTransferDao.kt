package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StockTransfer
import kotlinx.coroutines.flow.Flow

@Dao
interface StockTransferDao {

    @Query("SELECT * FROM stock_transfers ORDER BY transferDate DESC")
    fun getAllTransfers(): Flow<List<StockTransfer>>

    @Query("SELECT * FROM stock_transfers ORDER BY transferDate DESC")
    suspend fun getAllTransfersList(): List<StockTransfer>

    @Query("SELECT * FROM stock_transfers WHERE fromBranchId = :branchId OR toBranchId = :branchId ORDER BY transferDate DESC")
    fun getTransfersByBranch(branchId: String): Flow<List<StockTransfer>>

    @Query("SELECT * FROM stock_transfers WHERE id = :id LIMIT 1")
    suspend fun getTransferById(id: String): StockTransfer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: StockTransfer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfers(transfers: List<StockTransfer>)

    @Update
    suspend fun updateTransfer(transfer: StockTransfer)

    @Query("DELETE FROM stock_transfers WHERE id = :id")
    suspend fun deleteTransfer(id: String)

    @Query("DELETE FROM stock_transfers")
    suspend fun clearAllTransfers()

    @Query("SELECT COUNT(*) FROM stock_transfers")
    suspend fun getTransferCount(): Int
}
