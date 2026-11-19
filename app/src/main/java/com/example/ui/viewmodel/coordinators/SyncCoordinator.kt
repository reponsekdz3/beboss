package com.example.ui.viewmodel.coordinators

import android.app.Application
import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.User
import com.example.data.repository.BeBossRepository
import com.example.util.CloudSyncReport
import com.example.util.ConnectivityStatus
import com.example.util.EndpointPingResult
import com.example.util.LocalNetworkSyncServer
import com.example.util.NetworkConnectivityObserver
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SyncCoordinator(
    private val application: Application,
    private val db: AppDatabase,
    private val repository: BeBossRepository,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val deviceId: String,
    private val scope: CoroutineScope,
    private val currentUser: StateFlow<User?>,
    private val selectedBranchId: StateFlow<String>,
    private val shopProfile: StateFlow<ShopProfile>,
    private val allUsers: StateFlow<List<User>>,
    private val branches: StateFlow<List<Branch>>,
    private val allProducts: StateFlow<List<Product>>,
    private val customers: StateFlow<List<Customer>>,
    private val allSales: StateFlow<List<Sale>>,
    private val allCustomerPayments: StateFlow<List<CustomerPayment>>,
    private val allStockTransfers: StateFlow<List<StockTransfer>>,
    private val emitSnackbar: suspend (String) -> Unit,
    private val syncAdminCloud: suspend (Boolean) -> Unit
) {
    private var autoBranchSyncJob: Job? = null

    val connectivityStatus: StateFlow<ConnectivityStatus> = connectivityObserver.networkStatus
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), ConnectivityStatus.AVAILABLE)

    private val _cloudSyncReport = MutableStateFlow<CloudSyncReport?>(null)
    val cloudSyncReport: StateFlow<CloudSyncReport?> = _cloudSyncReport.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isP2pSyncing = MutableStateFlow(false)
    val isP2pSyncing: StateFlow<Boolean> = _isP2pSyncing.asStateFlow()

    val localNetworkSyncServer = LocalNetworkSyncServer(application, db)
    val localServerStatus = localNetworkSyncServer.serverStatus
    val localServerIp = localNetworkSyncServer.serverIpAddress
    val localConnectedClients = localNetworkSyncServer.connectedClientsCount
    val localLastReceivedPacket = localNetworkSyncServer.lastReceivedPacketSummary
    val discoveredHubs = localNetworkSyncServer.discoveredHubs
    val isDiscoveringHubs = localNetworkSyncServer.isDiscoveringHubs
    val peerSyncEvents = localNetworkSyncServer.peerSyncEvents

    fun setupAutoCloudSyncOnInternet() {
        scope.launch {
            repository.ensureDefaultBranches()
            connectivityStatus.collect { status ->
                if (status == ConnectivityStatus.AVAILABLE) {
                    _isSyncing.value = true
                    try {
                        syncAdminCloud(true)
                        val report = repository.performSync(
                            activeBranchId = selectedBranchId.value,
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

    fun scheduleAutoBranchSync(delayMs: Long = 1600L) {
        if (connectivityStatus.value != ConnectivityStatus.AVAILABLE) return
        autoBranchSyncJob?.cancel()
        autoBranchSyncJob = scope.launch {
            delay(delayMs)
            if (connectivityStatus.value == ConnectivityStatus.AVAILABLE && !_isSyncing.value) {
                val report = repository.performSync(
                    activeBranchId = selectedBranchId.value,
                    deviceId = deviceId
                )
                _cloudSyncReport.value = report
            }
        }
    }

    fun syncToCloudNow() {
        scope.launch {
            _isSyncing.value = true
            try {
                syncAdminCloud(true)
                val report = repository.performSync(
                    activeBranchId = selectedBranchId.value,
                    deviceId = deviceId
                )
                _cloudSyncReport.value = report
                emitSnackbar(report.message)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncToCloudWithEndpoint(endpoint: String? = null) {
        scope.launch {
            _isSyncing.value = true
            try {
                val report = repository.cloudSyncManager.syncAllDataToCloud(
                    serverEndpoint = endpoint,
                    activeBranchId = selectedBranchId.value,
                    deviceId = deviceId,
                    mergeRemoteSummary = { summary -> repository.mergeShopDataPackageLww(summary) }
                )
                _cloudSyncReport.value = report
                emitSnackbar(report.message)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun testEndpointPing(endpoint: String, onResult: (EndpointPingResult) -> Unit) {
        scope.launch {
            onResult(repository.testServerPing(endpoint))
        }
    }

    fun toggleLocalSyncServer(enable: Boolean) {
        if (enable) {
            val started = localNetworkSyncServer.startServer()
            if (started) {
                repository.logSyncEvent("P2P_SERVER", "SUCCESS", "Encrypted LAN Hub started on port ${localNetworkSyncServer.port}")
            } else {
                scope.launch {
                    emitSnackbar("Unable to start encrypted LAN Hub — configure BEBOSS_LAN_SYNC_SECRET.")
                }
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
        scope.launch {
            _isP2pSyncing.value = true
            val res = localNetworkSyncServer.syncWithMasterHub(hubIp, currentUser.value)
            _isP2pSyncing.value = false
            repository.logSyncEvent(
                type = "P2P_CLIENT",
                status = if (res.success) "SUCCESS" else "FAILED",
                summary = res.message,
                latencyMs = res.latencyMs
            )
            emitSnackbar(res.message)
        }
    }

    fun createBranchShareToken(branchIdFilter: String? = null, onReady: (String) -> Unit) {
        scope.launch {
            try {
                val json = ShopDataTransferManager.createExportJson(
                    profile = shopProfile.value,
                    users = allUsers.value,
                    products = allProducts.value,
                    customers = customers.value,
                    sales = allSales.value,
                    saleItems = repository.getAllSaleItemsList(),
                    payments = allCustomerPayments.value,
                    exportedByUser = currentUser.value,
                    branches = branches.value,
                    stockTransfers = allStockTransfers.value,
                    purchases = repository.getAllPurchasesList(),
                    branchIdFilter = branchIdFilter,
                    tombstones = repository.getAllTombstonesList()
                )
                val token = ShopDataTransferManager.createCompressedSyncToken(json)
                repository.logSyncEvent("BRANCH_TOKEN", "SUCCESS", "Generated signed BBSHARE1 token (${token.length} chars)")
                onReady(token)
            } catch (e: Exception) {
                emitSnackbar("Failed to build branch share token: ${e.message}")
            }
        }
    }

    fun exportFullBranchPackage(context: Context, branchIdFilter: String? = null, onReady: (File) -> Unit) {
        scope.launch {
            try {
                val file = ShopDataTransferManager.exportShopPackage(
                    context = context,
                    profile = shopProfile.value,
                    users = allUsers.value,
                    products = allProducts.value,
                    customers = customers.value,
                    sales = allSales.value,
                    saleItems = repository.getAllSaleItemsList(),
                    payments = allCustomerPayments.value,
                    exportedByUser = currentUser.value,
                    branches = branches.value,
                    stockTransfers = allStockTransfers.value,
                    purchases = repository.getAllPurchasesList(),
                    branchIdFilter = branchIdFilter,
                    tombstones = repository.getAllTombstonesList()
                )
                repository.logSyncEvent("FILE_EXPORT", "SUCCESS", "Exported ${file.name} with full branch ledger")
                onReady(file)
            } catch (e: Exception) {
                emitSnackbar("Export error: ${e.message}")
            }
        }
    }

    fun importBranchShareToken(tokenOrJson: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        scope.launch {
            try {
                val summary = ShopDataTransferManager.parseShopPackageString(tokenOrJson, enforceReplayProtection = true)
                val result = repository.importShopDataPackage(summary, currentUser.value)
                val msg = "Merged ${summary.branches.size} branches, ${result.mergedProducts} products, ${result.mergedSales} sales (${result.conflictsResolved} HLC conflicts resolved)."
                repository.logSyncEvent("BRANCH_IMPORT", "SUCCESS", msg)
                repository.recordLocalAudit(
                    actorName = currentUser.value?.name ?: "Operator",
                    actorRole = currentUser.value?.role?.name ?: "SYSTEM",
                    action = "DATASHARE_MERGE",
                    targetId = summary.branchScopeName.orEmpty(),
                    details = msg
                )
                emitSnackbar(msg)
                onDone(true, msg)
            } catch (e: Exception) {
                val err = "Branch DataShare import failed: ${e.message}"
                emitSnackbar(err)
                onDone(false, err)
            }
        }
    }

    fun importShopPackage(summary: ShopImportSummary) {
        scope.launch {
            try {
                repository.importShopDataPackage(summary, currentUser.value)
                repository.recordLocalAudit(
                    actorName = currentUser.value?.name ?: "Operator",
                    actorRole = currentUser.value?.role?.name ?: "SYSTEM",
                    action = "PACKAGE_IMPORT",
                    targetId = summary.shopName,
                    details = "Merged shop package from ${summary.exportedBy}"
                )
                emitSnackbar("Shop data package successfully restored!")
            } catch (e: Exception) {
                emitSnackbar("Error restoring shop data: ${e.message}")
            }
        }
    }

    fun cleanup() {
        localNetworkSyncServer.stopDiscovery()
        localNetworkSyncServer.stopServer()
    }
}
