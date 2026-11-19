package com.example.domain

import android.os.StatFs
import androidx.room.withTransaction
import com.example.data.dao.CustomerDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SyncQueueDao
import com.example.data.dao.SyncTombstoneDao
import com.example.data.db.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncTombstone
import com.example.util.ClockGuard
import com.example.util.DbWriteMutex
import com.example.util.HlcClock
import com.example.util.MoneyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
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
    private val syncQueueDao: SyncQueueDao,
    private val database: AppDatabase? = null,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) {
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun getSaleWithItems(saleId: String): SaleWithItems? = withContext(Dispatchers.IO) {
        val sale = saleDao.getSaleById(saleId) ?: return@withContext null
        val items = saleDao.getItemsForSale(saleId)
        SaleWithItems(sale, items)
    }

    /**
     * Executes atomic sale checkout inside a process mutex and single Room transaction:
     * 0. Validates free disk space (if file-backed) & exact integer-cents totals
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
        require(items.isNotEmpty()) { "Cannot checkout an empty cart." }
        ensureSufficientStorage()

        DbWriteMutex.withLock {
            val executeCheckout: suspend () -> Sale = {
                val now = ClockGuard.effectiveNowMs()
                val hlc = HlcClock.tick(now)
                val saleId = UUID.randomUUID().toString()

                val rawTotalCents = items.sumOf {
                    MoneyUtils.multiplyPriceQuantityCents(it.effectiveUnitPrice, it.quantity)
                }
                val discountCents = MoneyUtils.toCents(discountAmount).coerceAtMost(rawTotalCents)
                val netTotalCents = (rawTotalCents - discountCents).coerceAtLeast(0L)
                val totalCostCents = items.sumOf {
                    MoneyUtils.multiplyPriceQuantityCents(it.product.costPrice, it.quantity)
                }
                val profitCents = netTotalCents - totalCostCents
                val paidCents = MoneyUtils.toCents(amountPaid)

                val netTotal = MoneyUtils.centsToDouble(netTotalCents)
                val effectiveDiscount = MoneyUtils.centsToDouble(discountCents)
                val totalCost = MoneyUtils.centsToDouble(totalCostCents)
                val totalProfit = MoneyUtils.centsToDouble(profitCents)
                val normalizedPaid = MoneyUtils.centsToDouble(paidCents)

                val paymentStatus = when {
                    paidCents >= netTotalCents -> "PAID"
                    paidCents > 0L -> "PARTIAL"
                    else -> "UNPAID"
                }
                val unpaidDebtCents = (netTotalCents - paidCents).coerceAtLeast(0L)
                val unpaidDebt = MoneyUtils.centsToDouble(unpaidDebtCents)

                val receiptNum = "REC-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date(now))

                val sale = Sale(
                    id = saleId,
                    customerId = customerId,
                    customerName = customerName.ifBlank { "Walk-in Customer" },
                    saleDate = now,
                    totalAmount = netTotal,
                    totalCost = totalCost,
                    totalProfit = totalProfit,
                    discountAmount = effectiveDiscount,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    amountPaid = normalizedPaid,
                    notes = notes,
                    receiptNumber = receiptNum,
                    branchId = branchId,
                    branchName = branchName,
                    cashierId = cashierId,
                    cashierName = cashierName,
                    synced = false,
                    updatedAt = now,
                    hlcTimestamp = hlc
                )

                val saleItems = items.map { cart ->
                    val subCents = MoneyUtils.multiplyPriceQuantityCents(cart.effectiveUnitPrice, cart.quantity)
                    val itemCostCents = MoneyUtils.multiplyPriceQuantityCents(cart.product.costPrice, cart.quantity)
                    SaleItem(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        productId = cart.product.id,
                        productName = cart.product.name,
                        category = cart.product.category,
                        unit = cart.product.unit,
                        quantitySold = cart.quantity,
                        costPriceAtSale = MoneyUtils.roundMoney(cart.product.costPrice),
                        unitPriceAtSale = MoneyUtils.roundMoney(cart.effectiveUnitPrice),
                        subtotal = MoneyUtils.centsToDouble(subCents),
                        profit = MoneyUtils.centsToDouble(subCents - itemCostCents),
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

            if (database != null) {
                database.withTransaction { executeCheckout() }
            } else {
                executeCheckout()
            }
        }
    }

    suspend fun deleteSale(saleId: String, restockInventory: Boolean = true) = withContext(Dispatchers.IO) {
        DbWriteMutex.withLock {
            val executeDelete: suspend () -> Unit = {
                val sale = saleDao.getSaleById(saleId)
                if (sale != null) {
                    val items = saleDao.getItemsForSale(saleId)
                    val now = ClockGuard.effectiveNowMs()

                    if (restockInventory) {
                        for (item in items) {
                            productDao.incrementStock(item.productId, item.quantitySold, now)
                        }
                    }

                    val unpaidCents = (MoneyUtils.toCents(sale.totalAmount) - MoneyUtils.toCents(sale.amountPaid))
                        .coerceAtLeast(0L)
                    if (sale.customerId != null && unpaidCents > 0L) {
                        customerDao.recordDebtPayment(
                            sale.customerId,
                            MoneyUtils.centsToDouble(unpaidCents),
                            now
                        )
                    }

                    saleDao.deleteSaleItems(saleId)
                    saleDao.deleteSale(saleId)

                    syncTombstoneDao?.insertTombstone(
                        SyncTombstone(
                            id = SyncTombstone.keyFor("sales", saleId),
                            tableName = "sales",
                            recordId = saleId,
                            deletedAt = now,
                            hlcTimestamp = HlcClock.tick(now)
                        )
                    )

                    syncQueueDao.enqueue(
                        SyncQueueItem(
                            tableName = "sales",
                            recordId = saleId,
                            operation = "DELETE",
                            payloadJson = """{"deleted":true,"saleId":"$saleId"}"""
                        )
                    )
                }
            }

            if (database != null) {
                database.withTransaction { executeDelete() }
            } else {
                executeDelete()
            }
        }
    }

    suspend fun clearAllSales() = withContext(Dispatchers.IO) {
        DbWriteMutex.withLock {
            saleDao.clearAllSales()
            saleDao.clearAllSaleItems()
        }
    }

    private fun ensureSufficientStorage() {
        try {
            val path = database?.openHelper?.writableDatabase?.path ?: return
            if (path.isBlank() || path == ":memory:") return
            val parentDir = File(path).parentFile ?: return
            if (!parentDir.exists()) return
            val stat = StatFs(parentDir.absolutePath)
            if (stat.availableBytes in 1L until 2L * 1024L * 1024L) {
                throw IllegalStateException("Low device storage (< 2 MB free). Free space before completing transaction.")
            }
        } catch (ise: IllegalStateException) {
            throw ise
        } catch (_: Throwable) {
            // Best-effort pre-flight only
        }
    }
}
