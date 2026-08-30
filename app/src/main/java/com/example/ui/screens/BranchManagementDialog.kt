package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Branch
import com.example.data.model.Product
import com.example.data.model.Sale
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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Enterprise Multi-Branch Management & Logistics Hub
 * Features:
 * 1. Multi-Store Outlets & POS Terminals
 * 2. Inter-Branch Stock Transfers & Inventory Flow
 * 3. Multi-Branch Analytics & Revenue Comparison
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagementDialog(
    branches: List<Branch>,
    allUsers: List<User> = emptyList(),
    products: List<Product> = emptyList(),
    sales: List<Sale> = emptyList(),
    stockTransfers: List<StockTransfer> = emptyList(),
    currencySymbol: String = "FRw",
    selectedBranchId: String = "ALL",
    language: AppLanguage = AppLanguage.ENGLISH,
    onSelectActiveBranch: (String) -> Unit = {},
    onSaveBranch: (Branch) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onCreateStockTransfer: (productId: String, fromBranchId: String, fromBranchName: String, toBranchId: String, toBranchName: String, quantity: Double, notes: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteStockTransfer: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var branchToEdit by remember { mutableStateOf<Branch?>(null) }
    var isAddingNewBranch by remember { mutableStateOf(false) }
    var branchToDelete by remember { mutableStateOf<Branch?>(null) }
    var isCreatingTransfer by remember { mutableStateOf(false) }
    var transferToDelete by remember { mutableStateOf<StockTransfer?>(null) }

    val decimalFormat = remember { DecimalFormat("#,###") }
    val totalRevenueAll = remember(sales) { sales.sumOf { it.totalAmount } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ubuyobozi bw'Amashami (Multi-Branch)" else "Multi-Branch & Logistics Hub",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "${branches.size} Amashami • Ihererekanyamutungo rya Stock" else "${branches.size} Outlets • Inter-Store Transfers & Analytics",
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

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = OrangePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = OrangePrimary,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (language == AppLanguage.KINYARWANDA) "Amashami (${branches.size})" else "Outlets (${branches.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (language == AppLanguage.KINYARWANDA) "Stock Transfer (${stockTransfers.size})" else "Transfers (${stockTransfers.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (language == AppLanguage.KINYARWANDA) "Raporo" else "Analytics",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB CONTENT
                when (selectedTab) {
                    0 -> {
                        // TAB 0: OUTLETS & POS TERMINALS
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        branchToEdit = null
                                        isAddingNewBranch = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA) "Ongeraho Ishami Rishya" else "Add New Branch Outlet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(branches) { branch ->
                                    val assignedStaff = allUsers.filter { it.assignedBranchId == branch.id || (branch.isMainBranch && it.assignedBranchId.isBlank()) }
                                    val isCurrentActive = selectedBranchId == branch.id
                                    val branchSales = sales.filter { it.branchId == branch.id || (branch.isMainBranch && (it.branchId.isBlank() || it.branchId == "main_branch" || it.branchId == "ALL")) }
                                    val branchRevenue = branchSales.sumOf { it.totalAmount }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (branch.isMainBranch) Color(0xFFFFF7ED) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (branch.isMainBranch) OrangePrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(
                                                                if (branch.isActive) ProfitGreen else Color.Gray,
                                                                CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = branch.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = branch.code,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                if (branch.isMainBranch) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = OrangePrimary
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = if (language == AppLanguage.KINYARWANDA) "Ishami Rikuru" else "Main HQ",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(branch.address.ifBlank { "Kigali, Rwanda" }, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }

                                                if (branch.phone.isNotBlank()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${branch.phone}"))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                // ignore
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(branch.phone, fontSize = 11.5.sp, color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Staff & Sales Stats Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${if (language == AppLanguage.KINYARWANDA) "Manager" else "Manager"}: ${branch.managerName.ifBlank { "Store Supervisor" }}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = ProfitGreen.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "${decimalFormat.format(branchRevenue)} $currencySymbol (${branchSales.size} sales)",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ProfitGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onSelectActiveBranch(branch.id) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isCurrentActive) OrangePrimary.copy(alpha = 0.12f) else Color.Transparent
                                                    ),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (isCurrentActive) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isCurrentActive) (if (language == AppLanguage.KINYARWANDA) "Ishami Rikoreshwa" else "Active POS Outlet")
                                                            else (if (language == AppLanguage.KINYARWANDA) "Koresha ku Iposita" else "Select for POS"),
                                                        fontSize = 11.sp,
                                                        color = if (isCurrentActive) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            branchToEdit = branch
                                                            isAddingNewBranch = false
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(if (language == AppLanguage.KINYARWANDA) "Hindura" else "Edit", fontSize = 11.5.sp)
                                                    }

                                                    if (!branch.isMainBranch) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { branchToDelete = branch },
                                                            modifier = Modifier.size(30.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(16.dp))
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

                    1 -> {
                        // TAB 1: INTER-BRANCH STOCK TRANSFERS
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { isCreatingTransfer = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = branches.size >= 2 && products.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA) "Iherekanyamutungo Rishya" else "New Stock Transfer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }

                            if (branches.size < 2) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (language == AppLanguage.KINYARWANDA) "Ukeneye nibura amashami 2 kugira ngo wohereze ibicuruzwa hagati y'amashami." else "You need at least 2 branches to execute inter-branch stock transfers.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (stockTransfers.isEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (language == AppLanguage.KINYARWANDA) "Nta hererekanyamutungo rirakorwa" else "No Inter-Branch Transfers Yet",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (language == AppLanguage.KINYARWANDA) "Kanda kuri 'Iherekanyamutungo Rishya' hejuru wohereze ibicuruzwa hagati y'amashami." else "Move stock items between your retail branches with live inventory balance adjustments.",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(350.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(stockTransfers) { transfer ->
                                        val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(transfer.transferDate))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = OrangePrimary.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = transfer.transferNumber,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = OrangePrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = ProfitGreen.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = transfer.status,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ProfitGreen,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { transferToDelete = transfer },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "${transfer.quantity.toInt()} ${transfer.unit} • ${transfer.productName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(transfer.fromBranchName, fontSize = 11.sp, color = DarkNavy, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(transfer.toBranchName, fontSize = 11.sp, color = DarkNavy, fontWeight = FontWeight.SemiBold)
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("By: ${transfer.transferredBy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: MULTI-BRANCH ANALYTICS & COMPARISON
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Total Store Summary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.KINYARWANDA) "Igiteranyo cy'Amashami Yose" else "All Branches Combined Revenue",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${decimalFormat.format(totalRevenueAll)} $currencySymbol",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = ProfitGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Umusaruro w'Ishami ku Rindi" else "Branch Performance Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            branches.forEachIndexed { index, branch ->
                                val branchSales = sales.filter { it.branchId == branch.id || (branch.isMainBranch && (it.branchId.isBlank() || it.branchId == "main_branch" || it.branchId == "ALL")) }
                                val branchRevenue = branchSales.sumOf { it.totalAmount }
                                val percentage = if (totalRevenueAll > 0) (branchRevenue / totalRevenueAll).toFloat() else 0f
                                val momoSales = branchSales.filter { it.paymentMethod == "MOMO" || it.paymentMethod == "AIRTEL" }.sumOf { it.totalAmount }
                                val cashSales = branchSales.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (index == 0 && branchRevenue > 0) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Text(
                                                "${decimalFormat.format(branchRevenue)} $currencySymbol (${(percentage * 100).toInt()}%)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = ProfitGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { percentage },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = OrangePrimary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Orders: ${branchSales.size}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("MoMo: ${decimalFormat.format(momoSales)} • Cash: ${decimalFormat.format(cashSales)}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // Add / Edit Branch Form Dialog
    if (isAddingNewBranch || branchToEdit != null) {
        val branch = branchToEdit
        BranchFormDialog(
            initialBranch = branch,
            branchesCount = branches.size,
            allUsers = allUsers,
            language = language,
            onSave = { newOrUpdated ->
                onSaveBranch(newOrUpdated)
                isAddingNewBranch = false
                branchToEdit = null
            },
            onDismiss = {
                isAddingNewBranch = false
                branchToEdit = null
            }
        )
    }

    // Create Inter-Branch Stock Transfer Dialog
    if (isCreatingTransfer) {
        CreateStockTransferDialog(
            branches = branches,
            products = products,
            language = language,
            onTransfer = { prodId, fromId, fromName, toId, toName, qty, notes ->
                onCreateStockTransfer(prodId, fromId, fromName, toId, toName, qty, notes)
                isCreatingTransfer = false
            },
            onDismiss = { isCreatingTransfer = false }
        )
    }

    // Delete Branch Confirmation Dialog
    if (branchToDelete != null) {
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text(if (language == AppLanguage.KINYARWANDA) "Gusiba Ishami" else "Delete Branch Outlet", fontWeight = FontWeight.Bold) },
            text = { Text(if (language == AppLanguage.KINYARWANDA) "Wizera neza ko ushaka gusiba ishami '${branchToDelete!!.name}'?" else "Are you sure you want to remove '${branchToDelete!!.name}'? Its historic sales will remain archived.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBranch(branchToDelete!!.id)
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == AppLanguage.KINYARWANDA) "Siba" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { branchToDelete = null }) {
                    Text(if (language == AppLanguage.KINYARWANDA) "Kureka" else "Cancel")
                }
            }
        )
    }

    // Delete Transfer Confirmation Dialog
    if (transferToDelete != null) {
        AlertDialog(
            onDismissRequest = { transferToDelete = null },
            title = { Text(if (language == AppLanguage.KINYARWANDA) "Gusiba Transfer" else "Delete Transfer Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove transfer '${transferToDelete!!.transferNumber}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStockTransfer(transferToDelete!!.id)
                        transferToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { transferToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dedicated Modal to initiate Stock Transfers between branches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStockTransferDialog(
    branches: List<Branch>,
    products: List<Product>,
    language: AppLanguage,
    onTransfer: (productId: String, fromBranchId: String, fromBranchName: String, toBranchId: String, toBranchName: String, quantity: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var fromBranch by remember { mutableStateOf(branches.firstOrNull { it.isMainBranch } ?: branches.firstOrNull()) }
    var fromDropdownExpanded by remember { mutableStateOf(false) }

    var toBranch by remember { mutableStateOf(branches.firstOrNull { it != fromBranch } ?: branches.lastOrNull()) }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    var quantityInput by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    val currentStock = selectedProduct?.quantityInStock ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Kwohereza Ibicuruzwa" else "Transfer Stock",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = productDropdownExpanded,
                    onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.let { "${it.name} (Stock: ${it.quantityInStock.toInt()} ${it.unit})" } ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Ibicuruzwa Byoherezwa *" else "Select Product *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = productDropdownExpanded,
                        onDismissRequest = { productDropdownExpanded = false }
                    ) {
                        products.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.name} — ${p.quantityInStock.toInt()} ${p.unit} in stock") },
                                onClick = {
                                    selectedProduct = p
                                    productDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // From Branch
                ExposedDropdownMenuBox(
                    expanded = fromDropdownExpanded,
                    onExpandedChange = { fromDropdownExpanded = !fromDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = fromBranch?.name ?: "Source Branch",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Bivuye mu Ishami (From) *" else "From Branch *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = fromDropdownExpanded,
                        onDismissRequest = { fromDropdownExpanded = false }
                    ) {
                        branches.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = {
                                    fromBranch = b
                                    if (toBranch == b) {
                                        toBranch = branches.firstOrNull { it != b }
                                    }
                                    fromDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // To Branch
                ExposedDropdownMenuBox(
                    expanded = toDropdownExpanded,
                    onExpandedChange = { toDropdownExpanded = !toDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = toBranch?.name ?: "Destination Branch",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Bigeze mu Ishami (To) *" else "To Destination Branch *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = toDropdownExpanded,
                        onDismissRequest = { toDropdownExpanded = false }
                    ) {
                        branches.filter { it != fromBranch }.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = {
                                    toBranch = b
                                    toDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quantity Input
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Umubare w'ibicuruzwa (${selectedProduct?.unit ?: "pcs"}) *" else "Transfer Quantity (${selectedProduct?.unit ?: "pcs"}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Impamvu / Ibisobanuro" else "Notes / Driver / Vehicle") },
                    placeholder = { Text("e.g. Restock request, Van plate RAC 441Z") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                val parsedQty = quantityInput.toDoubleOrNull() ?: 0.0
                val isValid = selectedProduct != null && fromBranch != null && toBranch != null && fromBranch != toBranch && parsedQty > 0

                Button(
                    onClick = {
                        if (isValid) {
                            onTransfer(
                                selectedProduct!!.id,
                                fromBranch!!.id,
                                fromBranch!!.name,
                                toBranch!!.id,
                                toBranch!!.name,
                                parsedQty,
                                notes
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isValid
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == AppLanguage.KINYARWANDA) "Emeza Kwohereza Stock" else "Execute Stock Transfer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Branch Creation and Editing Form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchFormDialog(
    initialBranch: Branch?,
    branchesCount: Int,
    allUsers: List<User> = emptyList(),
    language: AppLanguage = AppLanguage.ENGLISH,
    onSave: (Branch) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialBranch?.name ?: "") }
    var code by remember { mutableStateOf(initialBranch?.code ?: "BR-0${branchesCount + 1}") }
    var address by remember { mutableStateOf(initialBranch?.address ?: "Kigali, Rwanda") }
    var phone by remember { mutableStateOf(initialBranch?.phone ?: "+250 ") }
    var managerUserId by remember { mutableStateOf(initialBranch?.managerUserId ?: "") }
    var managerName by remember { mutableStateOf(initialBranch?.managerName ?: "") }
    var isMain by remember { mutableStateOf(initialBranch?.isMainBranch ?: false) }
    var isActive by remember { mutableStateOf(initialBranch?.isActive ?: true) }
    var managerDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialBranch == null) {
                            if (language == AppLanguage.KINYARWANDA) "Ongeraho Ishami Rishya" else "Create New Shop Branch"
                        } else {
                            if (language == AppLanguage.KINYARWANDA) "Hindura Amakuru y'Ishami" else "Edit Branch Outlet"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Ishami *" else "Branch Outlet Name *") },
                    placeholder = { Text("e.g. Kimironko Market Sector 4") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Kode y'Ishami" else "Branch Code") },
                    placeholder = { Text("BR-01") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Telefoni y'Ishami" else "Branch Phone Number") },
                    placeholder = { Text("+250 788 123 456") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Aho Riherereye (Aderesi) *" else "Physical Address / Location *") },
                    placeholder = { Text("e.g. Nyarugenge Market Shop #42") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Manager Selector Dropdown
                if (allUsers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = managerDropdownExpanded,
                        onExpandedChange = { managerDropdownExpanded = !managerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = managerName.ifBlank { "Select Registered Manager" },
                            onValueChange = { managerName = it },
                            label = { Text(if (language == AppLanguage.KINYARWANDA) "Umuyobozi w'Ishami" else "Assigned Branch Manager") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = managerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = managerDropdownExpanded,
                            onDismissRequest = { managerDropdownExpanded = false }
                        ) {
                            allUsers.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.role.displayName})") },
                                    onClick = {
                                        managerUserId = u.id
                                        managerName = u.name
                                        managerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Umuyobozi w'Ishami" else "Branch Manager / Supervisor") },
                        placeholder = { Text("e.g. Aline Uwase") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Is Main HQ Branch switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ishami Rikuru ry'Ububiko (HQ)" else "Designate as Main Store HQ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Iri rizaba ishami ry'ibanze ry'ibaruramari ryose" else "Primary hub for central reporting and default inventory",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isMain,
                            onCheckedChange = { isMain = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (language == AppLanguage.KINYARWANDA) "Kureka" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val newOrUpdated = Branch(
                                    id = initialBranch?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    code = code.trim().ifBlank { "BR-01" },
                                    address = address.trim().ifBlank { "Kigali, Rwanda" },
                                    phone = phone.trim(),
                                    managerUserId = managerUserId,
                                    managerName = managerName.trim(),
                                    isMainBranch = isMain,
                                    isActive = isActive,
                                    createdAt = initialBranch?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(newOrUpdated)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (language == AppLanguage.KINYARWANDA) "Bika Ishami" else "Save Branch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
