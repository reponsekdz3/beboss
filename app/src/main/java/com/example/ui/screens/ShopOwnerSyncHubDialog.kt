package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.CloudSyncReport
import com.example.util.EndpointPingResult
import com.example.util.LocalServerStatus
import com.example.util.PdfReportGenerator
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import com.example.util.SyncAuditLog
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
    cloudSyncReport: CloudSyncReport?,
    isSyncing: Boolean,
    language: AppLanguage,
    localServerStatus: LocalServerStatus = LocalServerStatus.STOPPED,
    localServerIp: String? = null,
    localConnectedClients: Int = 0,
    localLastReceivedPacket: String? = null,
    isP2pSyncing: Boolean = false,
    syncAuditLogs: List<SyncAuditLog> = emptyList(),
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
    var importJsonText by remember { mutableStateOf("") }
    var mergeFeedback by remember { mutableStateOf<String?>(null) }
    var isMergeSuccess by remember { mutableStateOf(false) }

    var customEndpointUrl by remember { mutableStateOf(shopProfile.backendServerUrl.ifBlank { "https://api.beboss.app/v1" }) }
    var pingResultState by remember { mutableStateOf<EndpointPingResult?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    var targetMasterHubIp by remember { mutableStateOf("192.168.43.1") }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangeLight,
                            modifier = Modifier.size(40.dp)
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
                                text = if (language == AppLanguage.KINYARWANDA) "Ihuriro ryo Gusangiza Amashami" else "Universal Sync Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Cloud, Hotspot P2P & Amashami" else "Cloud, Local Hotspot P2P & Multi-Store",
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

                // Scrollable Tab Selector (5 full modes)
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
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cloud Sync", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Local WiFi P2P", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
                                Text("Branches", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import/Export", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
                                Text("Audit Log", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Cloud & Remote Server Sync
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.KINYARWANDA) "Ihuza ryuzuye rya Cloud Server" else "Bidirectional Cloud Sync",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkNavy
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA)
                                            "Bika ibicuruzwa, amashami, imyenda y'abakiriya n'amafaranga yinjiye kuri cloud server."
                                            else "Synchronize inventory, sales, customer debts, and branch records securely with remote server.",
                                        fontSize = 11.5.sp,
                                        color = InkMedium
                                    )
                                }
                            }

                            // Server URL input
                            OutlinedTextField(
                                value = customEndpointUrl,
                                onValueChange = {
                                    customEndpointUrl = it
                                    pingResultState = null
                                },
                                label = { Text("Cloud Backend Server Endpoint") },
                                placeholder = { Text("https://api.beboss.app/v1") },
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
                                            Icon(Icons.Default.NetworkCheck, contentDescription = "Ping Test", tint = OrangePrimary)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Ping status badge
                            if (pingResultState != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pingResultState!!.reachable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (pingResultState!!.reachable) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (pingResultState!!.reachable) ProfitGreen else LossRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pingResultState!!.message,
                                            color = if (pingResultState!!.reachable) ProfitGreen else LossRed,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Cloud Push & Pull Button
                            Button(
                                onClick = { onSyncCloudWithEndpoint(customEndpointUrl) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (language == AppLanguage.KINYARWANDA) "Birimo gusangizwa..." else "Syncing with Cloud...")
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA) "Sangiza kuri Cloud Ako Kanya" else "Push & Pull Cloud Sync Now",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Cloud Sync Summary Card if synced
                            if (cloudSyncReport != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Last Sync Status: ${cloudSyncReport.message}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkNavy)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Items Pushed: ${cloudSyncReport.itemsPushed}", fontSize = 11.sp, color = InkMedium)
                                            Text("Latency: ${cloudSyncReport.latencyMs}ms", fontSize = 11.sp, color = InkMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Local WiFi / Hotspot P2P Hub (Zero Mobile Data)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.KINYARWANDA) "Ihuriro rya Hotspot & WiFi (Data 0)" else "Offline WiFi / Hotspot Sync Hub",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkNavy
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA)
                                            "Huza telefone zawe n'abakozi binyuze kuri WiFi cyangwa Hotspot nta mafaranga ya interineti bisaba."
                                            else "Sync cashier devices directly to owner device via Local WiFi or phone Hotspot without consuming mobile data.",
                                        fontSize = 11.5.sp,
                                        color = InkMedium
                                    )
                                }
                            }

                            // Mode A: Master Hub Host Server Switch
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Host Master Hub Terminal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkNavy)
                                            Text(
                                                text = if (localServerStatus == LocalServerStatus.RUNNING) "Server Active on port 8989" else "Server Inactive",
                                                fontSize = 11.sp,
                                                color = if (localServerStatus == LocalServerStatus.RUNNING) ProfitGreen else InkMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Switch(
                                            checked = localServerStatus == LocalServerStatus.RUNNING,
                                            onCheckedChange = { onToggleLocalServer(it) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary, checkedTrackColor = OrangeLight)
                                        )
                                    }

                                    if (localServerStatus == LocalServerStatus.RUNNING && localServerIp != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("Terminal Server URL for Workers:", fontSize = 10.sp, color = InkMedium)
                                                Text("http://${localServerIp}:8989", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB), fontFamily = FontFamily.Monospace)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("Connected Workers: $localConnectedClients", fontSize = 11.sp, color = DarkNavy)
                                                if (!localLastReceivedPacket.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("Last packet: $localLastReceivedPacket", fontSize = 10.sp, color = ProfitGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Mode B: Client Cashier Mode: Connect to Master Hub IP
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Cashier Terminal / Connect to Master", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkNavy)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = targetMasterHubIp,
                                        onValueChange = { targetMasterHubIp = it },
                                        label = { Text("Master Hub IP Address") },
                                        placeholder = { Text("192.168.43.1") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onSyncWithLocalHub(targetMasterHubIp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                                        enabled = !isP2pSyncing && targetMasterHubIp.isNotBlank()
                                    ) {
                                        if (isP2pSyncing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Connecting & Syncing...")
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("1-Tap Sync with Master Terminal")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Multi-Branch & Worker Live Monitor
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Imiterere y'Amashami n'Abakozi" else "Active Branches & Assigned Staff",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            branches.forEach { branch ->
                                val branchStaff = allUsers.filter { it.assignedBranchId == branch.id || (branch.isMainBranch && it.assignedBranchId.isBlank()) }
                                val branchSales = sales.filter { it.branchId == branch.id }
                                val todayBranchRev = branchSales.sumOf { it.totalAmount }

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
                                                Text(
                                                    text = branch.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = DarkNavy
                                                )
                                            }

                                            if (branch.isMainBranch) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = OrangePrimary
                                                ) {
                                                    Text("HQ", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Staff: ${branchStaff.size} workers",
                                                fontSize = 12.sp,
                                                color = InkMedium
                                            )
                                            Text(
                                                text = "Sales: ${todayBranchRev.toInt()} ${shopProfile.currencySymbol}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ProfitGreen
                                            )
                                        }

                                        if (branchStaff.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Workers: " + branchStaff.joinToString(", ") { "${it.name} (${it.role.displayName})" },
                                                fontSize = 11.sp,
                                                color = InkDark
                                            )
                                        }
                                    }
                                }
                            }

                            // WhatsApp Sync Button
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
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Ohereza kuri WhatsApp ya Nyir'iduka" else "Send Branch Shift via WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }

                    3 -> {
                        // TAB 3: Import & Export File Packet
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Kwohereza & Kwinjiza Dosiyeyo" else "Encrypted Backup & File Portability",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Export Button
                            OutlinedButton(
                                onClick = {
                                    val exportedFile = ShopDataTransferManager.exportShopPackage(
                                        context = context,
                                        profile = shopProfile,
                                        users = allUsers,
                                        products = products,
                                        customers = customers,
                                        sales = sales,
                                        saleItems = emptyList(),
                                        payments = customerPayments,
                                        exportedByUser = currentUser
                                    )
                                    ShopDataTransferManager.shareBackupFile(context, exportedFile, "${shopProfile.shopName}-Backup")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Encrypted Backup (.beboss)")
                            }

                            HorizontalDivider()

                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Injiza JSON y'Ishami" else "Import & Merge Branch JSON",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = DarkNavy
                            )

                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = {
                                    importJsonText = it
                                    mergeFeedback = null
                                },
                                label = { Text("Paste JSON sync package") },
                                placeholder = { Text("{\"shopProfile\": {...}, \"sales\": [...]}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
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
                                        color = if (isMergeSuccess) ProfitGreen else Color(0xFFDC2626),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (importJsonText.isBlank()) {
                                        mergeFeedback = "Please paste valid JSON sync package text."
                                        isMergeSuccess = false
                                    } else {
                                        try {
                                            val summary = ShopDataTransferManager.parseShopPackageString(importJsonText)
                                            onImportBranchPacket(summary)
                                            isMergeSuccess = true
                                            mergeFeedback = "Merged ${summary.sales.size} sales & ${summary.products.size} products!"
                                            importJsonText = ""
                                        } catch (e: Exception) {
                                            isMergeSuccess = false
                                            mergeFeedback = "Merge error: ${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Process & Merge Data")
                            }
                        }
                    }

                    4 -> {
                        // TAB 4: Sync Audit Log & Telemetry
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Real-Time Sync Audit & Activity Log",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (syncAuditLogs.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No sync activity recorded yet. Run a Cloud Sync or WiFi Hotspot Sync to populate telemetry.",
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 11.5.sp,
                                        color = InkMedium
                                    )
                                }
                            } else {
                                syncAuditLogs.forEach { log ->
                                    val timeStr = dateFormat.format(Date(log.timestamp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (log.status == "SUCCESS") Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, if (log.status == "SUCCESS") Color(0xFFBBF7D0) else Color(0xFFFECACA)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = log.type,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = DarkNavy
                                                )
                                                Text(
                                                    text = if (log.latencyMs > 0) "${log.latencyMs}ms" else "",
                                                    fontSize = 10.5.sp,
                                                    color = InkMedium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = log.summary,
                                                fontSize = 11.sp,
                                                color = InkDark
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = timeStr,
                                                fontSize = 9.5.sp,
                                                color = InkMedium
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
    val timeStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    val branchName = user?.assignedBranchName?.ifBlank { "Main Store" } ?: "Main Store"
    val todaySales = sales.filter {
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = it.saleDate }
        cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
        cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
    }
    val totalTodayRev = todaySales.sumOf { it.totalAmount }
    val totalCash = todaySales.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
    val totalMomo = todaySales.filter { it.paymentMethod == "MOMO" || it.paymentMethod == "AIRTEL" }.sumOf { it.totalAmount }
    val totalDebt = todaySales.filter { it.paymentMethod == "CREDIT_DEBT" }.sumOf { it.totalAmount }

    return buildString {
        append("*${profile.shopName.ifBlank { "BeBoss Store" }} - Branch Sync Report*\n")
        append("*Branch:* $branchName\n")
        append("*Reporting Staff:* ${user?.name ?: "Branch Manager"} (${user?.role?.displayName ?: "Staff"})\n")
        append("*Timestamp:* $timeStr\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("*Today's Total Sales:* ${totalTodayRev.toInt()} ${profile.currencySymbol}\n")
        append("*Cash Collected:* ${totalCash.toInt()} ${profile.currencySymbol}\n")
        append("*Mobile Money (MoMo):* ${totalMomo.toInt()} ${profile.currencySymbol}\n")
        append("*Credit / Debts Given:* ${totalDebt.toInt()} ${profile.currencySymbol}\n")
        append("*Transactions Count:* ${todaySales.size}\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("*Active Staff at Branch (${users.size}):*\n")
        users.forEach { u ->
            append("• ${u.name} - ${u.role.displayName}\n")
        }
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("*Branch terminal synced successfully with BeBoss POS.*")
    }
}
