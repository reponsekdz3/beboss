package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.BranchPeerSyncEvent
import com.example.util.CloudSyncReport
import com.example.util.DiscoveredBranchHub
import com.example.util.EndpointPingResult
import com.example.util.LocalServerStatus
import com.example.util.PdfReportGenerator
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import com.example.util.SyncAuditLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShopOwnerSyncHubDialog(
    shopProfile: ShopProfile,
    currentUser: User?,
    allUsers: List<User>,
    branches: List<Branch>,
    sales: List<Sale>,
    products: List<Product>,
    customers: List<Customer>,
    customerPayments: List<CustomerPayment>,
    stockTransfers: List<StockTransfer> = emptyList(),
    cloudSyncReport: CloudSyncReport?,
    isSyncing: Boolean,
    language: AppLanguage,
    localServerStatus: LocalServerStatus = LocalServerStatus.STOPPED,
    localServerIp: String? = null,
    localConnectedClients: Int = 0,
    localLastReceivedPacket: String? = null,
    discoveredHubs: List<DiscoveredBranchHub> = emptyList(),
    isDiscoveringHubs: Boolean = false,
    peerSyncEvents: List<BranchPeerSyncEvent> = emptyList(),
    isP2pSyncing: Boolean = false,
    syncAuditLogs: List<SyncAuditLog> = emptyList(),
    onStartLanDiscovery: () -> Unit = {},
    onStopLanDiscovery: () -> Unit = {},
    onGenerateBranchShareToken: ((branchIdFilter: String?, onReady: (String) -> Unit) -> Unit)? = null,
    onExportFullBranchPackage: ((branchIdFilter: String?, onReady: (File) -> Unit) -> Unit)? = null,
    onSyncToCloudNow: () -> Unit,
    onSyncCloudWithEndpoint: (String?) -> Unit = {},
    onTestPing: (String, (EndpointPingResult) -> Unit) -> Unit = { _, _ -> },
    onToggleLocalServer: (Boolean) -> Unit = {},
    onSyncWithLocalHub: (String) -> Unit = {},
    onImportBranchPacket: (ShopImportSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var importTokenOrJson by remember { mutableStateOf("") }
    var generatedShareToken by remember { mutableStateOf<String?>(null) }
    var selectedScopeBranchId by remember { mutableStateOf("ALL") }
    var mergeFeedback by remember { mutableStateOf<String?>(null) }
    var isMergeSuccess by remember { mutableStateOf(false) }

    var customEndpointUrl by remember { mutableStateOf(shopProfile.backendServerUrl.ifBlank { "https://api.beboss.app/v1" }) }
    var pingResultState by remember { mutableStateOf<EndpointPingResult?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    var targetMasterHubIp by remember { mutableStateOf("192.168.43.1") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    DisposableEffect(Unit) {
        onStartLanDiscovery()
        onDispose {
            onStopLanDiscovery()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 680.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = OrangeLight,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ikoranabuhanga ryo Gusangiza Amashami" else "Branch DataShare Engine",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "LAN Mesh Auto-Discovery • Signed Tokens • Full Branch Sync",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = OrangePrimary,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WiFi Mesh (${discoveredHubs.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Token & File", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Branches (${branches.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cloud Server", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Audit (${syncAuditLogs.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Zero-Data LAN WiFi / Hotspot Mesh Auto-Discovery & P2P Sync
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 440.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Host Master Hub Switch
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Host Master Branch Hub (mDNS + UDP)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = if (localServerStatus == LocalServerStatus.RUNNING)
                                                    "Broadcasting on ${localServerIp ?: "LAN"}:8989 (HMAC Mesh)"
                                                    else "Enable to share & receive live branch stock/sales over WiFi",
                                                fontSize = 11.sp,
                                                color = if (localServerStatus == LocalServerStatus.RUNNING) ProfitGreen else InkMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Switch(
                                            checked = localServerStatus == LocalServerStatus.RUNNING,
                                            onCheckedChange = { onToggleLocalServer(it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight),
                                            modifier = Modifier.testTag("toggle_master_hub_switch")
                                        )
                                    }

                                    if (localServerStatus == LocalServerStatus.RUNNING && localServerIp != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0F172A), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Master Endpoint: http://$localServerIp:8989", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF6EE7B7), fontFamily = FontFamily.Monospace)
                                                Text("Active Connections: $localConnectedClients • Auto-Discovery Beacon Active", fontSize = 10.5.sp, color = Color(0xFFCBD5E1))
                                                if (!localLastReceivedPacket.isNullOrBlank()) {
                                                    Text("Last Merge: $localLastReceivedPacket", fontSize = 10.5.sp, color = Color(0xFFFDBA74))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Auto-Discovered LAN Terminals
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDiscoveringHubs) "Scanning WiFi/Hotspot Terminals..." else "Discovered Terminals",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = DarkNavy
                                            )
                                        }
                                        if (isDiscoveringHubs) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                                        }
                                    }

                                    if (discoveredHubs.isEmpty()) {
                                        Text(
                                            text = "No nearby BeBoss Master Hubs detected yet. Turn on 'Host Master Branch Hub' on the owner device on the same WiFi/Hotspot, or enter IP manually below.",
                                            fontSize = 11.sp,
                                            color = InkMedium
                                        )
                                    } else {
                                        discoveredHubs.forEach { hub ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${hub.shopName} • ${hub.branchName}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkNavy)
                                                        Text(
                                                            text = "${hub.ipAddress}:${hub.port} • ${hub.branchesCount} branches, ${hub.productsCount} items",
                                                            fontSize = 10.5.sp,
                                                            color = InkMedium,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { onSyncWithLocalHub(hub.ipAddress) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                                        shape = RoundedCornerShape(8.dp),
                                                        enabled = !isP2pSyncing
                                                    ) {
                                                        Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Manual IP Fallback Connect
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Direct IP Connect (Fallback)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    OutlinedTextField(
                                        value = targetMasterHubIp,
                                        onValueChange = { targetMasterHubIp = it },
                                        label = { Text("Master Hub IP Address") },
                                        placeholder = { Text("192.168.43.1") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = { onSyncWithLocalHub(targetMasterHubIp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("sync_local_hub_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                                        enabled = !isP2pSyncing && targetMasterHubIp.isNotBlank()
                                    ) {
                                        if (isP2pSyncing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Mesh Syncing...")
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Bidirectional Mesh Sync with IP", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (peerSyncEvents.isNotEmpty()) {
                                Text("Live Mesh Peer Activity", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                peerSyncEvents.take(4).forEach { ev ->
                                    Text("• [${dateFormat.format(Date(ev.timestamp))}] ${ev.peerName} (${ev.peerIp}): ${ev.summary}", fontSize = 11.sp, color = InkMedium)
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Signed Branch Share Token (BBSHARE1:) + Full .beboss Package
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 440.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Select Branch Scope to Share:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val scopeOptions = listOf(Pair("ALL", "All Branches (${branches.size})")) +
                                    branches.take(3).map { Pair(it.id, it.name.take(14)) }
                                scopeOptions.forEach { (id, label) ->
                                    val isSelected = selectedScopeBranchId == id
                                    Surface(
                                        onClick = { selectedScopeBranchId = id },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                            Text(
                                                text = label,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val filter = selectedScopeBranchId.takeIf { it != "ALL" }
                                        if (onGenerateBranchShareToken != null) {
                                            onGenerateBranchShareToken(filter) { tok ->
                                                generatedShareToken = tok
                                            }
                                        } else {
                                            val json = ShopDataTransferManager.createExportJson(
                                                profile = shopProfile,
                                                users = allUsers,
                                                products = products,
                                                customers = customers,
                                                sales = sales,
                                                saleItems = emptyList(),
                                                payments = customerPayments,
                                                exportedByUser = currentUser,
                                                branches = branches,
                                                stockTransfers = stockTransfers,
                                                branchIdFilter = filter
                                            )
                                            generatedShareToken = ShopDataTransferManager.createCompressedSyncToken(json)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("generate_branch_token_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate Token", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val filter = selectedScopeBranchId.takeIf { it != "ALL" }
                                        if (onExportFullBranchPackage != null) {
                                            onExportFullBranchPackage(filter) { file ->
                                                ShopDataTransferManager.shareBackupFile(context, file, shopProfile.shopName)
                                            }
                                        } else {
                                            val exportedFile = ShopDataTransferManager.exportShopPackage(
                                                context = context,
                                                profile = shopProfile,
                                                users = allUsers,
                                                products = products,
                                                customers = customers,
                                                sales = sales,
                                                saleItems = emptyList(),
                                                payments = customerPayments,
                                                exportedByUser = currentUser,
                                                branches = branches,
                                                stockTransfers = stockTransfers,
                                                branchIdFilter = filter
                                            )
                                            ShopDataTransferManager.shareBackupFile(context, exportedFile, "${shopProfile.shopName}-Branch")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(".beboss File", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!generatedShareToken.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Signed Compressed Token Ready (${generatedShareToken!!.length} chars)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                        Text(
                                            text = generatedShareToken!!.take(96) + "...",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = DarkNavy
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cm.setPrimaryClip(ClipData.newPlainText("BeBoss Branch Token", generatedShareToken))
                                                    Toast.makeText(context, "Copied BBSHARE1 token!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    PdfReportGenerator.sendWhatsAppDirect(context, shopProfile.phone, generatedShareToken!!)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("WhatsApp", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            Text("Import & Verify Branch Token (`BBSHARE1:...`) or JSON:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = importTokenOrJson,
                                onValueChange = {
                                    importTokenOrJson = it
                                    mergeFeedback = null
                                },
                                label = { Text("Paste BBSHARE1:... token or JSON package") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("import_branch_token_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (mergeFeedback != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isMergeSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = mergeFeedback!!,
                                        color = if (isMergeSuccess) ProfitGreen else LossRed,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (importTokenOrJson.isBlank()) {
                                        mergeFeedback = "Paste a BBSHARE1 token or JSON sync package."
                                        isMergeSuccess = false
                                    } else {
                                        try {
                                            val summary = ShopDataTransferManager.parseShopPackageString(importTokenOrJson)
                                            onImportBranchPacket(summary)
                                            isMergeSuccess = true
                                            mergeFeedback = "Verified & merged ${summary.branches.size} branches, ${summary.productCount} products, ${summary.saleCount} sales!"
                                            importTokenOrJson = ""
                                        } catch (e: Exception) {
                                            isMergeSuccess = false
                                            mergeFeedback = "Verification failed: ${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("merge_branch_token_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify Integrity & Merge Branch Data", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Multi-Branch & Worker Live Monitor
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 440.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Active Branches & Live Operations", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            branches.forEach { branch ->
                                val branchStaff = allUsers.filter { it.assignedBranchId == branch.id || (branch.isMainBranch && it.assignedBranchId.isBlank()) }
                                val branchSales = sales.filter { it.branchId == branch.id }
                                val branchStock = products.filter { it.branchId == branch.id || (branch.isMainBranch && it.branchId.isBlank()) }
                                val branchTransfers = stockTransfers.filter { it.fromBranchId == branch.id || it.toBranchId == branch.id }
                                val rev = branchSales.sumOf { it.totalAmount }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (branch.isMainBranch) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (branch.isMainBranch) OrangePrimary.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkNavy)
                                            }
                                            if (branch.isMainBranch) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = OrangePrimary) {
                                                    Text("HQ", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Items: ${branchStock.size} • Transfers: ${branchTransfers.size} • Staff: ${branchStaff.size}",
                                            fontSize = 11.5.sp,
                                            color = InkMedium
                                        )
                                        Text(
                                            text = "Revenue: ${rev.toInt()} ${shopProfile.currencySymbol} (${branchSales.size} sales)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val summaryText = buildBranchSyncWhatsAppText(
                                        profile = shopProfile,
                                        user = currentUser,
                                        branches = branches,
                                        sales = sales,
                                        users = allUsers,
                                        products = products
                                    )
                                    PdfReportGenerator.sendWhatsAppDirect(context, shopProfile.phone, summaryText)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Multi-Branch Summary via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: Automatic Internet Branch Sync (Supabase Relay + Optional Custom Backup Server)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 440.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ProfitGreen.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Automatic Internet Branch DataShare (LWW)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ProfitGreen
                                    )
                                    Text(
                                        text = "POS, stock, transfers, customers & debts stay 100% offline-first in Room SQLite, and automatically push/merge across all your branch devices via Supabase (branch_sync_snapshots) whenever internet is available.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customEndpointUrl,
                                onValueChange = {
                                    customEndpointUrl = it
                                    pingResultState = null
                                },
                                label = { Text("Optional Private HTTP Backup Server") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (customEndpointUrl.isNotBlank()) {
                                                isPinging = true
                                                onTestPing(customEndpointUrl) { res ->
                                                    isPinging = false
                                                    pingResultState = res
                                                }
                                            }
                                        }
                                    ) {
                                        if (isPinging) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.NetworkCheck, contentDescription = "Ping Endpoint", tint = OrangePrimary)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (pingResultState != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pingResultState!!.reachable) ProfitGreen.copy(alpha = 0.14f) else LossRed.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = pingResultState!!.message,
                                        color = if (pingResultState!!.reachable) ProfitGreen else LossRed,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { onSyncCloudWithEndpoint(customEndpointUrl) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("sync_branches_cloud_now_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                enabled = !isSyncing
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isSyncing) "Syncing Branches..." else "Sync Branches & Approvals Now", fontWeight = FontWeight.Bold)
                            }

                            if (cloudSyncReport != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = cloudSyncReport.message,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pushed: ${cloudSyncReport.itemsPushed} • Pulled: ${cloudSyncReport.itemsPulled} • Latency: ${cloudSyncReport.latencyMs}ms",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // TAB 4: Sync Audit Log
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp, max = 440.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Real-Time Sync Audit & Activity Log", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (syncAuditLogs.isEmpty()) {
                                Text("No sync events recorded in this session yet.", fontSize = 12.sp, color = InkMedium)
                            } else {
                                syncAuditLogs.forEach { log ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(log.type, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = OrangePrimary)
                                                Text("${log.status} • ${dateFormat.format(Date(log.timestamp))}", fontSize = 10.5.sp, color = InkMedium)
                                            }
                                            Text(log.summary, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
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

private fun buildBranchSyncWhatsAppText(
    profile: ShopProfile,
    user: User?,
    branches: List<Branch>,
    sales: List<Sale>,
    users: List<User>,
    products: List<Product>
): String {
    val totalSales = sales.sumOf { it.totalAmount }
    val lowStock = products.count { it.isLowStock }
    return buildString {
        append("*BEBOSS MULTI-BRANCH SHIFT REPORT*\n")
        append("Store: *${profile.shopName}*\n")
        append("Operator: ${user?.name ?: "Owner"} (@${user?.username ?: "owner"})\n")
        append("Total Sales: ${totalSales.toInt()} ${profile.currencySymbol} (${sales.size} receipts)\n")
        append("Catalog Items: ${products.size} ($lowStock low stock)\n")
        append("Active Branches: ${branches.size} • Staff: ${users.size}\n")
    }
}
