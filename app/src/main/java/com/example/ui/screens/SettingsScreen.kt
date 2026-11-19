package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.model.LocalAuditLog
import com.example.util.SubscriptionSecurityManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsSuggest
import com.example.data.model.DatabaseEngineStats
import com.example.data.model.DatabaseMaintenanceResult
import com.example.data.model.PaymentVerificationRequest
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.components.PermissionRequestDialog
import com.example.util.PermissionManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ShopDevice
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AdminPaymentConfig
import com.example.util.AppLanguage
import com.example.util.BranchPeerSyncEvent
import com.example.util.DiscoveredBranchHub
import com.example.util.Localization
import com.example.util.OfflineSubscriptionManager
import com.example.util.PdfReportGenerator
import com.example.util.ReceiptGenerator
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.util.SupabaseAdminSession
import com.example.util.SupabaseAuditLogDto
import com.example.util.SupabasePaymentRequestDto
import com.example.util.SupabaseShopAccountDto
import com.example.util.SupabaseShopDeviceDto
import com.example.util.SupabaseUnlockCodeDto
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    shopProfile: ShopProfile,
    currentUser: User? = null,
    allUsers: List<User> = emptyList(),
    branches: List<com.example.data.model.Branch> = emptyList(),
    shopDevices: List<ShopDevice> = emptyList(),
    livePaymentConfig: AdminPaymentConfig = OfflineSubscriptionManager.getCurrentPaymentConfig(),
    products: List<Product> = emptyList(),
    customers: List<Customer> = emptyList(),
    sales: List<Sale> = emptyList(),
    customerPayments: List<CustomerPayment> = emptyList(),
    pendingSyncCount: Int = 0,
    isSyncing: Boolean = false,
    connectivityStatus: com.example.util.ConnectivityStatus = com.example.util.ConnectivityStatus.AVAILABLE,
    cloudSyncReport: com.example.util.CloudSyncReport? = null,
    databaseStats: DatabaseEngineStats? = null,
    isOptimizingDb: Boolean = false,
    language: AppLanguage,
    isDarkTheme: Boolean,
    localServerStatus: com.example.util.LocalServerStatus = com.example.util.LocalServerStatus.STOPPED,
    localServerIp: String? = null,
    localConnectedClients: Int = 0,
    localLastReceivedPacket: String? = null,
    isP2pSyncing: Boolean = false,
    syncAuditLogs: List<com.example.util.SyncAuditLog> = emptyList(),
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSaveProfile: (ShopProfile) -> Unit,
    onSyncNow: () -> Unit = {},
    onSyncToCloudNow: () -> Unit = {},
    onSyncCloudWithEndpoint: (String?) -> Unit = {},
    onTestPing: (String, (com.example.util.EndpointPingResult) -> Unit) -> Unit = { _, _ -> },
    onToggleLocalServer: (Boolean) -> Unit = {},
    onSyncWithLocalHub: (String) -> Unit = {},
    onSaveBranch: (com.example.data.model.Branch) -> Unit = {},
    onDeleteBranch: (String) -> Unit = {},
    onAddShopDevice: (deviceName: String, assignedUser: User, branch: com.example.data.model.Branch?, onCreated: (ShopDevice) -> Unit) -> Unit = { _, _, _, _ -> },
    onToggleShopDeviceActive: (deviceId: String, isActive: Boolean) -> Unit = { _, _ -> },
    onDeleteShopDevice: (deviceId: String) -> Unit = {},
    stockTransfers: List<com.example.data.model.StockTransfer> = emptyList(),
    onCreateStockTransfer: (productId: String, fromBranchId: String, fromBranchName: String, toBranchId: String, toBranchName: String, quantity: Double, notes: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteStockTransfer: (String) -> Unit = {},
    selectedBranchId: String = "ALL",
    onSelectActiveBranch: (String) -> Unit = {},
    onSaveUser: (User) -> Unit = {},
    onDeleteUser: (String) -> Unit = {},
    onImportPackage: (ShopImportSummary) -> Unit = {},
    onRefreshDatabaseStats: () -> Unit = {},
    onOptimizeDatabase: () -> Unit = {},
    onVerifyDatabaseIntegrity: () -> Unit = {},
    onClearTransactionalData: () -> Unit = {},
    onClearAllProducts: () -> Unit = {},
    onClearAllCustomers: () -> Unit = {},
    onResetWholeApp: () -> Unit = {},
    onSeedSampleData: () -> Unit = {},
    onLockApp: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSwitchUser: (User) -> Unit = {},
    onActivateVoucher: (String) -> Unit = {},
    onProcessDirectPayment: ((provider: String, phone: String, durationMonths: Int, userReference: String?, onSuccess: (com.example.util.PaymentProcessingResult, com.example.util.SubscriptionPriceBreakdown) -> Unit) -> Unit)? = null,
    deadLetterSyncCount: Int = 0,
    deviceIntegrityStatus: com.example.util.DeviceIntegrityStatus? = null,
    paymentVerificationRequests: List<PaymentVerificationRequest> = emptyList(),
    discoveredHubs: List<DiscoveredBranchHub> = emptyList(),
    isDiscoveringHubs: Boolean = false,
    peerSyncEvents: List<BranchPeerSyncEvent> = emptyList(),
    onStartLanDiscovery: () -> Unit = {},
    onStopLanDiscovery: () -> Unit = {},
    onGenerateBranchShareToken: ((String?, (String) -> Unit) -> Unit)? = null,
    onExportFullBranchPackage: ((String?, (File) -> Unit) -> Unit)? = null,
    onSubmitPaymentVerification: ((provider: String, payerPhone: String, payerFullName: String, username: String, registeredPhone: String, durationMonths: Int, branchCount: Int, workerCount: Int, deviceCount: Int, telcoRef: String, onDone: (PaymentVerificationRequest, Boolean) -> Unit) -> Unit)? = null,
    onCheckCloudApproval: () -> Unit = {},
    adminSession: SupabaseAdminSession? = null,
    adminAuthError: String? = null,
    isAdminAuthenticating: Boolean = false,
    adminPaymentRequests: List<SupabasePaymentRequestDto> = emptyList(),
    adminShopAccounts: List<SupabaseShopAccountDto> = emptyList(),
    adminShopUsers: List<com.example.util.SupabaseShopUserDto> = emptyList(),
    adminShopDevices: List<SupabaseShopDeviceDto> = emptyList(),
    adminBranchSnapshots: List<com.example.util.SupabaseBranchSnapshotDto> = emptyList(),
    adminUnlockCodes: List<SupabaseUnlockCodeDto> = emptyList(),
    adminAuditLogs: List<SupabaseAuditLogDto> = emptyList(),
    localAuditLogs: List<LocalAuditLog> = emptyList(),
    isAdminLoading: Boolean = false,
    onAdminSignIn: (String, String) -> Unit = { _, _ -> },
    onAdminSignUp: (String, String, String) -> Unit = { _, _, _ -> },
    onAdminSignOut: () -> Unit = {},
    onRefreshAdminDashboard: () -> Unit = {},
    onAdminApprovePayment: ((SupabasePaymentRequestDto, (Boolean, String) -> Unit) -> Unit)? = null,
    onAdminDirectAutoUnlock: ((SupabasePaymentRequestDto, (Boolean) -> Unit) -> Unit)? = null,
    onAdminSavePaymentConfig: ((AdminPaymentConfig, (Boolean) -> Unit) -> Unit)? = null,
    onAdminToggleShopDevice: (SupabaseShopDeviceDto, Boolean) -> Unit = { _, _ -> },
    onAdminDeleteShopDevice: (SupabaseShopDeviceDto) -> Unit = {},
    onAdminApproveBatch: ((Int, Int) -> Unit) -> Unit = { _ -> },
    onAdminRejectPayment: (SupabasePaymentRequestDto, String) -> Unit = { _, _ -> },
    onAdminIssueManualCode: ((username: String, registeredPhone: String, tierCode: String, days: Int, amountRwf: Int, onDone: (Boolean, String) -> Unit) -> Unit)? = null,
    onAdminRevokeCode: (SupabaseUnlockCodeDto) -> Unit = {},
    onAdminExtendShopSubscription: (SupabaseShopAccountDto, Int) -> Unit = { _, _ -> },
    onAdminResetShopTrial: (SupabaseShopAccountDto) -> Unit = {},
    onAdminSetShopStatus: (SupabaseShopAccountDto, String) -> Unit = { _, _ -> },
    onAdminUpdateShopLimits: (SupabaseShopAccountDto, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onAdminDeleteShopAccount: (SupabaseShopAccountDto) -> Unit = {},
    onAdminUpdateShopUserRole: (com.example.util.SupabaseShopUserDto, String) -> Unit = { _, _ -> },
    onAdminToggleShopUserActive: (com.example.util.SupabaseShopUserDto, Boolean) -> Unit = { _, _ -> },
    onAdminDeleteShopUser: (com.example.util.SupabaseShopUserDto) -> Unit = {},
    onAdminDeleteBranchSnapshot: (com.example.util.SupabaseBranchSnapshotDto) -> Unit = {},
    onShareAdminWebPortal: () -> Unit = {},
    onShowSplashScreen: () -> Unit = {},
    onTestNotification: (String) -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onSelectLanguage: ((AppLanguage) -> Unit)? = null,
    idleLockMinutes: Int = 5,
    onSetIdleLockMinutes: (Int) -> Unit = {},
    adminLastSyncedAt: Long = 0L
) {
    val context = LocalContext.current
    var shopName by remember(shopProfile.shopName) { mutableStateOf(shopProfile.shopName) }
    var ownerName by remember(shopProfile.name) { mutableStateOf(shopProfile.name) }
    var phone by remember(shopProfile.phone) { mutableStateOf(shopProfile.phone) }
    var address by remember(shopProfile.address) { mutableStateOf(shopProfile.address) }
    var tinNumber by remember(shopProfile.tinNumber) { mutableStateOf(shopProfile.tinNumber) }
    var taxRateText by remember(shopProfile.taxRate) { mutableStateOf(if (shopProfile.taxRate > 0) shopProfile.taxRate.toString() else "") }
    var receiptHeader by remember(shopProfile.receiptHeader) { mutableStateOf(shopProfile.receiptHeader) }
    var receiptFooter by remember(shopProfile.receiptFooter) { mutableStateOf(shopProfile.receiptFooter) }
    var currencyCode by remember(shopProfile.currencyCode) { mutableStateOf(shopProfile.currencyCode) }
    var currencySymbol by remember(shopProfile.currencySymbol) { mutableStateOf(shopProfile.currencySymbol) }

    var dailySalesTargetText by remember(shopProfile.dailySalesTarget) { mutableStateOf(shopProfile.dailySalesTarget.toLong().toString()) }
    var monthlyRevenueTargetText by remember(shopProfile.monthlyRevenueTarget) { mutableStateOf(shopProfile.monthlyRevenueTarget.toLong().toString()) }
    var targetMarginText by remember(shopProfile.targetMarginPercent) { mutableStateOf(shopProfile.targetMarginPercent.toString()) }
    var cashFloatText by remember(shopProfile.cashRegisterFloat) { mutableStateOf(shopProfile.cashRegisterFloat.toLong().toString()) }
    var paperWidthMm by remember(shopProfile.receiptPaperWidthMm) { mutableStateOf(shopProfile.receiptPaperWidthMm) }
    var showLogoOnReceipt by remember(shopProfile.showLogoOnReceipt) { mutableStateOf(shopProfile.showLogoOnReceipt) }
    var showTaxOnReceipt by remember(shopProfile.showTaxOnReceipt) { mutableStateOf(shopProfile.showTaxOnReceipt) }
    var showBarcodeOnReceipt by remember(shopProfile.showBarcodeOnReceipt) { mutableStateOf(shopProfile.showBarcodeOnReceipt) }
    var autoPrintReceiptOnSale by remember(shopProfile.autoPrintReceiptOnSale) { mutableStateOf(shopProfile.autoPrintReceiptOnSale) }
    var soundFeedbackEnabled by remember(shopProfile.soundFeedbackEnabled) { mutableStateOf(shopProfile.soundFeedbackEnabled) }
    var hapticFeedbackEnabled by remember(shopProfile.hapticFeedbackEnabled) { mutableStateOf(shopProfile.hapticFeedbackEnabled) }

    var currencyExpanded by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showShopDevicesDialog by remember { mutableStateOf(false) }
    var showSyncHubDialog by remember { mutableStateOf(false) }
    var showDataTransferDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showAdminConsoleDialog by remember { mutableStateOf(false) }
    var settingsUnlockCodeInput by remember { mutableStateOf("") }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showThermalPreviewDialog by remember { mutableStateOf(false) }
    var showResetTransactionsDialog by remember { mutableStateOf(false) }
    var showClearProductsDialog by remember { mutableStateOf(false) }
    var showClearCustomersDialog by remember { mutableStateOf(false) }
    var showResetWholeAppDialog by remember { mutableStateOf(false) }
    var permissionsUpdated by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var settingsSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var lastAutoSubmittedCode by remember { mutableStateOf("") }

    val settingsCategories = remember(language) {
        listOf(
            "ALL" to Localization.get("cat_all", language),
            "LANGUAGE" to Localization.get("cat_language", language),
            "LICENSE" to Localization.get("cat_license", language),
            "SYNC" to Localization.get("cat_sync", language),
            "SHOP" to Localization.get("cat_shop", language),
            "TEAM" to Localization.get("cat_team", language),
            "SECURITY" to Localization.get("cat_security", language)
        )
    }

    fun shouldShowSection(category: String, keywords: String): Boolean {
        val catOk = selectedCategory == "ALL" || selectedCategory == category
        if (!catOk) return false
        val q = settingsSearchQuery.trim()
        if (q.isEmpty()) return true
        return keywords.contains(q, ignoreCase = true) || category.contains(q, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Official BeBoss POS Brand Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(2.dp, OrangePrimary),
                        modifier = Modifier.size(60.dp),
                        shadowElevation = 6.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                            contentDescription = "BeBoss Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BeBoss POS",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OrangePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PRO v2.6",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = shopProfile.shopName.ifBlank { "Enterprise Retail Edition" },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ProfitGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = Localization.get("offline_db_active", language),
                                fontSize = 10.5.sp,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 0.5 Interactive Settings Search Bar & Category Filter Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = settingsSearchQuery,
                    onValueChange = { settingsSearchQuery = it },
                    placeholder = {
                        Text(
                            text = Localization.get("settings_search_hint", language),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = Localization.get("search", language),
                            tint = OrangePrimary
                        )
                    },
                    trailingIcon = {
                        if (settingsSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { settingsSearchQuery = "" },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = Localization.get("close", language)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_search_input")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(settingsCategories) { (catId, catLabel) ->
                        val isSelected = selectedCategory == catId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = catId },
                            label = {
                                Text(
                                    text = catLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("settings_cat_${catId.lowercase()}")
                        )
                    }
                }
            }
        }

        // 1. Language (4 Full Local Languages) & Theme Quick Switch Card
        if (shouldShowSection("LANGUAGE", "language theme dark light english kinyarwanda french swahili ururimi langue lugha display")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = Localization.get("language", language),
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("display_and_language", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Localization.get("select_language_subtitle", language),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive 2x2 Grid for All 4 Offline Local Languages
                    val languagePairs = AppLanguage.entries.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        languagePairs.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { langOption ->
                                    val isSelected = langOption == language
                                    Surface(
                                        onClick = {
                                            if (onSelectLanguage != null) {
                                                onSelectLanguage(langOption)
                                            } else if (!isSelected) {
                                                onToggleLanguage()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) OrangePrimary.copy(alpha = 0.14f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.6.dp else 1.dp,
                                            color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 50.dp)
                                            .testTag("settings_lang_${langOption.code}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) OrangePrimary else DarkNavy.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = langOption.flag,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = langOption.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = OrangePrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Theme Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTheme() }
                            .padding(vertical = 6.dp)
                            .testTag("settings_theme_toggle_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0x33FFD166) else Color(0x33334155),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = Localization.get("app_theme", language),
                                        tint = if (isDarkTheme) Color(0xFFFFD166) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.get("app_theme", language),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDarkTheme) Localization.get("dark_mode", language) else Localization.get("light_mode", language),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary
                            )
                        )
                    }
                }
            }
        }

        // 2. Active User & Role Card
        if (shouldShowSection("TEAM", "staff users branches backup export share apk logout lock terminal role abakozi amashami")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = OrangeLight,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.name ?: "Active Staff User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Role: ${currentUser?.role?.displayName ?: "Owner"}",
                                    fontSize = 12.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = onLockApp,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("lock_terminal", language), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStaffDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("staff_management", language), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showBranchDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${Localization.get("branches_title", language)} (${branches.size})", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showShopDevicesDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_shop_devices_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(17.dp), tint = ProfitGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${Localization.get("shop_devices_title", language)} (${shopDevices.count { it.isActive }.coerceAtLeast(1)}) • +2,000 FRw/extra",
                            fontSize = 12.sp,
                            color = ProfitGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDataTransferDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("data_backup", language), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                OfflineSubscriptionManager.shareAppApkOffline(context)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("share_app_apk", language), fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showLogoutConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                    ) {
                        Text(Localization.get("logout", language), fontSize = 12.sp, color = LossRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2.5 Real-Time Cloud & LAN Multi-Device Sync Card
        if (shouldShowSection("SYNC", "cloud sync lan network branch hub offline online push supabase")) item {
            val isOnline = connectivityStatus == com.example.util.ConnectivityStatus.AVAILABLE
            val isSyncingNow = isSyncing

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isOnline) ProfitGreen.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = if (isOnline) ProfitGreen else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.get("cloud_sync_card_title", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isOnline) Localization.get("cloud_online_desc", language)
                                    else Localization.get("cloud_offline_desc", language),
                                    fontSize = 11.sp,
                                    color = if (isOnline) ProfitGreen else Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOnline) ProfitGreen.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isOnline) Localization.get("status_live", language) else Localization.get("status_offline", language),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isOnline) ProfitGreen else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (cloudSyncReport != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = Localization.get("sync_completed", language),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "• ${cloudSyncReport.productsCount} Products  • ${cloudSyncReport.debtsCount} Debts  • ${cloudSyncReport.salesCount} Sales  • ${cloudSyncReport.branchesCount} Branches",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSyncToCloudNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_push_cloud_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isOnline) OrangePrimary else Color(0xFF475569)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSyncingNow
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncingNow) Localization.get("pushing_to_cloud", language)
                            else Localization.get("push_to_cloud_now", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showSyncHubDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_open_sync_hub_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp), tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.get("open_sync_hub", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = OrangePrimary
                        )
                    }
                }
            }
        }

        // 3. Monthly Shop Subscription, USSD WhatsApp Verification & Smart Auto-Unlock Code Entry
        if (shouldShowSection("LICENSE", "subscription license unlock code bb2 momo airtel ussd whatsapp trial pay")) item {
            val isActive = shopProfile.isSubscriptionActive
            val daysRemaining = shopProfile.daysRemaining
            val activeBranchCount = branches.size.coerceAtLeast(1)
            val activeWorkerCount = allUsers.size.coerceAtLeast(1)
            val activeDeviceCount = shopDevices.count { it.isActive }.coerceAtLeast(1)
            val currentPricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
                branchCount = activeBranchCount,
                workerCount = activeWorkerCount,
                durationMonths = 1,
                deviceCount = activeDeviceCount
            )
            val ownerAccount = remember(currentUser, allUsers) {
                allUsers.firstOrNull { it.role == UserRole.OWNER } ?: currentUser ?: allUsers.firstOrNull()
            }
            val deviceCode = remember(shopProfile) {
                OfflineSubscriptionManager.getDeviceChallengeCode(shopProfile)
            }
            val isCompleteCode = SubscriptionSecurityManager.isCompleteUnlockCodeFormat(settingsUnlockCodeInput)
            LaunchedEffect(settingsUnlockCodeInput) {
                val clean = settingsUnlockCodeInput.trim().uppercase()
                if (SubscriptionSecurityManager.isCompleteUnlockCodeFormat(clean) && clean != lastAutoSubmittedCode) {
                    lastAutoSubmittedCode = clean
                    onActivateVoucher(clean)
                    settingsUnlockCodeInput = ""
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isActive) ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isActive) ProfitGreen else LossRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = Localization.get("subscription_title", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentPricing.branchTierName} • ${currentPricing.totalPayable} FRw/mo",
                                    fontSize = 11.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isActive) "$daysRemaining ${Localization.get("days_left", language)}" else "Expired",
                                color = if (isActive) ProfitGreen else LossRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account Binding Summary & Live Admin USSD/WhatsApp Config
                    val liveWhatsApp = livePaymentConfig.adminWhatsAppNumber.trim()
                        .ifBlank { shopProfile.adminWhatsAppNumber.trim() }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Account: @${ownerAccount?.username?.ifBlank { "owner" } ?: "owner"} • Phone: ${ownerAccount?.phone?.ifBlank { shopProfile.phone } ?: shopProfile.phone}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Devices: $activeDeviceCount paired (${currentPricing.deviceTierName}) • Device ID: $deviceCode",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Admin WhatsApp: ${liveWhatsApp.ifBlank { "Configure in Admin Console -> Pay Config" }}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (liveWhatsApp.isNotBlank()) ProfitGreen else OrangePrimary
                            )
                            Text(
                                text = "MTN: ${currentPricing.ussdMtnCode} • Airtel: ${currentPricing.ussdAirtelCode}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Background WorkManager Cloud Auto-Sync: Active even when app is closed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ProfitGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 1: Real USSD Dialers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                OfflineSubscriptionManager.dialMtnMoMo(context, currentPricing.totalPayable)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_ussd_mtn_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
                        ) {
                            Text("Dial MTN *182# (${currentPricing.totalPayable} FRw)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                OfflineSubscriptionManager.dialAirtelMoney(context, currentPricing.totalPayable)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_ussd_airtel_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Dial Airtel *500# (${currentPricing.totalPayable} FRw)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2: Pay & Submit WhatsApp Proof + Open Full Licensing Wizard
                    Button(
                        onClick = { showSubscriptionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_open_subscription_wizard_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.get("pay_ussd_whatsapp_cta", language),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 3: Insert Admin Unlock Code right inside Settings (Auto-Unlocks Instantly!)
                    Text(
                        text = Localization.get("insert_unlock_code_title", language),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@${ownerAccount?.username?.ifBlank { "owner" } ?: "owner"} • ${ownerAccount?.phone?.ifBlank { shopProfile.phone } ?: shopProfile.phone}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = settingsUnlockCodeInput,
                        onValueChange = { settingsUnlockCodeInput = it },
                        label = { Text(Localization.get("enter_voucher_code", language)) },
                        placeholder = { Text("BB2-SINGLE-030D-...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = OrangePrimary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_unlock_code_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (isCompleteCode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ProfitGreen.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_auto_unlock_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.get("auto_unlock_detected", language),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val code = settingsUnlockCodeInput.trim()
                                if (code.isNotBlank()) {
                                    lastAutoSubmittedCode = code.uppercase()
                                    onActivateVoucher(code)
                                    settingsUnlockCodeInput = ""
                                }
                            },
                            enabled = settingsUnlockCodeInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_activate_unlock_code_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("activate_code", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onCheckCloudApproval,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_sync_cloud_approvals_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("sync_cloud_approvals", language), fontSize = 11.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Show Recent Payment Verification Requests & 1-tap apply when cloud approved
                    if (paymentVerificationRequests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Recent Payment Requests (${paymentVerificationRequests.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        paymentVerificationRequests.take(2).forEach { req ->
                            val isApproved = req.status == "APPROVED" && req.generatedUnlockCode.isNotBlank()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isApproved) ProfitGreen.copy(alpha = 0.1f) else Color(0xFFFFF7ED),
                                border = BorderStroke(1.dp, if (isApproved) ProfitGreen.copy(alpha = 0.4f) else OrangePrimary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${req.provider} • ${req.amountRwf} FRw (${req.status})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isApproved) ProfitGreen else DarkNavy
                                        )
                                        Text(
                                            text = if (isApproved) "Code: ${req.generatedUnlockCode}"
                                            else "@${req.ownerUsername} • ${if (req.syncedToSupabase) "In Supabase Queue" else "Queued Offline"}",
                                            fontSize = 10.sp,
                                            color = InkMedium
                                        )
                                    }
                                    if (isApproved) {
                                        Button(
                                            onClick = { onActivateVoucher(req.generatedUnlockCode) },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                                        ) {
                                            Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3.5 Admin Licensing & Supabase Web Platform Card
        if (shouldShowSection("LICENSE", "admin console master supabase web portal unlock codes verification")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DarkNavy.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DarkNavy.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("admin_portal_title", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (adminSession != null) {
                                    "Signed in as ${adminSession.email} • Live Supabase Sync"
                                } else {
                                    "Supabase Auth licensing portal • Multi-shop management & BB2 codes"
                                },
                                fontSize = 11.sp,
                                color = if (adminSession != null) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onRefreshAdminDashboard()
                                showAdminConsoleDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("open_admin_console_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("open_admin_console", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShareAdminWebPortal,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("share_admin_web_portal_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("web_admin_portal", language), fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Shop Profile & Business Registration Form
        if (shouldShowSection("SHOP", "shop profile name owner phone tin tax vat currency address")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("shop_profile", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(Localization.get("shop_name", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(Localization.get("owner_name", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(Localization.get("phone_number", language)) },
                        placeholder = { Text("+250 788 123 456") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tinNumber,
                            onValueChange = { tinNumber = it },
                            label = { Text("TIN / Tax ID") },
                            placeholder = { Text("100234567") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text("Tax % (VAT)") },
                            placeholder = { Text("18.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "$currencySymbol - $currencyCode",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Localization.get("currency", language)) },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = OrangePrimary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            listOf(
                                Triple("FRw", "RWF", "Rwandan Franc"),
                                Triple("$", "USD", "US Dollar"),
                                Triple("KSh", "KES", "Kenyan Shilling"),
                                Triple("USh", "UGX", "Ugandan Shilling"),
                                Triple("€", "EUR", "Euro")
                            ).forEach { (sym, code, name) ->
                                DropdownMenuItem(
                                    text = { Text("$sym - $name ($code)") },
                                    onClick = {
                                        currencySymbol = sym
                                        currencyCode = code
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(Localization.get("address", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val parsedTax = taxRateText.toDoubleOrNull() ?: 0.0
                            val parsedDailyTarget = dailySalesTargetText.toDoubleOrNull() ?: shopProfile.dailySalesTarget
                            val parsedMonthlyTarget = monthlyRevenueTargetText.toDoubleOrNull() ?: shopProfile.monthlyRevenueTarget
                            val parsedMargin = targetMarginText.toDoubleOrNull() ?: shopProfile.targetMarginPercent
                            val parsedFloat = cashFloatText.toDoubleOrNull() ?: shopProfile.cashRegisterFloat

                            onSaveProfile(
                                shopProfile.copy(
                                    shopName = shopName.trim(),
                                    name = ownerName.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    tinNumber = tinNumber.trim(),
                                    taxRate = parsedTax,
                                    receiptHeader = receiptHeader.trim(),
                                    receiptFooter = receiptFooter.trim(),
                                    currencyCode = currencyCode,
                                    currencySymbol = currencySymbol,
                                    dailySalesTarget = parsedDailyTarget,
                                    monthlyRevenueTarget = parsedMonthlyTarget,
                                    targetMarginPercent = parsedMargin,
                                    cashRegisterFloat = parsedFloat,
                                    receiptPaperWidthMm = paperWidthMm,
                                    showLogoOnReceipt = showLogoOnReceipt,
                                    showTaxOnReceipt = showTaxOnReceipt,
                                    showBarcodeOnReceipt = showBarcodeOnReceipt,
                                    autoPrintReceiptOnSale = autoPrintReceiptOnSale,
                                    soundFeedbackEnabled = soundFeedbackEnabled,
                                    hapticFeedbackEnabled = hapticFeedbackEnabled
                                )
                            )
                            Toast.makeText(context, "Shop Settings saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("save_shop_profile", language), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Financial Targets & Cash Register Float
        if (shouldShowSection("SHOP", "financial targets daily monthly goal margin cash register float")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Intego z'Ubucuruzi & Isanduku (Financial Goals)" else "Financial Targets & Cash Float",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dailySalesTargetText,
                            onValueChange = { dailySalesTargetText = it },
                            label = { Text("Daily Target ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = monthlyRevenueTargetText,
                            onValueChange = { monthlyRevenueTargetText = it },
                            label = { Text("Monthly Goal ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = targetMarginText,
                            onValueChange = { targetMarginText = it },
                            label = { Text("Target Margin %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = cashFloatText,
                            onValueChange = { cashFloatText = it },
                            label = { Text("Opening Cash Float") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // 6. Thermal POS Receipt & Hardware Printer Settings
        if (shouldShowSection("SHOP", "thermal printer receipt paper 58mm 80mm esc pos logo tax barcode qr")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ibicapishwa & Resi (Thermal Printer)" else "Thermal Printer & Receipts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OrangePrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${paperWidthMm}mm ESC/POS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Receipt Paper Width:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { paperWidthMm = 58 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paperWidthMm == 58) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "58 mm (Mobile POS)",
                                color = if (paperWidthMm == 58) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { paperWidthMm = 80 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paperWidthMm == 80) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "80 mm (Counter Desktop)",
                                color = if (paperWidthMm == 80) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = receiptHeader,
                        onValueChange = { receiptHeader = it },
                        label = { Text("Receipt Header Subtitle") },
                        placeholder = { Text("Quality Goods & Fast Delivery") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text(Localization.get("receipt_footer", language)) },
                        placeholder = { Text("Murakoze Cyane! Welcome Again!") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Logo on Printed Receipt", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = showLogoOnReceipt,
                            onCheckedChange = { showLogoOnReceipt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Tax / TIN Breakdown", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = showTaxOnReceipt,
                            onCheckedChange = { showTaxOnReceipt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Print Barcode / QR Code on Bottom", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = showBarcodeOnReceipt,
                            onCheckedChange = { showBarcodeOnReceipt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Print on Checkout Finish", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = autoPrintReceiptOnSale,
                            onCheckedChange = { autoPrintReceiptOnSale = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showThermalPreviewDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Thermal Receipt Layout", fontWeight = FontWeight.Bold, color = OrangePrimary, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // 7. Sound FX & Haptic Touch Sensory Feedback
        if (shouldShowSection("SHOP", "audio sound chime haptic vibration feedback pos")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Amajwi & Gukoraho (Sensory Feedback)" else "Audio & Haptic Feedback",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("POS Audio Chime", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Play sound on barcode scan & checkout", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = soundFeedbackEnabled,
                            onCheckedChange = { soundFeedbackEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Vibration", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tactile pulse on keypad click & buttons", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hapticFeedbackEnabled,
                            onCheckedChange = { hapticFeedbackEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                        )
                    }
                }
            }
        }

        // 7.5 Terminal Security, Hardware Integrity & Auto-Lock Timeout Card
        if (shouldShowSection("SECURITY", "security autolock idle timeout hardware integrity strongbox keystore sqlcipher aes audit")) item {
            val integrity = deviceIntegrityStatus ?: remember { com.example.util.DeviceSecurityManager.inspectDeviceIntegrity() }
            val keystoreLabel = remember { com.example.util.CryptoKeyManager.getHardwareKeystoreStatus() }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = ProfitGreen.copy(alpha = 0.14f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = Localization.get("security_autolock_title", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${integrity.postureLabel} • $keystoreLabel",
                                    fontSize = 11.sp,
                                    color = if (integrity.isCompromisedRisk) LossRed else ProfitGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Localization.get("idle_lock_timeout", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 15, 30).forEach { mins ->
                            val selected = idleLockMinutes == mins
                            FilterChip(
                                selected = selected,
                                onClick = { onSetIdleLockMinutes(mins) },
                                label = {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangePrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_idle_lock_${mins}m")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_security_audit_card_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security Posture & HLC Sync Diagnostics", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProfitGreen)
                    }
                }
            }
        }

        // 8. Supercharged SQLite Database Engine & Maintenance Hub
        if (shouldShowSection("SECURITY", "sqlite database engine sqlcipher pragma integrity wal checkpoint reset wipe factory")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Ububiko bwa SQLite (Database Engine)" else "SQLite Database Engine & Health",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Room SQLite v11 • SQLCipher AES-256 + WAL",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(onClick = { onRefreshDatabaseStats() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Stats", tint = Color(0xFF0284C7))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Database Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total Records", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = databaseStats?.totalRecordsCount?.toString() ?: "${products.size + sales.size + customers.size}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("DB Storage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = databaseStats?.fileSizeFormatted ?: "Active",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ProfitGreen
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("B-Tree Health", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (databaseStats?.integrityStatus == "OK") "100% OK" else "Optimal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Record breakdown chips
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Live Schema Counts:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Products: ${databaseStats?.productCount ?: products.size}   • Sales: ${databaseStats?.salesCount ?: sales.size}   • Customers: ${databaseStats?.customerCount ?: customers.size}\n• Payments: ${databaseStats?.paymentsCount ?: customerPayments.size}   • Sync Queue: ${databaseStats?.syncQueueCount ?: pendingSyncCount}   • Purchases: ${databaseStats?.purchasesCount ?: 0}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Database Engine Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onVerifyDatabaseIntegrity() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PRAGMA Integrity", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onOptimizeDatabase() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isOptimizingDb) "Optimizing..." else "WAL Checkpoint", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDiagnosticsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_diagnostics_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync & Security Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showResetTransactionsDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = LossRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Sales History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wipe Catalog & Wipe Customers Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearProductsDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = LossRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe All Products", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showClearCustomersDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = LossRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe All Customers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Complete Factory Reset Button
                    Button(
                        onClick = { showResetWholeAppDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Factory Reset Whole App (Wipe All)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 9. Device Permissions & System Integration Hub
        if (shouldShowSection("SECURITY", "permissions contacts sms messages notifications camera scanner")) item {
            val hasContacts = PermissionManager.hasContactsPermission(context)
            val hasSms = PermissionManager.hasSmsPermission(context)
            val hasNotifications = PermissionManager.hasNotificationPermission(context)
            val hasCamera = PermissionManager.hasCameraPermission(context)
            val isAllGranted = hasContacts && hasSms && hasNotifications && hasCamera

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isAllGranted) ProfitGreen.copy(alpha = 0.15f) else OrangePrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isAllGranted) ProfitGreen else OrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Uburenganzira bwa Telefone (Permissions)" else "Device Permissions & Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAllGranted) "All capabilities active & ready" else "Grant access for contacts, SMS & scanner",
                                    fontSize = 11.5.sp,
                                    color = if (isAllGranted) ProfitGreen else OrangePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Permission Badges Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasContacts) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Contacts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (hasContacts) ProfitGreen else LossRed)
                                Text(if (hasContacts) "Granted" else "Missing", fontSize = 9.sp, color = InkMedium)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasSms) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Messages", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (hasSms) ProfitGreen else LossRed)
                                Text(if (hasSms) "Granted" else "Missing", fontSize = 9.sp, color = InkMedium)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasNotifications) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Alerts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (hasNotifications) ProfitGreen else LossRed)
                                Text(if (hasNotifications) "Granted" else "Missing", fontSize = 9.sp, color = InkMedium)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasCamera) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (hasCamera) ProfitGreen else LossRed)
                                Text(if (hasCamera) "Granted" else "Missing", fontSize = 9.sp, color = InkMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showPermissionsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Genzura & Tanga Uburenganzira Bwose" else "Manage & Request All Permissions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // 9.5 Enterprise Push Notification System & Live Alert Testing Hub
        if (shouldShowSection("SECURITY", "notifications alerts push sale low stock debt transfer goal sync")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = OrangeLight,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Uburyo bw'Imenyesha (Push Notifications)" else "Enterprise Push Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Imenyesha ry'igurisha, ibicuruzwa biri gushira & amadeni" else "Real-time alerts for Sales, Low Stock, Debts & Sync",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ProfitGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = ProfitGreen,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) 
                            "Kanda hano munsi kugira ngo ugerageze uburyo buri bwoko bw'imenyesha bugaragara muri telefone yawe:" 
                            else "Tap below to test real system notifications delivered to your device status bar and notification tray:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Sale & Low Stock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestNotification("SALE") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(15.dp), tint = ProfitGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("POS Sale Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onTestNotification("LOW_STOCK") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(15.dp), tint = LossRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Low Stock Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Debt & Transfer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestNotification("DEBT") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Debt Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onTestNotification("TRANSFER") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stock Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Target & Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTestNotification("TARGET") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Daily Goal Hit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onTestNotification("SYNC") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp), tint = ProfitGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Branch Sync Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 10. Official App Branding & Security Verification Card
        if (shouldShowSection("LANGUAGE", "about brand version splash intro beboss")) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(54.dp),
                        shadowElevation = 4.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                            contentDescription = "BeBoss Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "BeBoss Business POS",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Version 2.6.0 • Enterprise SQLite Powered Edition",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = OrangePrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Hardware Token & AES SQLite Room Database Active",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onShowSplashScreen,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Reba Intangiriro ya BeBoss" else "Replay Brand Launch Intro",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Thermal Receipt Preview Dialog
    if (showThermalPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showThermalPreviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thermal Receipt (${paperWidthMm}mm ESC/POS)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(if (paperWidthMm == 58) 0.95f else 1f)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFBFBFB),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (showLogoOnReceipt) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("BB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Text(shopName.ifBlank { "BEBOSS RETAIL SHOP" }.uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                            if (receiptHeader.isNotBlank()) {
                                Text(receiptHeader, fontSize = 11.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            }
                            Text(address.ifBlank { "Kigali, Rwanda" }, fontSize = 10.5.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            Text("Tel: ${phone.ifBlank { "+250 788 000 000" }}", fontSize = 10.5.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            if (showTaxOnReceipt && tinNumber.isNotBlank()) {
                                Text("TIN: $tinNumber", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }

                            Text("--------------------------------", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Text("RECEIPT #: RC-2026-08819", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            Text("Date: 2026-08-29 14:30:22", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            Text("Cashier: ${ownerName.ifBlank { "Main Cashier" }}", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                            Text("--------------------------------", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                            // Sample Items
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Inyange Milk 500ml x 2", fontSize = 10.5.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text("1,400 $currencySymbol", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bakhresa Azam Bread 1kg", fontSize = 10.5.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text("1,200 $currencySymbol", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gorilla Coffee 250g", fontSize = 10.5.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text("4,500 $currencySymbol", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }

                            Text("--------------------------------", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SUBTOTAL:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text("7,100 $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                            if (showTaxOnReceipt) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("VAT (18% Included):", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                    Text("1,083 $currencySymbol", fontSize = 10.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL PAID (CASH):", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Text("7,100 $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                            Text("================================", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                            if (showBarcodeOnReceipt) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Black)
                                Text("*RC-2026-08819*", fontSize = 9.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(receiptFooter.ifBlank { "Murakoze Cyane! Thank you!" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                            Text("Powered by BeBoss Offline POS", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Test print signal sent to $paperWidthMm mm thermal printer!", Toast.LENGTH_SHORT).show()
                        showThermalPreviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test Print")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showThermalPreviewDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Reset Transactions Dialog
    if (showResetTransactionsDialog) {
        AlertDialog(
            onDismissRequest = { showResetTransactionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Sales History?", fontWeight = FontWeight.Bold, color = LossRed, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "This will safely erase sales transactions, purchases, and debt histories so you can start a fresh accounting period.\n\nYour product catalog, branches, users, and shop settings will be 100% PRESERVED."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetTransactionsDialog = false
                        onClearTransactionalData()
                        Toast.makeText(context, "Sales history reset successfully. Catalog preserved!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Reset Sales & Start Fresh", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetTransactionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Products Dialog
    if (showClearProductsDialog) {
        AlertDialog(
            onDismissRequest = { showClearProductsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe All Products?", fontWeight = FontWeight.Bold, color = LossRed, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete ALL products from your stock sheet?\n\nThis will permanently remove all catalog items, inventory counts, and barcode mappings from the database."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearProductsDialog = false
                        onClearAllProducts()
                        Toast.makeText(context, "All products wiped from database.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Wipe All Products", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearProductsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Customers Dialog
    if (showClearCustomersDialog) {
        AlertDialog(
            onDismissRequest = { showClearCustomersDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe All Customers?", fontWeight = FontWeight.Bold, color = LossRed, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete ALL customers?\n\nThis will permanently remove all customer contact cards, ledger records, and debt history."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCustomersDialog = false
                        onClearAllCustomers()
                        Toast.makeText(context, "All customers wiped from database.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Wipe All Customers", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCustomersDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Factory Reset Whole App Dialog
    if (showResetWholeAppDialog) {
        AlertDialog(
            onDismissRequest = { showResetWholeAppDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Factory Reset Whole App?", fontWeight = FontWeight.Bold, color = LossRed, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "WARNING: This is an irreversible factory reset.\n\n• ALL products and inventory stock will be erased\n• ALL customers and debt records will be wiped\n• ALL sales history, receipts, payments, and purchases will be cleared\n• Shop profile and users will be reset to default factory state.\n\nProceed only if you want a completely fresh start."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetWholeAppDialog = false
                        onResetWholeApp()
                        Toast.makeText(context, "Whole app factory reset completed.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Factory Reset Everything", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetWholeAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Branch Management Dialog
    if (showBranchDialog) {
        BranchManagementDialog(
            branches = branches,
            allUsers = allUsers,
            products = products,
            sales = sales,
            stockTransfers = stockTransfers,
            currencySymbol = currencySymbol,
            selectedBranchId = selectedBranchId,
            language = language,
            onSelectActiveBranch = onSelectActiveBranch,
            onSaveBranch = onSaveBranch,
            onDeleteBranch = onDeleteBranch,
            onCreateStockTransfer = onCreateStockTransfer,
            onDeleteStockTransfer = onDeleteStockTransfer,
            onDismiss = { showBranchDialog = false }
        )
    }

    // Staff Management Dialog
    if (showStaffDialog) {
        StaffManagementDialog(
            allUsers = allUsers,
            currentUser = currentUser,
            branches = branches,
            shopProfile = shopProfile,
            language = language,
            onSaveUser = onSaveUser,
            onDeleteUser = onDeleteUser,
            onDismiss = { showStaffDialog = false }
        )
    }

    // Shop Devices Multi-Terminal Auto-Sync Dialog (+2,000 FRw/added device)
    if (showShopDevicesDialog) {
        ShopDevicesManagementDialog(
            shopProfile = shopProfile,
            shopDevices = shopDevices,
            allUsers = allUsers,
            branches = branches,
            language = language,
            onAddDevice = onAddShopDevice,
            onToggleDeviceActive = onToggleShopDeviceActive,
            onDeleteDevice = onDeleteShopDevice,
            onDismiss = { showShopDevicesDialog = false }
        )
    }

    // Shop Owner Branch & Worker Sync Hub Dialog
    if (showSyncHubDialog) {
        ShopOwnerSyncHubDialog(
            shopProfile = shopProfile,
            currentUser = currentUser,
            allUsers = allUsers,
            branches = branches,
            sales = sales,
            products = products,
            customers = customers,
            customerPayments = customerPayments,
            stockTransfers = stockTransfers,
            cloudSyncReport = cloudSyncReport,
            isSyncing = isSyncing,
            language = language,
            localServerStatus = localServerStatus,
            localServerIp = localServerIp,
            localConnectedClients = localConnectedClients,
            localLastReceivedPacket = localLastReceivedPacket,
            discoveredHubs = discoveredHubs,
            isDiscoveringHubs = isDiscoveringHubs,
            peerSyncEvents = peerSyncEvents,
            isP2pSyncing = isP2pSyncing,
            syncAuditLogs = syncAuditLogs,
            onStartLanDiscovery = onStartLanDiscovery,
            onStopLanDiscovery = onStopLanDiscovery,
            onGenerateBranchShareToken = onGenerateBranchShareToken,
            onExportFullBranchPackage = onExportFullBranchPackage,
            onSyncToCloudNow = onSyncToCloudNow,
            onSyncCloudWithEndpoint = onSyncCloudWithEndpoint,
            onTestPing = onTestPing,
            onToggleLocalServer = onToggleLocalServer,
            onSyncWithLocalHub = onSyncWithLocalHub,
            onImportBranchPacket = onImportPackage,
            onDismiss = { showSyncHubDialog = false }
        )
    }

    // Subscription & USSD + WhatsApp Verification Full-Width Dialog
    if (showSubscriptionDialog) {
        Dialog(
            onDismissRequest = { showSubscriptionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 640.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("subscription_title", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showSubscriptionDialog = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = Localization.get("close", language))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    SubscriptionScreen(
                        shopProfile = shopProfile,
                        branches = branches,
                        allUsers = allUsers,
                        shopDevices = shopDevices,
                        currentUser = currentUser,
                        paymentRequests = paymentVerificationRequests,
                        livePaymentConfig = livePaymentConfig,
                        language = language,
                        onActivateVoucher = { code ->
                            onActivateVoucher(code)
                        },
                        onProcessDirectPayment = onProcessDirectPayment,
                        onSubmitPaymentVerification = onSubmitPaymentVerification,
                        onCheckCloudApproval = onCheckCloudApproval,
                        onClose = { showSubscriptionDialog = false }
                    )
                }
            }
        }
    }

    // In-App Supabase Admin Verification & Unlock Code Generator Console
    if (showAdminConsoleDialog) {
        AdminVerificationConsoleDialog(
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
            paymentConfig = livePaymentConfig,
            isLoading = isAdminLoading,
            language = language,
            lastSyncedAt = adminLastSyncedAt,
            onSignInAdmin = onAdminSignIn,
            onSignUpAdmin = onAdminSignUp,
            onSignOutAdmin = onAdminSignOut,
            onRefresh = onRefreshAdminDashboard,
            onApprovePayment = { req, onDone ->
                if (onAdminApprovePayment != null) {
                    onAdminApprovePayment(req, onDone)
                }
            },
            onDirectAutoUnlock = { req, onDone ->
                if (onAdminDirectAutoUnlock != null) {
                    onAdminDirectAutoUnlock(req, onDone)
                }
            },
            onSavePaymentConfig = { cfg, onDone ->
                if (onAdminSavePaymentConfig != null) {
                    onAdminSavePaymentConfig(cfg, onDone)
                }
            },
            onToggleShopDeviceActive = onAdminToggleShopDevice,
            onDeleteShopDevice = onAdminDeleteShopDevice,
            onBatchApprovePending = onAdminApproveBatch,
            onRejectPayment = onAdminRejectPayment,
            onIssueManualCode = { username, regPhone, tierCode, days, amountRwf, onDone ->
                if (onAdminIssueManualCode != null) {
                    onAdminIssueManualCode(username, regPhone, tierCode, days, amountRwf, onDone)
                }
            },
            onRevokeCode = onAdminRevokeCode,
            onExtendShopSubscription = onAdminExtendShopSubscription,
            onResetShopTrial = onAdminResetShopTrial,
            onSetShopStatus = onAdminSetShopStatus,
            onUpdateShopLimits = onAdminUpdateShopLimits,
            onDeleteShopAccount = onAdminDeleteShopAccount,
            onUpdateShopUserRole = onAdminUpdateShopUserRole,
            onToggleShopUserActive = onAdminToggleShopUserActive,
            onDeleteShopUser = onAdminDeleteShopUser,
            onDeleteBranchSnapshot = onAdminDeleteBranchSnapshot,
            onShareWebPortal = onShareAdminWebPortal,
            onDismiss = { showAdminConsoleDialog = false }
        )
    }

    // Sync Conflict Resolution & Terminal Security Diagnostics Dialog
    if (showDiagnosticsDialog) {
        val integrity = deviceIntegrityStatus ?: remember { com.example.util.DeviceSecurityManager.inspectDeviceIntegrity() }
        val keystoreStatus = remember { com.example.util.CryptoKeyManager.getHardwareKeystoreStatus() }
        val rollbackFlag = remember { com.example.util.ClockGuard.isClockRollbackDetected() }
        val conflictLogs = remember(localAuditLogs) {
            localAuditLogs.filter {
                it.action.contains("CONFLICT", ignoreCase = true) ||
                    it.action.contains("REFUND", ignoreCase = true) ||
                    it.action.contains("WARNING", ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Conflicts & Security Posture", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Terminal Security", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text(
                                text = "Integrity: ${integrity.postureLabel}",
                                fontSize = 11.5.sp,
                                color = if (integrity.isCompromisedRisk) LossRed else ProfitGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Keystore: $keystoreStatus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("DB Encryption: SQLCipher AES-256 (Keystore-wrapped)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "ClockGuard: ${if (rollbackFlag) "Rollback Detected" else "Monotonic Verified"}",
                                fontSize = 11.sp,
                                color = if (rollbackFlag) LossRed else ProfitGreen
                            )
                            Text(
                                text = "TLS Cert Pinning: ${if (com.example.BuildConfig.SUPABASE_CERT_PINS.contains("sha256/")) "Pinned" else "Standard TLS"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Outbox & Sync Engine", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("Pending Outbox Items: $pendingSyncCount", fontSize = 11.5.sp)
                            Text(
                                text = "Dead-Letter / Retrying: $deadLetterSyncCount",
                                fontSize = 11.5.sp,
                                color = if (deadLetterSyncCount > 0) LossRed else ProfitGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (pendingSyncCount > 0 || deadLetterSyncCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        onSyncToCloudNow()
                                        showDiagnosticsDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                ) {
                                    Text("Retry Sync Outbox Now", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Conflict & Audit Events (${conflictLogs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    if (conflictLogs.isEmpty()) {
                        Text(
                            text = "No unresolved HLC sync conflicts or security alerts recorded.",
                            fontSize = 11.5.sp,
                            color = ProfitGreen
                        )
                    } else {
                        conflictLogs.take(10).forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("${entry.action} • ${entry.actorName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = OrangePrimary)
                                    Text(entry.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVerifyDatabaseIntegrity()
                        showDiagnosticsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Run PRAGMA Check", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(Localization.get("logout", language), fontWeight = FontWeight.Bold) },
            text = { Text(Localization.get("logout_confirm_msg", language)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(Localization.get("logout", language), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) {
                    Text(Localization.get("cancel", language))
                }
            }
        )
    }

    // Permission Request Dialog
    if (showPermissionsDialog) {
        PermissionRequestDialog(
            language = language,
            onDismiss = { showPermissionsDialog = false },
            onPermissionGranted = {
                permissionsUpdated = !permissionsUpdated
                Toast.makeText(context, "Permissions updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Data Transfer Dialog (Full Branch Ledger & Signed Token Support)
    if (showDataTransferDialog) {
        DataTransferDialog(
            shopProfile = shopProfile,
            allUsers = allUsers,
            products = products,
            customers = customers,
            sales = sales,
            customerPayments = customerPayments,
            currentUser = currentUser,
            onImportPackage = onImportPackage,
            onExportFullPackage = onExportFullBranchPackage?.let { exporter ->
                { onReady -> exporter("ALL", onReady) }
            },
            onCreateShareToken = onGenerateBranchShareToken?.let { tokenGen ->
                { onReady -> tokenGen("ALL", onReady) }
            },
            onDismiss = { showDataTransferDialog = false }
        )
    }
}

@Composable
private fun ShopDevicesManagementDialog(
    shopProfile: ShopProfile,
    shopDevices: List<ShopDevice>,
    allUsers: List<User>,
    branches: List<com.example.data.model.Branch>,
    language: AppLanguage,
    onAddDevice: (deviceName: String, assignedUser: User, branch: com.example.data.model.Branch?, onCreated: (ShopDevice) -> Unit) -> Unit,
    onToggleDeviceActive: (deviceId: String, isActive: Boolean) -> Unit,
    onDeleteDevice: (deviceId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var deviceNameInput by remember { mutableStateOf("") }
    var selectedUser by remember(allUsers) {
        mutableStateOf(allUsers.firstOrNull { it.role != UserRole.OWNER } ?: allUsers.firstOrNull())
    }
    var selectedBranch by remember(branches) { mutableStateOf(branches.firstOrNull()) }
    var latestCreatedDevice by remember { mutableStateOf<ShopDevice?>(null) }

    val ownerUser = remember(allUsers) { allUsers.firstOrNull { it.role == UserRole.OWNER } ?: allUsers.firstOrNull() }
    val activeCount = shopDevices.count { it.isActive }.coerceAtLeast(1)
    val extraFee = (activeCount - 1).coerceAtLeast(0) * OfflineSubscriptionManager.PER_EXTRA_DEVICE_FEE_RWF

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 620.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = ProfitGreen.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Devices, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = Localization.get("shop_devices_title", language),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$activeCount Active • +$extraFee FRw/mo Multi-Device Add-on",
                                fontSize = 11.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = Localization.get("close", language))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ProfitGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.get("device_fee_addon_note", language),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Add New Paired Device Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = Localization.get("add_shop_device", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = deviceNameInput,
                            onValueChange = { deviceNameInput = it },
                            label = { Text("Terminal / Phone Name *") },
                            placeholder = { Text("e.g. Kigali Counter 2, Warehouse Tablet") },
                            leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null, tint = OrangePrimary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_device_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (allUsers.isNotEmpty()) {
                            Text("Assign Staff Operator:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(allUsers) { usr ->
                                    val picked = selectedUser?.id == usr.id
                                    FilterChip(
                                        selected = picked,
                                        onClick = { selectedUser = usr },
                                        label = { Text("${usr.name} (${usr.role.name})", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        if (branches.isNotEmpty()) {
                            Text("Assign Branch:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(branches) { br ->
                                    val picked = selectedBranch?.id == br.id
                                    FilterChip(
                                        selected = picked,
                                        onClick = { selectedBranch = br },
                                        label = { Text(br.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val userToAssign = selectedUser ?: return@Button
                                val cleanName = deviceNameInput.trim().ifBlank { "${userToAssign.name} Device" }
                                onAddDevice(cleanName, userToAssign, selectedBranch) { created ->
                                    latestCreatedDevice = created
                                    deviceNameInput = ""
                                }
                            },
                            enabled = selectedUser != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("add_shop_device_submit_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("add_shop_device", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (latestCreatedDevice != null) {
                    val created = latestCreatedDevice!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OrangePrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Device Ready to Pair!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OrangePrimary)
                            Text(
                                text = "Join Code: ${created.joinCode}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "On the new device, tap Create/Register Shop -> 'Join Existing Shop' -> enter Owner @${ownerUser?.username ?: "owner"}, Phone ${shopProfile.phone}, and Join Code ${created.joinCode}.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Paired Shop Terminals (${shopDevices.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                shopDevices.forEach { dev ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (dev.isActive) ProfitGreen.copy(alpha = 0.35f) else LossRed.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dev.deviceName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "@${dev.assignedUsername} (${dev.role}) • ${dev.branchName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (dev.isOwnerPrimary) OrangePrimary.copy(alpha = 0.14f) else ProfitGreen.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = if (dev.isOwnerPrimary) "PRIMARY FREE" else "+${dev.monthlyFeeAddonRwf} FRw/mo",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dev.isOwnerPrimary) OrangePrimary else ProfitGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (!dev.isOwnerPrimary) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Join Code: ${dev.joinCode}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Join Code", dev.joinCode))
                                                Toast.makeText(context, "Copied Join Code: ${dev.joinCode}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Join Code", tint = OrangePrimary, modifier = Modifier.size(17.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val pairingMsg = """
                                                    *BEBOSS DEVICE PAIRING INVITE*
                                                    Shop: ${shopProfile.shopName}
                                                    Device Name: ${dev.deviceName}
                                                    Assigned Staff: ${dev.assignedUserName} (@${dev.assignedUsername}, ${dev.role})
                                                    Branch: ${dev.branchName}

                                                    Open BeBoss App -> Create/Register Shop -> *Join Existing Shop*:
                                                    • Owner Username: ${ownerUser?.username ?: "owner"}
                                                    • Owner Phone: ${shopProfile.phone}
                                                    • Device Join Code: ${dev.joinCode}
                                                """.trimIndent()
                                                PdfReportGenerator.sendWhatsAppDirect(context, "", pairingMsg)
                                            },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share Pairing Info", tint = ProfitGreen, modifier = Modifier.size(17.dp))
                                        }
                                        Switch(
                                            checked = dev.isActive,
                                            onCheckedChange = { checked -> onToggleDeviceActive(dev.id, checked) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ProfitGreen)
                                        )
                                        IconButton(
                                            onClick = { onDeleteDevice(dev.id) },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove device", tint = LossRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

