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
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.PurchaseRecord
import com.example.data.model.PurchaseSummary
import com.example.data.model.RedeemedVoucher
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopDevice
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
import com.example.data.model.LocalAuditLog
import com.example.util.ClockGuard
import com.example.util.CloudSyncManager
import com.example.util.CloudSyncReport
import com.example.util.ConflictResolutionEngine
import com.example.util.EndpointPingResult
import com.example.util.OfflineSubscriptionManager
import com.example.util.PaymentProcessingResult
import com.example.util.SecurityUtils
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import com.example.util.SupabaseAdminClient
import com.example.util.SupabaseSyncResult
import com.example.util.SyncAuditLog
import com.example.util.VoucherValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

data class BranchMergeResult(
    val mergedBranches: Int = 0,
    val mergedUsers: Int = 0,
    val mergedProducts: Int = 0,
    val mergedCustomers: Int = 0,
    val mergedSales: Int = 0,
    val mergedSaleItems: Int = 0,
    val mergedPayments: Int = 0,
    val mergedPurchases: Int = 0,
    val mergedTransfers: Int = 0,
    val conflictsResolved: Int = 0
) {
    val totalMergedCount: Int
        get() = mergedBranches + mergedUsers + mergedProducts + mergedCustomers +
            mergedSales + mergedSaleItems + mergedPayments + mergedPurchases + mergedTransfers
}

data class JoinShopCloudResult(
    val success: Boolean,
    val message: String,
    val shopName: String = "",
    val signedInUser: User? = null
)

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
    private val paymentVerificationDao = database.paymentVerificationDao()
    private val localAuditLogDao = database.localAuditLogDao()
    private val syncTombstoneDao = database.syncTombstoneDao()
    private val shopDeviceDao = database.shopDeviceDao()

    // Domain Managers
    val salesManager = SalesManager(saleDao, productDao, customerDao, syncQueueDao, database, syncTombstoneDao)
    val inventoryManager = InventoryManager(productDao, purchaseDao, syncQueueDao, syncTombstoneDao)
    val customerDebtManager = CustomerDebtManager(customerDao, customerPaymentDao, saleDao, syncQueueDao, syncTombstoneDao)
    val staffAuthManager = StaffAuthManager(userDao, syncQueueDao, syncTombstoneDao)
    val branchTransferManager = BranchTransferManager(branchDao, stockTransferDao, productDao, shopProfileDao, syncQueueDao, syncTombstoneDao)
    val subscriptionManager = SubscriptionManager(shopProfileDao, redeemedVoucherDao)
    val analyticsEngine = AnalyticsEngine(saleDao)
    val cloudSyncManager = CloudSyncManager(database)

    // Sync Audit Logs & Pending Queue & Local Audit Logs
    val syncAuditLogs: StateFlow<List<SyncAuditLog>> = cloudSyncManager.syncAuditLogs
    val pendingSyncCount: Flow<Int> = syncQueueDao.getPendingCount()
    val deadLetterSyncCount: Flow<Int> = syncQueueDao.getDeadLetterCountFlow()
    val localAuditLogs: Flow<List<LocalAuditLog>> = localAuditLogDao.getRecentLogs()

    suspend fun recordLocalAudit(actorName: String, actorRole: String, action: String, targetId: String = "", details: String = "") {
        localAuditLogDao.insertLog(
            LocalAuditLog(
                actorName = actorName,
                actorRole = actorRole,
                action = action,
                targetId = targetId,
                details = details
            )
        )
    }

    /**
     * Domain-layer RBAC guard (enforced in repository in addition to UI controls).
     */
    private suspend fun requirePermission(
        actor: User?,
        action: String,
        targetId: String = "",
        allowed: (User) -> Boolean
    ) {
        if (actor == null) return
        if (!actor.isActive || !allowed(actor)) {
            recordLocalAudit(
                actorName = actor.name,
                actorRole = actor.role.name,
                action = "RBAC_DENIED",
                targetId = targetId,
                details = "Blocked unauthorized action: $action"
            )
            throw SecurityException("Access denied: '${actor.name}' lacks permission for $action.")
        }
    }

    fun logSyncEvent(type: String, status: String, summary: String, latencyMs: Long = 0L) {
        cloudSyncManager.addAuditLog(type, status, summary, latencyMs)
    }

    suspend fun testServerPing(endpoint: String): EndpointPingResult =
        cloudSyncManager.testEndpointConnection(endpoint)

    suspend fun performSync(
        activeBranchId: String = "main_branch",
        deviceId: String = "DEV-LOCAL"
    ): CloudSyncReport =
        cloudSyncManager.syncAllDataToCloud(
            activeBranchId = activeBranchId,
            deviceId = deviceId,
            mergeRemoteSummary = { summary -> mergeShopDataPackageLww(summary) }
        )

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

    suspend fun saveBranch(branch: Branch, actor: User? = null) {
        requirePermission(actor, "MANAGE_BRANCHES", branch.id) {
            it.role == UserRole.OWNER || it.role == UserRole.MANAGER || it.canManageShopSettings
        }
        branchTransferManager.saveBranch(branch)
    }

    suspend fun deleteBranch(branchId: String, actor: User? = null) {
        requirePermission(actor, "DELETE_BRANCH", branchId) {
            it.role == UserRole.OWNER || it.canManageShopSettings || it.canDeleteRecords
        }
        branchTransferManager.deleteBranch(branchId)
    }

    suspend fun ensureDefaultBranches(): List<Branch> = branchTransferManager.ensureDefaultBranches()

    // -------------------------------------------------------------
    // USERS & AUTHENTICATION
    // -------------------------------------------------------------
    val allActiveUsers: Flow<List<User>> = staffAuthManager.allActiveUsers

    suspend fun getUserByPin(pin: String): User? = staffAuthManager.getUserByPin(pin)

    suspend fun getUserByUsername(username: String): User? = staffAuthManager.getUserByUsername(username)

    suspend fun saveUser(user: User, actor: User? = null) {
        requirePermission(actor, "MANAGE_STAFF", user.id) {
            it.role == UserRole.OWNER || it.canManageCollaborators
        }
        staffAuthManager.saveUser(user)
    }

    suspend fun deleteUser(userId: String, actor: User? = null) {
        requirePermission(actor, "DELETE_STAFF", userId) {
            it.role == UserRole.OWNER || it.canManageCollaborators || it.canDeleteRecords
        }
        staffAuthManager.deleteUser(userId)
    }

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

    suspend fun saveProduct(product: Product, actor: User? = null) {
        requirePermission(actor, "MANAGE_INVENTORY", product.id) { it.canManageInventory }
        inventoryManager.saveProduct(product)
    }

    suspend fun adjustStock(productId: String, adjustmentDelta: Double, reason: String, actor: User? = null) {
        requirePermission(actor, "ADJUST_STOCK", productId) { it.canManageInventory }
        inventoryManager.adjustStock(productId, adjustmentDelta, reason)
    }

    suspend fun deleteProduct(productId: String, actor: User? = null) {
        requirePermission(actor, "DELETE_PRODUCT", productId) {
            it.canDeleteRecords || it.canManageInventory
        }
        inventoryManager.deleteProduct(productId)
    }

    suspend fun restoreProduct(productId: String, actor: User? = null) {
        requirePermission(actor, "RESTORE_PRODUCT", productId) { it.canManageInventory }
        inventoryManager.restoreProduct(productId)
    }

    // -------------------------------------------------------------
    // CUSTOMERS & DEBTS
    // -------------------------------------------------------------
    val allCustomers: Flow<List<Customer>> = customerDebtManager.allCustomers
    val customersWithDebt: Flow<List<Customer>> = customerDebtManager.customersWithDebt
    val totalCustomerCount: Flow<Int> = customerDebtManager.totalCustomerCount
    val totalOutstandingDebt: Flow<Double> = customerDebtManager.totalOutstandingDebt

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDebtManager.searchCustomers(query)

    suspend fun getCustomerById(id: String): Customer? = customerDebtManager.getCustomerById(id)

    suspend fun saveCustomer(customer: Customer, actor: User? = null) {
        requirePermission(actor, "MANAGE_CUSTOMER", customer.id) { it.canManageCustomers }
        customerDebtManager.saveCustomer(customer)
    }

    suspend fun recordDebtPayment(customerId: String, paymentAmount: Double, actor: User? = null) {
        requirePermission(actor, "COLLECT_DEBT", customerId) {
            it.canCollectDebt || it.canManageCustomers
        }
        customerDebtManager.recordDebtPayment(customerId, paymentAmount)
    }

    suspend fun deleteCustomer(customerId: String, actor: User? = null) {
        requirePermission(actor, "DELETE_CUSTOMER", customerId) {
            it.canDeleteRecords || it.canManageCustomers
        }
        customerDebtManager.deleteCustomer(customerId)
    }

    suspend fun restoreCustomer(customerId: String, actor: User? = null) {
        requirePermission(actor, "RESTORE_CUSTOMER", customerId) { it.canManageCustomers }
        customerDebtManager.restoreCustomer(customerId)
    }

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
    // SUBSCRIPTION, VOUCHERS & SUPABASE ADMIN LICENSING
    // -------------------------------------------------------------
    val redeemedVouchers: Flow<List<RedeemedVoucher>> = subscriptionManager.redeemedVouchers
    val allPaymentRequests: Flow<List<PaymentVerificationRequest>> = paymentVerificationDao.getAllRequests()

    // -------------------------------------------------------------
    // SINGLE-SHOP MULTI-DEVICE MANAGEMENT (+2,000 FRw/month/device)
    // -------------------------------------------------------------
    val allShopDevices: Flow<List<ShopDevice>> = shopDeviceDao.getAllDevices()

    suspend fun getAllShopDevicesList(): List<ShopDevice> = shopDeviceDao.getAllDevicesList()

    suspend fun getActiveDevicesCount(): Int = shopDeviceDao.getActiveDevicesCount().coerceAtLeast(1)

    suspend fun ensureOwnerPrimaryDevice(
        deviceId: String,
        shopProfile: ShopProfile,
        ownerUser: User?,
        mainBranch: Branch?
    ): ShopDevice = withContext(Dispatchers.IO) {
        val existing = shopDeviceDao.getAllDevicesList()
        val existingPrimary = existing.firstOrNull { it.isOwnerPrimary }
        if (existingPrimary != null) return@withContext existingPrimary

        val username = ownerUser?.username?.ifBlank { "owner" } ?: "owner"
        val phone = ownerUser?.phone?.ifBlank { shopProfile.phone } ?: shopProfile.phone
        val shopSyncKey = SupabaseAdminClient.computeShopSyncKey(username, phone)
        val primary = ShopDevice(
            id = "${shopSyncKey}::PRIMARY",
            shopSyncKey = shopSyncKey,
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL?.ifBlank { "Owner Terminal" } ?: "Owner Terminal",
            joinCode = "OWNER",
            assignedUserId = ownerUser?.id.orEmpty(),
            assignedUsername = username,
            assignedUserName = ownerUser?.name?.ifBlank { shopProfile.name } ?: shopProfile.name,
            role = ownerUser?.role?.name ?: "OWNER",
            branchId = mainBranch?.id ?: "main_branch",
            branchName = mainBranch?.name ?: "Main Store",
            isOwnerPrimary = true,
            isActive = true,
            monthlyFeeAddonRwf = 0
        )
        shopDeviceDao.insertOrUpdate(primary)
        primary
    }

    suspend fun addSecondaryShopDevice(
        deviceName: String,
        assignedUser: User,
        branch: Branch?,
        actor: User? = null
    ): ShopDevice = withContext(Dispatchers.IO) {
        requirePermission(actor, "ADD_SHOP_DEVICE", deviceName) {
            it.role == UserRole.OWNER || it.role == UserRole.MANAGER || it.canManageShopSettings
        }
        val profile = getShopProfileDirect()
        val allUsers = userDao.getAllUsersDirect()
        val ownerUser = allUsers.firstOrNull { it.role == UserRole.OWNER } ?: allUsers.firstOrNull()
        val shopSyncKey = SupabaseAdminClient.computeShopSyncKey(
            ownerUser?.username.orEmpty(),
            ownerUser?.phone?.ifBlank { profile.phone } ?: profile.phone
        )
        val codeSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.ROOT)
        val joinCode = "BB-$codeSuffix"
        val now = System.currentTimeMillis()
        val newDev = ShopDevice(
            id = UUID.randomUUID().toString(),
            shopSyncKey = shopSyncKey,
            deviceId = "",
            deviceName = deviceName.trim().ifBlank { "${assignedUser.name}'s Terminal" },
            joinCode = joinCode,
            assignedUserId = assignedUser.id,
            assignedUsername = assignedUser.username.trim().lowercase(Locale.ROOT),
            assignedUserName = assignedUser.name,
            role = assignedUser.role.name,
            branchId = branch?.id ?: assignedUser.assignedBranchId.ifBlank { "main_branch" },
            branchName = branch?.name ?: assignedUser.assignedBranchName.ifBlank { "Main Store" },
            isOwnerPrimary = false,
            isActive = true,
            monthlyFeeAddonRwf = OfflineSubscriptionManager.PER_EXTRA_DEVICE_FEE_RWF,
            syncedToSupabase = false,
            createdAt = now,
            updatedAt = now
        )
        shopDeviceDao.insertOrUpdate(newDev)

        val branchesCount = branchDao.getBranchCount().coerceAtLeast(1)
        val workersCount = allUsers.count { it.isActive }.coerceAtLeast(1)
        val devicesCount = shopDeviceDao.getActiveDevicesCount().coerceAtLeast(1)
        val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = branchesCount,
            workerCount = workersCount,
            durationMonths = 1,
            deviceCount = devicesCount
        )
        val updatedProf = profile.copy(
            monthlyFeeRwf = newPricing.monthlySubtotal,
            deviceCountLimit = maxOf(profile.deviceCountLimit, devicesCount),
            updatedAt = now
        )
        shopProfileDao.insertOrUpdateProfile(updatedProf)

        if (SupabaseAdminClient.isConfigured()) {
            val pushed = SupabaseAdminClient.upsertShopDevice(shopSyncKey, updatedProf.shopName, newDev)
            if (pushed) {
                shopDeviceDao.insertOrUpdate(newDev.copy(syncedToSupabase = true))
            }
            SupabaseAdminClient.upsertShopAccount(
                shopProfile = updatedProf,
                ownerUser = ownerUser,
                branchCount = branchesCount,
                workerCount = workersCount,
                deviceCount = devicesCount
            )
        }
        recordLocalAudit(
            actorName = actor?.name ?: "Owner",
            actorRole = actor?.role?.name ?: "OWNER",
            action = "ADD_SHOP_DEVICE",
            targetId = newDev.deviceName,
            details = "Added paired device for @${newDev.assignedUsername} (Join Code: $joinCode, +2,000 FRw/mo)"
        )
        newDev
    }

    suspend fun toggleShopDeviceActive(deviceId: String, isActive: Boolean, actor: User? = null) =
        withContext(Dispatchers.IO) {
            requirePermission(actor, "TOGGLE_SHOP_DEVICE", deviceId) {
                it.role == UserRole.OWNER || it.canManageShopSettings
            }
            val existing = shopDeviceDao.getDeviceById(deviceId) ?: return@withContext
            if (existing.isOwnerPrimary) return@withContext
            val now = System.currentTimeMillis()
            val updated = existing.copy(isActive = isActive, syncedToSupabase = false, updatedAt = now)
            shopDeviceDao.insertOrUpdate(updated)

            val profile = getShopProfileDirect()
            val users = userDao.getAllUsersDirect()
            val ownerUser = users.firstOrNull { it.role == UserRole.OWNER } ?: users.firstOrNull()
            val devicesCount = shopDeviceDao.getActiveDevicesCount().coerceAtLeast(1)
            val branchesCount = branchDao.getBranchCount().coerceAtLeast(1)
            val workersCount = users.count { it.isActive }.coerceAtLeast(1)
            val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(branchesCount, workersCount, 1, devicesCount)
            val updatedProf = profile.copy(monthlyFeeRwf = newPricing.monthlySubtotal, updatedAt = now)
            shopProfileDao.insertOrUpdateProfile(updatedProf)

            if (SupabaseAdminClient.isConfigured()) {
                val ok = SupabaseAdminClient.upsertShopDevice(existing.shopSyncKey, profile.shopName, updated)
                if (ok) shopDeviceDao.insertOrUpdate(updated.copy(syncedToSupabase = true))
                SupabaseAdminClient.upsertShopAccount(updatedProf, ownerUser, branchesCount, workersCount, devicesCount)
            }
        }

    suspend fun deleteShopDevice(deviceRecordId: String, actor: User? = null) = withContext(Dispatchers.IO) {
        requirePermission(actor, "DELETE_SHOP_DEVICE", deviceRecordId) {
            it.role == UserRole.OWNER || it.canManageShopSettings
        }
        val existing = shopDeviceDao.getDeviceById(deviceRecordId) ?: return@withContext
        if (existing.isOwnerPrimary) return@withContext
        shopDeviceDao.deleteById(deviceRecordId)
        val profile = getShopProfileDirect()
        val users = userDao.getAllUsersDirect()
        val ownerUser = users.firstOrNull { it.role == UserRole.OWNER } ?: users.firstOrNull()
        val devicesCount = shopDeviceDao.getActiveDevicesCount().coerceAtLeast(1)
        val branchesCount = branchDao.getBranchCount().coerceAtLeast(1)
        val workersCount = users.count { it.isActive }.coerceAtLeast(1)
        val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(branchesCount, workersCount, 1, devicesCount)
        val updatedProf = profile.copy(monthlyFeeRwf = newPricing.monthlySubtotal, updatedAt = System.currentTimeMillis())
        shopProfileDao.insertOrUpdateProfile(updatedProf)
        if (SupabaseAdminClient.isConfigured()) {
            SupabaseAdminClient.deleteShopDeviceInCloud(existing.shopSyncKey, deviceRecordId)
            SupabaseAdminClient.upsertShopAccount(updatedProf, ownerUser, branchesCount, workersCount, devicesCount)
        }
    }

    /**
     * Pairs a new Android phone/tablet to an existing shop in Supabase using the shop owner's
     * username, registered phone, and Device Join Code generated by the shop owner (+2,000 FRw/device/mo).
     * Pulls all shop products, branches, sales, customers, users, and subscription state automatically.
     */
    suspend fun joinExistingShopFromCloud(
        context: Context,
        ownerUsername: String,
        ownerPhone: String,
        deviceJoinCode: String,
        thisDeviceName: String,
        assignedPin: String,
        assignedPassword: String
    ): JoinShopCloudResult = withContext(Dispatchers.IO) {
        if (!SupabaseAdminClient.isConfigured()) {
            return@withContext JoinShopCloudResult(
                success = false,
                message = "Supabase cloud configuration is required to pair this device."
            )
        }
        try {
            val cleanUser = ownerUsername.trim().lowercase(Locale.ROOT)
            val cleanPhone = ownerPhone.trim()
            val cleanJoin = deviceJoinCode.trim().uppercase(Locale.ROOT)
            val shopSyncKey = SupabaseAdminClient.computeShopSyncKey(cleanUser, cleanPhone)

            val remoteShop = SupabaseAdminClient.fetchShopAccountById(shopSyncKey)
                ?: return@withContext JoinShopCloudResult(
                    success = false,
                    message = "Shop account '@$cleanUser' ($cleanPhone) not found in BeBoss Cloud. Ask owner to create account & sync online first."
                )

            val remoteDevices = SupabaseAdminClient.fetchShopDevicesForShop(shopSyncKey)
            val matchedDevice = remoteDevices.firstOrNull {
                it.joinCode.equals(cleanJoin, ignoreCase = true) && it.isActive
            } ?: return@withContext JoinShopCloudResult(
                success = false,
                message = "Invalid or inactive Join Code '$cleanJoin'. Ask the shop owner to add this device in Settings -> Shop Devices."
            )

            val hardwareDeviceId = SupabaseAdminClient.getOrCreateDeviceId(context)
            val appCfg = SupabaseAdminClient.fetchPublicAppConfig()
            if (appCfg != null) {
                OfflineSubscriptionManager.saveDynamicPaymentConfig(context, appCfg)
            }

            // Pull all existing branch snapshots for this shop and merge into local SQLite
            val snapshots = SupabaseAdminClient.fetchShopBranchSnapshots(shopSyncKey)
            for (snap in snapshots) {
                try {
                    val decoded = ShopDataTransferManager.parseShopPackageString(snap.payloadToken)
                    mergeShopDataPackageLww(decoded)
                } catch (_: Exception) {
                }
            }

            // Ensure ShopProfile exists and mirrors cloud subscription state
            val existingProfile = shopProfileDao.getShopProfileDirect() ?: ShopProfile()
            val now = System.currentTimeMillis()
            val syncedProfile = existingProfile.copy(
                id = 1L,
                shopName = remoteShop.shopName,
                name = remoteShop.ownerName.ifBlank { existingProfile.name },
                phone = remoteShop.ownerPhone.ifBlank { cleanPhone },
                subscriptionStatus = remoteShop.subscriptionStatus,
                subscriptionExpiresAt = remoteShop.subscriptionExpiresAt,
                monthlyFeeRwf = remoteShop.monthlyFeeRwf,
                deviceCountLimit = remoteShop.deviceLimit,
                adminWhatsAppNumber = remoteShop.adminWhatsApp.ifBlank { appCfg?.adminWhatsAppNumber.orEmpty() },
                autoUnlockedAt = remoteShop.autoUnlockedAt,
                lastAppliedCloudUnlockAt = remoteShop.autoUnlockedAt,
                lastSyncedAt = now,
                updatedAt = now
            )
            shopProfileDao.insertOrUpdateProfile(syncedProfile)

            // Sync remote devices list locally and register this hardware ID on the matched slot
            for (rd in remoteDevices) {
                val isMine = rd.id == matchedDevice.id
                val localDev = ShopDevice(
                    id = rd.id,
                    shopSyncKey = rd.shopSyncKey,
                    deviceId = if (isMine) hardwareDeviceId else rd.deviceId,
                    deviceName = if (isMine) thisDeviceName.trim().ifBlank { rd.deviceName } else rd.deviceName,
                    joinCode = rd.joinCode,
                    assignedUserId = rd.assignedUserId,
                    assignedUsername = rd.assignedUsername,
                    assignedUserName = rd.assignedUserName,
                    role = rd.role,
                    branchId = rd.branchId,
                    branchName = rd.branchName,
                    isOwnerPrimary = rd.isOwnerPrimary,
                    isActive = rd.isActive,
                    monthlyFeeAddonRwf = rd.monthlyFeeAddonRwf,
                    lastSyncedAt = if (isMine) now else rd.lastSyncedAt,
                    syncedToSupabase = true,
                    createdAt = rd.createdAt,
                    updatedAt = if (isMine) now else rd.updatedAt
                )
                shopDeviceDao.insertOrUpdate(localDev)
                if (isMine) {
                    SupabaseAdminClient.upsertShopDevice(shopSyncKey, syncedProfile.shopName, localDev)
                }
            }

            // Create / update the assigned staff user on this terminal with the operator's chosen PIN & password
            val parsedRole = try { UserRole.valueOf(matchedDevice.role) } catch (_: Exception) { UserRole.CASHIER }
            val existingUser = userDao.getUserById(matchedDevice.assignedUserId)
                ?: userDao.getUserByUsername(matchedDevice.assignedUsername)
            val baseUser = User.defaultForRole(
                name = matchedDevice.assignedUserName.ifBlank { matchedDevice.assignedUsername },
                username = matchedDevice.assignedUsername.ifBlank { "staff" },
                role = parsedRole,
                pin = "",
                phone = ""
            )
            val pairedUser = (existingUser ?: baseUser).copy(
                id = existingUser?.id ?: matchedDevice.assignedUserId.ifBlank { baseUser.id },
                name = matchedDevice.assignedUserName.ifBlank { existingUser?.name ?: "Staff" },
                username = matchedDevice.assignedUsername.ifBlank { existingUser?.username ?: "staff" },
                role = parsedRole,
                assignedBranchId = matchedDevice.branchId.ifBlank { "main_branch" },
                assignedBranchName = matchedDevice.branchName.ifBlank { "Main Store" },
                pinHash = SecurityUtils.hashPin(assignedPin),
                password = SecurityUtils.hashPassword(assignedPassword),
                isActive = true,
                updatedAt = now
            )
            userDao.insertUser(pairedUser)
            ensureDefaultBranches()

            JoinShopCloudResult(
                success = true,
                message = "Device paired to '${remoteShop.shopName}'! All products, branches, sales & customers synced automatically.",
                shopName = remoteShop.shopName,
                signedInUser = pairedUser
            )
        } catch (e: Exception) {
            JoinShopCloudResult(
                success = false,
                message = "Cloud pairing failed: ${e.message}"
            )
        }
    }

    suspend fun redeemVoucher(
        voucherCode: String,
        shopProfile: ShopProfile,
        branchCount: Int,
        workerCount: Int,
        ownerUsername: String = "",
        ownerPhoneOverride: String = "",
        ownerUser: User? = null
    ): VoucherValidationResult {
        val cleanUpper = voucherCode.trim().uppercase()
        val effectiveUser = ownerUsername.ifBlank { ownerUser?.username.orEmpty() }
        val effectivePhone = ownerPhoneOverride.ifBlank { shopProfile.phone }

        val cloudRecord = if (SupabaseAdminClient.isConfigured()) {
            SupabaseAdminClient.verifyAndMarkCodeRedeemedInCloud(
                unlockCode = cleanUpper,
                username = effectiveUser,
                phone = effectivePhone
            )
        } else null

        if (cloudRecord?.status == "REVOKED") {
            return VoucherValidationResult(
                isValid = false,
                daysToAdd = 0,
                message = "This unlock code was revoked by BeBoss Admin and cannot be activated.",
                planName = ""
            )
        }

        val res = subscriptionManager.redeemVoucher(
            voucherCode = voucherCode,
            shopProfile = shopProfile,
            branchCount = branchCount,
            workerCount = workerCount,
            ownerUsername = effectiveUser,
            ownerPhoneOverride = effectivePhone
        )
        if (res.isValid) {
            val existingReqs = paymentVerificationDao.getAllRequestsList()
            existingReqs.filter { it.generatedUnlockCode.equals(cleanUpper, ignoreCase = true) }.forEach { req ->
                paymentVerificationDao.updateCloudApproval(req.id, "REDEEMED", cleanUpper, req.approvalMode)
            }
            if (SupabaseAdminClient.isConfigured()) {
                val updatedProf = shopProfileDao.getShopProfileDirect() ?: shopProfile
                val activeDevices = getActiveDevicesCount()
                SupabaseAdminClient.upsertShopAccount(
                    shopProfile = updatedProf,
                    ownerUser = ownerUser,
                    branchCount = branchCount,
                    workerCount = workerCount,
                    deviceCount = activeDevices
                )
            }
        }
        return res
    }

    suspend fun submitPaymentVerificationRequest(
        request: PaymentVerificationRequest,
        shopProfile: ShopProfile,
        ownerUser: User?
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. Always persist offline-first in Room
        paymentVerificationDao.insertOrUpdate(request)

        // 2. Sync to Supabase if configured & reachable
        if (SupabaseAdminClient.isConfigured()) {
            val uploaded = SupabaseAdminClient.submitPaymentRequest(request)
            SupabaseAdminClient.upsertShopAccount(
                shopProfile = shopProfile,
                ownerUser = ownerUser,
                branchCount = request.branchCount,
                workerCount = request.workerCount,
                deviceCount = request.deviceCount.coerceAtLeast(1)
            )
            if (uploaded) {
                paymentVerificationDao.markSynced(request.id)
            }
            return@withContext uploaded
        }
        false
    }

    suspend fun syncPendingAdminDataWithSupabase(
        shopProfile: ShopProfile,
        ownerUser: User?,
        branchCount: Int,
        workerCount: Int,
        deviceCount: Int = 1,
        context: Context? = null
    ): SupabaseSyncResult = withContext(Dispatchers.IO) {
        if (!SupabaseAdminClient.isConfigured()) {
            return@withContext SupabaseSyncResult(
                success = false,
                message = "Supabase credentials not set in AI Studio Secrets. Verification saved offline."
            )
        }
        try {
            val username = ownerUser?.username?.trim().orEmpty().ifBlank { "owner" }
            val phone = ownerUser?.phone?.ifBlank { shopProfile.phone } ?: shopProfile.phone
            val shopSyncKey = SupabaseAdminClient.computeShopSyncKey(username, phone)
            var directAutoUnlocked = false

            var effectiveProfile = shopProfile

            // 1. Fetch Admin's global live WhatsApp & MoMo/Airtel USSD numbers
            val appCfg = SupabaseAdminClient.fetchPublicAppConfig()
            if (appCfg != null) {
                OfflineSubscriptionManager.saveDynamicPaymentConfig(context, appCfg)
                if (appCfg.adminWhatsAppNumber.isNotBlank() && appCfg.adminWhatsAppNumber != effectiveProfile.adminWhatsAppNumber) {
                    effectiveProfile = effectiveProfile.copy(adminWhatsAppNumber = appCfg.adminWhatsAppNumber)
                    shopProfileDao.insertOrUpdateProfile(effectiveProfile)
                }
            }

            // 2. Fetch remote shop account & apply Admin direct auto-unlock / expiry extension / lock
            val remoteShop = SupabaseAdminClient.fetchShopAccountById(shopSyncKey)
            if (remoteShop != null) {
                ClockGuard.observeTrustedEpoch(remoteShop.updatedAt)
                val isDirectUnlockUpdate = remoteShop.autoUnlockedAt > effectiveProfile.lastAppliedCloudUnlockAt
                val isRemoteExtension = remoteShop.subscriptionStatus == ShopProfile.STATUS_ACTIVE &&
                    remoteShop.subscriptionExpiresAt > effectiveProfile.subscriptionExpiresAt + 60_000L
                val isRemoteLock = (remoteShop.subscriptionStatus == ShopProfile.STATUS_SUSPENDED ||
                    remoteShop.subscriptionStatus == ShopProfile.STATUS_EXPIRED) &&
                    remoteShop.updatedAt > effectiveProfile.updatedAt &&
                    remoteShop.autoUnlockedAt <= effectiveProfile.lastAppliedCloudUnlockAt

                if (isDirectUnlockUpdate || isRemoteExtension || isRemoteLock || remoteShop.updatedAt > effectiveProfile.updatedAt) {
                    if (isDirectUnlockUpdate || isRemoteExtension) directAutoUnlocked = true
                    effectiveProfile = effectiveProfile.copy(
                        subscriptionStatus = if (isDirectUnlockUpdate || isRemoteExtension) ShopProfile.STATUS_ACTIVE else remoteShop.subscriptionStatus,
                        subscriptionExpiresAt = if (isRemoteLock) remoteShop.subscriptionExpiresAt else maxOf(effectiveProfile.subscriptionExpiresAt, remoteShop.subscriptionExpiresAt),
                        deviceCountLimit = maxOf(effectiveProfile.deviceCountLimit, remoteShop.deviceLimit),
                        adminWhatsAppNumber = remoteShop.adminWhatsApp.ifBlank { effectiveProfile.adminWhatsAppNumber },
                        autoUnlockedAt = maxOf(effectiveProfile.autoUnlockedAt, remoteShop.autoUnlockedAt),
                        lastAppliedCloudUnlockAt = if (isDirectUnlockUpdate) remoteShop.autoUnlockedAt else effectiveProfile.lastAppliedCloudUnlockAt,
                        lastPaymentRef = if (isDirectUnlockUpdate) "ADMIN-DIRECT-UNLOCK" else effectiveProfile.lastPaymentRef,
                        updatedAt = maxOf(effectiveProfile.updatedAt, remoteShop.updatedAt)
                    )
                    shopProfileDao.insertOrUpdateProfile(effectiveProfile)
                }
            }

            // 3. Bidirectional Shop Devices sync (+2,000 FRw/device billing)
            val remoteDevices = SupabaseAdminClient.fetchShopDevicesForShop(shopSyncKey)
            for (rd in remoteDevices) {
                val localDev = shopDeviceDao.getDeviceById(rd.id)
                if (localDev == null || rd.updatedAt > localDev.updatedAt) {
                    shopDeviceDao.insertOrUpdate(
                        ShopDevice(
                            id = rd.id,
                            shopSyncKey = rd.shopSyncKey,
                            deviceId = rd.deviceId,
                            deviceName = rd.deviceName,
                            joinCode = rd.joinCode,
                            assignedUserId = rd.assignedUserId,
                            assignedUsername = rd.assignedUsername,
                            assignedUserName = rd.assignedUserName,
                            role = rd.role,
                            branchId = rd.branchId,
                            branchName = rd.branchName,
                            isOwnerPrimary = rd.isOwnerPrimary,
                            isActive = rd.isActive,
                            monthlyFeeAddonRwf = rd.monthlyFeeAddonRwf,
                            lastSyncedAt = rd.lastSyncedAt,
                            syncedToSupabase = true,
                            createdAt = rd.createdAt,
                            updatedAt = rd.updatedAt
                        )
                    )
                }
            }
            val effectiveDeviceCount = maxOf(deviceCount, shopDeviceDao.getActiveDevicesCount()).coerceAtLeast(1)
            for (ld in shopDeviceDao.getAllDevicesList()) {
                if (!ld.syncedToSupabase) {
                    if (SupabaseAdminClient.upsertShopDevice(shopSyncKey, effectiveProfile.shopName, ld)) {
                        shopDeviceDao.insertOrUpdate(ld.copy(syncedToSupabase = true))
                    }
                }
            }

            // 4. Pull Admin-updated staff user roles / active states
            val remoteUsers = SupabaseAdminClient.fetchShopUsersForShop(shopSyncKey)
            for (ru in remoteUsers) {
                val localUser = userDao.getUserById(ru.userId) ?: continue
                if (ru.updatedAt > localUser.updatedAt) {
                    val parsedRole = try { UserRole.valueOf(ru.role) } catch (_: Exception) { localUser.role }
                    userDao.updateUser(
                        localUser.copy(
                            role = parsedRole,
                            isActive = ru.isActive,
                            assignedBranchId = ru.assignedBranchId.ifBlank { localUser.assignedBranchId },
                            assignedBranchName = ru.assignedBranchName.ifBlank { localUser.assignedBranchName },
                            updatedAt = ru.updatedAt
                        )
                    )
                }
            }

            // 5. Push shop account & user directory cleanly to Supabase
            SupabaseAdminClient.upsertShopAccount(
                shopProfile = effectiveProfile,
                ownerUser = ownerUser,
                branchCount = branchCount,
                workerCount = workerCount,
                deviceCount = effectiveDeviceCount
            )
            SupabaseAdminClient.upsertShopUsers(
                shopSyncKey = shopSyncKey,
                shopName = effectiveProfile.shopName,
                users = userDao.getAllUsersDirect()
            )

            // 6. Push all unsynced offline payment requests
            val unsynced = paymentVerificationDao.getUnsyncedRequests()
            var pushed = 0
            for (req in unsynced) {
                if (SupabaseAdminClient.submitPaymentRequest(req)) {
                    paymentVerificationDao.markSynced(req.id)
                    pushed++
                }
            }

            // 7. Pull approved requests (either BB2 Unlock Code OR Admin Direct Auto-Unlock)
            val approvedDtos = SupabaseAdminClient.fetchApprovedRequestsForUser(username, phone)
            for (dto in approvedDtos) {
                val local = paymentVerificationDao.getRequestById(dto.id)
                if (local != null) {
                    paymentVerificationDao.updateCloudApproval(
                        id = dto.id,
                        status = dto.status,
                        unlockCode = dto.generatedUnlockCode,
                        approvalMode = dto.approvalMode,
                        updatedAt = dto.updatedAt
                    )
                } else {
                    paymentVerificationDao.insertOrUpdate(
                        PaymentVerificationRequest(
                            id = dto.id,
                            shopName = dto.shopName,
                            ownerUsername = dto.ownerUsername,
                            ownerPhone = dto.ownerPhone,
                            deviceChallenge = dto.deviceChallenge,
                            provider = dto.provider,
                            ussdCode = dto.ussdCode,
                            payerPhone = dto.payerPhone,
                            payerFullName = dto.payerFullName,
                            amountRwf = dto.amountRwf,
                            durationMonths = dto.durationMonths,
                            branchCount = dto.branchCount,
                            workerCount = dto.workerCount,
                            deviceCount = dto.deviceCount,
                            telcoReference = dto.telcoReference,
                            status = dto.status,
                            approvalMode = dto.approvalMode,
                            generatedUnlockCode = dto.generatedUnlockCode,
                            syncedToSupabase = true,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt
                        )
                    )
                }

                // If Admin chose Option 2 (DIRECT_AUTO_UNLOCK), ensure local ShopProfile is unlocked even without a BB2 code!
                if (dto.approvalMode == "DIRECT_AUTO_UNLOCK" && dto.updatedAt > effectiveProfile.lastAppliedCloudUnlockAt) {
                    val now = System.currentTimeMillis()
                    val daysAdded = dto.durationMonths.coerceAtLeast(1) * 30L
                    val minExpiry = maxOf(effectiveProfile.subscriptionExpiresAt, now) + daysAdded * 86_400_000L
                    val cloudExpiry = remoteShop?.subscriptionExpiresAt ?: 0L
                    effectiveProfile = effectiveProfile.copy(
                        subscriptionStatus = ShopProfile.STATUS_ACTIVE,
                        subscriptionExpiresAt = maxOf(minExpiry, cloudExpiry),
                        lastPaymentRef = "ADMIN-DIRECT-UNLOCK",
                        lastPaymentAmount = dto.amountRwf,
                        lastPaymentDate = dto.updatedAt,
                        deviceCountLimit = maxOf(effectiveProfile.deviceCountLimit, dto.deviceCount),
                        autoUnlockedAt = dto.updatedAt,
                        lastAppliedCloudUnlockAt = dto.updatedAt,
                        updatedAt = now
                    )
                    shopProfileDao.insertOrUpdateProfile(effectiveProfile)
                    directAutoUnlocked = true
                }
            }

            val codeReadyCount = approvedDtos.count { it.generatedUnlockCode.isNotBlank() }
            val msg = when {
                directAutoUnlocked -> "Subscription Auto-Unlocked directly by BeBoss Admin! All features are now active."
                codeReadyCount > 0 -> "Synced $pushed request(s). $codeReadyCount approved unlock code(s) ready from Admin!"
                else -> "Synced shop account, $effectiveDeviceCount device(s), and $pushed request(s) with Supabase Cloud."
            }
            SupabaseSyncResult(
                success = true,
                message = msg,
                approvedCodesFound = approvedDtos,
                directAutoUnlocked = directAutoUnlocked
            )
        } catch (e: Exception) {
            SupabaseSyncResult(success = false, message = "Supabase sync pending: ${e.message}")
        }
    }

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
    // DATA RESTORE & COMPLETE BRANCH SNAPSHOT HELPERS
    // -------------------------------------------------------------
    suspend fun getAllSaleItemsList(): List<SaleItem> = withContext(Dispatchers.IO) {
        saleDao.getAllSaleItemsList()
    }

    suspend fun getAllPurchasesList(): List<PurchaseRecord> = withContext(Dispatchers.IO) {
        purchaseDao.getAllPurchasesList()
    }

    suspend fun getAllTransfersList(): List<StockTransfer> = withContext(Dispatchers.IO) {
        stockTransferDao.getAllTransfersList()
    }

    suspend fun getAllTombstonesList(): List<com.example.data.model.SyncTombstone> = withContext(Dispatchers.IO) {
        syncTombstoneDao.getAllTombstonesList()
    }

    suspend fun importShopDataPackage(
        summary: ShopImportSummary,
        actor: User? = null
    ): BranchMergeResult = withContext(Dispatchers.IO) {
        requirePermission(actor, "IMPORT_DATA", summary.shopName) {
            it.canExportImportData || it.role == UserRole.OWNER || it.role == UserRole.MANAGER
        }
        mergeShopDataPackageLww(summary)
    }

    /**
     * Causal + Ledger-CRDT conflict-resolving merge across branches & devices.
     */
    suspend fun mergeShopDataPackageLww(summary: ShopImportSummary): BranchMergeResult =
        ConflictResolutionEngine.mergePackage(database, summary, "DataShare")

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

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        val formattedSize = if (sizeBytes > 1024 * 1024) {
            String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
        } else {
            String.format("%.1f KB", sizeBytes / 1024.0)
        }

        val writableDb = database.openHelper.writableDatabase
        val journalMode = queryPragmaString(writableDb, "PRAGMA journal_mode").uppercase().ifBlank { "WAL" }
        val pageSize = queryPragmaLong(writableDb, "PRAGMA page_size").takeIf { it > 0L } ?: 4096L
        val pageCount = queryPragmaLong(writableDb, "PRAGMA page_count")

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
            journalMode = journalMode,
            pageSize = pageSize,
            pageCount = pageCount,
            integrityStatus = "VERIFIED",
            lastOptimizedAt = System.currentTimeMillis()
        )
    }

    suspend fun optimizeDatabase(context: Context): DatabaseMaintenanceResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            syncQueueDao.purgeSyncedItems()
            val writableDb = database.openHelper.writableDatabase
            writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            writableDb.query("PRAGMA optimize").use { it.moveToFirst() }
            val stats = getDatabaseStats(context)
            val duration = System.currentTimeMillis() - start
            recordLocalAudit("System", "MAINTENANCE", "DB_OPTIMIZE", "beboss_database.db", "WAL checkpoint & PRAGMA optimize in ${duration}ms")
            DatabaseMaintenanceResult(
                success = true,
                message = "SQLite WAL checkpoint & query planner optimized in ${duration}ms!",
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
            val writableDb = database.openHelper.writableDatabase
            val status = queryPragmaString(writableDb, "PRAGMA integrity_check").ifBlank { "unknown" }
            val duration = System.currentTimeMillis() - start
            val ok = status.equals("ok", ignoreCase = true)
            recordLocalAudit("System", "MAINTENANCE", "DB_INTEGRITY", "beboss_database.db", "PRAGMA integrity_check = $status (${duration}ms)")
            DatabaseMaintenanceResult(
                success = ok,
                message = if (ok) "SQLite PRAGMA integrity_check passed (OK) in ${duration}ms" else "Integrity check reported: $status",
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

    private fun queryPragmaString(db: androidx.sqlite.db.SupportSQLiteDatabase, sql: String): String {
        return try {
            db.query(sql).use { cursor ->
                if (cursor.moveToFirst() && cursor.columnCount > 0) cursor.getString(0).orEmpty() else ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun queryPragmaLong(db: androidx.sqlite.db.SupportSQLiteDatabase, sql: String): Long {
        return try {
            db.query(sql).use { cursor ->
                if (cursor.moveToFirst() && cursor.columnCount > 0) cursor.getLong(0) else 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun clearTransactionalDataOnly() = withContext(Dispatchers.IO) {
        clearAllSalesData()
    }

    suspend fun clearAllProducts() = withContext(Dispatchers.IO) {
        inventoryManager.clearAllProducts()
    }

    suspend fun clearAllCustomers() = withContext(Dispatchers.IO) {
        customerDebtManager.clearAllCustomers()
    }

    suspend fun deleteSale(saleId: String, actor: User? = null) = withContext(Dispatchers.IO) {
        requirePermission(actor, "DELETE_SALE", saleId) {
            it.canDeleteRecords || it.role == UserRole.OWNER || it.role == UserRole.MANAGER
        }
        salesManager.deleteSale(saleId)
    }

    suspend fun clearAllSalesData() = withContext(Dispatchers.IO) {
        salesManager.clearAllSales()
        customerPaymentDao.clearAllPayments()
        purchaseDao.clearAllPurchases()
        stockTransferDao.clearAllTransfers()
        syncQueueDao.clearQueue()
    }

    suspend fun resetWholeApp(context: Context) = withContext(Dispatchers.IO) {
        // 1. Wipe all transactional records
        salesManager.clearAllSales()
        customerPaymentDao.clearAllPayments()
        purchaseDao.clearAllPurchases()
        stockTransferDao.clearAllTransfers()
        syncQueueDao.clearQueue()
        redeemedVoucherDao.clearAllVouchers()

        // 2. Wipe master products and customers
        inventoryManager.clearAllProducts()
        customerDebtManager.clearAllCustomers()

        // 3. Reset shop profile to clean fresh 4-day trial store
        val now = com.example.util.ClockGuard.effectiveNowMs()
        val freshProfile = ShopProfile(
            id = 1L,
            name = "",
            phone = "",
            email = "",
            shopName = "",
            address = "",
            currencyCode = "RWF",
            currencySymbol = "FRw",
            taxRate = 18.0,
            tinNumber = "",
            receiptFooter = "Thank you for shopping with us!",
            isOnlineSyncEnabled = true,
            backendServerUrl = "",
            subscriptionStatus = ShopProfile.STATUS_TRIAL,
            monthlyFeeRwf = 5000,
            subscriptionExpiresAt = now + ShopProfile.FREE_TRIAL_DURATION_MS,
            trialStartedAt = now,
            lastPaymentRef = "FREE-TRIAL-4D",
            lastPaymentAmount = 0
        )
        shopProfileDao.insertOrUpdateProfile(freshProfile)

        // 4. Ensure fresh default admin user & primary branch
        ensureDefaultAdminUser()
        ensureDefaultBranches()
    }

    // -------------------------------------------------------------
    // INITIALIZATION
    // -------------------------------------------------------------
    suspend fun initDatabase() = withContext(Dispatchers.IO) {
        val existingProfile = shopProfileDao.getShopProfileDirect()
        if (existingProfile == null) {
            val now = com.example.util.ClockGuard.effectiveNowMs()
            val defaultProfile = ShopProfile(
                id = 1L,
                name = "",
                phone = "",
                email = "",
                shopName = "",
                address = "",
                currencyCode = "RWF",
                currencySymbol = "FRw",
                taxRate = 18.0,
                tinNumber = "",
                receiptFooter = "Murakoze cyane! Thank you for shopping with us.",
                isOnlineSyncEnabled = true,
                backendServerUrl = "",
                subscriptionStatus = ShopProfile.STATUS_TRIAL,
                monthlyFeeRwf = 5000,
                subscriptionExpiresAt = now + ShopProfile.FREE_TRIAL_DURATION_MS,
                trialStartedAt = now,
                lastPaymentRef = "FREE-TRIAL-4D",
                lastPaymentAmount = 0
            )
            shopProfileDao.insertOrUpdateProfile(defaultProfile)
        }

        ensureDefaultAdminUser()
        ensureDefaultBranches()
    }
}
