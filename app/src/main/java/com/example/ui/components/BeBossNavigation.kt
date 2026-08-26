package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
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
    onSearchClick: () -> Unit = {},
    onQuickActionsClick: () -> Unit = {},
    onSyncClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLockClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo_1787747059788),
                        contentDescription = "BeBoss Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
            // Quick ⚡ Speed Actions Shortcut
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQuickActionsClick()
                },
                shape = RoundedCornerShape(10.dp),
                color = OrangePrimary.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Quick Actions", tint = OrangePrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "⚡",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary
                    )
                }
            }

            // Universal Search Shortcut
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSearchClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Language Switcher Toggle
            Surface(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(language.flag, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "EN" else "RW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Dark/White Theme Switcher Toggle
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

            // Quick Lock Terminal
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
        }
    )
}

@Composable
fun BeBossBottomNav(
    currentScreen: AppScreen,
    cartItemCount: Int,
    lowStockCount: Int,
    debtorCount: Int = 0,
    language: AppLanguage,
    onNavigate: (AppScreen) -> Unit,
    onOpenQuickActions: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Dashboard
            NavigationBarItem(
                selected = currentScreen == AppScreen.DASHBOARD,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(AppScreen.DASHBOARD)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Dashboard",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = Localization.get("dashboard", language),
                        fontSize = 10.sp,
                        fontWeight = if (currentScreen == AppScreen.DASHBOARD) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                )
            )

            // 2. POS Sale (Highlighted with Live Cart Badge)
            NavigationBarItem(
                selected = currentScreen == AppScreen.SALES_POS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(AppScreen.SALES_POS)
                },
                icon = {
                    if (cartItemCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = OrangePrimary,
                                    contentColor = Color.White
                                ) {
                                    Text("$cartItemCount", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "POS Sale",
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "POS Sale",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = Localization.get("sales_pos", language),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                )
            )

            // 3. Inventory (With Low Stock Alert Badge)
            NavigationBarItem(
                selected = currentScreen == AppScreen.INVENTORY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(AppScreen.INVENTORY)
                },
                icon = {
                    if (lowStockCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = LossRed,
                                    contentColor = Color.White
                                ) {
                                    Text("$lowStockCount", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Inventory",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Inventory",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = Localization.get("inventory", language),
                        fontSize = 10.sp,
                        fontWeight = if (currentScreen == AppScreen.INVENTORY) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                )
            )

            // 4. Analytics / Reports
            NavigationBarItem(
                selected = currentScreen == AppScreen.ANALYTICS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(AppScreen.ANALYTICS)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Analytics",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = Localization.get("analytics", language),
                        fontSize = 10.sp,
                        fontWeight = if (currentScreen == AppScreen.ANALYTICS) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                )
            )

            // 5. Customers & Debts (With Debtor Count Badge)
            NavigationBarItem(
                selected = currentScreen == AppScreen.CUSTOMERS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(AppScreen.CUSTOMERS)
                },
                icon = {
                    if (debtorCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ) {
                                    Text("$debtorCount", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Customers",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Customers",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = Localization.get("customers", language),
                        fontSize = 10.sp,
                        fontWeight = if (currentScreen == AppScreen.CUSTOMERS) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                )
            )

            // 6. Settings
            NavigationBarItem(
                selected = currentScreen == AppScreen.SETTINGS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(AppScreen.SETTINGS)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = Localization.get("settings", language),
                        fontSize = 10.sp,
                        fontWeight = if (currentScreen == AppScreen.SETTINGS) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}

