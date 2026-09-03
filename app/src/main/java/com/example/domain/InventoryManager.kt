package com.example.domain

import com.example.data.dao.ProductDao
import com.example.data.dao.PurchaseDao
import com.example.data.dao.SyncQueueDao
import com.example.data.model.InventoryValuation
import com.example.data.model.Product
import com.example.data.model.PurchaseRecord
import com.example.data.model.PurchaseSummary
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Domain Manager handling Catalog, Stock, Purchases & Inflow, and Inventory Valuation.
 */
class InventoryManager(
    private val productDao: ProductDao,
    private val purchaseDao: PurchaseDao,
    private val syncQueueDao: SyncQueueDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val categories: Flow<List<String>> = productDao.getAllCategories()
    val totalProductCount: Flow<Int> = productDao.getTotalProductCount()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val allPurchases: Flow<List<PurchaseRecord>> = purchaseDao.getAllPurchases()

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) allProducts else productDao.searchProducts(query.trim())
    }

    suspend fun getProductById(id: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        val isNew = productDao.getProductById(product.id) == null
        val toSave = product.copy(updatedAt = System.currentTimeMillis())
        productDao.insertProduct(toSave)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "products",
                recordId = toSave.id,
                operation = if (isNew) "CREATE" else "UPDATE",
                payloadJson = """{"name":"${toSave.name}","sellingPrice":${toSave.sellingPrice},"qty":${toSave.quantityInStock}}"""
            )
        )
    }

    suspend fun adjustStock(productId: String, adjustmentDelta: Double, reason: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val newStock = (product.quantityInStock + adjustmentDelta).coerceAtLeast(0.0)
        productDao.setStock(productId, newStock)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "products",
                recordId = productId,
                operation = "UPDATE",
                payloadJson = """{"stockAdjustment":$adjustmentDelta,"newStock":$newStock,"reason":"$reason"}"""
            )
        )
    }

    suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        productDao.deleteProductPermanently(productId)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "products",
                recordId = productId,
                operation = "DELETE",
                payloadJson = """{"id":"$productId"}"""
            )
        )
    }

    suspend fun clearAllProducts() = withContext(Dispatchers.IO) {
        productDao.clearAllProducts()
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "products",
                recordId = "ALL",
                operation = "DELETE_ALL",
                payloadJson = "{}"
            )
        )
    }

    suspend fun recordPurchase(
        productId: String,
        quantity: Double,
        unitCostPrice: Double,
        newSellingPrice: Double?,
        supplierName: String,
        supplierPhone: String,
        paymentStatus: String,
        invoiceNumber: String,
        notes: String,
        branchId: String = "main_branch",
        branchName: String = "Main Store",
        buyerId: String = "",
        buyerName: String = "Store Manager"
    ): PurchaseRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductById(productId)
        val prodName = product?.name ?: "Stock Inflow Item"
        val category = product?.category ?: "General"
        val activeSellingPrice = newSellingPrice ?: product?.sellingPrice ?: 0.0

        val invNum = invoiceNumber.trim().ifBlank {
            "INV-BUY-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date(now))
        }

        val purchase = PurchaseRecord(
            id = UUID.randomUUID().toString(),
            productId = productId,
            productName = prodName,
            category = category,
            supplierName = supplierName.trim().ifBlank { "Direct Supplier" },
            supplierPhone = supplierPhone.trim(),
            quantityPurchased = quantity.coerceAtLeast(0.0),
            unitCostPrice = unitCostPrice.coerceAtLeast(0.0),
            totalPurchaseCost = quantity * unitCostPrice,
            sellingPriceAtPurchase = activeSellingPrice,
            paymentStatus = paymentStatus,
            invoiceNumber = invNum,
            branchId = branchId,
            branchName = branchName,
            purchasedByUserId = buyerId,
            purchasedByName = buyerName,
            notes = notes.trim(),
            purchaseDate = now,
            createdAt = now
        )

        purchaseDao.insertPurchase(purchase)

        if (product != null) {
            val updatedProduct = product.copy(
                quantityInStock = (product.quantityInStock + quantity).coerceAtLeast(0.0),
                costPrice = if (unitCostPrice > 0) unitCostPrice else product.costPrice,
                sellingPrice = if (newSellingPrice != null && newSellingPrice > 0) newSellingPrice else product.sellingPrice,
                updatedAt = now
            )
            productDao.insertProduct(updatedProduct)
        }

        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "purchases",
                recordId = purchase.id,
                operation = "CREATE",
                payloadJson = """{"productId":"$productId","qty":$quantity,"unitCost":$unitCostPrice,"total":${purchase.totalPurchaseCost},"supplier":"$supplierName"}"""
            )
        )

        purchase
    }

    suspend fun deletePurchaseRecord(purchaseId: String) = withContext(Dispatchers.IO) {
        purchaseDao.deletePurchase(purchaseId)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "purchases",
                recordId = purchaseId,
                operation = "DELETE",
                payloadJson = """{"id":"$purchaseId"}"""
            )
        )
    }

    fun getInventoryValuation(): Flow<InventoryValuation> = allProducts.map { products ->
        var totalCost = 0.0
        var totalRetail = 0.0
        var totalUnits = 0.0
        var lowStock = 0
        var outOfStock = 0

        for (p in products) {
            val q = p.quantityInStock.coerceAtLeast(0.0)
            totalCost += q * p.costPrice.coerceAtLeast(0.0)
            totalRetail += q * p.sellingPrice.coerceAtLeast(0.0)
            totalUnits += q
            if (p.isLowStock) lowStock++
            if (p.isOutOfStock) outOfStock++
        }

        InventoryValuation(
            totalItems = products.size,
            totalUnitsInStock = totalUnits,
            totalCostValue = totalCost,
            totalRetailValue = totalRetail,
            potentialProfit = (totalRetail - totalCost).coerceAtLeast(0.0),
            lowStockCount = lowStock,
            outOfStockCount = outOfStock
        )
    }

    fun getPurchaseSummary(): Flow<PurchaseSummary> = allPurchases.map { purchases ->
        var totalCost = 0.0
        var totalUnits = 0.0
        val uniqueSuppliers = mutableSetOf<String>()

        for (p in purchases) {
            totalCost += p.totalPurchaseCost
            totalUnits += p.quantityPurchased
            if (p.supplierName.isNotBlank()) uniqueSuppliers.add(p.supplierName)
        }

        val avg = if (purchases.isNotEmpty()) totalCost / purchases.size else 0.0

        PurchaseSummary(
            totalPurchasesCount = purchases.size,
            totalUnitsBought = totalUnits,
            totalExpenditure = totalCost,
            uniqueSuppliersCount = uniqueSuppliers.size,
            averagePurchaseCost = avg
        )
    }
}
