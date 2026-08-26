package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllProductsList(): List<Product>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND quantityInStock <= lowStockThreshold ORDER BY quantityInStock ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT DISTINCT category FROM products WHERE isDeleted = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET quantityInStock = quantityInStock - :qtySold, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun decrementStock(productId: String, qtySold: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET quantityInStock = quantityInStock + :qtyAdded, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun incrementStock(productId: String, qtyAdded: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET quantityInStock = :newStock, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun setStock(productId: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun softDeleteProduct(productId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductPermanently(productId: String)

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
    fun getTotalProductCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0 AND quantityInStock <= lowStockThreshold")
    fun getLowStockCount(): Flow<Int>
}
