package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BeBossBottomNav
import com.example.ui.components.BeBossNavigationRail
import com.example.ui.components.BeBossTopBar
import com.example.ui.components.PermissionRequestDialog
import com.example.ui.components.QuickCustomSaleDialog
import com.example.ui.components.QuickSpeedDialSheet
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.UniversalSearchDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppSplashScreen
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
import com.example.util.BeBossLogger
import com.example.util.BeBossNotificationManager
import com.example.util.BiometricAuthManager
import com.example.util.ClockGuard
import com.example.util.ConnectivityStatus
import com.example.util.Localization
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    @Volatile
    private var activeViewModel: BeBossViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashTelemetryHook()
        ClockGuard.init(this)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setRecentsScreenshotEnabled(false)
        }
        window.decorView.rootView.filterTouchesWhenObscured = true
        enableEdgeToEdge()
        hideSystemNavigationBars()
        BeBossNotificationManager.createNotificationChannels(this)

        setContent {
            val viewModel: BeBossViewModel = viewModel()
            activeViewModel = viewModel
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                BeBossApp(viewModel = viewModel)
            }
        }
    }

    private fun installCrashTelemetryHook() {
        if (crashHookInstalled) return
        synchronized(MainActivity::class.java) {
            if (crashHookInstalled) return
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                BeBossLogger.e(
                    "BeBossUncaught",
                    "Uncaught exception on thread=${thread.name}",
                    throwable
                )
                previous?.uncaughtException(thread, throwable)
            }
            crashHookInstalled = true
        }
    }

    companion object {
        @Volatile
        private var crashHookInstalled = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            activeViewModel?.onUserInteraction()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemNavigationBars()
        }
    }

    /**
     * Automatically hides the phone bottom navigation bar (home, back, recent tasks)
     * in immersive mode so the app behaves like an enterprise hardware POS terminal.
     */
    private fun hideSystemNavigationBars() {
        try {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } catch (e: Exception) {
            // Graceful fallback on older vendor skins
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun BeBossApp(viewModel: BeBossViewModel = viewModel()) {
    val context = LocalContext.current
    val hostActivity = context as? Activity
    val widthSizeClass = if (hostActivity != null) {
        calculateWindowSizeClass(hostActivity).widthSizeClass
    } else {
        WindowWidthSizeClass.Compact
    }
    val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact
    val currentScreen by viewModel.currentScreen.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val inventoryValuation by viewModel.inventoryValuation.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val totalOutstandingDebt by viewModel.totalOutstandingDebt.collectAsState()
    val cartState by viewModel.cartState.collectAsState()
    val heldCarts by viewModel.heldCarts.collectAsState()
    val deviceIntegrityStatus by viewModel.deviceIntegrityStatus.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val purchaseSummary by viewModel.purchaseSummary.collectAsState()
    val allCustomerPayments by viewModel.allCustomerPayments.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val profitLossSummary by viewModel.profitLossSummary.collectAsState()
    val dailyChartPoints by viewModel.dailyChartPoints.collectAsState()
    val topProducts by viewModel.topProducts.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val selectedPeriod by viewModel.selectedAnalyticsPeriod.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val deadLetterSyncCount by viewModel.deadLetterSyncCount.collectAsState()
    val startupIntegrityOk by viewModel.startupIntegrityOk.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val activeReceipt by viewModel.activeReceiptSale.collectAsState()

    // Auth, Theme & Language state
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val shopDevices by viewModel.shopDevices.collectAsState()
    val selectedBranchId by viewModel.selectedBranchId.collectAsState()
    val connectivityStatus by viewModel.connectivityStatus.collectAsState()
    val cloudSyncReport by viewModel.cloudSyncReport.collectAsState()
    val localServerStatus by viewModel.localServerStatus.collectAsState()
    val localServerIp by viewModel.localServerIp.collectAsState()
    val localConnectedClients by viewModel.localConnectedClients.collectAsState()
    val localLastReceivedPacket by viewModel.localLastReceivedPacket.collectAsState()
    val isP2pSyncing by viewModel.isP2pSyncing.collectAsState()
    val syncAuditLogs by viewModel.syncAuditLogs.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isRegistrationNeeded by viewModel.isRegistrationNeeded.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val databaseStats by viewModel.databaseStats.collectAsState()
    val isOptimizingDb by viewModel.isOptimizingDb.collectAsState()
    val stockTransfers by viewModel.allStockTransfers.collectAsState()
    val paymentVerificationRequests by viewModel.paymentVerificationRequests.collectAsState()
    val discoveredHubs by viewModel.discoveredHubs.collectAsState()
    val isDiscoveringHubs by viewModel.isDiscoveringHubs.collectAsState()
    val peerSyncEvents by viewModel.peerSyncEvents.collectAsState()
    val adminSession by viewModel.adminSession.collectAsState()
    val adminAuthError by viewModel.adminAuthError.collectAsState()
    val isAdminAuthenticating by viewModel.isAdminAuthenticating.collectAsState()
    val adminPaymentRequests by viewModel.adminPaymentRequests.collectAsState()
    val adminShopAccounts by viewModel.adminShopAccounts.collectAsState()
    val adminShopUsers by viewModel.adminShopUsers.collectAsState()
    val adminShopDevices by viewModel.adminShopDevices.collectAsState()
    val adminBranchSnapshots by viewModel.adminBranchSnapshots.collectAsState()
    val adminUnlockCodes by viewModel.adminUnlockCodes.collectAsState()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsState()
    val localAuditLogs by viewModel.localAuditLogs.collectAsState()
    val adminPaymentConfig by viewModel.adminPaymentConfig.collectAsState()
    val isAdminLoading by viewModel.isAdminLoading.collectAsState()
    val adminLastSyncedAt by viewModel.adminLastSyncedAt.collectAsState()
    val idleLockMinutes by viewModel.idleLockMinutes.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSplashScreen by remember { mutableStateOf(true) }
    var showQuickSpeedDialSheet by remember { mutableStateOf(false) }
    var showQuickCustomSaleDialog by remember { mutableStateOf(false) }
    var showUniversalSearchDialog by remember { mutableStateOf(false) }
    var showPermissionOnboardingDialog by remember { mutableStateOf(false) }
    var showPaywallAdminConsole by remember { mutableStateOf(false) }

    val debtorsCount = remember(customers) { customers.count { it.debtBalance > 0 } }

    // Proactively check crucial hardware and phone permissions right after launch
    LaunchedEffect(showSplashScreen) {
        if (!showSplashScreen && !com.example.util.PermissionManager.hasAllCrucialPermissions(context)) {
            showPermissionOnboardingDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showSplashScreen) {
        AppSplashScreen(
            isDarkTheme = isDarkTheme,
            durationMs = 520L,
            onSplashFinished = { showSplashScreen = false }
        )
    } else if (isRegistrationNeeded) {
        ShopRegistrationScreen(
            language = currentLanguage,
            isDarkTheme = isDarkTheme,
            authError = authError,
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
            onJoinExistingShop = { ownerUsername, ownerPhone, deviceJoinCode, thisDeviceName, pin, password ->
                viewModel.joinExistingShopFromCloud(
                    ownerUsername = ownerUsername,
                    ownerPhone = ownerPhone,
                    deviceJoinCode = deviceJoinCode,
                    thisDeviceName = thisDeviceName,
                    pin = pin,
                    password = password
                )
            },
            onClearError = { viewModel.clearAuthError() },
            onCancel = { viewModel.cancelRegistration() },
            onToggleLanguage = { viewModel.toggleLanguage() },
            onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
            onToggleTheme = { viewModel.toggleDarkMode() }
        )
    } else if (isLocked) {
        val activity = context as? Activity
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
                if (activity != null && BiometricAuthManager.isBiometricAvailable(context)) {
                    BiometricAuthManager.promptBiometric(
                        activity = activity,
                        title = Localization.get("biometric_prompt_title", currentLanguage),
                        subtitle = Localization.get("biometric_prompt_subtitle", currentLanguage),
                        negativeButtonText = Localization.get("biometric_use_pin", currentLanguage),
                        onSuccess = { viewModel.unlockWithBiometric(verifiedByHardware = true) },
                        onError = { msg -> viewModel.reportBiometricError(msg) },
                        onFailed = { viewModel.reportBiometricCancelled() }
                    )
                } else {
                    viewModel.reportBiometricError(
                        Localization.get("biometric_unavailable", currentLanguage)
                    )
                }
            },
            onSelectUser = { user -> viewModel.selectLockedOperator(user) },
            onClearError = { viewModel.clearAuthError() },
            onToggleLanguage = { viewModel.toggleLanguage() },
            onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
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
                    onSettingsClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    onSyncClick = { viewModel.performSync() },
                    onHistoryClick = { viewModel.navigateTo(AppScreen.SALES_HISTORY) },
                    onLockClick = { viewModel.lockApp() }
                )
            },
            bottomBar = {
                if (!useNavRail) {
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
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    BeBossNavigationRail(
                        currentScreen = currentScreen,
                        cartItemCount = cartState.items.size,
                        lowStockCount = lowStockProducts.size,
                        debtorCount = debtorsCount,
                        language = currentLanguage,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onOpenQuickActions = { showQuickSpeedDialSheet = true }
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1140.dp)
                    ) {
                        BeBossRuntimeStatusBanner(
                            connectivityStatus = connectivityStatus,
                            pendingSyncCount = pendingSyncCount,
                            deadLetterSyncCount = deadLetterSyncCount,
                            startupIntegrityOk = startupIntegrityOk,
                            clockRollbackDetected = ClockGuard.isClockRollbackDetected(),
                            language = currentLanguage,
                            onRetrySync = { viewModel.performSync() },
                            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                branches = branches,
                                selectedBranchId = selectedBranchId,
                                connectivityStatus = connectivityStatus,
                                isSyncing = isSyncing,
                                cloudSyncReport = cloudSyncReport,
                                onSelectBranch = { branchId -> viewModel.selectBranch(branchId) },
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
                                heldCarts = heldCarts,
                                shopProfile = shopProfile,
                                language = currentLanguage,
                                onAddToCart = { p -> viewModel.addToCart(p) },
                                onUpdateQuantity = { pId, qty -> viewModel.updateCartItemQuantity(pId, qty) },
                                onRemoveItem = { pId -> viewModel.removeCartItem(pId) },
                                onClearCart = { viewModel.clearCart() },
                                onSelectCustomer = { c -> viewModel.setCartCustomer(c) },
                                onSetPaymentMethod = { m -> viewModel.setCartPaymentMethod(m) },
                                onSetDiscount = { d -> viewModel.setCartDiscount(d) },
                                onSetAmountPaid = { a -> viewModel.setCartAmountPaidInput(a) },
                                onSetSplitCash = { s -> viewModel.setCartSplitCashInput(s) },
                                onSetSplitMomo = { s -> viewModel.setCartSplitMomoInput(s) },
                                onSetNotes = { n -> viewModel.setCartNotes(n) },
                                onHoldCart = { lbl -> viewModel.holdCurrentCart(lbl) },
                                onResumeHeldCart = { id -> viewModel.resumeHeldCart(id) },
                                onDeleteHeldCart = { id -> viewModel.deleteHeldCart(id) },
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
                                purchases = allPurchases,
                                purchaseSummary = purchaseSummary,
                                language = currentLanguage,
                                onSaveProduct = { p -> viewModel.saveProduct(p) },
                                onAdjustStock = { pId, delta, reason -> viewModel.adjustStock(pId, delta, reason) },
                                onDeleteProduct = { pId -> viewModel.deleteProduct(pId) },
                                onClearAllProducts = { viewModel.clearAllProducts() },
                                onRecordPurchase = { prodId, qty, cost, sellPrice, supplier, phone, status, inv, notes ->
                                    viewModel.recordPurchaseOrder(
                                        productId = prodId,
                                        quantity = qty,
                                        unitCostPrice = cost,
                                        newSellingPrice = sellPrice,
                                        supplierName = supplier,
                                        supplierPhone = phone,
                                        paymentStatus = status,
                                        invoiceNumber = inv,
                                        notes = notes
                                    )
                                },
                                onDeletePurchase = { pId -> viewModel.deletePurchaseRecord(pId) }
                            )
                        }

                        AppScreen.ANALYTICS -> {
                            AnalyticsScreen(
                                selectedPeriod = selectedPeriod,
                                summary = profitLossSummary,
                                chartPoints = dailyChartPoints,
                                topProducts = topProducts,
                                categoryShares = categoryBreakdown,
                                products = allProducts,
                                shopProfile = shopProfile,
                                language = currentLanguage,
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
                                language = currentLanguage,
                                onSaveCustomer = { c -> viewModel.saveCustomer(c) },
                                onRecordPayment = { cId, amt -> viewModel.recordDebtPayment(cId, amt) },
                                onDeleteCustomer = { cId -> viewModel.deleteCustomer(cId) },
                                onClearAllCustomers = { viewModel.clearAllCustomers() }
                            )
                        }

                        AppScreen.SALES_HISTORY -> {
                            SalesHistoryScreen(
                                sales = allSales,
                                shopProfile = shopProfile,
                                language = currentLanguage,
                                onOpenReceipt = { saleId -> viewModel.openReceipt(saleId) },
                                onDeleteSale = { saleId, reason -> viewModel.deleteSale(saleId, reason) }
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                shopProfile = shopProfile,
                                currentUser = currentUser,
                                allUsers = allUsers,
                                branches = branches,
                                shopDevices = shopDevices,
                                livePaymentConfig = adminPaymentConfig,
                                products = allProducts,
                                customers = customers,
                                sales = allSales,
                                customerPayments = allCustomerPayments,
                                pendingSyncCount = pendingSyncCount,
                                isSyncing = isSyncing,
                                connectivityStatus = connectivityStatus,
                                cloudSyncReport = cloudSyncReport,
                                databaseStats = databaseStats,
                                isOptimizingDb = isOptimizingDb,
                                language = currentLanguage,
                                isDarkTheme = isDarkTheme,
                                localServerStatus = localServerStatus,
                                localServerIp = localServerIp,
                                localConnectedClients = localConnectedClients,
                                localLastReceivedPacket = localLastReceivedPacket,
                                isP2pSyncing = isP2pSyncing,
                                syncAuditLogs = syncAuditLogs,
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onToggleTheme = { viewModel.toggleDarkMode() },
                                onSaveProfile = { prof -> viewModel.updateShopProfile(prof) },
                                onSyncNow = { viewModel.performSync() },
                                onSyncToCloudNow = { viewModel.syncToCloudNow() },
                                onSyncCloudWithEndpoint = { endpoint -> viewModel.syncToCloudWithEndpoint(endpoint) },
                                onTestPing = { endpoint, cb -> viewModel.testEndpointPing(endpoint, cb) },
                                onToggleLocalServer = { enable -> viewModel.toggleLocalSyncServer(enable) },
                                onSyncWithLocalHub = { hubIp -> viewModel.syncWithLocalMasterHub(hubIp) },
                                onRefreshDatabaseStats = { viewModel.refreshDatabaseStats() },
                                onOptimizeDatabase = { viewModel.optimizeDatabase() },
                                onVerifyDatabaseIntegrity = { viewModel.verifyDatabaseIntegrity() },
                                onClearTransactionalData = { viewModel.clearTransactionalDataOnly() },
                                onClearAllProducts = { viewModel.clearAllProducts() },
                                onClearAllCustomers = { viewModel.clearAllCustomers() },
                                onResetWholeApp = { viewModel.resetWholeApp() },
                                onSeedSampleData = { viewModel.seedSampleCatalog() },
                                onSaveBranch = { b -> viewModel.saveBranch(b) },
                                onDeleteBranch = { bId -> viewModel.deleteBranch(bId) },
                                onAddShopDevice = { deviceName, assignedUser, branch, onCreated ->
                                    viewModel.addSecondaryShopDevice(deviceName, assignedUser, branch, onCreated)
                                },
                                onToggleShopDeviceActive = { deviceId, isActive ->
                                    viewModel.toggleShopDeviceActive(deviceId, isActive)
                                },
                                onDeleteShopDevice = { deviceId ->
                                    viewModel.deleteShopDevice(deviceId)
                                },
                                onCreateStockTransfer = { prodId, fromId, fromName, toId, toName, qty, notes ->
                                    viewModel.createStockTransfer(prodId, fromId, fromName, toId, toName, qty, notes)
                                },
                                onDeleteStockTransfer = { id -> viewModel.deleteStockTransfer(id) },
                                selectedBranchId = selectedBranchId,
                                onSelectActiveBranch = { branchId -> viewModel.selectBranch(branchId) },
                                onSaveUser = { u -> viewModel.saveUser(u) },
                                onDeleteUser = { uId -> viewModel.deleteUser(uId) },
                                onImportPackage = { summary -> viewModel.importShopPackage(summary) },
                                onLockApp = { viewModel.lockApp() },
                                onLogout = { viewModel.logoutUser() },
                                onSwitchUser = { u -> viewModel.switchUser(u) },
                                onActivateVoucher = { code -> viewModel.activateSubscriptionVoucher(code) },
                                onProcessDirectPayment = { provider, phone, durationMonths, userRef, onSuccess ->
                                    viewModel.processDirectSubscriptionPayment(
                                        provider = provider,
                                        phone = phone,
                                        durationMonths = durationMonths,
                                        userReference = userRef,
                                        onResult = onSuccess
                                    )
                                },
                                deadLetterSyncCount = deadLetterSyncCount,
                                deviceIntegrityStatus = deviceIntegrityStatus,
                                paymentVerificationRequests = paymentVerificationRequests,
                                discoveredHubs = discoveredHubs,
                                isDiscoveringHubs = isDiscoveringHubs,
                                peerSyncEvents = peerSyncEvents,
                                onStartLanDiscovery = { viewModel.startLanDiscovery() },
                                onStopLanDiscovery = { viewModel.stopLanDiscovery() },
                                onGenerateBranchShareToken = { branchFilter, onReady ->
                                    viewModel.createBranchShareToken(branchFilter, onReady)
                                },
                                onExportFullBranchPackage = { branchFilter, onReady ->
                                    viewModel.exportFullBranchPackage(context, branchFilter, onReady)
                                },
                                onSubmitPaymentVerification = { provider, payerPhone, payerFullName, username, regPhone, months, branchCount, workerCount, deviceCount, telcoRef, onDone ->
                                    viewModel.submitUssdPaymentProof(
                                        provider = provider,
                                        payerPhone = payerPhone,
                                        username = username,
                                        registeredPhone = regPhone,
                                        durationMonths = months,
                                        branchCount = branchCount,
                                        workerCount = workerCount,
                                        telcoReference = telcoRef,
                                        payerFullName = payerFullName,
                                        deviceCount = deviceCount,
                                        onSubmitted = onDone
                                    )
                                },
                                onCheckCloudApproval = { viewModel.checkCloudUnlockApprovals() },
                                adminSession = adminSession,
                                adminAuthError = adminAuthError,
                                isAdminAuthenticating = isAdminAuthenticating,
                                adminPaymentRequests = adminPaymentRequests,
                                adminShopAccounts = adminShopAccounts,
                                adminShopUsers = adminShopUsers,
                                adminShopDevices = adminShopDevices,
                                adminBranchSnapshots = adminBranchSnapshots,
                                adminUnlockCodes = adminUnlockCodes,
                                adminAuditLogs = adminAuditLogs,
                                localAuditLogs = localAuditLogs,
                                isAdminLoading = isAdminLoading,
                                onAdminSignIn = { email, password -> viewModel.signInSupabaseAdmin(email, password) },
                                onAdminSignUp = { email, password, fullName -> viewModel.signUpSupabaseAdmin(email, password, fullName) },
                                onAdminSignOut = { viewModel.signOutSupabaseAdmin() },
                                onRefreshAdminDashboard = { viewModel.refreshAdminDashboardData() },
                                onAdminApprovePayment = { req, onDone ->
                                    viewModel.adminApproveAndGenerateCode(req, onDone)
                                },
                                onAdminDirectAutoUnlock = { req, onDone ->
                                    viewModel.adminDirectAutoUnlockPayment(req, onDone)
                                },
                                onAdminSavePaymentConfig = { cfg, onDone ->
                                    viewModel.adminSavePaymentConfig(cfg, onDone)
                                },
                                onAdminToggleShopDevice = { dev, active ->
                                    viewModel.adminToggleShopDeviceActive(dev, active)
                                },
                                onAdminDeleteShopDevice = { dev ->
                                    viewModel.adminDeleteShopDevice(dev)
                                },
                                onAdminApproveBatch = { onDone ->
                                    viewModel.adminApproveBatchPendingPayments(onDone)
                                },
                                onAdminRejectPayment = { req, reason ->
                                    viewModel.adminRejectPayment(req, reason)
                                },
                                onAdminIssueManualCode = { username, regPhone, tier, days, amt, onDone ->
                                    viewModel.adminIssueManualCode(username, regPhone, tier, days, amt, onDone)
                                },
                                onAdminRevokeCode = { codeDto ->
                                    viewModel.adminRevokeCode(codeDto)
                                },
                                onAdminExtendShopSubscription = { shop, addDays ->
                                    viewModel.adminExtendShopSubscription(shop, addDays)
                                },
                                onAdminResetShopTrial = { shop ->
                                    viewModel.adminResetShopTrial(shop)
                                },
                                onAdminSetShopStatus = { shop, status ->
                                    viewModel.adminSetShopStatus(shop, status)
                                },
                                onAdminUpdateShopLimits = { shop, branchLimit, workerLimit, deviceLimit ->
                                    viewModel.adminUpdateShopLimits(shop, branchLimit, workerLimit, deviceLimit)
                                },
                                onAdminDeleteShopAccount = { shop ->
                                    viewModel.adminDeleteShopAccount(shop)
                                },
                                onAdminUpdateShopUserRole = { user, role ->
                                    viewModel.adminUpdateShopUserRole(user, role)
                                },
                                onAdminToggleShopUserActive = { user, active ->
                                    viewModel.adminToggleShopUserActive(user, active)
                                },
                                onAdminDeleteShopUser = { user ->
                                    viewModel.adminDeleteShopUser(user)
                                },
                                onAdminDeleteBranchSnapshot = { snap ->
                                    viewModel.adminDeleteBranchSnapshot(snap)
                                },
                                onShareAdminWebPortal = { viewModel.shareAdminWebPortal(context) },
                                onShowSplashScreen = { showSplashScreen = true },
                                onTestNotification = { type -> viewModel.testNotification(type) },
                                onRequestPermissions = { showPermissionOnboardingDialog = true },
                                onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                                idleLockMinutes = idleLockMinutes,
                                onSetIdleLockMinutes = { mins -> viewModel.setIdleLockMinutes(mins) },
                                adminLastSyncedAt = adminLastSyncedAt
                            )
                        }
                    }
                            }
                        }
                    }
                }
            }
        }
    }

    // Universal Permission Request Dialog (Proactive on first launch + On-Demand from Settings/Features)
    if (showPermissionOnboardingDialog) {
        PermissionRequestDialog(
            language = currentLanguage,
            onDismiss = { showPermissionOnboardingDialog = false }
        )
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
            purchases = allPurchases,
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
            },
            onQuickRestock = { p ->
                viewModel.navigateTo(AppScreen.INVENTORY)
            }
        )
    }

    // Active Subscription Hard-Lock Paywall (Non-dismissible after 4-day free trial or remote lock)
    if (!shopProfile.isSubscriptionActive && !isLocked && !isRegistrationNeeded) {
        com.example.ui.screens.SubscriptionPaywallDialog(
            shopProfile = shopProfile,
            branches = branches,
            allUsers = allUsers,
            shopDevices = shopDevices,
            currentUser = currentUser,
            paymentRequests = paymentVerificationRequests,
            livePaymentConfig = adminPaymentConfig,
            language = currentLanguage,
            onActivateVoucher = { code -> viewModel.activateSubscriptionVoucher(code) },
            onProcessDirectPayment = { provider, phone, durationMonths, userRef, onSuccess ->
                viewModel.processDirectSubscriptionPayment(
                    provider = provider,
                    phone = phone,
                    durationMonths = durationMonths,
                    userReference = userRef,
                    onResult = onSuccess
                )
            },
            onSubmitPaymentVerification = { provider, payerPhone, payerFullName, username, regPhone, months, branchCount, workerCount, deviceCount, telcoRef, onDone ->
                viewModel.submitUssdPaymentProof(
                    provider = provider,
                    payerPhone = payerPhone,
                    username = username,
                    registeredPhone = regPhone,
                    durationMonths = months,
                    branchCount = branchCount,
                    workerCount = workerCount,
                    telcoReference = telcoRef,
                    payerFullName = payerFullName,
                    deviceCount = deviceCount,
                    onSubmitted = onDone
                )
            },
            onCheckCloudApproval = { viewModel.checkCloudUnlockApprovals() },
            onOpenAdminConsole = { showPaywallAdminConsole = true }
        )
    }

    // Master Admin Console accessible even when a terminal is on the Subscription Hard-Lock Paywall
    if (showPaywallAdminConsole) {
        com.example.ui.screens.AdminVerificationConsoleDialog(
            shopProfile = shopProfile,
            adminSession = adminSession,
            adminAuthError = adminAuthError,
            isAdminAuthenticating = isAdminAuthenticating,
            paymentRequests = adminPaymentRequests,
            shopAccounts = adminShopAccounts,
            shopUsers = adminShopUsers,
            shopDevices = adminShopDevices,
            branchSnapshots = adminBranchSnapshots,
            unlockCodes = adminUnlockCodes,
            auditLogs = adminAuditLogs,
            localAuditLogs = localAuditLogs,
            paymentConfig = adminPaymentConfig,
            isLoading = isAdminLoading,
            language = currentLanguage,
            lastSyncedAt = adminLastSyncedAt,
            onSignInAdmin = { email, password -> viewModel.signInSupabaseAdmin(email, password) },
            onSignUpAdmin = { email, password, fullName -> viewModel.signUpSupabaseAdmin(email, password, fullName) },
            onSignOutAdmin = { viewModel.signOutSupabaseAdmin() },
            onRefresh = { viewModel.refreshAdminDashboardData() },
            onApprovePayment = { req, onDone -> viewModel.adminApproveAndGenerateCode(req, onDone) },
            onDirectAutoUnlock = { req, onDone -> viewModel.adminDirectAutoUnlockPayment(req, onDone) },
            onSavePaymentConfig = { cfg, onDone -> viewModel.adminSavePaymentConfig(cfg, onDone) },
            onToggleShopDeviceActive = { dev, active -> viewModel.adminToggleShopDeviceActive(dev, active) },
            onDeleteShopDevice = { dev -> viewModel.adminDeleteShopDevice(dev) },
            onBatchApprovePending = { onDone -> viewModel.adminApproveBatchPendingPayments(onDone) },
            onRejectPayment = { req, reason -> viewModel.adminRejectPayment(req, reason) },
            onIssueManualCode = { username, regPhone, tier, days, amt, onDone ->
                viewModel.adminIssueManualCode(username, regPhone, tier, days, amt, onDone)
            },
            onRevokeCode = { codeDto -> viewModel.adminRevokeCode(codeDto) },
            onExtendShopSubscription = { shop, addDays -> viewModel.adminExtendShopSubscription(shop, addDays) },
            onResetShopTrial = { shop -> viewModel.adminResetShopTrial(shop) },
            onSetShopStatus = { shop, status -> viewModel.adminSetShopStatus(shop, status) },
            onUpdateShopLimits = { shop, branchLimit, workerLimit, deviceLimit ->
                viewModel.adminUpdateShopLimits(shop, branchLimit, workerLimit, deviceLimit)
            },
            onDeleteShopAccount = { shop -> viewModel.adminDeleteShopAccount(shop) },
            onUpdateShopUserRole = { user, role -> viewModel.adminUpdateShopUserRole(user, role) },
            onToggleShopUserActive = { user, active -> viewModel.adminToggleShopUserActive(user, active) },
            onDeleteShopUser = { user -> viewModel.adminDeleteShopUser(user) },
            onDeleteBranchSnapshot = { snap -> viewModel.adminDeleteBranchSnapshot(snap) },
            onShareWebPortal = { viewModel.shareAdminWebPortal(context) },
            onDismiss = { showPaywallAdminConsole = false }
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

@Composable
private fun BeBossRuntimeStatusBanner(
    connectivityStatus: ConnectivityStatus,
    pendingSyncCount: Int,
    deadLetterSyncCount: Int,
    startupIntegrityOk: Boolean?,
    clockRollbackDetected: Boolean,
    language: AppLanguage,
    onRetrySync: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isOfflineWithQueue = connectivityStatus != ConnectivityStatus.AVAILABLE && pendingSyncCount > 0
    val showBanner = startupIntegrityOk == false || clockRollbackDetected || deadLetterSyncCount > 0 || isOfflineWithQueue
    AnimatedVisibility(visible = showBanner) {
        val isCritical = startupIntegrityOk == false || clockRollbackDetected || deadLetterSyncCount > 0
        val containerColor = if (isCritical) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
        val contentColor = if (isCritical) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
        val icon = when {
            startupIntegrityOk == false || clockRollbackDetected -> Icons.Default.Warning
            deadLetterSyncCount > 0 -> Icons.Default.SyncProblem
            else -> Icons.Default.CloudOff
        }
        val message = when {
            startupIntegrityOk == false -> Localization.get("banner_db_warning", language)
            clockRollbackDetected -> Localization.get("banner_clock_rollback", language)
            deadLetterSyncCount > 0 -> "${Localization.get("banner_dead_letter", language)} ($deadLetterSyncCount)"
            else -> "${Localization.get("banner_offline_queue", language)} ($pendingSyncCount)"
        }
        val actionLabel = when {
            deadLetterSyncCount > 0 -> Localization.get("retry", language)
            isCritical -> Localization.get("inspect", language)
            else -> null
        }

        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("runtime_status_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (actionLabel != null) {
                    TextButton(
                        onClick = { if (deadLetterSyncCount > 0) onRetrySync() else onOpenSettings() },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
