package com.example.domain

import com.example.data.dao.CustomerDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SyncQueueDao
import com.example.data.model.CartItem
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Domain Manager handling Point-Of-Sale transactions, Cart logic, Atomic Checkout,
 * and Sales reporting.
 */
class SalesManager(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val syncQueueDao: SyncQueueDao
) {
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun getSaleWithItems(saleId: String): SaleWithItems? = withContext(Dispatchers.IO) {
        val sale = saleDao.getSaleById(saleId) ?: return@withContext null
        val items = saleDao.getItemsForSale(saleId)
        SaleWithItems(sale, items)
    }

    /**
     * Executes atomic sale checkout:
     * 1. Inserts Sale
     * 2. Inserts SaleItems
     * 3. Decrements inventory for each product
     * 4. Updates customer debt balance if sale has unpaid/debt portion
     * 5. Enqueues sync records
     */
    suspend fun processSale(
        items: List<CartItem>,
        customerId: String?,
        customerName: String,
        discountAmount: Double,
        paymentMethod: String,
        amountPaid: Double,
        notes: String,
        branchId: String = "main_branch",
        branchName: String = "Main Store",
        cashierId: String = "",
        cashierName: String = "Shop Operator"
    ): Sale = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val saleId = UUID.randomUUID().toString()

        val rawTotal = items.sumOf { (it.effectiveUnitPrice * it.quantity).coerceAtLeast(0.0) }
        val netTotal = (rawTotal - discountAmount).coerceAtLeast(0.0)
        val totalCost = items.sumOf { (it.product.costPrice * it.quantity).coerceAtLeast(0.0) }
        val totalProfit = netTotal - totalCost

        val paymentStatus = when {
            amountPaid >= netTotal -> "PAID"
            amountPaid > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }
        val unpaidDebt = (netTotal - amountPaid).coerceAtLeast(0.0)

        val receiptNum = "REC-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date(now))

        val sale = Sale(
            id = saleId,
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            saleDate = now,
            totalAmount = netTotal,
            totalCost = totalCost,
            totalProfit = totalProfit,
            discountAmount = discountAmount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            amountPaid = amountPaid,
            notes = notes,
            receiptNumber = receiptNum,
            branchId = branchId,
            branchName = branchName,
            cashierId = cashierId,
            cashierName = cashierName,
            synced = false,
            updatedAt = now
        )

        val saleItems = items.map { cart ->
            val sub = (cart.effectiveUnitPrice * cart.quantity).coerceAtLeast(0.0)
            val itemCost = (cart.product.costPrice * cart.quantity).coerceAtLeast(0.0)
            SaleItem(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = cart.product.id,
                productName = cart.product.name,
                category = cart.product.category,
                unit = cart.product.unit,
                quantitySold = cart.quantity,
                costPriceAtSale = cart.product.costPrice,
                unitPriceAtSale = cart.effectiveUnitPrice,
                subtotal = sub,
                profit = sub - itemCost,
                branchId = branchId
            )
        }

        // Write sale and line items
        saleDao.insertSale(sale)
        saleDao.insertSaleItems(saleItems)

        // Decrement product inventory safely
        for (item in items) {
            productDao.decrementStock(item.product.id, item.quantity, now)
        }

        // Update customer debt if applicable
        if (customerId != null && unpaidDebt > 0.0) {
            customerDao.addDebt(customerId, unpaidDebt, now)
        }

        // Enqueue cloud sync
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "sales",
                recordId = saleId,
                operation = "CREATE",
                payloadJson = """{"total":$netTotal,"itemsCount":${items.size},"receipt":"$receiptNum"}"""
            )
        )

        sale
    }

    suspend fun deleteSale(saleId: String, restockInventory: Boolean = true) = withContext(Dispatchers.IO) {
        val sale = saleDao.getSaleById(saleId) ?: return@withContext
        val items = saleDao.getItemsForSale(saleId)
        val now = System.currentTimeMillis()

        if (restockInventory) {
            for (item in items) {
                productDao.incrementStock(item.productId, item.quantitySold, now)
            }
        }

        val unpaidDebt = (sale.totalAmount - sale.amountPaid).coerceAtLeast(0.0)
        if (sale.customerId != null && unpaidDebt > 0.0) {
            customerDao.recordDebtPayment(sale.customerId, unpaidDebt, now)
        }

        saleDao.deleteSaleItems(saleId)
        saleDao.deleteSale(saleId)

        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "sales",
                recordId = saleId,
                operation = "DELETE",
                payloadJson = """{"deleted":true,"saleId":"$saleId"}"""
            )
        )
    }

    suspend fun clearAllSales() = withContext(Dispatchers.IO) {
        saleDao.clearAllSales()
        saleDao.clearAllSaleItems()
    }
}
