package com.example.ui.viewmodel

import android.app.Application
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

enum class AppScreen(val title: String) {
    DASHBOARD("Dashboard"),
    SALES_POS("New Sale"),
    INVENTORY("Inventory"),
    ANALYTICS("Reports & P/L"),
    CUSTOMERS("Customers & Debt"),
    SALES_HISTORY("Sales History"),
    SETTINGS("Settings & Sync")
}

data class CartState(
    val items: List<CartItem> = emptyList(),
    val selectedCustomerId: String? = null,
    val selectedCustomerName: String = "Walk-in Customer",
    val discountAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, MOMO, CARD, CREDIT_DEBT
    val amountPaidInput: String = "",
    val notes: String = ""
) {
    val rawTotal: Double
        get() = items.sumOf { it.product.sellingPrice * it.quantity }

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

    private val db = AppDatabase.getDatabase(application)
    val repository = BeBossRepository(db)

    // Authentication & Staff State
    val allUsers: StateFlow<List<User>> = repository.allActiveUsers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Current Screen
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
    val pendingSyncItems: StateFlow<List<SyncQueueItem>> = repository.pendingSyncItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Dialogs / Sheets State
    private val _activeReceiptSale = MutableStateFlow<SaleWithItems?>(null)
    val activeReceiptSale: StateFlow<SaleWithItems?> = _activeReceiptSale.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        // Automatically check if database is empty and seed helpful starter catalog and default admin user
        viewModelScope.launch {
            val user = repository.ensureDefaultAdminUser()
            _currentUser.value = user

            val count = repository.totalProductCount.stateIn(viewModelScope).value
            if (count == 0) {
                repository.seedSampleShopData()
            }
        }
    }

    // ------------------------------------------------------------------------
    // AUTHENTICATION & STAFF MANAGEMENT
    // ------------------------------------------------------------------------
    fun loginWithPin(pin: String): Boolean {
        var success = false
        viewModelScope.launch {
            val user = repository.getUserByPin(pin)
            if (user != null) {
                _currentUser.value = user
                _isLocked.value = false
                _authError.value = null
                repository.updateLastLogin(user.id)
                _snackbarMessage.emit("Welcome back, ${user.name}!")
                success = true
            } else {
                _authError.value = "Incorrect PIN code. Try again."
            }
        }
        return success
    }

    fun loginWithCredentials(username: String, pinOrPass: String): Boolean {
        viewModelScope.launch {
            val user = repository.getUserByUsername(username)
            if (user != null && (user.pinHash == pinOrPass || user.password == pinOrPass)) {
                _currentUser.value = user
                _isLocked.value = false
                _authError.value = null
                repository.updateLastLogin(user.id)
                _snackbarMessage.emit("Logged in as ${user.name} (${user.role.displayName})")
            } else {
                _authError.value = "Invalid username or credentials"
            }
        }
        return true
    }

    fun switchUser(user: User) {
        _currentUser.value = user
        _isLocked.value = false
        _authError.value = null
        viewModelScope.launch {
            repository.updateLastLogin(user.id)
            _snackbarMessage.emit("Switched active user to: ${user.name}")
        }
    }

    fun lockApp() {
        _isLocked.value = true
    }

    fun unlockAppWithPin(pin: String): Boolean {
        val active = _currentUser.value
        if (active != null && active.pinHash == pin) {
            _isLocked.value = false
            _authError.value = null
            return true
        }
        // Also check if any valid user PIN matches
        viewModelScope.launch {
            val matchedUser = repository.getUserByPin(pin)
            if (matchedUser != null) {
                _currentUser.value = matchedUser
                _isLocked.value = false
                _authError.value = null
                _snackbarMessage.emit("Unlocked as ${matchedUser.name}")
            } else {
                _authError.value = "Incorrect PIN. Try again."
            }
        }
        return false
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
                _snackbarMessage.emit("Cannot delete currently logged-in account.")
                return@launch
            }
            repository.deleteUser(userId)
            _snackbarMessage.emit("Staff account removed.")
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun getPaymentsForCustomer(customerId: String) = repository.getPaymentsForCustomer(customerId)
    fun getSalesForCustomer(customerId: String) = repository.getSalesForCustomer(customerId)

    fun recordCustomerPaymentWithReceipt(
        customerId: String,
        customerName: String,
        amount: Double,
        paymentMethod: String = "Cash",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val payment = CustomerPayment(
                customerId = customerId,
                customerName = customerName,
                amount = amount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.recordCustomerPayment(payment)
            _snackbarMessage.emit("Payment of ${amount.toInt()} FRw received and recorded!")
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // ------------------------------------------------------------------------
    // CART & POS ACTIONS
    // ------------------------------------------------------------------------
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val current = _cartState.value.items.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = (existing.quantity + quantity).coerceAtMost(product.quantityInStock.coerceAtLeast(1.0))
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
                val sale = repository.processSale(
                    items = state.items,
                    customerId = state.selectedCustomerId,
                    customerName = state.selectedCustomerName,
                    discountAmount = state.discountAmount,
                    paymentMethod = state.paymentMethod,
                    amountPaid = state.effectiveAmountPaid,
                    notes = state.notes
                )
                val saleWithItems = repository.getSaleWithItems(sale.id)
                _activeReceiptSale.value = saleWithItems
                clearCart()
                _snackbarMessage.emit("Sale recorded! Stock updated.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Error processing sale: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // PRODUCT ACTIONS
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
            _snackbarMessage.emit("Product '${product.name}' saved.")
        }
    }

    fun adjustStock(productId: String, delta: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta, reason)
            _snackbarMessage.emit("Stock adjusted (${if (delta > 0) "+$delta" else "$delta"}).")
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _snackbarMessage.emit("Product deleted.")
        }
    }

    // ------------------------------------------------------------------------
    // CUSTOMER ACTIONS
    // ------------------------------------------------------------------------
    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            _snackbarMessage.emit("Customer '${customer.name}' saved.")
        }
    }

    fun recordDebtPayment(customerId: String, amount: Double) {
        viewModelScope.launch {
            repository.recordDebtPayment(customerId, amount)
            _snackbarMessage.emit("Recorded payment of $amount FRw.")
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            _snackbarMessage.emit("Customer removed.")
        }
    }

    // ------------------------------------------------------------------------
    // ANALYTICS & SYNC ACTIONS
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
            _snackbarMessage.emit("Shop profile updated.")
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleShopData()
            _snackbarMessage.emit("Sample inventory and sales loaded.")
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
}
