package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.model.LocalAuditLog
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.PurchaseRecord
import com.example.data.model.PurchaseSummary
import com.example.data.model.Sale
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.TopProductReport
import com.example.data.model.User
import com.example.data.repository.BeBossRepository
import com.example.ui.viewmodel.coordinators.AdminConsoleCoordinator
import com.example.ui.viewmodel.coordinators.AuthSessionCoordinator
import com.example.ui.viewmodel.coordinators.DatabaseMaintenanceCoordinator
import com.example.ui.viewmodel.coordinators.PosCartCoordinator
import com.example.ui.viewmodel.coordinators.SyncCoordinator
import com.example.util.AppLanguage
import com.example.util.BeBossNotificationManager
import com.example.util.CloudSyncReport
import com.example.util.ConnectivityStatus
import com.example.util.DeviceIntegrityStatus
import com.example.util.DeviceSecurityManager
import com.example.util.EndpointPingResult
import com.example.util.NetworkConnectivityObserver
import com.example.util.OfflineSubscriptionManager
import com.example.util.PaymentProcessingResult
import com.example.util.ShopImportSummary
import com.example.util.SubscriptionPriceBreakdown
import com.example.util.SupabaseAdminClient
import com.example.util.SupabaseAdminSession
import com.example.util.SupabaseAuditLogDto
import com.example.util.SupabaseBranchSnapshotDto
import com.example.util.SupabasePaymentRequestDto
import com.example.util.SupabaseShopAccountDto
import com.example.util.SupabaseShopUserDto
import com.example.util.SupabaseUnlockCodeDto
import com.example.util.SyncAuditLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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

@OptIn(ExperimentalCoroutinesApi::class)
class BeBossViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("beboss_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(application)
    val repository = BeBossRepository(db)
    private val connectivityObserver = NetworkConnectivityObserver(application)
    private val deviceId: String = SupabaseAdminClient.getOrCreateDeviceId(application)

    private val _deviceIntegrityStatus = MutableStateFlow(DeviceSecurityManager.inspectDeviceIntegrity())
    val deviceIntegrityStatus: StateFlow<DeviceIntegrityStatus> = _deviceIntegrityStatus.asStateFlow()

    private var idleLockJob: Job? = null
    private val idleLockTimeoutMs: Long = 5 * 60_000L

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Language & Theme State
    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("lang", "en") == "rw") AppLanguage.KINYARWANDA else AppLanguage.ENGLISH
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Branch & Navigation State
    private val _selectedBranchId = MutableStateFlow("ALL")
    val selectedBranchId: StateFlow<String> = _selectedBranchId.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Core Reactive Room Flows
    val shopProfile: StateFlow<ShopProfile> = repository.shopProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopProfile()
    )

    val allUsers: StateFlow<List<User>> = repository.allActiveUsers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val branches: StateFlow<List<Branch>> = repository.allBranches.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allStockTransfers: StateFlow<List<StockTransfer>> = repository.allStockTransfers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
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

    // Sales & Purchases
    val allSales: StateFlow<List<Sale>> = repository.allSales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

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

    val todaySummary: StateFlow<ProfitLossSummary> = repository.getProfitLossForPeriod(AnalyticsPeriod.TODAY)
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProfitLossSummary(0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, true, "Today")
        )

    // Sync Queue, Audit & Licensing Flows
    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val deadLetterSyncCount: StateFlow<Int> = repository.deadLetterSyncCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val paymentVerificationRequests: StateFlow<List<PaymentVerificationRequest>> =
        repository.allPaymentRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localAuditLogs: StateFlow<List<LocalAuditLog>> = repository.localAuditLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val syncAuditLogs: StateFlow<List<SyncAuditLog>> = repository.syncAuditLogs

    // ------------------------------------------------------------------------
    // FEATURE COORDINATORS
    // ------------------------------------------------------------------------
    private val authCoordinator = AuthSessionCoordinator(
        application = application,
        prefs = prefs,
        db = db,
        repository = repository,
        scope = viewModelScope,
        language = currentLanguage,
        emitSnackbar = { msg -> _snackbarMessage.emit(msg) }
    )

    private lateinit var adminCoordinator: AdminConsoleCoordinator
    private lateinit var syncCoordinator: SyncCoordinator
    private lateinit var posCoordinator: PosCartCoordinator
    private lateinit var dbCoordinator: DatabaseMaintenanceCoordinator

    init {
        syncCoordinator = SyncCoordinator(
            application = application,
            db = db,
            repository = repository,
            connectivityObserver = connectivityObserver,
            deviceId = deviceId,
            scope = viewModelScope,
            currentUser = authCoordinator.currentUser,
            selectedBranchId = selectedBranchId,
            shopProfile = shopProfile,
            allUsers = allUsers,
            branches = branches,
            allProducts = allProducts,
            customers = customers,
            allSales = allSales,
            allCustomerPayments = allCustomerPayments,
            allStockTransfers = allStockTransfers,
            emitSnackbar = { msg -> _snackbarMessage.emit(msg) },
            syncAdminCloud = { silent -> adminCoordinator.syncAdminDataToSupabaseSuspended(silent) }
        )

        adminCoordinator = AdminConsoleCoordinator(
            repository = repository,
            scope = viewModelScope,
            currentUser = authCoordinator.currentUser,
            allUsers = allUsers,
            branches = branches,
            shopProfile = shopProfile,
            emitSnackbar = { msg -> _snackbarMessage.emit(msg) },
            triggerCloudSync = { syncCoordinator.syncToCloudNow() }
        )

        posCoordinator = PosCartCoordinator(
            application = application,
            repository = repository,
            scope = viewModelScope,
            language = currentLanguage,
            currentUser = authCoordinator.currentUser,
            selectedBranchId = selectedBranchId,
            branches = branches,
            shopProfile = shopProfile,
            allSales = allSales,
            todaySummary = todaySummary,
            lowStockProducts = lowStockProducts,
            emitSnackbar = { msg -> _snackbarMessage.emit(msg) },
            onMutationSuccess = { syncCoordinator.scheduleAutoBranchSync() }
        )

        dbCoordinator = DatabaseMaintenanceCoordinator(
            application = application,
            repository = repository,
            scope = viewModelScope,
            language = currentLanguage,
            onClearUiSessionState = {
                posCoordinator.clearCart()
                posCoordinator.closeReceipt()
            },
            emitSnackbar = { msg -> _snackbarMessage.emit(msg) }
        )

        authCoordinator.checkAuthenticationAndInit()
        syncCoordinator.setupAutoCloudSyncOnInternet()
        dbCoordinator.verifyStartupIntegritySilently()

        val integrity = _deviceIntegrityStatus.value
        if (integrity.isCompromisedRisk) {
            viewModelScope.launch {
                repository.recordLocalAudit(
                    actorName = "System",
                    actorRole = "SECURITY",
                    action = "DEVICE_INTEGRITY_WARNING",
                    targetId = deviceId,
                    details = integrity.signals.joinToString("; ")
                )
            }
        }
        onUserInteraction()
    }

    fun onUserInteraction() {
        idleLockJob?.cancel()
        if (!isLocked.value && !isRegistrationNeeded.value) {
            idleLockJob = viewModelScope.launch {
                delay(idleLockTimeoutMs)
                if (!isLocked.value && !isRegistrationNeeded.value) {
                    lockApp()
                }
            }
        }
    }

    // Delegated Auth Flows
    val currentUser: StateFlow<User?> = authCoordinator.currentUser
    val isLocked: StateFlow<Boolean> = authCoordinator.isLocked
    val isRegistrationNeeded: StateFlow<Boolean> = authCoordinator.isRegistrationNeeded
    val authError: StateFlow<String?> = authCoordinator.authError

    // Delegated POS Flows
    val cartState: StateFlow<CartState> = posCoordinator.cartState
    val activeReceiptSale: StateFlow<SaleWithItems?> = posCoordinator.activeReceiptSale

    // Delegated Sync & LAN P2P Flows
    val connectivityStatus: StateFlow<ConnectivityStatus> = syncCoordinator.connectivityStatus
    val cloudSyncReport: StateFlow<CloudSyncReport?> = syncCoordinator.cloudSyncReport
    val isSyncing: StateFlow<Boolean> = syncCoordinator.isSyncing
    val isP2pSyncing: StateFlow<Boolean> = syncCoordinator.isP2pSyncing
    val localNetworkSyncServer = syncCoordinator.localNetworkSyncServer
    val localServerStatus = syncCoordinator.localServerStatus
    val localServerIp = syncCoordinator.localServerIp
    val localConnectedClients = syncCoordinator.localConnectedClients
    val localLastReceivedPacket = syncCoordinator.localLastReceivedPacket
    val discoveredHubs = syncCoordinator.discoveredHubs
    val isDiscoveringHubs = syncCoordinator.isDiscoveringHubs
    val peerSyncEvents = syncCoordinator.peerSyncEvents

    // Delegated Admin Console Flows
    val adminSession: StateFlow<SupabaseAdminSession?> = adminCoordinator.adminSession
    val adminAuthError: StateFlow<String?> = adminCoordinator.adminAuthError
    val isAdminAuthenticating: StateFlow<Boolean> = adminCoordinator.isAdminAuthenticating
    val adminPaymentRequests: StateFlow<List<SupabasePaymentRequestDto>> = adminCoordinator.adminPaymentRequests
    val adminShopAccounts: StateFlow<List<SupabaseShopAccountDto>> = adminCoordinator.adminShopAccounts
    val adminShopUsers: StateFlow<List<SupabaseShopUserDto>> = adminCoordinator.adminShopUsers
    val adminBranchSnapshots: StateFlow<List<SupabaseBranchSnapshotDto>> = adminCoordinator.adminBranchSnapshots
    val adminUnlockCodes: StateFlow<List<SupabaseUnlockCodeDto>> = adminCoordinator.adminUnlockCodes
    val adminAuditLogs: StateFlow<List<SupabaseAuditLogDto>> = adminCoordinator.adminAuditLogs
    val isAdminLoading: StateFlow<Boolean> = adminCoordinator.isAdminLoading

    // Delegated Database Engine Flows
    val databaseStats: StateFlow<DatabaseEngineStats?> = dbCoordinator.databaseStats
    val databaseMaintenanceResult: StateFlow<DatabaseMaintenanceResult?> = dbCoordinator.databaseMaintenanceResult
    val isOptimizingDb: StateFlow<Boolean> = dbCoordinator.isOptimizingDb
    val startupIntegrityOk: StateFlow<Boolean?> = dbCoordinator.startupIntegrityOk

    // ------------------------------------------------------------------------
    // LANGUAGE, THEME & NAVIGATION
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

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectBranch(branchId: String) {
        _selectedBranchId.value = branchId
    }

    // ------------------------------------------------------------------------
    // AUTHENTICATION DELEGATION
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
    ) = authCoordinator.registerNewShopAndOwner(
        shopName, currencyCode, currencySymbol, address, phone,
        ownerFullName, username, pin, password
    )

    fun loginWithPin(pin: String): Boolean = authCoordinator.loginWithPin(pin)
    fun loginWithCredentials(username: String, pinOrPass: String): Boolean = authCoordinator.loginWithCredentials(username, pinOrPass)
    fun unlockAppWithPin(pin: String): Boolean = authCoordinator.unlockAppWithPin(pin)
    fun unlockWithBiometric(verifiedByHardware: Boolean) = authCoordinator.unlockWithBiometric(verifiedByHardware)
    fun reportBiometricError(message: String) = authCoordinator.reportBiometricError(message)
    fun reportBiometricCancelled() = authCoordinator.reportBiometricCancelled()
    fun selectLockedOperator(user: User) = authCoordinator.selectLockedOperator(user)
    fun lockApp() = authCoordinator.lockApp()
    fun logoutUser() = authCoordinator.logoutUser()
    fun switchUser(user: User) = authCoordinator.switchUser(user)
    fun showRegistrationScreen() = authCoordinator.showRegistrationScreen()
    fun cancelRegistration() = authCoordinator.cancelRegistration()
    fun clearAuthError() = authCoordinator.clearAuthError()

    // ------------------------------------------------------------------------
    // POS & CART DELEGATION
    // ------------------------------------------------------------------------
    fun addToCart(product: Product, quantity: Double = 1.0) = posCoordinator.addToCart(product, quantity)
    fun updateCartItemQuantity(productId: String, newQuantity: Double) = posCoordinator.updateCartItemQuantity(productId, newQuantity)
    fun removeCartItem(productId: String) = posCoordinator.removeCartItem(productId)
    fun clearCart() = posCoordinator.clearCart()
    fun setCartCustomer(customer: Customer?) = posCoordinator.setCartCustomer(customer)
    fun setCartPaymentMethod(method: String) = posCoordinator.setCartPaymentMethod(method)
    fun setCartDiscount(discount: Double) = posCoordinator.setCartDiscount(discount)
    fun setCartAmountPaidInput(input: String) = posCoordinator.setCartAmountPaidInput(input)
    fun setCartNotes(notes: String) = posCoordinator.setCartNotes(notes)
    fun checkoutSale() = posCoordinator.checkoutSale()
    fun performQuickCustomSale(description: String, amount: Double, paymentMethod: String, isDirectCheckout: Boolean = true) =
        posCoordinator.performQuickCustomSale(description, amount, paymentMethod, isDirectCheckout)
    fun openReceipt(saleId: String) = posCoordinator.openReceipt(saleId)
    fun openLastReceipt() = posCoordinator.openLastReceipt()
    fun closeReceipt() = posCoordinator.closeReceipt()
    fun shareDailyWhatsAppSummary(context: Context) = posCoordinator.shareDailyWhatsAppSummary(context)

    // ------------------------------------------------------------------------
    // SYNC & P2P DELEGATION
    // ------------------------------------------------------------------------
    fun scheduleAutoBranchSync(delayMs: Long = 1600L) = syncCoordinator.scheduleAutoBranchSync(delayMs)
    fun performSync() = syncCoordinator.syncToCloudNow()
    fun syncToCloudNow() = syncCoordinator.syncToCloudNow()
    fun syncToCloudWithEndpoint(endpoint: String? = null) = syncCoordinator.syncToCloudWithEndpoint(endpoint)
    fun testEndpointPing(endpoint: String, onResult: (EndpointPingResult) -> Unit) = syncCoordinator.testEndpointPing(endpoint, onResult)
    fun toggleLocalSyncServer(enable: Boolean) = syncCoordinator.toggleLocalSyncServer(enable)
    fun startLanDiscovery() = syncCoordinator.startLanDiscovery()
    fun stopLanDiscovery() = syncCoordinator.stopLanDiscovery()
    fun syncWithLocalMasterHub(hubIp: String) = syncCoordinator.syncWithLocalMasterHub(hubIp)
    fun createBranchShareToken(branchIdFilter: String? = null, onReady: (String) -> Unit) = syncCoordinator.createBranchShareToken(branchIdFilter, onReady)
    fun exportFullBranchPackage(context: Context, branchIdFilter: String? = null, onReady: (File) -> Unit) =
        syncCoordinator.exportFullBranchPackage(context, branchIdFilter, onReady)
    fun importBranchShareToken(tokenOrJson: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) =
        syncCoordinator.importBranchShareToken(tokenOrJson, onDone)
    fun importShopPackage(summary: ShopImportSummary) = syncCoordinator.importShopPackage(summary)

    // ------------------------------------------------------------------------
    // ADMIN CONSOLE & SUBSCRIPTION DELEGATION
    // ------------------------------------------------------------------------
    fun signInSupabaseAdmin(email: String, password: String) = adminCoordinator.signInSupabaseAdmin(email, password)
    fun signUpSupabaseAdmin(email: String, password: String, fullName: String) = adminCoordinator.signUpSupabaseAdmin(email, password, fullName)
    fun signOutSupabaseAdmin() = adminCoordinator.signOutSupabaseAdmin()
    fun activateSubscriptionVoucher(code: String) = adminCoordinator.activateSubscriptionVoucher(code)
    fun submitUssdPaymentProof(
        provider: String,
        payerPhone: String,
        username: String,
        registeredPhone: String,
        durationMonths: Int,
        branchCount: Int,
        workerCount: Int,
        telcoReference: String = "",
        onSubmitted: (PaymentVerificationRequest, Boolean) -> Unit = { _, _ -> }
    ) = adminCoordinator.submitUssdPaymentProof(
        provider, payerPhone, username, registeredPhone, durationMonths,
        branchCount, workerCount, telcoReference, onSubmitted
    )

    fun syncAdminDataToSupabase(silent: Boolean = false) = adminCoordinator.syncAdminDataToSupabase(silent)
    fun checkCloudUnlockApprovals() = adminCoordinator.checkCloudUnlockApprovals()
    fun refreshAdminDashboardData() = adminCoordinator.refreshAdminDashboardData()
    fun adminApproveAndGenerateCode(request: SupabasePaymentRequestDto, onGenerated: (Boolean, String) -> Unit) =
        adminCoordinator.adminApproveAndGenerateCode(request, onGenerated)
    fun adminApproveBatchPendingPayments(onDone: (Int, Int) -> Unit = { _, _ -> }) =
        adminCoordinator.adminApproveBatchPendingPayments(onDone)
    fun adminRejectPayment(request: SupabasePaymentRequestDto, reason: String, onDone: (Boolean) -> Unit = {}) =
        adminCoordinator.adminRejectPayment(request, reason, onDone)
    fun adminRevokeCode(codeDto: SupabaseUnlockCodeDto) = adminCoordinator.adminRevokeCode(codeDto)
    fun adminIssueManualCode(
        username: String,
        registeredPhone: String,
        tierCode: String,
        days: Int,
        amountRwf: Int,
        onGenerated: (Boolean, String) -> Unit
    ) = adminCoordinator.adminIssueManualCode(username, registeredPhone, tierCode, days, amountRwf, onGenerated)

    fun adminExtendShopSubscription(shop: SupabaseShopAccountDto, addDays: Int) = adminCoordinator.adminExtendShopSubscription(shop, addDays)
    fun adminSetShopStatus(shop: SupabaseShopAccountDto, status: String) = adminCoordinator.adminSetShopStatus(shop, status)
    fun adminUpdateShopLimits(shop: SupabaseShopAccountDto, branchCount: Int, workerCount: Int) =
        adminCoordinator.adminUpdateShopLimits(shop, branchCount, workerCount)
    fun adminDeleteShopAccount(shop: SupabaseShopAccountDto) = adminCoordinator.adminDeleteShopAccount(shop)
    fun adminUpdateShopUserRole(user: SupabaseShopUserDto, newRole: String) = adminCoordinator.adminUpdateShopUserRole(user, newRole)
    fun adminToggleShopUserActive(user: SupabaseShopUserDto, newActive: Boolean) = adminCoordinator.adminToggleShopUserActive(user, newActive)
    fun adminDeleteShopUser(user: SupabaseShopUserDto) = adminCoordinator.adminDeleteShopUser(user)
    fun adminDeleteBranchSnapshot(snapshot: SupabaseBranchSnapshotDto) = adminCoordinator.adminDeleteBranchSnapshot(snapshot)
    fun shareAdminWebPortal(context: Context) = adminCoordinator.shareAdminWebPortal(context)
    fun processDirectSubscriptionPayment(
        provider: String,
        phone: String,
        durationMonths: Int,
        userReference: String? = null,
        onResult: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit
    ) = adminCoordinator.processDirectSubscriptionPayment(provider, phone, durationMonths, userReference, onResult)

    // ------------------------------------------------------------------------
    // BRANCH, STAFF, PRODUCT, PURCHASE, CUSTOMER MUTATIONS
    // ------------------------------------------------------------------------
    fun saveBranch(branch: Branch) {
        viewModelScope.launch {
            try {
                repository.saveBranch(branch, actor = currentUser.value)
                val allBranches = db.branchDao().getAllActiveBranchesList()
                val allStaff = db.userDao().getAllActiveUsersList()
                val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
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
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Branch error: ${e.message}")
            }
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBranch(branchId, actor = currentUser.value)
                val allBranches = db.branchDao().getAllActiveBranchesList()
                val allStaff = db.userDao().getAllActiveUsersList()
                val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
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
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Delete branch error: ${e.message}")
            }
        }
    }

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
                val user = currentUser.value
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

    fun saveUser(user: User) {
        viewModelScope.launch {
            try {
                repository.saveUser(user, actor = currentUser.value)
                val allBranches = db.branchDao().getAllActiveBranchesList()
                val allStaff = db.userDao().getAllActiveUsersList()
                val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
                    branchCount = allBranches.size.coerceAtLeast(1),
                    workerCount = allStaff.size.coerceAtLeast(1)
                )
                repository.updateShopProfile(
                    shopProfile.value.copy(
                        monthlyFeeRwf = newPricing.monthlySubtotal,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                repository.recordLocalAudit(
                    actorName = currentUser.value?.name ?: "Owner",
                    actorRole = currentUser.value?.role?.name ?: "OWNER",
                    action = "STAFF_SAVED",
                    targetId = user.username,
                    details = "Saved staff member '${user.name}' (${user.role.name})"
                )
                scheduleAutoBranchSync()
                _snackbarMessage.emit("Staff member '${user.name}' saved (${newPricing.monthlySubtotal} FRw/mo total plan).")
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Staff save error: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                if (currentUser.value?.id == userId) {
                    _snackbarMessage.emit("Cannot delete the currently active account.")
                    return@launch
                }
                repository.deleteUser(userId, actor = currentUser.value)
                val allBranches = db.branchDao().getAllActiveBranchesList()
                val allStaff = db.userDao().getAllActiveUsersList()
                val newPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
                    branchCount = allBranches.size.coerceAtLeast(1),
                    workerCount = allStaff.size.coerceAtLeast(1)
                )
                repository.updateShopProfile(
                    shopProfile.value.copy(
                        monthlyFeeRwf = newPricing.monthlySubtotal,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                repository.recordLocalAudit(
                    actorName = currentUser.value?.name ?: "Owner",
                    actorRole = currentUser.value?.role?.name ?: "OWNER",
                    action = "STAFF_REMOVED",
                    targetId = userId,
                    details = "Removed staff account"
                )
                scheduleAutoBranchSync()
                _snackbarMessage.emit("Staff account removed.")
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Staff delete error: ${e.message}")
            }
        }
    }

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
            try {
                repository.saveProduct(product, actor = currentUser.value)
                scheduleAutoBranchSync()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Igicuruzwa '${product.name}' cyabitswe neza."
                else "Product '${product.name}' saved."
                _snackbarMessage.emit(msg)
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Product save error: ${e.message}")
            }
        }
    }

    fun adjustStock(productId: String, delta: Double, reason: String) {
        viewModelScope.launch {
            try {
                repository.adjustStock(productId, delta, reason, actor = currentUser.value)
                scheduleAutoBranchSync()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Ingano y'ububiko yahinduwe (${if (delta > 0) "+$delta" else "$delta"})."
                else "Stock adjusted (${if (delta > 0) "+$delta" else "$delta"})."
                _snackbarMessage.emit(msg)
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Stock adjust error: ${e.message}")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId, actor = currentUser.value)
                scheduleAutoBranchSync()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Igicuruzwa cyasibwe."
                else "Product deleted."
                _snackbarMessage.emit(msg)
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Delete product error: ${e.message}")
            }
        }
    }

    fun restoreDeletedProduct(productId: String) {
        viewModelScope.launch {
            try {
                repository.restoreProduct(productId, actor = currentUser.value)
                scheduleAutoBranchSync()
                _snackbarMessage.emit("Product restored.")
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Restore error: ${e.message}")
            }
        }
    }

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
            val user = currentUser.value
            val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                ?: branches.value.firstOrNull { it.isMainBranch }
                ?: Branch()

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
            val user = currentUser.value
            val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                ?: branches.value.firstOrNull { it.isMainBranch }
                ?: Branch()

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

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                repository.saveCustomer(customer, actor = currentUser.value)
                scheduleAutoBranchSync()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Umukiriya '${customer.name}' yanditswe neza."
                else "Customer '${customer.name}' saved."
                _snackbarMessage.emit(msg)
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Customer save error: ${e.message}")
            }
        }
    }

    fun recordDebtPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String = "Cash",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val user = currentUser.value
                val branch = branches.value.firstOrNull { it.id == _selectedBranchId.value }
                    ?: branches.value.firstOrNull { it.isMainBranch }
                    ?: Branch()

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
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Debt payment error: ${e.message}")
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customerId, actor = currentUser.value)
                scheduleAutoBranchSync()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Umukiriya yasibwe."
                else "Customer removed."
                _snackbarMessage.emit(msg)
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Customer remove error: ${e.message}")
            }
        }
    }

    fun restoreDeletedCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                repository.restoreCustomer(customerId, actor = currentUser.value)
                scheduleAutoBranchSync()
                _snackbarMessage.emit("Customer restored.")
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Restore error: ${e.message}")
            }
        }
    }

    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSale(saleId, actor = currentUser.value)
                refreshDatabaseStats()
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA)
                    "Ibyagurishijwe byasibwe, ububiko bwasubijweho."
                else "Sale transaction deleted and stock restored."
                _snackbarMessage.emit(msg)
            } catch (e: SecurityException) {
                _snackbarMessage.emit(e.message ?: "Permission denied")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to delete sale: ${e.message}")
            }
        }
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _selectedAnalyticsPeriod.value = period
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

    // ------------------------------------------------------------------------
    // DATABASE ENGINE DELEGATION
    // ------------------------------------------------------------------------
    fun refreshDatabaseStats(context: Context? = null) = dbCoordinator.refreshDatabaseStats(context)
    fun optimizeDatabase(context: Context? = null) = dbCoordinator.optimizeDatabase(context)
    fun verifyDatabaseIntegrity() = dbCoordinator.verifyDatabaseIntegrity()
    fun clearTransactionalDataOnly(context: Context? = null) = dbCoordinator.clearTransactionalDataOnly(context)
    fun clearAllProducts(context: Context? = null) = dbCoordinator.clearAllProducts(context) {
        posCoordinator.clearCart()
        _productSearchQuery.value = ""
    }

    fun clearAllCustomers(context: Context? = null) = dbCoordinator.clearAllCustomers(context) {
        _customerSearchQuery.value = ""
    }

    fun resetWholeApp(context: Context? = null) = dbCoordinator.resetWholeApp(context) {
        _productSearchQuery.value = ""
        _customerSearchQuery.value = ""
    }

    fun seedSampleData() = dbCoordinator.seedSampleData()
    fun seedSampleCatalog() = dbCoordinator.seedSampleData()

    override fun onCleared() {
        super.onCleared()
        idleLockJob?.cancel()
        syncCoordinator.cleanup()
    }
}
