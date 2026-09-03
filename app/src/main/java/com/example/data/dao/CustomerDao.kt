package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllCustomersList(): List<Customer>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 AND debtBalance > 0 ORDER BY debtBalance DESC")
    fun getCustomersWithDebt(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET debtBalance = debtBalance + :amount, updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun addDebt(customerId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET debtBalance = MAX(0.0, debtBalance - :amount), updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun recordDebtPayment(customerId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0")
    fun getTotalCustomerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0")
    suspend fun getCustomerCountDirect(): Int

    @Query("UPDATE customers SET debtBalance = 0.0, updatedAt = :updatedAt WHERE isDeleted = 0")
    suspend fun resetAllDebtsToZero(updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(SUM(debtBalance), 0.0) FROM customers WHERE isDeleted = 0")
    fun getTotalOutstandingDebt(): Flow<Double>

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerPermanently(customerId: String)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}
