package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AppScreen
import com.example.util.AppLanguage
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeBossTopBar(
    currentScreen: AppScreen,
    shopProfile: ShopProfile,
    currentUser: User? = null,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    language: AppLanguage,
    isDarkTheme: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSyncClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLockClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = Localization.get(currentScreen.stringKey, language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shopProfile.shopName.ifBlank { "BeBoss Store" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentUser != null) {
                            Text(
                                text = " • ${currentUser.name.split(" ").first()}",
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
            // Language Switcher Toggle in Header
            Surface(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(12.dp),
                color = OrangePrimary.copy(alpha = 0.12f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(language.flag, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "EN" else "RW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
            }

            // Dark/White Theme Switcher Toggle in Header
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = if (isDarkTheme) Color(0xFFFFD166) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Quick Lock Button
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = Localization.get("lock_terminal", language),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Sales History Shortcut
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = Localization.get("sales_history", language),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
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
    language: AppLanguage,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onNavigate(AppScreen.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text(Localization.get("dashboard", language), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = OrangePrimary.copy(alpha = 0.15f)
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
            label = { Text(Localization.get("sales_pos", language), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = OrangePrimary.copy(alpha = 0.15f)
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
            label = { Text(Localization.get("inventory", language), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = OrangePrimary.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.ANALYTICS,
            onClick = { onNavigate(AppScreen.ANALYTICS) },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
            label = { Text(Localization.get("analytics", language), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = OrangePrimary.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.CUSTOMERS,
            onClick = { onNavigate(AppScreen.CUSTOMERS) },
            icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
            label = { Text(Localization.get("customers", language), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = OrangePrimary.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.SETTINGS,
            onClick = { onNavigate(AppScreen.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text(Localization.get("settings", language), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor = OrangePrimary.copy(alpha = 0.15f)
            )
        )
    }
}
