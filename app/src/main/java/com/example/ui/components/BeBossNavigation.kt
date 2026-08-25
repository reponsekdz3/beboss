package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopProfile
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeBossTopBar(
    currentScreen: AppScreen,
    shopProfile: ShopProfile,
    currentUser: com.example.data.model.User? = null,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLockClick: () -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = InkDark
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(OrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shopProfile.shopName,
                            fontSize = 11.sp,
                            color = InkMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentUser != null) {
                            Text(
                                text = " • ${currentUser.name} (${currentUser.role.displayName})",
                                fontSize = 10.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        actions = {
            // Offline sync indicator / button
            Surface(
                onClick = onSyncClick,
                shape = CircleShape,
                color = if (pendingSyncCount > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Default.Sync else if (pendingSyncCount > 0) Icons.Default.Sync else Icons.Default.CloudDone,
                        contentDescription = "Sync",
                        modifier = Modifier.size(15.dp),
                        tint = if (pendingSyncCount > 0) Color(0xFFB45309) else ProfitGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncing) "Syncing" else if (pendingSyncCount > 0) "$pendingSyncCount Offline" else "Online",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pendingSyncCount > 0) Color(0xFFB45309) else ProfitGreen
                    )
                }
            }

            // Lock Terminal Button
            IconButton(onClick = onLockClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = "Lock Terminal",
                    tint = InkDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Sales History",
                    tint = InkDark
                )
            }
        }
    )
}

@Composable
fun BeBossBottomNav(
    currentScreen: AppScreen,
    cartItemCount: Int,
    lowStockCount: Int,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onNavigate(AppScreen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = Color(0xFFFFF0E6)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.SALES_POS,
            onClick = { onNavigate(AppScreen.SALES_POS) },
            icon = {
                if (cartItemCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = OrangePrimary) {
                                Text("$cartItemCount")
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "POS Sale")
                    }
                } else {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "POS Sale")
                }
            },
            label = { Text("POS Sale", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = Color(0xFFFFF0E6)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.INVENTORY,
            onClick = { onNavigate(AppScreen.INVENTORY) },
            icon = {
                if (lowStockCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color(0xFFDC2626)) {
                                Text("$lowStockCount")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                    }
                } else {
                    Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                }
            },
            label = { Text("Stock", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = Color(0xFFFFF0E6)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.ANALYTICS,
            onClick = { onNavigate(AppScreen.ANALYTICS) },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
            label = { Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = Color(0xFFFFF0E6)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.CUSTOMERS,
            onClick = { onNavigate(AppScreen.CUSTOMERS) },
            icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
            label = { Text("Customers", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = Color(0xFFFFF0E6)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.SETTINGS,
            onClick = { onNavigate(AppScreen.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = Color(0xFFFFF0E6)
            )
        )
    }
}
