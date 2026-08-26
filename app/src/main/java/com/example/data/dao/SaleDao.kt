package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    suspend fun getAllSalesList(): List<Sale>

    @Query("SELECT * FROM sales WHERE saleDate >= :startTime AND saleDate <= :endTime ORDER BY saleDate DESC")
    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY saleDate DESC")
    fun getSalesForCustomer(customerId: String): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: String): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: String): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun observeItemsForSale(saleId: String): Flow<List<SaleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("UPDATE sales SET synced = 1, updatedAt = :updatedAt WHERE id = :saleId")
    suspend fun markSaleSynced(saleId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM sales WHERE synced = 0")
    suspend fun getUnsyncedSales(): List<Sale>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE saleDate >= :startTime AND saleDate <= :endTime")
    fun getRevenueBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalProfit), 0.0) FROM sales WHERE saleDate >= :startTime AND saleDate <= :endTime")
    fun getProfitBetween(startTime: Long, endTime: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM sales WHERE saleDate >= :startTime AND saleDate <= :endTime")
    fun getSalesCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("""
        SELECT si.productId, si.productName, SUM(si.quantitySold) as totalQuantity, SUM(si.subtotal) as totalRevenue, SUM(si.profit) as totalProfit 
        FROM sale_items si 
        INNER JOIN sales s ON si.saleId = s.id 
        WHERE s.saleDate >= :startTime AND s.saleDate <= :endTime 
        GROUP BY si.productId 
        ORDER BY totalProfit DESC 
        LIMIT :limit
    """)
    fun getTopProductsByProfit(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<TopProductRaw>>

    @Query("""
        SELECT si.productId, si.productName, SUM(si.quantitySold) as totalQuantity, SUM(si.subtotal) as totalRevenue, SUM(si.profit) as totalProfit 
        FROM sale_items si 
        INNER JOIN sales s ON si.saleId = s.id 
        WHERE s.saleDate >= :startTime AND s.saleDate <= :endTime 
        GROUP BY si.productId 
        ORDER BY totalQuantity DESC 
        LIMIT :limit
    """)
    fun getTopProductsByQuantity(startTime: Long, endTime: Long, limit: Int = 10): Flow<List<TopProductRaw>>

    @Query("""
        SELECT si.category, SUM(si.subtotal) as totalRevenue 
        FROM sale_items si 
        INNER JOIN sales s ON si.saleId = s.id 
        WHERE s.saleDate >= :startTime AND s.saleDate <= :endTime 
        GROUP BY si.category 
        ORDER BY totalRevenue DESC
    """)
    fun getCategorySalesBreakdown(startTime: Long, endTime: Long): Flow<List<CategorySalesRaw>>
}

data class TopProductRaw(
    val productId: String,
    val productName: String,
    val totalQuantity: Double,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class CategorySalesRaw(
    val category: String,
    val totalRevenue: Double
)
