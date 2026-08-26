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
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.data.model.SyncQueueItem
import com.example.data.model.TopProductReport
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BeBossRepository
import com.example.data.repository.CartItem
import com.example.data.repository.SyncResult
import com.example.util.AppLanguage
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

    // Network Connectivity & Cloud Sync State
    val connectivityStatus: StateFlow<com.example.util.ConnectivityStatus> = connectivityObserver.networkStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.ConnectivityStatus.AVAILABLE)

    private val _cloudSyncReport = MutableStateFlow<com.example.util.CloudSyncReport?>(null)
    val cloudSyncReport: StateFlow<com.example.util.CloudSyncReport?> = _cloudSyncReport.asStateFlow()

    // Multi-Branch Management
    val branches: StateFlow<List<com.example.data.model.Branch>> = repository.allBranches.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedBranchId = MutableStateFlow("ALL")
    val selectedBranchId: StateFlow<String> = _selectedBranchId.asStateFlow()

    fun selectBranch(branchId: String) {
        _selectedBranchId.value = branchId
    }

    fun saveBranch(branch: com.example.data.model.Branch) {
        viewModelScope.launch {
            repository.saveBranch(branch)
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                "Ishami '${branch.name}' ryabitswe neza." 
                else "Branch '${branch.name}' saved."
            _snackbarMessage.emit(msg)
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            repository.deleteBranch(branchId)
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
                    val report = repository.cloudSyncManager.syncAllDataToCloud()
                    _cloudSyncReport.value = report
                }
            }
        }
    }

    fun syncToCloudNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            val report = repository.cloudSyncManager.syncAllDataToCloud()
            _cloudSyncReport.value = report
            _isSyncing.value = false
            _snackbarMessage.emit(report.message)
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
            val isValid = user != null && (
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
            _snackbarMessage.emit("Staff member '${user.name}' saved.")
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            if (_currentUser.value?.id == userId) {
                _snackbarMessage.emit("Cannot delete the currently active account.")
                return@launch
            }
            repository.deleteUser(userId)
            _snackbarMessage.emit("Staff account removed.")
        }
    }

    // ------------------------------------------------------------------------
    // SUBSCRIPTION & MULTI-BRANCH / WORKER BILLING
    // ------------------------------------------------------------------------
    fun activateSubscriptionVoucher(code: String) {
        viewModelScope.launch {
            val currentProf = shopProfile.value
            val branchCount = branches.value.size.coerceAtLeast(1)
            val workerCount = allUsers.value.size.coerceAtLeast(1)
            val result = com.example.util.OfflineSubscriptionManager.validateVoucherCode(
                inputCode = code,
                shopProfile = currentProf,
                branchCount = branchCount,
                workerCount = workerCount
            )
            if (result.isValid) {
                val currentExpiry = if (currentProf.subscriptionExpiresAt > System.currentTimeMillis()) 
                    currentProf.subscriptionExpiresAt 
                    else System.currentTimeMillis()
                val newExpiry = currentExpiry + (result.daysToAdd.toLong() * 24 * 60 * 60 * 1000)

                val updatedProf = currentProf.copy(
                    subscriptionStatus = "ACTIVE",
                    subscriptionExpiresAt = newExpiry,
                    lastPaymentRef = code.trim().uppercase(),
                    lastPaymentAmount = result.verifiedAmount,
                    lastPaymentDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateShopProfile(updatedProf)
                _snackbarMessage.emit(result.message)
                // Auto-sync subscription status to cloud immediately
                syncToCloudNow()
            } else {
                _snackbarMessage.emit(result.message)
            }
        }
    }

    fun processDirectSubscriptionPayment(
        provider: String,
        phone: String,
        durationMonths: Int,
        onSuccess: (com.example.util.PaymentProcessingResult, com.example.util.SubscriptionPriceBreakdown) -> Unit
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
            val result = com.example.util.OfflineSubscriptionManager.processDirectMoMoPayment(
                shopProfile = currentProf,
                payerPhone = phone,
                provider = provider,
                branchCount = branchCount,
                workerCount = workerCount,
                durationMonths = durationMonths
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
                // Auto-sync subscription extension to database & Firebase cloud
                syncToCloudNow()
                onSuccess(result, breakdown)
            }
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
                val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                    "Igurisha ryakozwe neza! Inyemezabuguzi irasohotse." 
                    else "Sale recorded successfully! Receipt ready."
                _snackbarMessage.emit(msg)
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
            append("📊 *${shop.shopName.ifBlank { "BeBoss Store" }} - Daily Business Report*\n")
            append("📅 $timeStr\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("💰 *Today's Revenue:* ${summary.totalRevenue.toInt()} ${shop.currencySymbol}\n")
            append("📈 *Net Estimated Profit:* ${summary.netProfit.toInt()} ${shop.currencySymbol}\n")
            append("🧾 *Transactions Completed:* ${summary.totalSalesCount}\n")
            append("📦 *Items Sold:* ${summary.totalItemsSold.toInt()}\n")
            append("🎯 *Average Basket Value:* ${summary.averageOrderValue.toInt()} ${shop.currencySymbol}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (lowStock.isNotEmpty()) {
                append("⚠️ *Low Stock Alert (${lowStock.size} items):*\n")
                lowStock.take(5).forEach { p ->
                    append("• ${p.name}: ${p.quantityInStock.toInt()} ${p.unit} left\n")
                }
            } else {
                append("✅ All product stocks are healthy!\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🚀 _Generated with BeBoss Offline Business POS_")
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
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                "Igicuruzwa '${product.name}' cyabitswe neza." 
                else "Product '${product.name}' saved."
            _snackbarMessage.emit(msg)
        }
    }

    fun adjustStock(productId: String, delta: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta, reason)
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                "Ingano y'ububiko yahinduwe (${if (delta > 0) "+$delta" else "$delta"})." 
                else "Stock adjusted (${if (delta > 0) "+$delta" else "$delta"})."
            _snackbarMessage.emit(msg)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                "Igicuruzwa cyasibwe." 
                else "Product deleted."
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

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            val msg = if (_currentLanguage.value == AppLanguage.KINYARWANDA) 
                "Umukiriya yasibwe." 
                else "Customer removed."
            _snackbarMessage.emit(msg)
        }
    }

    // ------------------------------------------------------------------------
    // ANALYTICS & REPORTS
    // ------------------------------------------------------------------------
    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _selectedAnalyticsPeriod.value = period
    }

    fun performSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.performSync()
            _isSyncing.value = false
            _snackbarMessage.emit(result.message)
        }
    }

    fun updateShopProfile(profile: ShopProfile) {
        viewModelScope.launch {
            repository.updateShopProfile(profile)
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
}
