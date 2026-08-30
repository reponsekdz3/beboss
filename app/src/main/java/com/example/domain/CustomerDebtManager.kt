package com.example.domain

import com.example.data.dao.CustomerDao
import com.example.data.dao.CustomerPaymentDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SyncQueueDao
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Sale
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Domain Manager handling Customer CRM, Debts, Installment Payments, and Credit Limits.
 */
class CustomerDebtManager(
    private val customerDao: CustomerDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val saleDao: SaleDao,
    private val syncQueueDao: SyncQueueDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val customersWithDebt: Flow<List<Customer>> = customerDao.getCustomersWithDebt()
    val totalCustomerCount: Flow<Int> = customerDao.getTotalCustomerCount()
    val totalOutstandingDebt: Flow<Double> = customerDao.getTotalOutstandingDebt()
    val allCustomerPayments: Flow<List<CustomerPayment>> = customerPaymentDao.getAllPayments()

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return if (query.isBlank()) allCustomers else customerDao.searchCustomers(query.trim())
    }

    suspend fun getCustomerById(id: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>> {
        return customerPaymentDao.getPaymentsForCustomer(customerId)
    }

    fun getSalesForCustomer(customerId: String): Flow<List<Sale>> {
        return saleDao.getSalesForCustomer(customerId)
    }

    suspend fun saveCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        val isNew = customerDao.getCustomerById(customer.id) == null
        val toSave = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.insertCustomer(toSave)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "customers",
                recordId = toSave.id,
                operation = if (isNew) "CREATE" else "UPDATE",
                payloadJson = """{"name":"${toSave.name}","phone":"${toSave.phone}"}"""
            )
        )
    }

    suspend fun recordCustomerPayment(payment: CustomerPayment): CustomerPayment = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(payment.customerId)
        val currentDebt = customer?.debtBalance ?: 0.0
        val remaining = (currentDebt - payment.amount).coerceAtLeast(0.0)

        val updatedPayment = payment.copy(
            previousDebt = currentDebt,
            remainingDebt = remaining
        )

        customerPaymentDao.insertPayment(updatedPayment)
        customerDao.recordDebtPayment(payment.customerId, payment.amount)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "customer_payments",
                recordId = updatedPayment.id,
                operation = "CREATE",
                payloadJson = """{"customerId":"${updatedPayment.customerId}","amount":${updatedPayment.amount},"prevDebt":$currentDebt,"remDebt":$remaining,"method":"${updatedPayment.paymentMethod}"}"""
            )
        )

        updatedPayment
    }

    suspend fun recordDebtPayment(customerId: String, paymentAmount: Double) = withContext(Dispatchers.IO) {
        customerDao.recordDebtPayment(customerId, paymentAmount)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "customers",
                recordId = customerId,
                operation = "UPDATE",
                payloadJson = """{"debtPayment":$paymentAmount}"""
            )
        )
    }

    suspend fun deleteCustomer(customerId: String) = withContext(Dispatchers.IO) {
        customerDao.softDeleteCustomer(customerId)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "customers",
                recordId = customerId,
                operation = "DELETE",
                payloadJson = """{"id":"$customerId"}"""
            )
        )
    }
}
