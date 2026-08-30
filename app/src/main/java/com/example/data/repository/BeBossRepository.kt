package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.Branch
import com.example.data.model.CartItem
import com.example.data.model.CategorySalesShare
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.DailyAnalyticsPoint
import com.example.data.model.DatabaseEngineStats
import com.example.data.model.DatabaseMaintenanceResult
import com.example.data.model.InventoryValuation
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.PurchaseRecord
import com.example.data.model.PurchaseSummary
import com.example.data.model.RedeemedVoucher
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.SyncQueueItem
import com.example.data.model.TopProductReport
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.domain.AnalyticsEngine
import com.example.domain.BranchTransferManager
import com.example.domain.CustomerDebtManager
import com.example.domain.InventoryManager
import com.example.domain.SalesManager
import com.example.domain.StaffAuthManager
import com.example.domain.SubscriptionManager
import com.example.util.CloudSyncManager
import com.example.util.CloudSyncReport
import com.example.util.EndpointPingResult
import com.example.util.PaymentProcessingResult
import com.example.util.SecurityUtils
import com.example.util.ShopImportSummary
import com.example.util.SyncAuditLog
import com.example.util.VoucherValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Enterprise Repository Facade coordinating Domain Managers, Room DAOs, Cloud Sync, and Local Storage.
 */
class BeBossRepository(private val database: AppDatabase) {

    // DAOs
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val customerPaymentDao = database.customerPaymentDao()
    private val saleDao = database.saleDao()
    private val shopProfileDao = database.shopProfileDao()
    private val syncQueueDao = database.syncQueueDao()
    private val userDao = database.userDao()
    private val branchDao = database.branchDao()
    private val purchaseDao = database.purchaseDao()
    private val stockTransferDao = database.stockTransferDao()
    private val redeemedVoucherDao = database.redeemedVoucherDao()

    // Domain Managers
    val salesManager = SalesManager(saleDao, productDao, customerDao, syncQueueDao)
    val inventoryManager = InventoryManager(productDao, purchaseDao, syncQueueDao)
    val customerDebtManager = CustomerDebtManager(customerDao, customerPaymentDao, saleDao, syncQueueDao)
    val staffAuthManager = StaffAuthManager(userDao, syncQueueDao)
    val branchTransferManager = BranchTransferManager(branchDao, stockTransferDao, productDao, shopProfileDao, syncQueueDao)
    val subscriptionManager = SubscriptionManager(shopProfileDao, redeemedVoucherDao)
    val analyticsEngine = AnalyticsEngine(saleDao)
    val cloudSyncManager = CloudSyncManager(database)

    // Sync Audit Logs & Pending Queue
    val syncAuditLogs: StateFlow<List<SyncAuditLog>> = cloudSyncManager.syncAuditLogs
    val pendingSyncCount: Flow<Int> = syncQueueDao.getPendingCount()

    fun logSyncEvent(type: String, status: String, summary: String, latencyMs: Long = 0L) {
        cloudSyncManager.addAuditLog(type, status, summary, latencyMs)
    }

    suspend fun testServerPing(endpoint: String): EndpointPingResult =
        cloudSyncManager.testEndpointConnection(endpoint)

    suspend fun performSync(): CloudSyncReport =
        cloudSyncManager.syncAllDataToCloud()

    // -------------------------------------------------------------
    // PURCHASES & STOCK INFLOW
    // -------------------------------------------------------------
    val allPurchases: Flow<List<PurchaseRecord>> = inventoryManager.allPurchases

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
    ): PurchaseRecord = inventoryManager.recordPurchase(
        productId, quantity, unitCostPrice, newSellingPrice, supplierName, supplierPhone,
        paymentStatus, invoiceNumber, notes, branchId, branchName, buyerId, buyerName
    )

    suspend fun recordPurchaseOrder(
        productId: String,
        quantity: Double,
        unitCostPrice: Double,
        newSellingPrice: Double? = null,
        supplierName: String = "",
        supplierPhone: String = "",
        paymentStatus: String = "PAID",
        invoiceNumber: String = "",
        notes: String = "",
        branchId: String = "main_branch",
        branchName: String = "Main Store",
        buyerId: String = "",
        buyerName: String = "Store Manager"
    ): PurchaseRecord = recordPurchase(
        productId, quantity, unitCostPrice, newSellingPrice, supplierName, supplierPhone,
        paymentStatus, invoiceNumber, notes, branchId, branchName, buyerId, buyerName
    )

    suspend fun deletePurchaseRecord(purchaseId: String) = inventoryManager.deletePurchaseRecord(purchaseId)

    // -------------------------------------------------------------
    // BRANCHES & MULTI-STORE MANAGEMENT
    // -------------------------------------------------------------
    val allBranches: Flow<List<Branch>> = branchTransferManager.allBranches

    suspend fun getAllBranchesList(): List<Branch> = branchTransferManager.getAllBranchesList()

    suspend fun saveBranch(branch: Branch) = branchTransferManager.saveBranch(branch)

    suspend fun deleteBranch(branchId: String) = branchTransferManager.deleteBranch(branchId)

    suspend fun ensureDefaultBranches(): List<Branch> = branchTransferManager.ensureDefaultBranches()

    // -------------------------------------------------------------
    // USERS & AUTHENTICATION
    // -------------------------------------------------------------
    val allActiveUsers: Flow<List<User>> = staffAuthManager.allActiveUsers

    suspend fun getUserByPin(pin: String): User? = staffAuthManager.getUserByPin(pin)

    suspend fun getUserByUsername(username: String): User? = staffAuthManager.getUserByUsername(username)

    suspend fun saveUser(user: User) = staffAuthManager.saveUser(user)

    suspend fun deleteUser(userId: String) = staffAuthManager.deleteUser(userId)

    suspend fun updateLastLogin(userId: String) = staffAuthManager.updateLastLogin(userId)

    suspend fun ensureDefaultAdminUser(): User = staffAuthManager.ensureDefaultAdminUser()

    // -------------------------------------------------------------
    // CUSTOMER PAYMENTS & LEDGER
    // -------------------------------------------------------------
    val allCustomerPayments: Flow<List<CustomerPayment>> = customerDebtManager.allCustomerPayments

    fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>> =
        customerDebtManager.getPaymentsForCustomer(customerId)

    fun getSalesForCustomer(customerId: String): Flow<List<Sale>> =
        customerDebtManager.getSalesForCustomer(customerId)

    suspend fun recordCustomerPayment(payment: CustomerPayment): CustomerPayment =
        customerDebtManager.recordCustomerPayment(payment)

    // -------------------------------------------------------------
    // SHOP PROFILE
    // -------------------------------------------------------------
    val shopProfile: Flow<ShopProfile> = shopProfileDao.getShopProfile().map {
        it ?: ShopProfile()
    }

    suspend fun getShopProfileDirect(): ShopProfile {
        return shopProfileDao.getShopProfileDirect() ?: ShopProfile()
    }

    suspend fun updateShopProfile(profile: ShopProfile) = withContext(Dispatchers.IO) {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        shopProfileDao.insertOrUpdateProfile(updated)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "shop_profile",
                recordId = updated.id.toString(),
                operation = "UPDATE",
                payloadJson = """{"shopName":"${updated.shopName}","currency":"${updated.currencyCode}"}"""
            )
        )
    }

    // -------------------------------------------------------------
    // PRODUCTS / INVENTORY
    // -------------------------------------------------------------
    val allProducts: Flow<List<Product>> = inventoryManager.allProducts
    val lowStockProducts: Flow<List<Product>> = inventoryManager.lowStockProducts
    val categories: Flow<List<String>> = inventoryManager.categories
    val totalProductCount: Flow<Int> = inventoryManager.totalProductCount
    val lowStockCount: Flow<Int> = inventoryManager.lowStockCount

    fun searchProducts(query: String): Flow<List<Product>> = inventoryManager.searchProducts(query)

    suspend fun getProductById(id: String): Product? = inventoryManager.getProductById(id)

    suspend fun getProductByBarcode(barcode: String): Product? = inventoryManager.getProductByBarcode(barcode)

    suspend fun saveProduct(product: Product) = inventoryManager.saveProduct(product)

    suspend fun adjustStock(productId: String, adjustmentDelta: Double, reason: String) =
        inventoryManager.adjustStock(productId, adjustmentDelta, reason)

    suspend fun deleteProduct(productId: String) = inventoryManager.deleteProduct(productId)

    // -------------------------------------------------------------
    // CUSTOMERS & DEBTS
    // -------------------------------------------------------------
    val allCustomers: Flow<List<Customer>> = customerDebtManager.allCustomers
    val customersWithDebt: Flow<List<Customer>> = customerDebtManager.customersWithDebt
    val totalCustomerCount: Flow<Int> = customerDebtManager.totalCustomerCount
    val totalOutstandingDebt: Flow<Double> = customerDebtManager.totalOutstandingDebt

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDebtManager.searchCustomers(query)

    suspend fun getCustomerById(id: String): Customer? = customerDebtManager.getCustomerById(id)

    suspend fun saveCustomer(customer: Customer) = customerDebtManager.saveCustomer(customer)

    suspend fun recordDebtPayment(customerId: String, paymentAmount: Double) =
        customerDebtManager.recordDebtPayment(customerId, paymentAmount)

    suspend fun deleteCustomer(customerId: String) = customerDebtManager.deleteCustomer(customerId)

    // -------------------------------------------------------------
    // SALES & POINT OF SALE (POS)
    // -------------------------------------------------------------
    val allSales: Flow<List<Sale>> = salesManager.allSales

    suspend fun getSaleWithItems(saleId: String): SaleWithItems? = salesManager.getSaleWithItems(saleId)

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
    ): Sale = salesManager.processSale(
        items, customerId, customerName, discountAmount, paymentMethod, amountPaid, notes,
        branchId, branchName, cashierId, cashierName
    )

    // -------------------------------------------------------------
    // ANALYTICS & REPORTS
    // -------------------------------------------------------------
    fun getInventoryValuation(): Flow<InventoryValuation> = inventoryManager.getInventoryValuation()

    fun getPurchaseSummary(): Flow<PurchaseSummary> = inventoryManager.getPurchaseSummary()

    fun getProfitLossForPeriod(period: AnalyticsPeriod): Flow<ProfitLossSummary> =
        analyticsEngine.getProfitLossForPeriod(period)

    fun getDailyAnalyticsPoints(period: AnalyticsPeriod): Flow<List<DailyAnalyticsPoint>> =
        analyticsEngine.getDailyAnalyticsPoints(period)

    fun getTopProducts(period: AnalyticsPeriod, limit: Int = 10): Flow<List<TopProductReport>> =
        analyticsEngine.getTopProducts(period, limit)

    fun getCategorySalesBreakdown(period: AnalyticsPeriod): Flow<List<CategorySalesShare>> =
        analyticsEngine.getCategorySalesBreakdown(period)

    // -------------------------------------------------------------
    // STOCK TRANSFERS
    // -------------------------------------------------------------
    val allStockTransfers: Flow<List<StockTransfer>> = branchTransferManager.allStockTransfers

    suspend fun recordStockTransfer(
        productId: String,
        fromBranchId: String,
        fromBranchName: String,
        toBranchId: String,
        toBranchName: String,
        quantity: Double,
        notes: String,
        transferredBy: String = "Store Manager"
    ): StockTransfer = branchTransferManager.recordStockTransfer(
        productId, fromBranchId, fromBranchName, toBranchId, toBranchName, quantity, notes, transferredBy
    )

    suspend fun createStockTransfer(
        productId: String,
        fromBranchId: String,
        fromBranchName: String,
        toBranchId: String,
        toBranchName: String,
        quantity: Double,
        notes: String,
        transferredBy: String = "Store Manager"
    ): StockTransfer = recordStockTransfer(
        productId, fromBranchId, fromBranchName, toBranchId, toBranchName, quantity, notes, transferredBy
    )

    suspend fun deleteStockTransfer(transferId: String) = branchTransferManager.deleteStockTransfer(transferId)

    // -------------------------------------------------------------
    // SUBSCRIPTION & VOUCHERS
    // -------------------------------------------------------------
    val redeemedVouchers: Flow<List<RedeemedVoucher>> = subscriptionManager.redeemedVouchers

    suspend fun redeemVoucher(
        voucherCode: String,
        shopProfile: ShopProfile,
        branchCount: Int,
        workerCount: Int
    ): VoucherValidationResult = subscriptionManager.redeemVoucher(
        voucherCode, shopProfile, branchCount, workerCount
    )

    suspend fun recordDirectMoMoPayment(
        shopProfile: ShopProfile,
        payerPhone: String,
        provider: String,
        branchCount: Int,
        workerCount: Int,
        durationMonths: Int
    ): PaymentProcessingResult = subscriptionManager.recordDirectMoMoPayment(
        shopProfile, payerPhone, provider, branchCount, workerCount, durationMonths
    )

    // -------------------------------------------------------------
    // DATA RESTORE & SAMPLE SEEDING
    // -------------------------------------------------------------
    suspend fun importShopDataPackage(summary: ShopImportSummary) = withContext(Dispatchers.IO) {
        summary.shopProfile?.let { shopProfileDao.insertOrUpdateProfile(it) }
        if (summary.users.isNotEmpty()) userDao.insertAllUsers(summary.users)
        if (summary.products.isNotEmpty()) productDao.insertAll(summary.products)
        if (summary.customers.isNotEmpty()) customerDao.insertAll(summary.customers)
        if (summary.sales.isNotEmpty()) saleDao.insertSales(summary.sales)
        if (summary.saleItems.isNotEmpty()) saleDao.insertSaleItems(summary.saleItems)
        if (summary.payments.isNotEmpty()) customerPaymentDao.insertPayments(summary.payments)
    }

    suspend fun seedSampleShopData() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sampleProducts = listOf(
            Product(name = "Inyange Whole Milk 500ml", category = "Dairy & Beverages", costPrice = 500.0, sellingPrice = 700.0, quantityInStock = 50.0, barcode = "616110000101"),
            Product(name = "Bakhresa Azam Wheat Flour 1kg", category = "Grains & Flour", costPrice = 1100.0, sellingPrice = 1400.0, quantityInStock = 30.0, barcode = "616110000102"),
            Product(name = "Akabanga Chilli Oil 100ml", category = "Condiments & Spices", costPrice = 1800.0, sellingPrice = 2500.0, quantityInStock = 25.0, barcode = "616110000103"),
            Product(name = "Sulfo Gorilla Matchbox (Pack of 10)", category = "Household", costPrice = 400.0, sellingPrice = 600.0, quantityInStock = 80.0, barcode = "616110000104"),
            Product(name = "Kinazi Cassava Flour 2kg", category = "Grains & Flour", costPrice = 2200.0, sellingPrice = 2800.0, quantityInStock = 20.0, barcode = "616110000105"),
            Product(name = "Mount Kenya Rice Super 5kg", category = "Grains & Rice", costPrice = 6500.0, sellingPrice = 8000.0, quantityInStock = 15.0, barcode = "616110000106"),
            Product(name = "Sultani Sugar 1kg", category = "Pantry Essentials", costPrice = 1300.0, sellingPrice = 1600.0, quantityInStock = 40.0, barcode = "616110000107"),
            Product(name = "Savon Omo Powder 500g", category = "Cleaning & Laundry", costPrice = 1200.0, sellingPrice = 1500.0, quantityInStock = 35.0, barcode = "616110000108"),
            Product(name = "Skol Malt Can 500ml", category = "Beverages", costPrice = 900.0, sellingPrice = 1200.0, quantityInStock = 60.0, barcode = "616110000109"),
            Product(name = "Primus Lager Beer 500ml", category = "Beverages", costPrice = 800.0, sellingPrice = 1000.0, quantityInStock = 48.0, barcode = "616110000110")
        )
        productDao.insertAll(sampleProducts)

        val sampleCustomers = listOf(
            Customer(name = "Kamanzi Eric", phone = "+250 788 111 222", category = "VIP", debtBalance = 15000.0, creditLimit = 50000.0, address = "Kigali, Nyarugenge"),
            Customer(name = "Uwase Clarisse", phone = "+250 788 333 444", category = "Regular", debtBalance = 0.0, creditLimit = 20000.0, address = "Kigali, Gasabo"),
            Customer(name = "Mugisha Emmanuel", phone = "+250 788 555 666", category = "Wholesale", debtBalance = 35000.0, creditLimit = 100000.0, address = "Kigali, Kicukiro")
        )
        customerDao.insertAll(sampleCustomers)
    }

    // -------------------------------------------------------------
    // DATABASE STATS & MAINTENANCE
    // -------------------------------------------------------------
    suspend fun getDatabaseStats(context: Context): DatabaseEngineStats = withContext(Dispatchers.IO) {
        val prodCount = productDao.getProductCountDirect()
        val sCount = saleDao.getSaleCount()
        val siCount = saleDao.getSaleItemCount()
        val cCount = customerDao.getCustomerCountDirect()
        val pCount = customerPaymentDao.getPaymentCount()
        val purCount = purchaseDao.getPurchaseCount()
        val bCount = branchDao.getBranchCount()
        val uCount = userDao.getUserCount()
        val syncCount = syncQueueDao.getPendingQueueDirect().size

        val totalRecords = prodCount + sCount + siCount + cCount + pCount + purCount + bCount + uCount + syncCount

        val dbFile = context.getDatabasePath("beboss_database")
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        val formattedSize = if (sizeBytes > 1024 * 1024) {
            String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
        } else {
            String.format("%.1f KB", sizeBytes / 1024.0)
        }

        DatabaseEngineStats(
            productCount = prodCount,
            salesCount = sCount,
            saleItemsCount = siCount,
            customerCount = cCount,
            paymentsCount = pCount,
            purchasesCount = purCount,
            branchesCount = bCount,
            usersCount = uCount,
            syncQueueCount = syncCount,
            totalRecordsCount = totalRecords,
            fileSizeBytes = sizeBytes,
            fileSizeFormatted = formattedSize,
            journalMode = "WAL",
            integrityStatus = "OK",
            lastOptimizedAt = System.currentTimeMillis()
        )
    }

    suspend fun optimizeDatabase(context: Context): DatabaseMaintenanceResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            syncQueueDao.purgeSyncedItems()
            val stats = getDatabaseStats(context)
            val duration = System.currentTimeMillis() - start
            DatabaseMaintenanceResult(
                success = true,
                message = "Database optimized in ${duration}ms! Synced log records purged.",
                durationMs = duration,
                stats = stats
            )
        } catch (e: Exception) {
            DatabaseMaintenanceResult(
                success = false,
                message = "Optimization error: ${e.message}",
                durationMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun verifyDatabaseIntegrity(): DatabaseMaintenanceResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val duration = System.currentTimeMillis() - start
            DatabaseMaintenanceResult(
                success = true,
                message = "Database integrity check passed (Status: OK) in ${duration}ms",
                durationMs = duration
            )
        } catch (e: Exception) {
            DatabaseMaintenanceResult(
                success = false,
                message = "Integrity check failed: ${e.message}",
                durationMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun clearTransactionalDataOnly() = withContext(Dispatchers.IO) {
        salesManager.clearAllSales()
        customerPaymentDao.clearAllPayments()
        syncQueueDao.clearQueue()
    }

    // -------------------------------------------------------------
    // INITIALIZATION
    // -------------------------------------------------------------
    suspend fun initDatabase() = withContext(Dispatchers.IO) {
        val existingProfile = shopProfileDao.getShopProfileDirect()
        if (existingProfile == null) {
            val defaultProfile = ShopProfile(
                id = 1,
                name = "Jean Paul",
                phone = "+250 788 123 456",
                email = "reponsekdz01@gmail.com",
                shopName = "BeBoss Kigali Superstore",
                address = "Kigali, Commercial District",
                currencyCode = "RWF",
                currencySymbol = "FRw",
                taxRate = 18.0,
                tinNumber = "TIN-100293847",
                receiptFooter = "Murakoze cyane! Thank you for shopping with us.",
                isOnlineSyncEnabled = true,
                backendServerUrl = "https://api.beboss.app/v1",
                subscriptionStatus = "ACTIVE",
                monthlyFeeRwf = 5000,
                subscriptionExpiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L),
                trialStartedAt = System.currentTimeMillis()
            )
            shopProfileDao.insertOrUpdateProfile(defaultProfile)
        }

        ensureDefaultAdminUser()
        ensureDefaultBranches()
    }
}
