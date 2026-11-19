package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GopherFontFamily
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
    onSettingsClick: () -> Unit = {},
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Official BeBoss Logo with Golden Glow Ring
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFFB74D)))),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                        contentDescription = "BeBoss Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = Localization.get(currentScreen.stringKey, language),
                        fontFamily = GopherFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shopProfile.shopName.ifBlank { "BeBoss Store" },
                            fontFamily = GopherFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentUser != null) {
                            Text(
                                text = " • ${currentUser.name.split(" ").first()}",
                                fontFamily = GopherFontFamily,
                                fontSize = 10.5.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        actions = {
            // Quick Actions Shortcut
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQuickActionsClick()
                },
                shape = RoundedCornerShape(10.dp),
                color = OrangePrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.3f)),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Quick Actions", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "POS",
                        fontFamily = GopherFontFamily,
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = Localization.get("language", language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = language.flag,
                        fontFamily = GopherFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Settings Header Shortcut Icon
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSettingsClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = Localization.get("settings", language),
                    tint = if (currentScreen == AppScreen.SETTINGS) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
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

/**
 * Ultra-Modern Enterprise Floating Capsule Bottom Navigation Dock
 * Featuring smooth active pill indicators, glowing accents, tactile haptics, and live badges
 */
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

    val navItems = listOf(
        NavItem(
            screen = AppScreen.DASHBOARD,
            label = Localization.get("dashboard", language),
            icon = Icons.Default.Dashboard,
            badgeCount = 0
        ),
        NavItem(
            screen = AppScreen.SALES_POS,
            label = Localization.get("sales_pos", language),
            icon = Icons.Default.AddShoppingCart,
            badgeCount = cartItemCount,
            isBadgePrimary = true
        ),
        NavItem(
            screen = AppScreen.INVENTORY,
            label = Localization.get("inventory", language),
            icon = Icons.Default.Inventory2,
            badgeCount = lowStockCount,
            isBadgeError = true
        ),
        NavItem(
            screen = AppScreen.ANALYTICS,
            label = Localization.get("analytics", language),
            icon = Icons.Default.BarChart,
            badgeCount = 0
        ),
        NavItem(
            screen = AppScreen.CUSTOMERS,
            label = Localization.get("customers", language),
            icon = Icons.Default.People,
            badgeCount = debtorCount
        ),
        NavItem(
            screen = AppScreen.SETTINGS,
            label = Localization.get("settings", language),
            icon = Icons.Default.Settings,
            badgeCount = 0
        )
    )

    val selectedIndex = navItems.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    OrangePrimary.copy(alpha = 0.28f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    Color(0xFFFFB74D).copy(alpha = 0.24f)
                )
            )
        ),
        shadowElevation = 18.dp,
        tonalElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val slotWidth = maxWidth / navItems.size
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 360f
                ),
                label = "bottom_nav_sliding_pill"
            )

            // Smooth sliding gradient active pill underneath the tabs
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .height(54.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                OrangePrimary.copy(alpha = 0.22f),
                                Color(0xFFFFB74D).copy(alpha = 0.14f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = OrangePrimary.copy(alpha = 0.32f),
                        shape = RoundedCornerShape(18.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.90f
                            isSelected -> 1.06f
                            else -> 1.0f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "nav_scale"
                    )
                    val contentTint by animateColorAsState(
                        targetValue = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "nav_content_tint"
                    )

                    Box(
                        modifier = Modifier
                            .width(slotWidth)
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("bottom_nav_${item.screen.stringKey}")
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true, color = OrangePrimary)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate(item.screen)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount > 0) {
                                        Badge(
                                            containerColor = when {
                                                item.isBadgeError -> LossRed
                                                item.isBadgePrimary -> OrangePrimary
                                                else -> Color(0xFFD97706)
                                            },
                                            contentColor = Color.White,
                                            modifier = Modifier.size(if (item.badgeCount > 9) 18.dp else 16.dp)
                                        ) {
                                            Text(
                                                text = if (item.badgeCount > 99) "99+" else "${item.badgeCount}",
                                                fontFamily = GopherFontFamily,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = contentTint,
                                    modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = item.label,
                                fontFamily = GopherFontFamily,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = contentTint,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 16.dp else 0.dp)
                                    .height(2.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) OrangePrimary else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Navigation Rail for Medium/Expanded tablets and foldables.
 */
@Composable
fun BeBossNavigationRail(
    currentScreen: AppScreen,
    cartItemCount: Int,
    lowStockCount: Int,
    debtorCount: Int = 0,
    language: AppLanguage,
    onNavigate: (AppScreen) -> Unit,
    onOpenQuickActions: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val navItems = listOf(
        NavItem(
            screen = AppScreen.DASHBOARD,
            label = Localization.get("dashboard", language),
            icon = Icons.Default.Dashboard,
            badgeCount = 0
        ),
        NavItem(
            screen = AppScreen.SALES_POS,
            label = Localization.get("sales_pos", language),
            icon = Icons.Default.AddShoppingCart,
            badgeCount = cartItemCount,
            isBadgePrimary = true
        ),
        NavItem(
            screen = AppScreen.INVENTORY,
            label = Localization.get("inventory", language),
            icon = Icons.Default.Inventory2,
            badgeCount = lowStockCount,
            isBadgeError = true
        ),
        NavItem(
            screen = AppScreen.ANALYTICS,
            label = Localization.get("analytics", language),
            icon = Icons.Default.BarChart,
            badgeCount = 0
        ),
        NavItem(
            screen = AppScreen.CUSTOMERS,
            label = Localization.get("customers", language),
            icon = Icons.Default.People,
            badgeCount = debtorCount
        ),
        NavItem(
            screen = AppScreen.SETTINGS,
            label = Localization.get("settings", language),
            icon = Icons.Default.Settings,
            badgeCount = 0
        )
    )

    androidx.compose.material3.NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenQuickActions()
                },
                containerColor = OrangePrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 10.dp)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Quick POS Actions")
            }
        }
    ) {
        navItems.forEach { item ->
            val isSelected = currentScreen == item.screen
            androidx.compose.material3.NavigationRailItem(
                selected = isSelected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigate(item.screen)
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge(
                                    containerColor = when {
                                        item.isBadgeError -> LossRed
                                        item.isBadgePrimary -> OrangePrimary
                                        else -> Color(0xFFD97706)
                                    },
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (item.badgeCount > 99) "99+" else "${item.badgeCount}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontFamily = GopherFontFamily,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

private data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val isBadgePrimary: Boolean = false,
    val isBadgeError: Boolean = false
)
