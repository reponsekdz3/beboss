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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Surface
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
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.CloudSyncReport
import com.example.util.PdfReportGenerator
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
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
    onSyncToCloudNow: () -> Unit,
    onImportBranchPacket: (ShopImportSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var importJsonText by remember { mutableStateOf("") }
    var mergeFeedback by remember { mutableStateOf<String?>(null) }
    var isMergeSuccess by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Urubuga rwo Gusangiza Amashami" else "Branch & Worker Sync Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ihuza ry'Amashami n'Abakozi" else "Owner & Multi-Store Live Sync",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = OrangePrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Gusangiza Nyir'iduka" else "Send to Owner",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ihuriro ry'Amashami" else "Branch Monitor",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Kwinjiza Amakuru" else "Import & Merge",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Send Branch Shift & Sales Packet to Shop Owner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.KINYARWANDA) "Gusangiza Nyir'ubucuruzi ako kanya" else "Instant Shift Sync to Owner",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkNavy
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA)
                                            "Kanda hano hasi wohereze raporo yuzuye y'ibicurujwe n'amafaranga yinjiye kuri WhatsApp ya nyir'iduka (${shopProfile.phone.ifBlank { "Registered Owner" }})."
                                            else "Send full branch sales, payments collected, and inventory updates directly to the store owner (${shopProfile.phone.ifBlank { "Registered Owner" }}).",
                                        fontSize = 11.5.sp,
                                        color = InkMedium
                                    )
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
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Ohereza kuri WhatsApp ya Nyir'iduka" else "Send Branch Sync via WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Cloud Push Button
                            Button(
                                onClick = onSyncToCloudNow,
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
                                    Text(if (language == AppLanguage.KINYARWANDA) "Birimo gusangizwa..." else "Syncing to Cloud...")
                                } else {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA) "Sangiza kuri Server / Cloud" else "Push All Data to Cloud Server",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Share Backup File Button
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
                                    ShopDataTransferManager.shareBackupFile(context, exportedFile, "${shopProfile.shopName}-BranchSync")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (language == AppLanguage.KINYARWANDA) "Sangiza Dosiyeyo (Bluetooth/File)" else "Share Encrypted Sync File")
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Multi-Branch & Worker Live Monitor
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Trigger Cloud Sync
                            Button(
                                onClick = onSyncToCloudNow,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                enabled = !isSyncing
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (language == AppLanguage.KINYARWANDA) "Vugurura Ihuza Ryose" else "Force Full Multi-Store Sync")
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Import & Merge Branch Packet
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Injiza no Gufatanya Amakuru y'Ishami" else "Import & Merge Branch Data Packet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA)
                                    "Koporora ubutumwa cyangwa JSON yoherejwe n'umukozi w'ishami maze ubishyire hano."
                                    else "Paste the JSON sync package received from a branch worker or another terminal to merge records into this master database.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = {
                                    importJsonText = it
                                    mergeFeedback = null
                                },
                                label = { Text("Paste Branch Sync JSON Package") },
                                placeholder = { Text("{\"shopId\": 1, \"branches\": [...], \"sales\": [...]}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
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
                                        modifier = Modifier.padding(10.dp)
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
                                            mergeFeedback = "Successfully merged ${summary.sales.size} sales, ${summary.products.size} products & ${summary.users.size} staff records!"
                                            importJsonText = ""
                                        } catch (e: Exception) {
                                            isMergeSuccess = false
                                            mergeFeedback = "Merge failed: ${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (language == AppLanguage.KINYARWANDA) "Injiza & Huza Amakuru" else "Process & Merge Branch Data")
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
        append("🏢 *${profile.shopName.ifBlank { "BeBoss Store" }} - Branch Sync Report*\n")
        append("📍 *Branch:* $branchName\n")
        append("👤 *Reporting Staff:* ${user?.name ?: "Branch Manager"} (${user?.role?.displayName ?: "Staff"})\n")
        append("📅 *Timestamp:* $timeStr\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("💰 *Today's Total Sales:* ${totalTodayRev.toInt()} ${profile.currencySymbol}\n")
        append("💵 *Cash Collected:* ${totalCash.toInt()} ${profile.currencySymbol}\n")
        append("📱 *Mobile Money (MoMo):* ${totalMomo.toInt()} ${profile.currencySymbol}\n")
        append("📝 *Credit / Debts Given:* ${totalDebt.toInt()} ${profile.currencySymbol}\n")
        append("🧾 *Transactions Count:* ${todaySales.size}\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("👥 *Active Staff at Branch (${users.size}):*\n")
        users.forEach { u ->
            append("• ${u.name} - ${u.role.displayName}\n")
        }
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("✅ *Branch terminal synced successfully with BeBoss POS.*")
    }
}
