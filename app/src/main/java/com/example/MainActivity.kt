package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BeBossBottomNav
import com.example.ui.components.BeBossTopBar
import com.example.ui.components.QuickCustomSaleDialog
import com.example.ui.components.QuickSpeedDialSheet
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.UniversalSearchDialog
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
import com.example.util.AppLanguage
import com.example.util.BiometricAuthManager
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {
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
    val context = LocalContext.current
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
    val branches by viewModel.branches.collectAsState()
    val selectedBranchId by viewModel.selectedBranchId.collectAsState()
    val connectivityStatus by viewModel.connectivityStatus.collectAsState()
    val cloudSyncReport by viewModel.cloudSyncReport.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isRegistrationNeeded by viewModel.isRegistrationNeeded.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showQuickSpeedDialSheet by remember { mutableStateOf(false) }
    var showQuickCustomSaleDialog by remember { mutableStateOf(false) }
    var showUniversalSearchDialog by remember { mutableStateOf(false) }

    val debtorsCount = remember(customers) { customers.count { it.debtBalance > 0 } }

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
        val fragmentActivity = context as? FragmentActivity
        AuthLockScreen(
            currentUser = currentUser,
            allUsers = allUsers,
            shopProfile = shopProfile,
            authError = authError,
            language = currentLanguage,
            isDarkTheme = isDarkTheme,
            onUnlockWithPin = { pin -> viewModel.unlockAppWithPin(pin) },
            onLoginWithCredentials = { user, pass -> viewModel.loginWithCredentials(user, pass) },
            onBiometricUnlock = {
                if (fragmentActivity != null && BiometricAuthManager.isBiometricAvailable(context)) {
                    BiometricAuthManager.promptBiometric(
                        activity = fragmentActivity,
                        title = if (currentLanguage == AppLanguage.KINYARWANDA) "Kugenzura Igikumwe" else "Fingerprint Verification",
                        subtitle = if (currentLanguage == AppLanguage.KINYARWANDA) "Koresha igikumwe gufungura BeBoss" else "Verify fingerprint to unlock BeBoss",
                        negativeButtonText = if (currentLanguage == AppLanguage.KINYARWANDA) "Koresha PIN" else "Use PIN Instead",
                        onSuccess = { viewModel.unlockWithBiometric() },
                        onError = { /* fallback */ },
                        onFailed = { /* failed */ }
                    )
                } else {
                    viewModel.unlockWithBiometric()
                }
            },
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
                    onSearchClick = { showUniversalSearchDialog = true },
                    onQuickActionsClick = { showQuickSpeedDialSheet = true },
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
                    debtorCount = debtorsCount,
                    language = currentLanguage,
                    onNavigate = { screen -> viewModel.navigateTo(screen) },
                    onOpenQuickActions = { showQuickSpeedDialSheet = true }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(90))
                    },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
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
                                branches = branches,
                                products = allProducts,
                                customers = customers,
                                sales = allSales,
                                customerPayments = allCustomerPayments,
                                pendingSyncCount = pendingSyncCount,
                                isSyncing = isSyncing,
                                connectivityStatus = connectivityStatus,
                                cloudSyncReport = cloudSyncReport,
                                language = currentLanguage,
                                isDarkTheme = isDarkTheme,
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onToggleTheme = { viewModel.toggleDarkMode() },
                                onSaveProfile = { prof -> viewModel.updateShopProfile(prof) },
                                onSyncNow = { viewModel.performSync() },
                                onSyncToCloudNow = { viewModel.syncToCloudNow() },
                                onSaveBranch = { b -> viewModel.saveBranch(b) },
                                onDeleteBranch = { bId -> viewModel.deleteBranch(bId) },
                                onSaveUser = { u -> viewModel.saveUser(u) },
                                onDeleteUser = { uId -> viewModel.deleteUser(uId) },
                                onImportPackage = { summary -> viewModel.importShopPackage(summary) },
                                onLockApp = { viewModel.lockApp() },
                                onLogout = { viewModel.logoutUser() },
                                onSwitchUser = { u -> viewModel.switchUser(u) },
                                onActivateVoucher = { code -> viewModel.activateSubscriptionVoucher(code) },
                                onGrantEmergencyGrace = { viewModel.grantEmergencyGracePeriod() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Speed Dial Sheet (⚡ Fast Actions)
    if (showQuickSpeedDialSheet) {
        QuickSpeedDialSheet(
            language = currentLanguage,
            onDismiss = { showQuickSpeedDialSheet = false },
            onQuickCustomSale = { showQuickCustomSaleDialog = true },
            onQuickAddProduct = { viewModel.navigateTo(AppScreen.INVENTORY) },
            onQuickNewCustomer = { viewModel.navigateTo(AppScreen.CUSTOMERS) },
            onUniversalSearch = { showUniversalSearchDialog = true },
            onShareDailySummary = { viewModel.shareDailyWhatsAppSummary(context) },
            onViewLastReceipt = { viewModel.openLastReceipt() }
        )
    }

    // Quick Custom Sale / Cashier Instant Checkout Modal
    if (showQuickCustomSaleDialog) {
        QuickCustomSaleDialog(
            currency = shopProfile.currencySymbol,
            language = currentLanguage,
            onDismiss = { showQuickCustomSaleDialog = false },
            onCompleteQuickSale = { desc, amt, method, isDirect ->
                viewModel.performQuickCustomSale(desc, amt, method, isDirect)
            }
        )
    }

    // Instant Universal Search Modal
    if (showUniversalSearchDialog) {
        UniversalSearchDialog(
            products = allProducts,
            customers = customers,
            sales = allSales,
            shopProfile = shopProfile,
            language = currentLanguage,
            onDismiss = { showUniversalSearchDialog = false },
            onAddToCart = { p ->
                viewModel.addToCart(p)
                viewModel.navigateTo(AppScreen.SALES_POS)
            },
            onOpenReceipt = { id -> viewModel.openReceipt(id) },
            onSelectCustomer = { c ->
                viewModel.setCartCustomer(c)
                viewModel.navigateTo(AppScreen.SALES_POS)
            }
        )
    }

    // Active Subscription Paywall Lock (If 5,000 RWF monthly fee is expired)
    if (!shopProfile.isSubscriptionActive && !isLocked && !isRegistrationNeeded) {
        com.example.ui.screens.SubscriptionPaywallDialog(
            shopProfile = shopProfile,
            language = currentLanguage,
            onActivateVoucher = { code -> viewModel.activateSubscriptionVoucher(code) },
            onGrantEmergencyGrace = { viewModel.grantEmergencyGracePeriod() }
        )
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
