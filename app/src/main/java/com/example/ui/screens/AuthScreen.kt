package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GopherFontFamily
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.SecurityUtils
import kotlin.random.Random

@Composable
fun AuthLockScreen(
    currentUser: User?,
    allUsers: List<User>,
    shopProfile: ShopProfile,
    authError: String?,
    language: AppLanguage,
    isDarkTheme: Boolean,
    onUnlockWithPin: (String) -> Unit,
    onLoginWithCredentials: (String, String) -> Unit,
    onBiometricUnlock: () -> Unit = {},
    onSelectUser: (User) -> Unit,
    onClearError: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectLanguage: ((AppLanguage) -> Unit)? = null,
    onToggleTheme: () -> Unit,
    onOpenRegister: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: PIN, 1: Password
    var pinInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf(currentUser?.username ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val activeUser = currentUser ?: allUsers.firstOrNull()
    val haptic = LocalHapticFeedback.current

    val bgColors = if (isDarkTheme) {
        listOf(Color(0xFF0A1020), DarkNavy, Color(0xFF060B16))
    } else {
        listOf(Color(0xFFFFFBF7), Color(0xFFF4F6FA), Color(0xFFE8EEF7))
    }
    val cardBg = if (isDarkTheme) Color(0xFF172033) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF2B3A55) else Color(0xFFE2E8F0)
    val fieldBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val topPillBg = if (isDarkTheme) Color(0x26FFFFFF) else Color.White
    val topPillBorder = if (isDarkTheme) Color(0x38FFFFFF) else Color(0xFFCBD5E1)
    val topPillText = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val tabContainerBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgColors))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar Controls: 1-Tap 4-Language Selector + Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLanguage.entries.forEach { langOption ->
                        val isSelected = langOption == language
                        Surface(
                            onClick = {
                                if (onSelectLanguage != null) onSelectLanguage(langOption) else onToggleLanguage()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) OrangePrimary else topPillBg,
                            border = BorderStroke(1.dp, if (isSelected) OrangePrimary else topPillBorder),
                            shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("auth_lang_${langOption.code}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = langOption.flag,
                                    fontFamily = GopherFontFamily,
                                    color = if (isSelected) Color.White else topPillText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = onToggleTheme,
                    shape = CircleShape,
                    color = topPillBg,
                    border = BorderStroke(1.dp, topPillBorder),
                    shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = Localization.get("theme", language),
                            tint = if (isDarkTheme) Color(0xFFFFD166) else OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Store Identity & Live Branch / Offline Security Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFFB74D)))),
                    modifier = Modifier.size(72.dp),
                    shadowElevation = if (isDarkTheme) 10.dp else 6.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                        contentDescription = "BeBoss Store Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = shopProfile.shopName.ifBlank { "BeBoss Terminal" },
                    fontFamily = GopherFontFamily,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OrangePrimary.copy(alpha = if (isDarkTheme) 0.16f else 0.12f),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(ProfitGreen, CircleShape))
                        Text(
                            text = if (activeUser != null) {
                                "${activeUser.assignedBranchName.ifBlank { "HQ" }.uppercase()} • @${activeUser.username}"
                            } else {
                                Localization.get("offline_branch_pos_badge", language)
                            },
                            fontFamily = GopherFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary,
                            letterSpacing = 0.7.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Branch-Aware Operator Carousel
            if (allUsers.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.get("operator_terminal_header", language),
                        fontFamily = GopherFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allUsers) { user ->
                            val isSelected = activeUser?.id == user.id
                            val badgeColor = when (user.role) {
                                UserRole.OWNER -> OrangePrimary
                                UserRole.MANAGER -> Color(0xFF2563EB)
                                UserRole.CASHIER -> ProfitGreen
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) OrangePrimary.copy(alpha = 0.18f) else cardBg,
                                border = if (isSelected) BorderStroke(1.8.dp, OrangePrimary) else BorderStroke(1.dp, cardBorder),
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectUser(user)
                                        pinInput = ""
                                        usernameInput = user.username
                                        onClearError()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(badgeColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            fontFamily = GopherFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = user.name.split(" ").firstOrNull() ?: user.name,
                                            fontFamily = GopherFontFamily,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "${user.role.name} • ${user.assignedBranchName.ifBlank { "HQ" }}",
                                            fontFamily = GopherFontFamily,
                                            fontSize = 9.5.sp,
                                            color = if (isSelected) OrangePrimary else textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Interactive Unlock Card (PIN Pad + Biometric / Password Auth)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(if (isDarkTheme) 8.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = tabContainerBg,
                        contentColor = OrangePrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; onClearError() },
                            text = {
                                Text(
                                    text = Localization.get("fast_pin_tab", language),
                                    fontFamily = GopherFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) OrangePrimary else textSecondary
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; onClearError() },
                            text = {
                                Text(
                                    text = Localization.get("credentials_tab", language),
                                    fontFamily = GopherFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) OrangePrimary else textSecondary
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedVisibility(visible = authError != null) {
                        if (authError != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LossRed.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, LossRed.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Text(
                                    text = authError,
                                    fontFamily = GopherFontFamily,
                                    fontSize = 12.sp,
                                    color = LossRed,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            for (i in 0 until 4) {
                                val filled = i < pinInput.length
                                val dotSize by animateDpAsState(
                                    targetValue = if (filled) 18.dp else 14.dp,
                                    animationSpec = spring(),
                                    label = "pinDot"
                                )
                                val dotColor by animateColorAsState(
                                    targetValue = if (filled) OrangePrimary else if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    label = "pinColor"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .background(dotColor, CircleShape)
                                        .border(
                                            1.5.dp,
                                            if (filled) OrangePrimary else if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        PinKeypad(
                            isDarkTheme = isDarkTheme,
                            onDigit = { digit ->
                                if (pinInput.length < 4) {
                                    val next = pinInput + digit
                                    pinInput = next
                                    onClearError()
                                    if (next.length == 4) {
                                        onUnlockWithPin(next)
                                    }
                                }
                            },
                            onBackspace = {
                                if (pinInput.isNotEmpty()) {
                                    pinInput = pinInput.dropLast(1)
                                    onClearError()
                                }
                            },
                            onInstantUnlock = onBiometricUnlock
                        )
                    } else {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(Localization.get("username_or_phone", language), fontFamily = GopherFontFamily) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = cardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(Localization.get("password", language), fontFamily = GopherFontFamily) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = Localization.get("toggle_password", language),
                                        tint = textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = cardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                    onLoginWithCredentials(usernameInput.trim(), passwordInput.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("unlock", language), fontFamily = GopherFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Register New Store CTA
            OutlinedButton(
                onClick = onOpenRegister,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("open_register_button")
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.get("create_store_account", language),
                    fontFamily = GopherFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 3-Step Interactive Shop Registration Wizard:
 * Step 1: Business Name, Category & Currency
 * Step 2: Owner Registered Phone (Bound to USSD/WhatsApp Unlock Codes) & HQ Address
 * Step 3: Owner @username, 4-Digit Fast PIN, Password Strength Meter & Launch
 */
@Composable
fun ShopRegistrationScreen(
    language: AppLanguage,
    isDarkTheme: Boolean,
    onRegister: (shopName: String, currencyCode: String, currencySymbol: String, address: String, phone: String, ownerName: String, username: String, pin: String, password: String) -> Unit,
    onCancel: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectLanguage: ((AppLanguage) -> Unit)? = null,
    onToggleTheme: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0, 1, 2
    var shopName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("sector_retail") }
    var currencySymbol by remember { mutableStateOf("FRw") }
    var currencyCode by remember { mutableStateOf("RWF") }
    var phone by remember { mutableStateOf("+250 ") }
    var address by remember { mutableStateOf("Kigali, Rwanda") }
    var ownerName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        Pair("sector_retail", Icons.Default.Storefront),
        Pair("sector_fashion", Icons.Default.Checkroom),
        Pair("sector_electronics", Icons.Default.Smartphone),
        Pair("sector_pharmacy", Icons.Default.LocalPharmacy),
        Pair("sector_hardware", Icons.Default.Build),
        Pair("sector_restaurant", Icons.Default.Restaurant),
        Pair("sector_wholesale", Icons.Default.Inventory2),
        Pair("sector_salon", Icons.Default.ContentCut)
    )

    val currencies = listOf(
        Triple("FRw", "RWF", "Rwanda"),
        Triple("$", "USD", "USD"),
        Triple("KSh", "KES", "Kenya"),
        Triple("USh", "UGX", "Uganda"),
        Triple("€", "EUR", "Euro")
    )

    val bgColors = if (isDarkTheme) {
        listOf(Color(0xFF0A1020), DarkNavy, Color(0xFF060B16))
    } else {
        listOf(Color(0xFFFFFBF7), Color(0xFFF4F6FA), Color(0xFFE8EEF7))
    }
    val cardBg = if (isDarkTheme) Color(0xFF172033) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF2B3A55) else Color(0xFFE2E8F0)
    val fieldBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val topPillBg = if (isDarkTheme) Color(0x26FFFFFF) else Color.White
    val topPillBorder = if (isDarkTheme) Color(0x38FFFFFF) else Color(0xFFCBD5E1)
    val topPillText = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val chipBg = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val chipBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    val stepTitles = listOf(
        Localization.get("reg_step_1", language),
        Localization.get("reg_step_2", language),
        Localization.get("reg_step_3", language)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgColors))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Controls: 1-Tap 4-Language Selector + Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLanguage.entries.forEach { langOption ->
                        val isSelected = langOption == language
                        Surface(
                            onClick = {
                                if (onSelectLanguage != null) onSelectLanguage(langOption) else onToggleLanguage()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) OrangePrimary else topPillBg,
                            border = BorderStroke(1.dp, if (isSelected) OrangePrimary else topPillBorder),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("reg_lang_${langOption.code}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = langOption.flag,
                                    fontFamily = GopherFontFamily,
                                    color = if (isSelected) Color.White else topPillText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = onToggleTheme,
                    shape = CircleShape,
                    color = topPillBg,
                    border = BorderStroke(1.dp, topPillBorder),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = Localization.get("theme", language),
                            tint = if (isDarkTheme) Color(0xFFFFD166) else OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFFB74D)))),
                modifier = Modifier.size(64.dp),
                shadowElevation = if (isDarkTheme) 8.dp else 4.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                    contentDescription = "BeBoss Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Localization.get("create_store_account", language),
                fontFamily = GopherFontFamily,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = Localization.get("reg_subtitle", language),
                fontFamily = GopherFontFamily,
                fontSize = 11.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Step Interactive Stepper Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stepTitles.forEachIndexed { idx, label ->
                    val isCurrent = step == idx
                    val isCompleted = idx < step
                    Surface(
                        onClick = {
                            if (idx <= step) {
                                errorMessage = null
                                step = idx
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isCurrent -> OrangePrimary
                            isCompleted -> ProfitGreen.copy(alpha = 0.16f)
                            else -> cardBg
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrent -> OrangePrimary
                                isCompleted -> ProfitGreen
                                else -> cardBorder
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = label,
                                fontFamily = GopherFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCurrent -> Color.White
                                    isCompleted -> ProfitGreen
                                    else -> textSecondary
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = errorMessage != null) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LossRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            fontFamily = GopherFontFamily,
                            fontSize = 12.sp,
                            color = LossRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(if (isDarkTheme) 8.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "RegistrationStep"
                    ) { currentStep ->
                        when (currentStep) {
                            0 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = shopName,
                                        onValueChange = { shopName = it; errorMessage = null },
                                        label = { Text(Localization.get("reg_shop_name_label", language), fontFamily = GopherFontFamily) },
                                        placeholder = { Text(Localization.get("reg_shop_name_placeholder", language)) },
                                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_shop_name_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = cardBorder
                                        )
                                    )

                                    Column {
                                        Text(Localization.get("reg_sector_label", language), fontFamily = GopherFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(categories) { (catKey, catIcon) ->
                                                val selected = selectedCategory == catKey
                                                val catLabel = Localization.get(catKey, language)
                                                Surface(
                                                    onClick = { selectedCategory = catKey },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (selected) OrangePrimary.copy(alpha = 0.2f) else chipBg,
                                                    border = if (selected) BorderStroke(1.5.dp, OrangePrimary) else BorderStroke(1.dp, chipBorder),
                                                    modifier = Modifier.heightIn(min = 42.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(catIcon, contentDescription = catLabel, tint = if (selected) OrangePrimary else textSecondary, modifier = Modifier.size(16.dp))
                                                        Text(
                                                            text = catLabel,
                                                            fontFamily = GopherFontFamily,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (selected) textPrimary else textSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column {
                                        Text(Localization.get("reg_currency_label", language), fontFamily = GopherFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            currencies.forEach { (sym, code, _) ->
                                                val isSelected = currencyCode == code
                                                Surface(
                                                    onClick = {
                                                        currencySymbol = sym
                                                        currencyCode = code
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) OrangePrimary else chipBg,
                                                    border = BorderStroke(1.dp, if (isSelected) OrangePrimary else chipBorder),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(sym, fontFamily = GopherFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else OrangePrimary)
                                                        Text(code, fontFamily = GopherFontFamily, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White.copy(alpha = 0.9f) else textSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it; errorMessage = null },
                                        label = { Text(Localization.get("reg_phone_label", language), fontFamily = GopherFontFamily) },
                                        placeholder = { Text(Localization.get("reg_phone_placeholder", language)) },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_phone_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = cardBorder
                                        )
                                    )

                                    OutlinedTextField(
                                        value = address,
                                        onValueChange = { address = it },
                                        label = { Text(Localization.get("address", language), fontFamily = GopherFontFamily) },
                                        placeholder = { Text(Localization.get("reg_address_placeholder", language)) },
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = OrangePrimary) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = cardBorder
                                        )
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ProfitGreen.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = ProfitGreen)
                                            Column {
                                                Text(
                                                    text = Localization.get("reg_trial_card_title", language),
                                                    fontFamily = GopherFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ProfitGreen
                                                )
                                                Text(
                                                    text = Localization.get("reg_trial_card_desc", language),
                                                    fontFamily = GopherFontFamily,
                                                    fontSize = 10.5.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                val normalizedUsername = username.trim().lowercase().replace(" ", "")
                                val strengthScore = remember(password) {
                                    var s = 0
                                    if (password.length >= 6) s++
                                    if (password.any { it.isDigit() }) s++
                                    if (password.any { it.isUpperCase() } || password.any { !it.isLetterOrDigit() }) s++
                                    s
                                }
                                val strengthColor = when (strengthScore) {
                                    0, 1 -> LossRed
                                    2 -> WarningAmber
                                    else -> ProfitGreen
                                }
                                val strengthLabel = when (strengthScore) {
                                    0 -> Localization.get("strength_too_short", language)
                                    1 -> Localization.get("strength_basic", language)
                                    2 -> Localization.get("strength_good", language)
                                    else -> Localization.get("strength_strong", language)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = ownerName,
                                        onValueChange = { ownerName = it; errorMessage = null },
                                        label = { Text(Localization.get("reg_owner_name_label", language), fontFamily = GopherFontFamily) },
                                        placeholder = { Text(Localization.get("reg_owner_name_placeholder", language)) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_owner_name_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = cardBorder
                                        )
                                    )

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it; errorMessage = null },
                                        label = { Text(Localization.get("reg_username_label", language), fontFamily = GopherFontFamily) },
                                        placeholder = { Text(Localization.get("reg_username_placeholder", language)) },
                                        leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OrangePrimary) },
                                        supportingText = {
                                            if (normalizedUsername.isNotEmpty()) {
                                                Text("${Localization.get("reg_account_handle", language)}: @$normalizedUsername", fontSize = 10.5.sp, color = OrangePrimary)
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_username_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = fieldBg,
                                            unfocusedContainerColor = fieldBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = cardBorder
                                        )
                                    )

                                    Column {
                                        OutlinedTextField(
                                            value = pin,
                                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it; errorMessage = null },
                                            label = { Text(Localization.get("reg_pin_label", language), fontFamily = GopherFontFamily) },
                                            placeholder = { Text(Localization.get("reg_pin_placeholder", language)) },
                                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = OrangePrimary) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            visualTransformation = PasswordVisualTransformation(),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("register_pin_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = fieldBg,
                                                unfocusedContainerColor = fieldBg,
                                                focusedTextColor = textPrimary,
                                                unfocusedTextColor = textPrimary,
                                                focusedBorderColor = OrangePrimary,
                                                unfocusedBorderColor = cardBorder
                                            )
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Surface(
                                                onClick = { pin = SecurityUtils.generateStrongRandomPin() },
                                                shape = RoundedCornerShape(8.dp),
                                                color = OrangePrimary.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
                                                modifier = Modifier.heightIn(min = 36.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                                    Text(
                                                        text = Localization.get("reg_gen_pin", language),
                                                        fontFamily = GopherFontFamily,
                                                        fontSize = 11.sp,
                                                        color = OrangePrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column {
                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = { password = it; errorMessage = null },
                                            label = { Text(Localization.get("password", language) + " *", fontFamily = GopherFontFamily) },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary) },
                                            trailingIcon = {
                                                IconButton(onClick = { showPassword = !showPassword }) {
                                                    Icon(
                                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = Localization.get("toggle_password", language),
                                                        tint = textSecondary
                                                    )
                                                }
                                            },
                                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("register_password_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = fieldBg,
                                                unfocusedContainerColor = fieldBg,
                                                focusedTextColor = textPrimary,
                                                unfocusedTextColor = textPrimary,
                                                focusedBorderColor = OrangePrimary,
                                                unfocusedBorderColor = cardBorder
                                            )
                                        )
                                        if (password.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = { ((strengthScore + 0.5f) / 3.5f).coerceIn(0.15f, 1f) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = strengthColor,
                                                    trackColor = cardBorder
                                                )
                                                Text(strengthLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = strengthColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Wizard Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    step--
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.get("back", language), fontFamily = GopherFontFamily, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Text(Localization.get("cancel", language), fontFamily = GopherFontFamily, color = textSecondary)
                            }
                        }

                        if (step < 2) {
                            Button(
                                onClick = {
                                    if (step == 0 && shopName.isBlank()) {
                                        errorMessage = Localization.get("reg_err_shop_name", language)
                                    } else if (step == 1 && phone.filter { it.isDigit() }.length < 9) {
                                        errorMessage = Localization.get("reg_err_phone", language)
                                    } else {
                                        errorMessage = null
                                        step++
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .heightIn(min = 50.dp)
                                    .testTag("register_next_step_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                Text(Localization.get("continue_btn", language), fontFamily = GopherFontFamily, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (ownerName.isBlank()) {
                                        errorMessage = Localization.get("reg_err_owner_name", language)
                                    } else if (username.isBlank()) {
                                        errorMessage = Localization.get("reg_err_username", language)
                                    } else if (pin.length != 4) {
                                        errorMessage = Localization.get("reg_err_pin", language)
                                    } else if (password.length < 6) {
                                        errorMessage = Localization.get("reg_err_password", language)
                                    } else {
                                        errorMessage = null
                                        onRegister(
                                            shopName.trim(),
                                            currencyCode.trim(),
                                            currencySymbol.trim(),
                                            address.trim(),
                                            phone.trim(),
                                            ownerName.trim(),
                                            username.trim().lowercase().replace(" ", ""),
                                            pin.trim(),
                                            password.trim()
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.6f)
                                    .heightIn(min = 50.dp)
                                    .testTag("register_launch_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.get("reg_create_launch", language),
                                    fontFamily = GopherFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
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
private fun PinKeypad(
    isDarkTheme: Boolean = true,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onInstantUnlock: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val keyBg = if (isDarkTheme) Color(0xFF0F172A) else Color.White
    val keyBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val keyTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val delBg = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
    val delIconColor = if (isDarkTheme) Color.White else Color(0xFF334155)

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIO", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    when (key) {
                        "DEL" -> {
                            Surface(
                                shape = CircleShape,
                                color = delBg,
                                border = BorderStroke(1.dp, keyBorder),
                                modifier = Modifier
                                    .size(58.dp)
                                    .testTag("pin_backspace_button")
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBackspace()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = delIconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        "BIO" -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = if (isDarkTheme) 0.22f else 0.15f),
                                border = BorderStroke(1.5.dp, ProfitGreen),
                                modifier = Modifier
                                    .size(58.dp)
                                    .testTag("biometric_unlock_button")
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onInstantUnlock()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = CircleShape,
                                color = keyBg,
                                border = BorderStroke(1.2.dp, keyBorder),
                                shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
                                modifier = Modifier
                                    .size(58.dp)
                                    .testTag("pin_keypad_$key")
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDigit(key)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        fontFamily = GopherFontFamily,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = keyTextColor
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
