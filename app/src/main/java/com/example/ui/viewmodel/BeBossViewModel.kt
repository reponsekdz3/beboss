package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.CategorySalesShare
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.DailyAnalyticsPoint
import com.example.data.model.InventoryValuation
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.PurchaseRecord
import com.example.data.model.PurchaseSummary
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.SyncQueueItem
import com.example.data.model.TopProductReport
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.CartItem
import com.example.data.repository.BeBossRepository
import com.example.util.SyncAuditLog
import com.example.util.AppLanguage
import com.example.util.BeBossNotificationManager
import com.example.util.Localization
import com.example.util.ShopImportSummary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen(val stringKey: String, val title: String) {
    DASHBOARD("dashboard", "Dashboard"),
    SALES_POS("sales_pos", "POS Sale"),
    INVENTORY("inventory", "Inventory"),
    ANALYTICS("analytics", "Reports & P/L"),
    CUSTOMERS("customers", "Customers & Debt"),
    SALES_HISTORY("sales_history", "Sales History"),
    SETTINGS("settings", "Settings")
}

data class CartState(
    val items: List<CartItem> = emptyList(),
    val selectedCustomerId: String? = null,
    val selectedCustomerName: String = "Walk-in Customer",
    val discountAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, MOMO, AIRTEL, CARD, CREDIT_DEBT
    val amountPaidInput: String = "",
    val notes: String = ""
) {
    val rawTotal: Double
        get() = items.sumOf { it.effectiveUnitPrice * it.quantity }

    val netTotal: Double
        get() = (rawTotal - discountAmount).coerceAtLeast(0.0)

    val totalCost: Double
        get() = items.sumOf { it.product.costPrice * it.quantity }

    val estimatedProfit: Double
        get() = netTotal - totalCost

    val effectiveAmountPaid: Double
        get() {
            val parsed = amountPaidInput.toDoubleOrNull()
            return when {
                parsed != null -> parsed
                paymentMethod == "CREDIT_DEBT" -> 0.0
                else -> netTotal
            }
        }

    val totalUnitsCount: Double
        get() = items.sumOf { it.quantity }
}

class BeBossViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("beboss_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(application)
    val repository = BeBossRepository(db)
    private val connectivityObserver = com.example.util.NetworkConnectivityObserver(application)
    private val deviceId: String = com.example.util.SupabaseAdminClient.getOrCreateDeviceId(application)
    private var autoBranchSyncJob: kotlinx.coroutines.Job? = null

    // Network Connectivity & Cloud Sync State
    val connectivityStatus: StateFlow<com.example.util.ConnectivityStatus> = connectivityObserver.networkStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.ConnectivityStatus.AVAILABLE)

    private val _cloudSyncReport = MutableStateFlow<com.example.util.CloudSyncReport?>(null)
    val cloudSyncReport: StateFlow<com.example.util.CloudSyncReport?> = _cloudSyncReport.asStateFlow()

    fun scheduleAutoBranchSync(delayMs: Long = 1600L) {
        if (connectivityStatus.value != com.example.util.ConnectivityStatus.AVAILABLE) return
        autoBranchSyncJob?.cancel()
        autoBranchSyncJob = viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            if (connectivityStatus.value == com.example.util.ConnectivityStatus.AVAILABLE && !_isSyncing.value) {
                val report = repository.performSync(
                    activeBranchId = _selectedBranchId.value,
                    deviceId = deviceId
                )
                _cloudSyncReport.value = report
            }
        }
    }

    // Multi-Branch Management & Logistics
    val branches: StateFlow<List<com.example.data.model.Branch>> = repository.allBranches.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allStockTransfers: StateFlow<List<StockTransfer>> = repository.allStockTransfers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun createStockTransfer(
        productId: String,
        fromBranchId: String,
        fromBranchName: String,
        toBranchId: String,
        toBranchName: String,
        quantity: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val user = _currentUser.value
                val transfer = repository.createStockTransfer(
                    productId = productId,
                    fromBranchId = fromBranchId,
                    fromBranchName = fromBranchName,
                    toBranchId = toBranchId,
                    toBranchName = toBranchName,
                    quantity = quantity,
                    notes = notes,
                    transferredBy = user?.name ?: "Store Manager"
                )
                // Real system notification trigger
                BeBossNotificationManager.sendStockTransferNotification(getApplication(), transfer)
                scheduleAutoBranchSync()

                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Kwohereza ibicuruzwa (${quantity.toInt()} ${transfer.productName}) muri $toBranchName byakozwe neza!"
                    else "Stock transfer of ${quantity.toInt()} ${transfer.productName} to $toBranchName recorded!"
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Transfer error: ${e.message}")
            }
        }
    }

    fun deleteStockTransfer(id: String) {
        viewModelScope.launch {
            repository.deleteStockTransfer(id)
            scheduleAutoBranchSync()
            _snackbarMessage.emit("Stock transfer log removed.")
        }
    }

    // Local P2P WiFi / Hotspot Sync Hub + LAN Auto-Discovery
    val localNetworkSyncServer = com.example.util.LocalNetworkSyncServer(application, db)
    val localServerStatus = localNetworkSyncServer.serverStatus
    val localServerIp = localNetworkSyncServer.serverIpAddress
    val localConnectedClients = localNetworkSyncServer.connectedClientsCount
    val localLastReceivedPacket = localNetworkSyncServer.lastReceivedPacketSummary
    val discoveredHubs = localNetworkSyncServer.discoveredHubs
    val isDiscoveringHubs = localNetworkSyncServer.isDiscoveringHubs
    val peerSyncEvents = localNetworkSyncServer.peerSyncEvents
    val syncAuditLogs = repository.syncAuditLogs

    private val _isP2pSyncing = MutableStateFlow(false)
    val isP2pSyncing: StateFlow<Boolean> = _isP2pSyncing.asStateFlow()

    fun toggleLocalSyncServer(enable: Boolean) {
        if (enable) {
            val started = localNetworkSyncServer.startServer()
            if (started) {
                repository.logSyncEvent("P2P_SERVER", "SUCCESS", "Local Hub Server started on port ${localNetworkSyncServer.port}")
            }
        } else {
            localNetworkSyncServer.stopServer()
            repository.logSyncEvent("P2P_SERVER", "SUCCESS", "Local Hub Server stopped")
        }
    }

    fun startLanDiscovery() {
        localNetworkSyncServer.startDiscovery()
    }

    fun stopLanDiscovery() {
        localNetworkSyncServer.stopDiscovery()
    }

    fun syncWithLocalMasterHub(hubIp: String) {
        viewModelScope.launch {
            _isP2pSyncing.value = true
            val res = localNetworkSyncServer.syncWithMasterHub(hubIp, _currentUser.value)
            _isP2pSyncing.value = false
            repository.logSyncEvent(
                type = "P2P_CLIENT",
                status = if (res.success) "SUCCESS" else "FAILED",
                summary = res.message,
                latencyMs = res.latencyMs
            )
            _snackbarMessage.emit(res.message)
        }
    }

    fun createBranchShareToken(branchIdFilter: String? = null, onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = com.example.util.ShopDataTransferManager.createExportJson(
                    profile = shopProfile.value,
                    users = allUsers.value,
                    products = allProducts.value,
                    customers = customers.value,
                    sales = allSales.value,
                    saleItems = repository.getAllSaleItemsList(),
                    payments = allCustomerPayments.value,
                    exportedByUser = _currentUser.value,
                    branches = branches.value,
                    stockTransfers = allStockTransfers.value,
                    purchases = repository.getAllPurchasesList(),
                    branchIdFilter = branchIdFilter
                )
                val token = com.example.util.ShopDataTransferManager.createCompressedSyncToken(json)
                repository.logSyncEvent("BRANCH_TOKEN", "SUCCESS", "Generated signed BBSHARE1 token (${token.length} chars)")
                onReady(token)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to build branch share token: ${e.message}")
            }
        }
    }

    fun exportFullBranchPackage(context: Context, branchIdFilter: String? = null, onReady: (java.io.File) -> Unit) {
        viewModelScope.launch {
            try {
                val file = com.example.util.ShopDataTransferManager.exportShopPackage(
                    context = context,
                    profile = shopProfile.value,
                    users = allUsers.value,
                    products = allProducts.value,
                    customers = customers.value,
                    sales = allSales.value,
                    saleItems = repository.getAllSaleItemsList(),
                    payments = allCustomerPayments.value,
                    exportedByUser = _currentUser.value,
                    branches = branches.value,
                    stockTransfers = allStockTransfers.value,
                    purchases = repository.getAllPurchasesList(),
                    branchIdFilter = branchIdFilter
                )
                repository.logSyncEvent("FILE_EXPORT", "SUCCESS", "Exported ${file.name} with full branch ledger")
                onReady(file)
            } catch (e: Exception) {
                _snackbarMessage.emit("Export error: ${e.message}")
            }
        }
    }

    fun importBranchShareToken(tokenOrJson: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val summary = com.example.util.ShopDataTransferManager.parseShopPackageString(tokenOrJson)
                repository.importShopDataPackage(summary)
                val msg = "Merged ${summary.branches.size} branches, ${summary.productCount} products, ${summary.saleCount} sales & ${summary.stockTransfers.size} transfers!"
                repository.logSyncEvent("BRANCH_IMPORT", "SUCCESS", msg)
                _snackbarMessage.emit(msg)
                onDone(true, msg)
            } catch (e: Exception) {
                val err = "Branch DataShare import failed: ${e.message}"
                _snackbarMessage.emit(err)
                onDone(false, err)
            }
        }
    }

    fun testEndpointPing(endpoint: String, onResult: (com.example.util.EndpointPingResult) -> Unit) {
        viewModelScope.launch {
            val res = repository.testServerPing(endpoint)
            onResult(res)
        }
    }

    fun syncToCloudWithEndpoint(endpoint: String? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val report = repository.cloudSyncManager.syncAllDataToCloud(
                serverEndpoint = endpoint,
                activeBranchId = _selectedBranchId.value,
                deviceId = deviceId,
                mergeRemoteSummary = { summary -> repository.mergeShopDataPackageLww(summary) }
            )
            _cloudSyncReport.value = report
            _isSyncing.value = false
            _snackbarMessage.emit(report.message)
        }
    }

    private val _selectedBranchId = MutableStateFlow("ALL")
    val selectedBranchId: StateFlow<String> = _selectedBranchId.asStateFlow()

    fun selectBranch(branchId: String) {
        _selectedBranchId.value = branchId
    }

    fun saveBranch(branch: com.example.data.model.Branch) {
        viewModelScope.launch {
            repository.saveBranch(branch)
            val allBranches = db.branchDao().getAllActiveBranchesList()
            val allStaff = db.userDao().getAllActiveUsersList()
            val newPricing = com.example.util.OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = allBranches.size.coerceAtLeast(1),
                workerCount = allStaff.size.coerceAtLeast(1)
            )
            repository.updateShopProfile(
                shopProfile.value.copy(
                    monthlyFeeRwf = newPricing.monthlySubtotal,
                    updatedAt = System.currentTimeMillis()
                )
            )
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Ishami '${branch.name}' ryabitswe neza (${newPricing.monthlySubtotal} FRw/mo)."
                else "Branch '${branch.name}' saved (${newPricing.monthlySubtotal} FRw/mo)."
            _snackbarMessage.emit(msg)
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            repository.deleteBranch(branchId)
            val allBranches = db.branchDao().getAllActiveBranchesList()
            val allStaff = db.userDao().getAllActiveUsersList()
            val newPricing = com.example.util.OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = allBranches.size.coerceAtLeast(1),
                workerCount = allStaff.size.coerceAtLeast(1)
            )
            repository.updateShopProfile(
                shopProfile.value.copy(
                    monthlyFeeRwf = newPricing.monthlySubtotal,
                    updatedAt = System.currentTimeMillis()
                )
            )
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Ishami ryasibwe."
                else "Branch removed."
            _snackbarMessage.emit(msg)
        }
    }

    // Language & Theme State
    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("lang", "en") == "rw") AppLanguage.KINYARWANDA else AppLanguage.ENGLISH
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Authentication & Security State
    val allUsers: StateFlow<List<User>> = repository.allActiveUsers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Screen is locked by default until credentials entered
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isRegistrationNeeded = MutableStateFlow(false)
    val isRegistrationNeeded: StateFlow<Boolean> = _isRegistrationNeeded.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Shop Profile
    val shopProfile: StateFlow<ShopProfile> = repository.shopProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopProfile()
    )

    // Products / Stock
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedProductCategory = MutableStateFlow("All")
    val selectedProductCategory: StateFlow<String> = _selectedProductCategory.asStateFlow()

    private val _showOnlyLowStock = MutableStateFlow(false)
    val showOnlyLowStock: StateFlow<Boolean> = _showOnlyLowStock.asStateFlow()

    val allProducts: StateFlow<List<Product>> = _productSearchQuery
        .flatMapLatest { query -> repository.searchProducts(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val categories: StateFlow<List<String>> = repository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val inventoryValuation: StateFlow<InventoryValuation> = repository.getInventoryValuation().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        InventoryValuation(0, 0.0, 0.0, 0.0, 0.0, 0, 0)
    )

    // Customers
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    val customers: StateFlow<List<Customer>> = _customerSearchQuery
        .flatMapLatest { query -> repository.searchCustomers(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDebt: StateFlow<List<Customer>> = repository.customersWithDebt.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val totalOutstandingDebt: StateFlow<Double> = repository.totalOutstandingDebt.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0
    )

    val allCustomerPayments: StateFlow<List<CustomerPayment>> = repository.allCustomerPayments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Sales POS Cart
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    // Sales History
    val allSales: StateFlow<List<Sale>> = repository.allSales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Purchases & Stock Inflow History (Excel Ledger)
    val allPurchases: StateFlow<List<PurchaseRecord>> = repository.allPurchases.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val purchaseSummary: StateFlow<PurchaseSummary> = allPurchases.map { purchases ->
        val totalCost = purchases.sumOf { it.totalPurchaseCost }
        val totalUnits = purchases.sumOf { it.quantityPurchased }
        val suppliers = purchases.map { it.supplierName.lowercase() }.distinct().size
        val avg = if (purchases.isNotEmpty()) totalCost / purchases.size else 0.0
        PurchaseSummary(
            totalPurchasesCount = purchases.size,
            totalUnitsBought = totalUnits,
            totalExpenditure = totalCost,
            uniqueSuppliersCount = suppliers,
            averagePurchaseCost = avg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PurchaseSummary())

    // Analytics
    private val _selectedAnalyticsPeriod = MutableStateFlow(AnalyticsPeriod.THIS_WEEK)
    val selectedAnalyticsPeriod: StateFlow<AnalyticsPeriod> = _selectedAnalyticsPeriod.asStateFlow()

    val profitLossSummary: StateFlow<ProfitLossSummary> = _selectedAnalyticsPeriod
        .flatMapLatest { period -> repository.getProfitLossForPeriod(period) }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProfitLossSummary(0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, true, "This Week")
        )

    val dailyChartPoints: StateFlow<List<DailyAnalyticsPoint>> = _selectedAnalyticsPeriod
        .flatMapLatest { period -> repository.getDailyAnalyticsPoints(period) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topProducts: StateFlow<List<TopProductReport>> = _selectedAnalyticsPeriod
        .flatMapLatest { period -> repository.getTopProducts(period) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBreakdown: StateFlow<List<CategorySalesShare>> = _selectedAnalyticsPeriod
        .flatMapLatest { period -> repository.getCategorySalesBreakdown(period) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today KPI for Dashboard
    val todaySummary: StateFlow<ProfitLossSummary> = repository.getProfitLossForPeriod(AnalyticsPeriod.TODAY)
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProfitLossSummary(0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, true, "Today")
        )

    // Sync
    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Dialogs / Sheets State
    private val _activeReceiptSale = MutableStateFlow<SaleWithItems?>(null)
    val activeReceiptSale: StateFlow<SaleWithItems?> = _activeReceiptSale.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        checkAuthenticationAndInit()
        setupAutoCloudSyncOnInternet()
    }

    private fun setupAutoCloudSyncOnInternet() {
        viewModelScope.launch {
            repository.ensureDefaultBranches()
            connectivityStatus.collect { status ->
                if (status == com.example.util.ConnectivityStatus.AVAILABLE) {
                    _isSyncing.value = true
                    try {
                        syncAdminDataToSupabase(silent = true)
                        val report = repository.performSync(
                            activeBranchId = _selectedBranchId.value,
                            deviceId = deviceId
                        )
                        _cloudSyncReport.value = report
                    } finally {
                        _isSyncing.value = false
                    }
                }
            }
        }
    }

    fun syncToCloudNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                syncAdminDataToSupabase(silent = true)
                val report = repository.performSync(
                    activeBranchId = _selectedBranchId.value,
                    deviceId = deviceId
                )
                _cloudSyncReport.value = report
                _snackbarMessage.emit(report.message)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun checkAuthenticationAndInit() {
        viewModelScope.launch {
            val savedUserId = prefs.getString("active_user_id", null)
            if (!savedUserId.isNullOrBlank()) {
                val savedUser = db.userDao().getUserById(savedUserId)
                if (savedUser != null && savedUser.isActive) {
                    _currentUser.value = savedUser
                    _isLocked.value = false
                    _isRegistrationNeeded.value = false
                    return@launch
                }
            }

            // If no saved user, check total users
            val count = db.userDao().getUserCount()
            if (count == 0) {
                _isRegistrationNeeded.value = true
                _isLocked.value = true
            } else {
                val firstUser = db.userDao().getAllActiveUsers().stateIn(viewModelScope).value.firstOrNull()
                if (firstUser != null) {
                    _currentUser.value = firstUser
                    // Auto keep logged in for best seamless experience
                    prefs.edit().putString("active_user_id", firstUser.id).apply()
                    _isLocked.value = false
                    _isRegistrationNeeded.value = false
                } else {
                    _isRegistrationNeeded.value = false
                    _isLocked.value = true
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // LANGUAGE & THEME TOGGLES
    // ------------------------------------------------------------------------
    fun toggleLanguage() {
        val next = if (_currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.KINYARWANDA else AppLanguage.ENGLISH
        _currentLanguage.value = next
        prefs.edit().putString("lang", next.code).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("lang", lang.code).apply()
    }

    fun toggleDarkMode() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        prefs.edit().putBoolean("dark_theme", next).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    // ------------------------------------------------------------------------
    // ACCOUNT SETUP & AUTHENTICATION
    // ------------------------------------------------------------------------
    fun registerNewShopAndOwner(
        shopName: String,
        currencyCode: String,
        currencySymbol: String,
        address: String,
        phone: String,
        ownerFullName: String,
        username: String,
        pin: String,
        password: String
    ) {
        viewModelScope.launch {
            try {
                // Create Shop Profile with 30-day active trial
                val profile = ShopProfile(
                    id = 1L,
                    name = ownerFullName.trim(),
                    phone = phone.trim(),
                    email = "$username@beboss.rw",
                    shopName = shopName.trim(),
                    address = address.trim().ifBlank { "Kigali, Rwanda" },
                    currencyCode = currencyCode.trim(),
                    currencySymbol = currencySymbol.trim(),
                    subscriptionStatus = "ACTIVE",
                    monthlyFeeRwf = 5000,
                    subscriptionExpiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                    trialStartedAt = System.currentTimeMillis(),
                    receiptFooter = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                        "Murakoze cyane! Mwongere kugaruka!" 
                        else "Thank you for your business! Please visit again!"
                )
                repository.updateShopProfile(profile)

                // Create Master Owner User Account
                val ownerUser = User(
                    id = UUID.randomUUID().toString(),
                    name = ownerFullName.trim(),
                    username = username.trim().lowercase(),
                    phone = phone.trim(),
                    pinHash = pin.trim(),
                    password = password.trim(),
                    role = UserRole.OWNER,
                    profileColorHex = "#FF6B1A",
                    canSellPOS = true,
                    canApplyDiscounts = true,
                    canManageInventory = true,
                    canViewCostAndProfit = true,
                    canViewAnalytics = true,
                    canManageCustomers = true,
                    canCollectDebt = true,
                    canDeleteRecords = true,
                    canExportReports = true,
                    canManageCollaborators = true,
                    canManageShopSettings = true,
                    canExportImportData = true
                )
                repository.saveUser(ownerUser)

                // Create and initialize the Real Primary Branch using actual owner and shop details
                val mainBranch = com.example.data.model.Branch(
                    id = "main_branch",
                    name = "${shopName.trim()} (HQ)",
                    code = "HQ-01",
                    address = address.trim().ifBlank { "Kigali, Rwanda" },
                    phone = phone.trim(),
                    managerName = ownerFullName.trim(),
                    isMainBranch = true,
                    colorHex = "#FF6B1A"
                )
                repository.saveBranch(mainBranch)

                // Ensure no mock placeholder branches exist
                repository.deleteBranch("branch_kimironko")
                repository.deleteBranch("branch_nyabugogo")

                prefs.edit().putString("active_user_id", ownerUser.id).apply()
                _currentUser.value = ownerUser
                _isRegistrationNeeded.value = false
                _isLocked.value = false
                _authError.value = null
                _snackbarMessage.emit("Store '$shopName' & Owner account created successfully!")
            } catch (e: Exception) {
                _authError.value = "Registration failed: ${e.message}"
            }
        }
    }

    fun loginWithPin(pin: String): Boolean {
        viewModelScope.launch {
            val user = repository.getUserByPin(pin.trim())
            if (user != null) {
                _currentUser.value = user
                _isLocked.value = false
                _authError.value = null
                prefs.edit().putString("active_user_id", user.id).apply()
                repository.updateLastLogin(user.id)
                val welcome = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                    "Murakaza neza, ${user.name}!" 
                    else "Welcome back, ${user.name}!"
                _snackbarMessage.emit(welcome)
            } else {
                _authError.value = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                    "Umubare wa PIN si wo. Ongera ugerageze." 
                    else "Incorrect PIN code. Please try again."
            }
        }
        return true
    }

    fun loginWithCredentials(username: String, pinOrPass: String): Boolean {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username.trim())
            if (user != null && !user.isActive) {
                _authError.value = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Konti yawe yahagaritswe n'Umuyobozi Mukuru (Admin)."
                else "Account suspended by Master Administrator."
                return@launch
            }
            val isValid = user != null && user.isActive && (
                com.example.util.SecurityUtils.verifyPin(pinOrPass.trim(), user.pinHash) ||
                com.example.util.SecurityUtils.verifyPassword(pinOrPass.trim(), user.password)
            )
            if (user != null && isValid) {
                _currentUser.value = user
                _isLocked.value = false
                _authError.value = null
                prefs.edit().putString("active_user_id", user.id).apply()
                repository.updateLastLogin(user.id)
                val welcome = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Winjiye nka ${user.name} (${user.role.displayName})"
                else "Logged in as ${user.name} (${user.role.displayName})"
                _snackbarMessage.emit(welcome)
            } else {
                _authError.value = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Izina cyangwa ijambobanga si byo"
                else "Invalid username or password"
            }
        }
        return true
    }

    fun unlockAppWithPin(pin: String): Boolean {
        val active = _currentUser.value
        if (active != null && com.example.util.SecurityUtils.verifyPin(pin.trim(), active.pinHash)) {
            _isLocked.value = false
            _authError.value = null
            prefs.edit().putString("active_user_id", active.id).apply()
            return true
        }
        return loginWithPin(pin)
    }

    fun unlockWithBiometric() {
        val active = _currentUser.value
        if (active != null) {
            _isLocked.value = false
            _authError.value = null
            prefs.edit().putString("active_user_id", active.id).apply()
            viewModelScope.launch {
                val welcome = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Urubuga rufunguwe n'igikumwe: ${active.name}"
                    else "Unlocked with biometric: ${active.name}"
                _snackbarMessage.emit(welcome)
            }
        } else {
            // Pick primary active user or admin
            viewModelScope.launch {
                val defaultUser = repository.ensureDefaultAdminUser()
                _currentUser.value = defaultUser
                _isLocked.value = false
                _authError.value = null
                prefs.edit().putString("active_user_id", defaultUser.id).apply()
                _snackbarMessage.emit("Unlocked: ${defaultUser.name}")
            }
        }
    }

    fun lockApp() {
        _isLocked.value = true
        _authError.value = null
    }

    fun logoutUser() {
        prefs.edit().remove("active_user_id").apply()
        _currentUser.value = null
        _isLocked.value = true
        _authError.value = null
        viewModelScope.launch {
            _snackbarMessage.emit("Logged out successfully.")
        }
    }

    fun switchUser(user: User) {
        _currentUser.value = user
        _isLocked.value = false
        _authError.value = null
        prefs.edit().putString("active_user_id", user.id).apply()
        viewModelScope.launch {
            repository.updateLastLogin(user.id)
            _snackbarMessage.emit("Active operator: ${user.name}")
        }
    }

    fun showRegistrationScreen() {
        _isRegistrationNeeded.value = true
    }

    fun cancelRegistration() {
        _isRegistrationNeeded.value = false
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
            val allBranches = db.branchDao().getAllActiveBranchesList()
            val allStaff = db.userDao().getAllActiveUsersList()
            val newPricing = com.example.util.OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = allBranches.size.coerceAtLeast(1),
                workerCount = allStaff.size.coerceAtLeast(1)
            )
            repository.updateShopProfile(
                shopProfile.value.copy(
                    monthlyFeeRwf = newPricing.monthlySubtotal,
                    updatedAt = System.currentTimeMillis()
                )
            )
            scheduleAutoBranchSync()
            _snackbarMessage.emit("Staff member '${user.name}' saved (${newPricing.monthlySubtotal} FRw/mo total plan).")
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            if (_currentUser.value?.id == userId) {
                _snackbarMessage.emit("Cannot delete the currently active account.")
                return@launch
            }
            repository.deleteUser(userId)
            val allBranches = db.branchDao().getAllActiveBranchesList()
            val allStaff = db.userDao().getAllActiveUsersList()
            val newPricing = com.example.util.OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = allBranches.size.coerceAtLeast(1),
                workerCount = allStaff.size.coerceAtLeast(1)
            )
            repository.updateShopProfile(
                shopProfile.value.copy(
                    monthlyFeeRwf = newPricing.monthlySubtotal,
                    updatedAt = System.currentTimeMillis()
                )
            )
            scheduleAutoBranchSync()
            _snackbarMessage.emit("Staff account removed.")
        }
    }

    // ------------------------------------------------------------------------
    // SUBSCRIPTION, USSD VERIFICATION & SUPABASE ADMIN LICENSING
    // ------------------------------------------------------------------------
    val paymentVerificationRequests: StateFlow<List<com.example.data.model.PaymentVerificationRequest>> =
        repository.allPaymentRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _adminSession = MutableStateFlow<com.example.util.SupabaseAdminSession?>(
        com.example.util.SupabaseAdminClient.getCurrentAdminSession()
    )
    val adminSession: StateFlow<com.example.util.SupabaseAdminSession?> = _adminSession.asStateFlow()

    private val _adminAuthError = MutableStateFlow<String?>(null)
    val adminAuthError: StateFlow<String?> = _adminAuthError.asStateFlow()

    private val _isAdminAuthenticating = MutableStateFlow(false)
    val isAdminAuthenticating: StateFlow<Boolean> = _isAdminAuthenticating.asStateFlow()

    private val _adminPaymentRequests = MutableStateFlow<List<com.example.util.SupabasePaymentRequestDto>>(emptyList())
    val adminPaymentRequests: StateFlow<List<com.example.util.SupabasePaymentRequestDto>> = _adminPaymentRequests.asStateFlow()

    private val _adminShopAccounts = MutableStateFlow<List<com.example.util.SupabaseShopAccountDto>>(emptyList())
    val adminShopAccounts: StateFlow<List<com.example.util.SupabaseShopAccountDto>> = _adminShopAccounts.asStateFlow()

    private val _adminShopUsers = MutableStateFlow<List<com.example.util.SupabaseShopUserDto>>(emptyList())
    val adminShopUsers: StateFlow<List<com.example.util.SupabaseShopUserDto>> = _adminShopUsers.asStateFlow()

    private val _adminBranchSnapshots = MutableStateFlow<List<com.example.util.SupabaseBranchSnapshotDto>>(emptyList())
    val adminBranchSnapshots: StateFlow<List<com.example.util.SupabaseBranchSnapshotDto>> = _adminBranchSnapshots.asStateFlow()

    private val _adminUnlockCodes = MutableStateFlow<List<com.example.util.SupabaseUnlockCodeDto>>(emptyList())
    val adminUnlockCodes: StateFlow<List<com.example.util.SupabaseUnlockCodeDto>> = _adminUnlockCodes.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<com.example.util.SupabaseAuditLogDto>>(emptyList())
    val adminAuditLogs: StateFlow<List<com.example.util.SupabaseAuditLogDto>> = _adminAuditLogs.asStateFlow()

    private val _isAdminLoading = MutableStateFlow(false)
    val isAdminLoading: StateFlow<Boolean> = _isAdminLoading.asStateFlow()

    private fun resolveOwnerUser(): User? {
        return _currentUser.value
            ?: allUsers.value.firstOrNull { it.role == UserRole.OWNER }
            ?: allUsers.value.firstOrNull()
    }

    fun signInSupabaseAdmin(email: String, password: String) {
        viewModelScope.launch {
            _isAdminAuthenticating.value = true
            _adminAuthError.value = null
            val res = com.example.util.SupabaseAdminClient.signInAdmin(email, password)
            _isAdminAuthenticating.value = false
            res.onSuccess { sess ->
                _adminSession.value = sess
                refreshAdminDashboardData()
                _snackbarMessage.emit("Authenticated as Master Admin (${sess.email})")
            }.onFailure { err ->
                _adminAuthError.value = err.message ?: "Admin sign-in failed"
            }
        }
    }

    fun signUpSupabaseAdmin(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _isAdminAuthenticating.value = true
            _adminAuthError.value = null
            val res = com.example.util.SupabaseAdminClient.signUpAdmin(email, password, fullName)
            _isAdminAuthenticating.value = false
            res.onSuccess { sess ->
                _adminSession.value = sess
                refreshAdminDashboardData()
                _snackbarMessage.emit("Master Admin profile initialized (${sess.email})")
            }.onFailure { err ->
                _adminAuthError.value = err.message ?: "Admin initialization failed"
            }
        }
    }

    fun signOutSupabaseAdmin() {
        viewModelScope.launch {
            com.example.util.SupabaseAdminClient.signOutAdmin()
            _adminSession.value = null
            _adminPaymentRequests.value = emptyList()
            _adminShopAccounts.value = emptyList()
            _adminShopUsers.value = emptyList()
            _adminBranchSnapshots.value = emptyList()
            _adminUnlockCodes.value = emptyList()
            _adminAuditLogs.value = emptyList()
            _adminAuthError.value = null
        }
    }

    fun activateSubscriptionVoucher(code: String) {
        viewModelScope.launch {
            val currentProf = shopProfile.value
            val owner = resolveOwnerUser()
            val branchCount = branches.value.size.coerceAtLeast(1)
            val workerCount = allUsers.value.size.coerceAtLeast(1)
            val result = repository.redeemVoucher(
                voucherCode = code,
                shopProfile = currentProf,
                branchCount = branchCount,
                workerCount = workerCount,
                ownerUsername = owner?.username.orEmpty(),
                ownerPhoneOverride = owner?.phone.orEmpty(),
                ownerUser = owner
            )
            _snackbarMessage.emit(result.message)
            if (result.isValid) {
                syncAdminDataToSupabase(silent = true)
            }
        }
    }

    fun submitUssdPaymentProof(
        provider: String,
        payerPhone: String,
        username: String,
        registeredPhone: String,
        durationMonths: Int,
        branchCount: Int,
        workerCount: Int,
        telcoReference: String = "",
        onSubmitted: (com.example.data.model.PaymentVerificationRequest, Boolean) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val profile = shopProfile.value
            val owner = resolveOwnerUser()
            val breakdown = com.example.util.OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths
            )
            val ussd = if (provider.contains("AIRTEL", ignoreCase = true)) breakdown.ussdAirtelCode else breakdown.ussdMtnCode
            val cleanUsername = username.trim().lowercase().ifBlank { owner?.username ?: "owner" }
            val cleanRegPhone = registeredPhone.trim().ifBlank { profile.phone }
            val req = com.example.data.model.PaymentVerificationRequest(
                shopName = profile.shopName,
                ownerUsername = cleanUsername,
                ownerPhone = cleanRegPhone,
                deviceChallenge = com.example.util.SubscriptionSecurityManager.computeDeviceChallenge(profile),
                provider = provider,
                ussdCode = ussd,
                payerPhone = payerPhone.trim().ifBlank { cleanRegPhone },
                amountRwf = breakdown.totalPayable,
                durationMonths = durationMonths,
                branchCount = branchCount.coerceAtLeast(1),
                workerCount = workerCount.coerceAtLeast(1),
                telcoReference = telcoReference.trim().uppercase(),
                status = "PENDING_VERIFICATION",
                syncedToSupabase = false
            )
            val synced = repository.submitPaymentVerificationRequest(req, profile, owner)
            repository.logSyncEvent(
                type = "SUPABASE_ADMIN",
                status = if (synced) "SUCCESS" else "QUEUED",
                summary = if (synced) "Payment proof synced to Supabase Admin Queue" else "Saved offline — auto-syncs to Supabase when internet returns"
            )
            _snackbarMessage.emit(
                if (synced) "Payment verification submitted to Admin Cloud! Send WhatsApp proof now."
                else "Saved offline! Will sync to Supabase automatically when internet returns. Send WhatsApp proof now."
            )
            onSubmitted(req, synced)
        }
    }

    fun syncAdminDataToSupabase(silent: Boolean = false) {
        viewModelScope.launch {
            val profile = shopProfile.value
            val owner = resolveOwnerUser()
            val branchCount = branches.value.size.coerceAtLeast(1)
            val workerCount = allUsers.value.size.coerceAtLeast(1)
            val res = repository.syncPendingAdminDataWithSupabase(
                shopProfile = profile,
                ownerUser = owner,
                branchCount = branchCount,
                workerCount = workerCount
            )
            if (!silent || res.approvedCodesFound.isNotEmpty()) {
                _snackbarMessage.emit(res.message)
            }
        }
    }

    fun checkCloudUnlockApprovals() {
        syncAdminDataToSupabase(silent = false)
    }

    fun refreshAdminDashboardData() {
        if (_adminSession.value == null) return
        viewModelScope.launch {
            _isAdminLoading.value = true
            _adminPaymentRequests.value = com.example.util.SupabaseAdminClient.fetchAllPaymentRequests()
            _adminShopAccounts.value = com.example.util.SupabaseAdminClient.fetchAllShopAccounts()
            _adminShopUsers.value = com.example.util.SupabaseAdminClient.fetchAllShopUsers()
            _adminBranchSnapshots.value = com.example.util.SupabaseAdminClient.fetchAllBranchSnapshots()
            _adminUnlockCodes.value = com.example.util.SupabaseAdminClient.fetchAllUnlockCodes()
            _adminAuditLogs.value = com.example.util.SupabaseAdminClient.fetchAdminAuditLogs()
            _isAdminLoading.value = false
        }
    }

    fun adminApproveAndGenerateCode(
        request: com.example.util.SupabasePaymentRequestDto,
        onGenerated: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val adminName = _adminSession.value?.fullName ?: _currentUser.value?.name ?: "BeBoss Official Admin"
            val (ok, code) = com.example.util.SupabaseAdminClient.approvePaymentAndIssueCode(request, adminName)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Approved & generated unlock code: $code")
            } else {
                _snackbarMessage.emit("Admin Supabase approval failed — sign in as Admin first.")
            }
            _isAdminLoading.value = false
            onGenerated(ok, code)
        }
    }

    fun adminRejectPayment(
        request: com.example.util.SupabasePaymentRequestDto,
        reason: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.rejectPaymentRequest(request, reason)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Payment request marked REJECTED.")
            } else {
                _snackbarMessage.emit("Could not reject request in Supabase.")
            }
            _isAdminLoading.value = false
            onDone(ok)
        }
    }

    fun adminRevokeCode(codeDto: com.example.util.SupabaseUnlockCodeDto) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.revokeUnlockCode(codeDto)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Revoked unlock code ${codeDto.unlockCode}.")
            } else {
                _snackbarMessage.emit("Could not revoke code in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminIssueManualCode(
        username: String,
        registeredPhone: String,
        tierCode: String,
        days: Int,
        amountRwf: Int,
        onGenerated: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val adminName = _adminSession.value?.fullName ?: _currentUser.value?.name ?: "BeBoss Official Admin"
            val deviceCode = com.example.util.SubscriptionSecurityManager.computeDeviceChallenge(shopProfile.value)
            val (ok, code) = com.example.util.SupabaseAdminClient.issueManualUnlockCode(
                username = username,
                registeredPhone = registeredPhone,
                deviceChallenge = deviceCode,
                tierCode = tierCode,
                days = days,
                amountRwf = amountRwf,
                adminName = adminName
            )
            if (ok) refreshAdminDashboardData()
            _isAdminLoading.value = false
            onGenerated(ok, code)
        }
    }

    fun adminExtendShopSubscription(shop: com.example.util.SupabaseShopAccountDto, addDays: Int) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminExtendShopSubscription(shop, addDays)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Extended ${shop.shopName} license by +$addDays days.")
            } else {
                _snackbarMessage.emit("Failed extending shop license in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminSetShopStatus(shop: com.example.util.SupabaseShopAccountDto, status: String) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminSetShopStatus(shop, status)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("${shop.shopName} set to $status.")
            } else {
                _snackbarMessage.emit("Failed updating shop status.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminDeleteShopAccount(shop: com.example.util.SupabaseShopAccountDto) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminDeleteShopAccount(shop)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Deleted shop record ${shop.shopName}.")
            } else {
                _snackbarMessage.emit("Failed deleting shop record.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminUpdateShopUserRole(user: com.example.util.SupabaseShopUserDto, newRole: String) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminUpdateShopUser(user, newRole = newRole)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("@${user.username} role updated to $newRole.")
            } else {
                _snackbarMessage.emit("Failed updating user role in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminToggleShopUserActive(user: com.example.util.SupabaseShopUserDto, newActive: Boolean) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminUpdateShopUser(user, isActive = newActive)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("@${user.username} ${if (newActive) "reactivated" else "suspended"}.")
            } else {
                _snackbarMessage.emit("Failed updating user status in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminDeleteShopUser(user: com.example.util.SupabaseShopUserDto) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminDeleteShopUser(user)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Removed user @${user.username} from directory.")
            } else {
                _snackbarMessage.emit("Failed deleting user in Supabase.")
            }
            _isAdminLoading.value = false
        }
    }

    fun adminDeleteBranchSnapshot(snapshot: com.example.util.SupabaseBranchSnapshotDto) {
        viewModelScope.launch {
            _isAdminLoading.value = true
            val ok = com.example.util.SupabaseAdminClient.adminDeleteBranchSnapshot(snapshot)
            if (ok) {
                refreshAdminDashboardData()
                _snackbarMessage.emit("Removed branch snapshot (${snapshot.branchName}).")
            } else {
                _snackbarMessage.emit("Failed removing branch snapshot.")
            }
            _isAdminLoading.value = false
        }
    }

    fun shareAdminWebPortal(context: Context) {
        try {
            com.example.util.ShopDataTransferManager.exportAndShareAdminWebPortal(context)
        } catch (e: Exception) {
            viewModelScope.launch { _snackbarMessage.emit("Cannot export Web Portal: ${e.message}") }
        }
    }

    fun processDirectSubscriptionPayment(
        provider: String,
        phone: String,
        durationMonths: Int,
        userReference: String? = null,
        onResult: (com.example.util.PaymentProcessingResult, com.example.util.SubscriptionPriceBreakdown) -> Unit
    ) {
        viewModelScope.launch {
            val currentProf = shopProfile.value
            val branchCount = branches.value.size.coerceAtLeast(1)
            val workerCount = allUsers.value.size.coerceAtLeast(1)
            val breakdown = com.example.util.OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths
            )
            val pastRefs = setOfNotNull(currentProf.lastPaymentRef)
            val result = com.example.util.OfflineSubscriptionManager.processDirectMoMoPayment(
                shopProfile = currentProf,
                payerPhone = phone,
                provider = provider,
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths,
                smsTransactionReference = userReference,
                alreadyUsedRefs = pastRefs
            )

            if (result.isSuccess) {
                val currentExpiry = if (currentProf.subscriptionExpiresAt > System.currentTimeMillis())
                    currentProf.subscriptionExpiresAt
                    else System.currentTimeMillis()
                val newExpiry = currentExpiry + (result.planDays.toLong() * 24 * 60 * 60 * 1000)

                val updatedProf = currentProf.copy(
                    subscriptionStatus = "ACTIVE",
                    subscriptionExpiresAt = newExpiry,
                    lastPaymentRef = result.transactionRef,
                    lastPaymentAmount = result.amountPaid,
                    lastPaymentDate = result.timestamp,
                    monthlyFeeRwf = breakdown.monthlySubtotal,
                    updatedAt = result.timestamp
                )
                repository.updateShopProfile(updatedProf)
                _snackbarMessage.emit(result.message)
                syncToCloudNow()
            } else {
                _snackbarMessage.emit(result.message)
            }
            onResult(result, breakdown)
        }
    }

    fun grantEmergencyGracePeriod() {
        viewModelScope.launch {
            val currentProf = shopProfile.value
            val newExpiry = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000)
            val updatedProf = currentProf.copy(
                subscriptionStatus = "ACTIVE",
                subscriptionExpiresAt = newExpiry,
                lastPaymentRef = "EMERGENCY-GRACE-3D",
                updatedAt = System.currentTimeMillis()
            )
            repository.updateShopProfile(updatedProf)
            _snackbarMessage.emit("3-Day Emergency Grace Period activated!")
            syncToCloudNow()
        }
    }

    // ------------------------------------------------------------------------
    // NAVIGATION
    // ------------------------------------------------------------------------
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // ------------------------------------------------------------------------
    // POS & CART
    // ------------------------------------------------------------------------
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val current = _cartState.value.items.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + quantity
            current[index] = existing.copy(quantity = newQty)
        } else {
            current.add(CartItem(product = product, quantity = quantity))
        }
        _cartState.value = _cartState.value.copy(items = current)
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Double) {
        val current = _cartState.value.items.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQuantity)
            }
            _cartState.value = _cartState.value.copy(items = current)
        }
    }

    fun removeCartItem(productId: String) {
        val current = _cartState.value.items.filterNot { it.product.id == productId }
        _cartState.value = _cartState.value.copy(items = current)
    }

    fun clearCart() {
        _cartState.value = CartState()
    }

    fun setCartCustomer(customer: Customer?) {
        _cartState.value = _cartState.value.copy(
            selectedCustomerId = customer?.id,
            selectedCustomerName = customer?.name ?: "Walk-in Customer"
        )
    }

    fun setCartPaymentMethod(method: String) {
        _cartState.value = _cartState.value.copy(paymentMethod = method)
    }

    fun setCartDiscount(discount: Double) {
        _cartState.value = _cartState.value.copy(discountAmount = discount.coerceAtLeast(0.0))
    }

    fun setCartAmountPaidInput(input: String) {
        _cartState.value = _cartState.value.copy(amountPaidInput = input)
    }

    fun setCartNotes(notes: String) {
        _cartState.value = _cartState.value.copy(notes = notes)
    }

    fun checkoutSale() {
        val state = _cartState.value
        if (state.items.isEmpty()) return

        viewModelScope.launch {
            try {
                val user = _currentUser.value
                val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                    ?: branches.value.firstOrNull { it.isMainBranch }
                    ?: com.example.data.model.Branch()

                val sale = repository.processSale(
                    items = state.items,
                    customerId = state.selectedCustomerId,
                    customerName = state.selectedCustomerName,
                    discountAmount = state.discountAmount,
                    paymentMethod = state.paymentMethod,
                    amountPaid = state.effectiveAmountPaid,
                    notes = state.notes,
                    branchId = branch.id,
                    branchName = branch.name,
                    cashierId = user?.id ?: "",
                    cashierName = user?.name ?: "Shop Operator"
                )
                val saleWithItems = repository.getSaleWithItems(sale.id)
                _activeReceiptSale.value = saleWithItems
                clearCart()

                // Real system notification triggers
                BeBossNotificationManager.sendSaleCompletedNotification(getApplication(), sale, shopProfile.value)

                // Check for low stock items and notify
                val currentLowStock = repository.getProductById(state.items.firstOrNull()?.product?.id ?: "")
                if (currentLowStock != null && currentLowStock.isLowStock) {
                    BeBossNotificationManager.sendLowStockNotification(
                        getApplication(),
                        currentLowStock.name,
                        currentLowStock.quantityInStock,
                        currentLowStock.lowStockThreshold,
                        currentLowStock.unit
                    )
                }

                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Igurisha ryakozwe neza! Inyemezabuguzi irasohotse."
                    else "Sale recorded successfully! Receipt ready."
                _snackbarMessage.emit(msg)
                scheduleAutoBranchSync()
            } catch (e: Exception) {
                _snackbarMessage.emit("Error processing sale: ${e.message}")
            }
        }
    }

    fun performQuickCustomSale(description: String, amount: Double, paymentMethod: String, isDirectCheckout: Boolean = true) {
        if (amount <= 0) return
        val itemName = description.trim().ifBlank { "Custom Quick Item" }
        val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
            ?: branches.value.firstOrNull { it.isMainBranch }
            ?: com.example.data.model.Branch()
        val user = _currentUser.value

        val customProduct = Product(
            id = "custom_${System.currentTimeMillis()}",
            name = itemName,
            category = "Quick Sale",
            costPrice = amount * 0.7, // estimated margin for uncataloged item
            sellingPrice = amount,
            quantityInStock = 999.0,
            unit = "item",
            branchId = branch.id
        )

        if (!isDirectCheckout) {
            addToCart(customProduct, 1.0)
            viewModelScope.launch {
                _snackbarMessage.emit("Added '$itemName' (${amount.toInt()} RWF) to POS Cart.")
            }
            return
        }

        viewModelScope.launch {
            try {
                val sale = repository.processSale(
                    items = listOf(CartItem(product = customProduct, quantity = 1.0, customPrice = amount)),
                    customerId = null,
                    customerName = "Quick Cash Customer",
                    discountAmount = 0.0,
                    paymentMethod = paymentMethod,
                    amountPaid = amount,
                    notes = "Quick POS Sale: $itemName",
                    branchId = branch.id,
                    branchName = branch.name,
                    cashierId = user?.id ?: "",
                    cashierName = user?.name ?: "Shop Operator"
                )
                val saleWithItems = repository.getSaleWithItems(sale.id)
                _activeReceiptSale.value = saleWithItems
                scheduleAutoBranchSync()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Igurisha ry'ako kanya (${amount.toInt()} FRw) ryakozwe neza!"
                    else "Quick sale (${amount.toInt()} RWF) completed! Receipt ready."
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Quick sale error: ${e.message}")
            }
        }
    }

    fun openLastReceipt() {
        val last = allSales.value.firstOrNull()
        if (last != null) {
            openReceipt(last.id)
        } else {
            viewModelScope.launch {
                _snackbarMessage.emit("No recent sales to display yet.")
            }
        }
    }

    fun shareDailyWhatsAppSummary(context: Context) {
        val shop = shopProfile.value
        val summary = todaySummary.value
        val lowStock = lowStockProducts.value
        val timeStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val text = buildString {
            append("*${shop.shopName.ifBlank { "BeBoss Store" }} - Daily Business Report*\n")
            append("$timeStr\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("*Today's Revenue:* ${summary.totalRevenue.toInt()} ${shop.currencySymbol}\n")
            append("*Net Estimated Profit:* ${summary.netProfit.toInt()} ${shop.currencySymbol}\n")
            append("*Transactions Completed:* ${summary.totalSalesCount}\n")
            append("*Items Sold:* ${summary.totalItemsSold.toInt()}\n")
            append("*Average Basket Value:* ${summary.averageOrderValue.toInt()} ${shop.currencySymbol}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (lowStock.isNotEmpty()) {
                append("*Low Stock Alert (${lowStock.size} items):*\n")
                lowStock.take(5).forEach { p ->
                    append("• ${p.name}: ${p.quantityInStock.toInt()} ${p.unit} left\n")
                }
            } else {
                append("All product stocks are healthy.\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("_Generated with BeBoss POS_")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Daily Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // ------------------------------------------------------------------------
    // INVENTORY / PRODUCTS
    // ------------------------------------------------------------------------
    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setSelectedProductCategory(cat: String) {
        _selectedProductCategory.value = cat
    }

    fun toggleLowStockFilter() {
        _showOnlyLowStock.value = !_showOnlyLowStock.value
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Igicuruzwa '${product.name}' cyabitswe neza."
                else "Product '${product.name}' saved."
            _snackbarMessage.emit(msg)
        }
    }

    fun adjustStock(productId: String, delta: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta, reason)
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Ingano y'ububiko yahinduwe (${if (delta > 0) "+$delta" else "$delta"})."
                else "Stock adjusted (${if (delta > 0) "+$delta" else "$delta"})."
            _snackbarMessage.emit(msg)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Igicuruzwa cyasibwe."
                else "Product deleted."
            _snackbarMessage.emit(msg)
        }
    }

    // ------------------------------------------------------------------------
    // PURCHASES & SUPPLIER GOODS INFLOW (EXCEL SPREADSHEET LEDGER)
    // ------------------------------------------------------------------------
    fun recordPurchaseOrder(
        productId: String,
        quantity: Double,
        unitCostPrice: Double,
        newSellingPrice: Double? = null,
        supplierName: String,
        supplierPhone: String = "",
        paymentStatus: String = "PAID_CASH",
        invoiceNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                ?: branches.value.firstOrNull { it.isMainBranch }
                ?: com.example.data.model.Branch()

            val purchase = repository.recordPurchaseOrder(
                productId = productId,
                quantity = quantity,
                unitCostPrice = unitCostPrice,
                newSellingPrice = newSellingPrice,
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                paymentStatus = paymentStatus,
                invoiceNumber = invoiceNumber,
                branchId = branch.id,
                branchName = branch.name,
                buyerId = user?.id ?: "",
                buyerName = user?.name ?: "Shop Manager",
                notes = notes
            )
            scheduleAutoBranchSync()

            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Kugura ibicuruzwa (${purchase.quantityPurchased.toInt()} ${purchase.productName}) byanditswe neza muri Excel Ledger!"
                else "Stock In / Purchase order for ${purchase.quantityPurchased.toInt()} ${purchase.productName} recorded successfully in Excel Ledger!"
            _snackbarMessage.emit(msg)
        }
    }

    fun quickRestockAndAddToCart(
        product: Product,
        quantityToAdd: Double,
        unitCost: Double,
        supplierName: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                ?: branches.value.firstOrNull { it.isMainBranch }
                ?: com.example.data.model.Branch()

            repository.recordPurchaseOrder(
                productId = product.id,
                quantity = quantityToAdd,
                unitCostPrice = unitCost,
                newSellingPrice = product.sellingPrice,
                supplierName = supplierName.ifBlank { "Immediate Supplier" },
                branchId = branch.id,
                branchName = branch.name,
                buyerId = user?.id ?: "",
                buyerName = user?.name ?: "Shop Manager",
                notes = "Quick POS 1-Tap Restock"
            )

            val updatedProd = repository.getProductById(product.id) ?: product.copy(quantityInStock = product.quantityInStock + quantityToAdd)
            addToCart(updatedProd, 1.0)
            scheduleAutoBranchSync()

            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "${product.name} yongerewe mu bubiko (+${quantityToAdd.toInt()}) kandi yongerewe muri POS Cart!"
                else "Restocked ${product.name} (+${quantityToAdd.toInt()} units) and added to POS cart!"
            _snackbarMessage.emit(msg)
        }
    }

    fun deletePurchaseRecord(purchaseId: String) {
        viewModelScope.launch {
            repository.deletePurchaseRecord(purchaseId)
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Inyandiko yo kugura yasibwe."
                else "Purchase order removed from ledger."
            _snackbarMessage.emit(msg)
        }
    }

    // ------------------------------------------------------------------------
    // CUSTOMERS & DEBTS
    // ------------------------------------------------------------------------
    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Umukiriya '${customer.name}' yanditswe neza."
                else "Customer '${customer.name}' saved."
            _snackbarMessage.emit(msg)
        }
    }

    fun recordDebtPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String = "Cash",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                ?: branches.value.firstOrNull { it.isMainBranch }
                ?: com.example.data.model.Branch()

            val payment = CustomerPayment(
                customerId = customerId,
                customerName = customers.value.firstOrNull { it.id == customerId }?.name ?: "Customer",
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes,
                recordedBy = user?.name ?: "Shop Operator",
                branchId = branch.id,
                branchName = branch.name
            )

            val updatedPayment = repository.recordCustomerPayment(payment)
            val symbol = shopProfile.value.currencySymbol

            // Real notification trigger
            BeBossNotificationManager.sendDebtPaymentNotification(
                context = getApplication(),
                customerName = payment.customerName,
                amountPaid = amount,
                remainingDebt = updatedPayment.remainingDebt,
                currencySymbol = symbol
            )
            scheduleAutoBranchSync()

            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) {
                if (updatedPayment.remainingDebt <= 0) {
                    "Kwishyura ${amount.toInt()} $symbol byakiriwe! UMWENDA WARANGIYE NEZA (0 $symbol)!"
                } else {
                    "Kwishyura ${amount.toInt()} $symbol byakiriwe. Hasigaye ${updatedPayment.remainingDebt.toInt()} $symbol."
                }
            } else {
                if (updatedPayment.remainingDebt <= 0) {
                    "Payment of ${amount.toInt()} $symbol received! DEBT FULLY CLEARED (0 $symbol)!"
                } else {
                    "Payment of ${amount.toInt()} $symbol received. Remaining debt: ${updatedPayment.remainingDebt.toInt()} $symbol."
                }
            }
            _snackbarMessage.emit(msg)
        }
    }

    /**
     * Diagnostic testing helper to demonstrate real push notifications across all enterprise channels.
     */
    fun testNotification(channelType: String) {
        val app = getApplication<Application>()
        val shop = shopProfile.value
        when (channelType) {
            "SALE" -> {
                val dummySale = Sale(
                    id = UUID.randomUUID().toString(),
                    customerName = "Makuza Eric",
                    saleDate = System.currentTimeMillis(),
                    totalAmount = 45000.0,
                    paymentMethod = "MOMO",
                    receiptNumber = "REC-DEMO-001"
                )
                BeBossNotificationManager.sendSaleCompletedNotification(app, dummySale, shop)
            }
            "LOW_STOCK" -> {
                BeBossNotificationManager.sendLowStockNotification(app, "Inyange Milk 500ml", 2.0, 5.0, "Cartons")
            }
            "DEBT" -> {
                BeBossNotificationManager.sendDebtPaymentNotification(app, "Gasana Jean", 25000.0, 10000.0, shop.currencySymbol)
            }
            "TRANSFER" -> {
                val branchList = branches.value
                val from = branchList.firstOrNull()?.name ?: "${shop.shopName} (HQ)"
                val to = branchList.getOrNull(1)?.name ?: "Outlet Branch"
                val prod = allProducts.value.firstOrNull()
                val transfer = StockTransfer(
                    transferNumber = "TRF-${(1000..9999).random()}",
                    productId = prod?.id ?: "prod_sample",
                    productName = prod?.name ?: "Inventory Goods",
                    fromBranchId = branchList.firstOrNull()?.id ?: "main_branch",
                    fromBranchName = from,
                    toBranchId = branchList.getOrNull(1)?.id ?: "branch_secondary",
                    toBranchName = to,
                    quantity = 15.0,
                    unit = prod?.unit ?: "units"
                )
                BeBossNotificationManager.sendStockTransferNotification(app, transfer)
            }
            "TARGET" -> {
                BeBossNotificationManager.sendDailyTargetReachedNotification(app, 250000.0, 200000.0, shop.currencySymbol)
            }
            "SYNC" -> {
                val branchName = branches.value.firstOrNull()?.name ?: "${shop.shopName} (HQ)"
                BeBossNotificationManager.sendBranchSyncNotification(app, branchName, allProducts.value.size)
            }
        }
        viewModelScope.launch {
            _snackbarMessage.emit("Sent real Android notification: $channelType")
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                "Umukiriya yasibwe." 
                else "Customer removed."
            _snackbarMessage.emit(msg)
        }
    }

    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSale(saleId)
                refreshDatabaseStats()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Ibyagurishijwe byasibwe, ububiko bwasubijweho."
                    else "Sale transaction deleted and stock restored."
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to delete sale: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // ANALYTICS & REPORTS
    // ------------------------------------------------------------------------
    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _selectedAnalyticsPeriod.value = period
    }

    fun performSync() {
        syncToCloudNow()
    }

    fun updateShopProfile(profile: ShopProfile) {
        viewModelScope.launch {
            repository.updateShopProfile(profile)
            scheduleAutoBranchSync()
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                "Amakuru y'ububiko yavuguruwe."
                else "Shop profile updated."
            _snackbarMessage.emit(msg)
        }
    }

    fun openReceipt(saleId: String) {
        viewModelScope.launch {
            val s = repository.getSaleWithItems(saleId)
            _activeReceiptSale.value = s
        }
    }

    fun closeReceipt() {
        _activeReceiptSale.value = null
    }

    fun importShopPackage(summary: ShopImportSummary) {
        viewModelScope.launch {
            try {
                repository.importShopDataPackage(summary)
                _snackbarMessage.emit("Shop data package successfully restored!")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error restoring shop data: ${e.message}")
            }
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            try {
                repository.seedSampleShopData()
                _snackbarMessage.emit("Sample demo data loaded successfully!")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to load sample data: ${e.message}")
            }
        }
    }

    fun seedSampleCatalog() {
        seedSampleData()
    }

    // ------------------------------------------------------------------------
    // SQLITE DATABASE ENGINE STATE & OPERATIONS
    // ------------------------------------------------------------------------
    private val _databaseStats = MutableStateFlow<com.example.data.model.DatabaseEngineStats?>(null)
    val databaseStats: StateFlow<com.example.data.model.DatabaseEngineStats?> = _databaseStats.asStateFlow()

    private val _databaseMaintenanceResult = MutableStateFlow<com.example.data.model.DatabaseMaintenanceResult?>(null)
    val databaseMaintenanceResult: StateFlow<com.example.data.model.DatabaseMaintenanceResult?> = _databaseMaintenanceResult.asStateFlow()

    private val _isOptimizingDb = MutableStateFlow(false)
    val isOptimizingDb: StateFlow<Boolean> = _isOptimizingDb.asStateFlow()

    fun refreshDatabaseStats(context: android.content.Context? = null) {
        val targetCtx = context ?: getApplication<Application>()
        viewModelScope.launch {
            try {
                val stats = repository.getDatabaseStats(targetCtx)
                _databaseStats.value = stats
            } catch (e: Exception) {
                android.util.Log.e("BeBossViewModel", "Failed to get database stats: ${e.message}")
            }
        }
    }

    fun optimizeDatabase(context: android.content.Context? = null) {
        val targetCtx = context ?: getApplication<Application>()
        viewModelScope.launch {
            _isOptimizingDb.value = true
            try {
                val result = repository.optimizeDatabase(targetCtx)
                _databaseMaintenanceResult.value = result
                result.stats?.let { _databaseStats.value = it }
                _snackbarMessage.emit(result.message)
            } catch (e: Exception) {
                _snackbarMessage.emit("Optimization failed: ${e.message}")
            } finally {
                _isOptimizingDb.value = false
            }
        }
    }

    fun verifyDatabaseIntegrity() {
        viewModelScope.launch {
            _isOptimizingDb.value = true
            try {
                val result = repository.verifyDatabaseIntegrity()
                _databaseMaintenanceResult.value = result
                _snackbarMessage.emit(result.message)
            } catch (e: Exception) {
                _snackbarMessage.emit("Integrity check failed: ${e.message}")
            } finally {
                _isOptimizingDb.value = false
            }
        }
    }

    fun clearTransactionalDataOnly(context: android.content.Context? = null) {
        val targetCtx = context ?: getApplication<Application>()
        viewModelScope.launch {
            try {
                repository.clearTransactionalDataOnly()
                clearCart()
                closeReceipt()
                refreshDatabaseStats(targetCtx)
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Amateka yose yo kugurisha yasibwe! Ibicuruzwa n'abakiriya byagumyeho."
                    else "All sales & transaction history reset! Products & customers preserved."
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to reset transactions: ${e.message}")
            }
        }
    }

    fun clearAllProducts(context: android.content.Context? = null) {
        val targetCtx = context ?: getApplication<Application>()
        viewModelScope.launch {
            try {
                repository.clearAllProducts()
                clearCart()
                _productSearchQuery.value = ""
                refreshDatabaseStats(targetCtx)
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Ibicuruzwa byose byasibwe mu bubiko!"
                    else "All products and inventory stock wiped from database!"
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to clear products: ${e.message}")
            }
        }
    }

    fun clearAllCustomers(context: android.content.Context? = null) {
        val targetCtx = context ?: getApplication<Application>()
        viewModelScope.launch {
            try {
                repository.clearAllCustomers()
                _customerSearchQuery.value = ""
                refreshDatabaseStats(targetCtx)
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Abakiriya bose n'imyenda byasibwe!"
                    else "All customers and credit debt records cleared!"
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to clear customers: ${e.message}")
            }
        }
    }

    fun resetWholeApp(context: android.content.Context? = null) {
        val targetCtx = context ?: getApplication<Application>()
        viewModelScope.launch {
            try {
                repository.resetWholeApp(targetCtx)
                clearCart()
                closeReceipt()
                _productSearchQuery.value = ""
                _customerSearchQuery.value = ""
                refreshDatabaseStats(targetCtx)
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Porogaramu yose yasubijwe ku ntangiriro (Factory Reset)! Amakuru yose yasibwe."
                    else "Whole app factory reset complete! All products, customers & transactions wiped clean."
                _snackbarMessage.emit(msg)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to reset app: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        localNetworkSyncServer.stopDiscovery()
        localNetworkSyncServer.stopServer()
    }
}
