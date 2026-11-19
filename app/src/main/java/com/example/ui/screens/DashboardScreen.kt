package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Branch
import com.example.data.model.InventoryValuation
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.ui.viewmodel.AppScreen
import com.example.util.AppLanguage
import com.example.util.CloudSyncReport
import com.example.util.CloudSyncState
import com.example.util.ConnectivityStatus
import com.example.util.Localization
import com.example.util.ReceiptGenerator

@Composable
fun DashboardScreen(
    shopProfile: ShopProfile,
    todaySummary: ProfitLossSummary,
    inventoryValuation: InventoryValuation,
    lowStockProducts: List<Product>,
    recentSales: List<Sale>,
    currentUser: User?,
    language: AppLanguage,
    branches: List<Branch> = emptyList(),
    selectedBranchId: String = "ALL",
    connectivityStatus: ConnectivityStatus = ConnectivityStatus.AVAILABLE,
    isSyncing: Boolean = false,
    cloudSyncReport: CloudSyncReport? = null,
    onSelectBranch: (String) -> Unit = {},
    onNavigate: (AppScreen) -> Unit,
    onOpenReceipt: (String) -> Unit,
    onRestockClick: (Product) -> Unit,
    onAddProductClick: () -> Unit
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }
    val activeBranchLabel = remember(branches, selectedBranchId) {
        if (selectedBranchId == "ALL") {
            "All Branches (${branches.size.coerceAtLeast(1)})"
        } else {
            branches.firstOrNull { it.id == selectedBranchId }?.name ?: "HQ Branch"
        }
    }
    val daysRemaining = shopProfile.daysRemaining

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Operational Branch, Auto-Sync & Monthly License Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { if (branches.isNotEmpty()) branchMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("dashboard_branch_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeBranchLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (branches.isNotEmpty()) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch branch", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = branchMenuExpanded,
                        onDismissRequest = { branchMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Branches (${branches.size})") },
                            onClick = {
                                onSelectBranch("ALL")
                                branchMenuExpanded = false
                            }
                        )
                        branches.forEach { branch ->
                            DropdownMenuItem(
                                text = { Text("${branch.name} (${branch.code})") },
                                onClick = {
                                    onSelectBranch(branch.id)
                                    branchMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Auto Branch Sync Status Pill
                val syncIcon = when {
                    isSyncing -> Icons.Default.CloudSync
                    connectivityStatus == ConnectivityStatus.AVAILABLE -> Icons.Default.CloudDone
                    else -> Icons.Default.CloudOff
                }
                val syncColor = when {
                    isSyncing -> OrangePrimary
                    connectivityStatus == ConnectivityStatus.AVAILABLE -> ProfitGreen
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val syncLabel = when {
                    isSyncing -> "Syncing..."
                    connectivityStatus == ConnectivityStatus.AVAILABLE && cloudSyncReport?.state == CloudSyncState.SYNCED -> "Synced"
                    connectivityStatus == ConnectivityStatus.AVAILABLE -> "Auto-Sync"
                    else -> "Offline"
                }

                Surface(
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    shape = RoundedCornerShape(12.dp),
                    color = syncColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, syncColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("dashboard_sync_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(syncIcon, contentDescription = "Sync status", tint = syncColor, modifier = Modifier.size(15.dp))
                        Text(syncLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = syncColor)
                    }
                }

                // Monthly Subscription Countdown Pill
                val subColor = when {
                    daysRemaining <= 0 -> LossRed
                    daysRemaining <= 5 -> WarningAmber
                    else -> ProfitGreen
                }
                Surface(
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    shape = RoundedCornerShape(12.dp),
                    color = subColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, subColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("dashboard_license_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "License status", tint = subColor, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (daysRemaining > 0) "${daysRemaining}d" else "Renew",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subColor
                        )
                    }
                }
            }
        }

        // Hero Today Sales Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E2022), Color(0xFF2D3142))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Localization.get("today_overview", language),
                                color = Color(0xFFFFD8BF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Text(
                                    text = "${todaySummary.totalSalesCount} ${Localization.get("today_orders", language)}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = ReceiptGenerator.formatMoney(todaySummary.totalRevenue, shopProfile),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = Localization.get("today_revenue", language),
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Strip (Profit & Margin)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Net Profit
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x2216A34A)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            Localization.get("today_profit", language), 
                                            fontSize = 11.sp, 
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ReceiptGenerator.formatMoney(todaySummary.netProfit, shopProfile),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }

                            // Current Stock Value
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x22FF6B1A)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9E66),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            Localization.get("total_stock_value", language), 
                                            fontSize = 11.sp, 
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ReceiptGenerator.formatMoney(inventoryValuation.totalCostValue, shopProfile),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9E66)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Primary Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onNavigate(AppScreen.SALES_POS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Localization.get("new_sale_action", language).uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = Localization.get("add_product_action", language),
                        icon = Icons.Default.Add,
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        iconColor = OrangePrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onAddProductClick
                    )

                    QuickActionButton(
                        title = Localization.get("record_payment_action", language),
                        icon = Icons.Default.AttachMoney,
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        iconColor = ProfitGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.CUSTOMERS) }
                    )

                    QuickActionButton(
                        title = Localization.get("financial_reports_action", language),
                        icon = Icons.Default.Assessment,
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        iconColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.ANALYTICS) }
                    )
                }
            }
        }

        // Low Stock Alert Banner
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmberLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${Localization.get("low_stock_warning", language)} (${lowStockProducts.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = Localization.get("view_all", language),
                                color = WarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigate(AppScreen.INVENTORY) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lowStockProducts.take(3).forEach { product ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${Localization.get("quantity_in_stock", language)}: ${product.quantityInStock.toInt()} ${product.unit} (Min: ${product.lowStockThreshold.toInt()})",
                                                fontSize = 11.sp,
                                                color = Color(0xFFDC2626),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onRestockClick(product) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(Localization.get("restock", language), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.get("recent_sales_title", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = Localization.get("view_all", language),
                    color = OrangePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(AppScreen.SALES_HISTORY) }
                )
            }
        }

        if (recentSales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.get("no_sales_yet", language),
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(recentSales.take(5), key = { it.id }) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReceipt(sale.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (sale.paymentMethod == "CREDIT_DEBT") Color(0xFFFEE2E2) else ProfitGreenLight,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (sale.paymentMethod == "CREDIT_DEBT") Icons.Default.Warning else Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = if (sale.paymentMethod == "CREDIT_DEBT") Color(0xFFDC2626) else ProfitGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = sale.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${sale.receiptNumber} • ${ReceiptGenerator.formatDate(sale.saleDate)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = ReceiptGenerator.formatMoney(sale.totalAmount, shopProfile),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (sale.paymentMethod == "CREDIT_DEBT") Color(0xFFFEE2E2) else ProfitGreenLight
                            ) {
                                Text(
                                    text = if (sale.paymentMethod == "CREDIT_DEBT") Localization.get("credit_debt", language) else sale.paymentMethod,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sale.paymentMethod == "CREDIT_DEBT") Color(0xFFDC2626) else ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
