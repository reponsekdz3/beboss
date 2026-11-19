package com.example.domain

import com.example.data.dao.CustomerDao
import com.example.data.dao.CustomerPaymentDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SyncQueueDao
import com.example.data.dao.SyncTombstoneDao
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Sale
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncTombstone
import com.example.util.ClockGuard
import com.example.util.DbWriteMutex
import com.example.util.HlcClock
import com.example.util.MoneyUtils
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
    private val syncQueueDao: SyncQueueDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
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
        DbWriteMutex.withLock {
            val existing = customerDao.getCustomerById(customer.id)
            val isNew = existing == null
            val now = ClockGuard.effectiveNowMs()
            val toSave = customer.copy(
                isDeleted = false,
                debtBalance = MoneyUtils.roundMoney(customer.debtBalance.coerceAtLeast(0.0)),
                updatedAt = now,
                syncVersion = (existing?.syncVersion ?: 0L) + 1L,
                hlcTimestamp = HlcClock.tick(now)
            )
            customerDao.insertCustomer(toSave)
            syncTombstoneDao?.deleteTombstone(SyncTombstone.keyFor("customers", toSave.id))
            syncQueueDao.enqueue(
                SyncQueueItem(
                    tableName = "customers",
                    recordId = toSave.id,
                    operation = if (isNew) "CREATE" else "UPDATE",
                    payloadJson = """{"name":"${toSave.name}","phone":"${toSave.phone}"}"""
                )
            )
        }
    }

    suspend fun recordCustomerPayment(payment: CustomerPayment): CustomerPayment = withContext(Dispatchers.IO) {
        DbWriteMutex.withLock {
            val now = ClockGuard.effectiveNowMs()
            val customer = customerDao.getCustomerById(payment.customerId)
            val currentDebtCents = MoneyUtils.toCents(customer?.debtBalance ?: 0.0)
            val payCents = MoneyUtils.toCents(payment.amount)
            val remainingCents = (currentDebtCents - payCents).coerceAtLeast(0L)
            val currentDebt = MoneyUtils.centsToDouble(currentDebtCents)
            val remaining = MoneyUtils.centsToDouble(remainingCents)

            val updatedPayment = payment.copy(
                amount = MoneyUtils.centsToDouble(payCents),
                previousDebt = currentDebt,
                remainingDebt = remaining,
                paymentDate = now
            )

            customerPaymentDao.insertPayment(updatedPayment)
            customerDao.recordDebtPayment(payment.customerId, updatedPayment.amount, now)
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
    }

    suspend fun recordDebtPayment(customerId: String, paymentAmount: Double) = withContext(Dispatchers.IO) {
        DbWriteMutex.withLock {
            val now = ClockGuard.effectiveNowMs()
            val cleanAmt = MoneyUtils.roundMoney(paymentAmount.coerceAtLeast(0.0))
            customerDao.recordDebtPayment(customerId, cleanAmt, now)
            syncQueueDao.enqueue(
                SyncQueueItem(
                    tableName = "customers",
                    recordId = customerId,
                    operation = "UPDATE",
                    payloadJson = """{"debtPayment":$cleanAmt}"""
                )
            )
        }
    }

    suspend fun deleteCustomer(customerId: String) = withContext(Dispatchers.IO) {
        DbWriteMutex.withLock {
            val now = ClockGuard.effectiveNowMs()
            customerDao.softDeleteCustomer(customerId, now)
            syncTombstoneDao?.insertTombstone(
                SyncTombstone(
                    id = SyncTombstone.keyFor("customers", customerId),
                    tableName = "customers",
                    recordId = customerId,
                    deletedAt = now,
                    hlcTimestamp = HlcClock.tick(now)
                )
            )
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

    suspend fun restoreCustomer(customerId: String) = withContext(Dispatchers.IO) {
        DbWriteMutex.withLock {
            val now = ClockGuard.effectiveNowMs()
            customerDao.restoreCustomer(customerId, now)
            syncTombstoneDao?.deleteTombstone(SyncTombstone.keyFor("customers", customerId))
            syncQueueDao.enqueue(
                SyncQueueItem(
                    tableName = "customers",
                    recordId = customerId,
                    operation = "UPDATE",
                    payloadJson = """{"restored":true}"""
                )
            )
        }
    }

    suspend fun clearAllCustomers() = withContext(Dispatchers.IO) {
        customerDao.clearAllCustomers()
        customerPaymentDao.clearAllPayments()
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "customers",
                recordId = "ALL",
                operation = "DELETE_ALL",
                payloadJson = "{}"
            )
        )
    }
}
