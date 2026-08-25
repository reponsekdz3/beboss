package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BeBossBottomNav
import com.example.ui.components.BeBossTopBar
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthLockScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.SalesPosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopRegistrationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BeBossViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BeBossViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                BeBossApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BeBossApp(viewModel: BeBossViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val inventoryValuation by viewModel.inventoryValuation.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val totalOutstandingDebt by viewModel.totalOutstandingDebt.collectAsState()
    val cartState by viewModel.cartState.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allCustomerPayments by viewModel.allCustomerPayments.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val profitLossSummary by viewModel.profitLossSummary.collectAsState()
    val dailyChartPoints by viewModel.dailyChartPoints.collectAsState()
    val topProducts by viewModel.topProducts.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val selectedPeriod by viewModel.selectedAnalyticsPeriod.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val activeReceipt by viewModel.activeReceiptSale.collectAsState()

    // Auth, Theme & Language state
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isRegistrationNeeded by viewModel.isRegistrationNeeded.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (isRegistrationNeeded) {
        ShopRegistrationScreen(
            language = currentLanguage,
            isDarkTheme = isDarkTheme,
            onRegister = { shopName, currencyCode, currencySymbol, address, phone, ownerName, username, pin, password ->
                viewModel.registerNewShopAndOwner(
                    shopName = shopName,
                    currencyCode = currencyCode,
                    currencySymbol = currencySymbol,
                    address = address,
                    phone = phone,
                    ownerFullName = ownerName,
                    username = username,
                    pin = pin,
                    password = password
                )
            },
            onCancel = { viewModel.cancelRegistration() },
            onToggleLanguage = { viewModel.toggleLanguage() },
            onToggleTheme = { viewModel.toggleDarkMode() }
        )
    } else if (isLocked) {
        AuthLockScreen(
            currentUser = currentUser,
            allUsers = allUsers,
            shopProfile = shopProfile,
            authError = authError,
            language = currentLanguage,
            isDarkTheme = isDarkTheme,
            onUnlockWithPin = { pin -> viewModel.unlockAppWithPin(pin) },
            onLoginWithCredentials = { user, pass -> viewModel.loginWithCredentials(user, pass) },
            onSelectUser = { user -> viewModel.switchUser(user) },
            onClearError = { viewModel.clearAuthError() },
            onToggleLanguage = { viewModel.toggleLanguage() },
            onToggleTheme = { viewModel.toggleDarkMode() },
            onOpenRegister = { viewModel.showRegistrationScreen() }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BeBossTopBar(
                    currentScreen = currentScreen,
                    shopProfile = shopProfile,
                    currentUser = currentUser,
                    pendingSyncCount = pendingSyncCount,
                    isSyncing = isSyncing,
                    language = currentLanguage,
                    isDarkTheme = isDarkTheme,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleTheme = { viewModel.toggleDarkMode() },
                    onSyncClick = { viewModel.performSync() },
                    onHistoryClick = { viewModel.navigateTo(AppScreen.SALES_HISTORY) },
                    onLockClick = { viewModel.lockApp() }
                )
            },
            bottomBar = {
                BeBossBottomNav(
                    currentScreen = currentScreen,
                    cartItemCount = cartState.items.size,
                    lowStockCount = lowStockProducts.size,
                    language = currentLanguage,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            shopProfile = shopProfile,
                            todaySummary = todaySummary,
                            inventoryValuation = inventoryValuation,
                            lowStockProducts = lowStockProducts,
                            recentSales = allSales,
                            currentUser = currentUser,
                            language = currentLanguage,
                            onNavigate = { screen -> viewModel.navigateTo(screen) },
                            onOpenReceipt = { saleId -> viewModel.openReceipt(saleId) },
                            onRestockClick = { product ->
                                viewModel.navigateTo(AppScreen.INVENTORY)
                            },
                            onAddProductClick = {
                                viewModel.navigateTo(AppScreen.INVENTORY)
                            }
                        )
                    }

                    AppScreen.SALES_POS -> {
                        SalesPosScreen(
                            products = allProducts,
                            categories = categories,
                            customers = customers,
                            cartState = cartState,
                            shopProfile = shopProfile,
                            onAddToCart = { p -> viewModel.addToCart(p) },
                            onUpdateQuantity = { pId, qty -> viewModel.updateCartItemQuantity(pId, qty) },
                            onRemoveItem = { pId -> viewModel.removeCartItem(pId) },
                            onClearCart = { viewModel.clearCart() },
                            onSelectCustomer = { c -> viewModel.setCartCustomer(c) },
                            onSetPaymentMethod = { m -> viewModel.setCartPaymentMethod(m) },
                            onSetDiscount = { d -> viewModel.setCartDiscount(d) },
                            onSetAmountPaid = { a -> viewModel.setCartAmountPaidInput(a) },
                            onSetNotes = { n -> viewModel.setCartNotes(n) },
                            onCheckout = { viewModel.checkoutSale() },
                            onQuickAddProduct = { viewModel.navigateTo(AppScreen.INVENTORY) }
                        )
                    }

                    AppScreen.INVENTORY -> {
                        InventoryScreen(
                            products = allProducts,
                            categories = categories,
                            inventoryValuation = inventoryValuation,
                            shopProfile = shopProfile,
                            onSaveProduct = { p -> viewModel.saveProduct(p) },
                            onAdjustStock = { pId, delta, reason -> viewModel.adjustStock(pId, delta, reason) },
                            onDeleteProduct = { pId -> viewModel.deleteProduct(pId) }
                        )
                    }

                    AppScreen.ANALYTICS -> {
                        AnalyticsScreen(
                            selectedPeriod = selectedPeriod,
                            summary = profitLossSummary,
                            chartPoints = dailyChartPoints,
                            topProducts = topProducts,
                            categoryShares = categoryBreakdown,
                            shopProfile = shopProfile,
                            onPeriodSelected = { p -> viewModel.setAnalyticsPeriod(p) }
                        )
                    }

                    AppScreen.CUSTOMERS -> {
                        CustomersScreen(
                            customers = customers,
                            totalOutstandingDebt = totalOutstandingDebt,
                            shopProfile = shopProfile,
                            allSales = allSales,
                            allPayments = allCustomerPayments,
                            onSaveCustomer = { c -> viewModel.saveCustomer(c) },
                            onRecordPayment = { cId, amt -> viewModel.recordDebtPayment(cId, amt) },
                            onDeleteCustomer = { cId -> viewModel.deleteCustomer(cId) }
                        )
                    }

                    AppScreen.SALES_HISTORY -> {
                        SalesHistoryScreen(
                            sales = allSales,
                            shopProfile = shopProfile,
                            onOpenReceipt = { saleId -> viewModel.openReceipt(saleId) }
                        )
                    }

                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            shopProfile = shopProfile,
                            currentUser = currentUser,
                            allUsers = allUsers,
                            products = allProducts,
                            customers = customers,
                            sales = allSales,
                            customerPayments = allCustomerPayments,
                            pendingSyncCount = pendingSyncCount,
                            isSyncing = isSyncing,
                            language = currentLanguage,
                            isDarkTheme = isDarkTheme,
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onToggleTheme = { viewModel.toggleDarkMode() },
                            onSaveProfile = { prof -> viewModel.updateShopProfile(prof) },
                            onSyncNow = { viewModel.performSync() },
                            onSaveUser = { u -> viewModel.saveUser(u) },
                            onDeleteUser = { uId -> viewModel.deleteUser(uId) },
                            onImportPackage = { summary -> viewModel.importShopPackage(summary) },
                            onLockApp = { viewModel.lockApp() },
                            onSwitchUser = { u -> viewModel.switchUser(u) }
                        )
                    }
                }
            }
        }
    }

    // Active Interactive Receipt Dialog
    if (activeReceipt != null) {
        ReceiptDialog(
            saleWithItems = activeReceipt!!,
            shopProfile = shopProfile,
            onDismiss = { viewModel.closeReceipt() }
        )
    }
}
