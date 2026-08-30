package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PurchaseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<PurchaseRecord>>

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    suspend fun getAllPurchasesList(): List<PurchaseRecord>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: String): PurchaseRecord?

    @Query("SELECT * FROM purchases WHERE productId = :productId ORDER BY purchaseDate DESC")
    fun getPurchasesForProduct(productId: String): Flow<List<PurchaseRecord>>

    @Query("SELECT * FROM purchases WHERE branchId = :branchId ORDER BY purchaseDate DESC")
    fun getPurchasesByBranch(branchId: String): Flow<List<PurchaseRecord>>

    @Query("SELECT * FROM purchases WHERE supplierName LIKE '%' || :supplier || '%' ORDER BY purchaseDate DESC")
    fun getPurchasesBySupplier(supplier: String): Flow<List<PurchaseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PurchaseRecord>)

    @Update
    suspend fun updatePurchase(purchase: PurchaseRecord)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deletePurchase(id: String)

    @Query("DELETE FROM purchases")
    suspend fun clearAllPurchases()

    @Query("SELECT COUNT(*) FROM purchases")
    suspend fun getPurchaseCount(): Int

    @Query("SELECT SUM(totalPurchaseCost) FROM purchases")
    suspend fun getTotalPurchaseExpenditure(): Double?

    @Query("SELECT SUM(quantityPurchased) FROM purchases")
    suspend fun getTotalUnitsPurchased(): Double?
}
