package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GopherFontFamily
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Localization
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkNavy, Color(0xFF0F172A), Color(0xFF020617))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher Badge
                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = "Language", tint = Color.White, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "EN" else "RW",
                            fontFamily = GopherFontFamily,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Theme Switcher Button
                Surface(
                    onClick = onToggleTheme,
                    shape = CircleShape,
                    color = Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
                            tint = if (isDarkTheme) Color(0xFFFFD166) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Official Brand Logo & Shop Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFFB74D)))),
                    modifier = Modifier.size(72.dp),
                    shadowElevation = 10.dp
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = shopProfile.shopName.ifBlank { "BeBoss Store" },
                    fontFamily = GopherFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OrangePrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(ProfitGreen, CircleShape))
                        Text(
                            text = "SECURE POS TERMINAL",
                            fontFamily = GopherFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Quick Cashier / Operator Selection Row
            if (allUsers.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "HITAMO UMUKOZI WO KWINJIRA" else "SELECT CASHIER / OPERATOR",
                        fontFamily = GopherFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allUsers) { user ->
                            val isSelected = activeUser?.id == user.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) OrangePrimary.copy(alpha = 0.22f) else Color(0xFF1E293B),
                                border = if (isSelected) BorderStroke(1.8.dp, OrangePrimary) else BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
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
                                            .size(28.dp)
                                            .background(
                                                when (user.role) {
                                                    UserRole.OWNER -> OrangePrimary
                                                    UserRole.MANAGER -> Color(0xFF2563EB)
                                                    UserRole.CASHIER -> ProfitGreen
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            fontFamily = GopherFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = user.name.split(" ").firstOrNull() ?: user.name,
                                            fontFamily = GopherFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = user.role.displayName.split(" ").first(),
                                            fontFamily = GopherFontFamily,
                                            fontSize = 9.5.sp,
                                            color = if (isSelected) OrangePrimary else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive PIN / Password Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF0F172A),
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
                                    if (language == AppLanguage.KINYARWANDA) "PIN Y'Ibanga" else "Quick 4-Digit PIN",
                                    fontFamily = GopherFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; onClearError() },
                            text = {
                                Text(
                                    if (language == AppLanguage.KINYARWANDA) "Ijambobanga" else "Staff Password",
                                    fontFamily = GopherFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (authError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LossRed.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, LossRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError,
                                fontFamily = GopherFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (selectedTab == 0) {
                        // PIN Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            for (i in 0 until 4) {
                                val filled = i < pinInput.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            if (filled) OrangePrimary else Color(0xFF334155),
                                            CircleShape
                                        )
                                        .border(
                                            1.5.dp,
                                            if (filled) OrangePrimary else Color(0xFF475569),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Numeric Keypad with Haptics & Biometrics
                        PinKeypad(
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
                            onInstantUnlock = {
                                onBiometricUnlock()
                            }
                        )
                    } else {
                        // Password Form Mode
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(Localization.get("username_or_phone", language), fontFamily = GopherFontFamily) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
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
                                        contentDescription = "Toggle password",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
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
                                .height(48.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Register New Store / Business Setup Button
            OutlinedButton(
                onClick = onOpenRegister,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
 * Interactive, Step-Wizard Shop Registration with Gopher Font,
 * Category Presets, Currency Badges, and Instant PIN Generation
 */
@Composable
fun ShopRegistrationScreen(
    language: AppLanguage,
    isDarkTheme: Boolean,
    onRegister: (shopName: String, currencyCode: String, currencySymbol: String, address: String, phone: String, ownerName: String, username: String, pin: String, password: String) -> Unit,
    onCancel: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Retail & Grocery") }
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
        Pair("Retail & Grocery", Icons.Default.Storefront),
        Pair("Fashion & Boutique", Icons.Default.Checkroom),
        Pair("Electronics & Phones", Icons.Default.Smartphone),
        Pair("Pharmacy & Health", Icons.Default.LocalPharmacy),
        Pair("Hardware & Tools", Icons.Default.Build),
        Pair("Restaurant & Bar", Icons.Default.Restaurant),
        Pair("Wholesale Supply", Icons.Default.Inventory2),
        Pair("Salon & Spa", Icons.Default.ContentCut)
    )

    val currencies = listOf(
        Triple("FRw", "RWF", "Rwanda"),
        Triple("$", "USD", "US Dollar"),
        Triple("KSh", "KES", "Kenya"),
        Triple("USh", "UGX", "Uganda"),
        Triple("€", "EUR", "Euro")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkNavy, Color(0xFF0F172A), Color(0xFF020617))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = "Language", tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(if (language == AppLanguage.ENGLISH) "EN" else "RW", fontFamily = GopherFontFamily, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = onToggleTheme,
                    shape = CircleShape,
                    color = Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Official Brand Logo
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(OrangePrimary, Color(0xFFFFB74D)))),
                modifier = Modifier.size(68.dp),
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                    contentDescription = "BeBoss Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = Localization.get("create_store_account", language),
                fontFamily = GopherFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (language == AppLanguage.KINYARWANDA)
                    "Kora ububiko bwawe utangire gucuruza byihuse no kwakira raporo."
                    else "Register your shop profile & master owner credentials to launch.",
                fontFamily = GopherFontFamily,
                fontSize = 12.5.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LossRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        fontFamily = GopherFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: SHOP INFORMATION
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("1", fontFamily = GopherFontFamily, fontWeight = FontWeight.Bold, color = OrangePrimary, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "AMAKURU Y'UBUBIKO" else "SHOP INFORMATION",
                            fontFamily = GopherFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Ububiko / Isoko *" else "Shop / Business Name *", fontFamily = GopherFontFamily) },
                        placeholder = { Text("e.g. Kigali Smart Groceries") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Business Category Chips
                    Column {
                        Text(
                            text = "Business Type / Category:",
                            fontFamily = GopherFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { (catName, catIcon) ->
                                val selected = selectedCategory == catName
                                Surface(
                                    onClick = { selectedCategory = catName },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) OrangePrimary.copy(alpha = 0.25f) else Color(0xFF0F172A),
                                    border = if (selected) BorderStroke(1.5.dp, OrangePrimary) else BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = catName,
                                            tint = if (selected) OrangePrimary else Color(0xFF94A3B8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = catName,
                                            fontFamily = GopherFontFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Currency Selector Row
                    Column {
                        Text(
                            text = "Default Currency Denomination:",
                            fontFamily = GopherFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currencies.forEach { (sym, code, _) ->
                                val isSelected = currencyCode == code
                                Surface(
                                    onClick = {
                                        currencySymbol = sym
                                        currencyCode = code
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) OrangePrimary else Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, if (isSelected) OrangePrimary else Color(0xFF334155)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = sym,
                                            fontFamily = GopherFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else OrangePrimary
                                        )
                                        Text(
                                            text = code,
                                            fontFamily = GopherFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(Localization.get("phone_number", language), fontFamily = GopherFontFamily) },
                        placeholder = { Text("+250 788 123 456") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(Localization.get("address", language), fontFamily = GopherFontFamily) },
                        placeholder = { Text("e.g. Kigali, Nyarugenge Market #12") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = OrangePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // SECTION 2: MASTER OWNER CREDENTIALS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("2", fontFamily = GopherFontFamily, fontWeight = FontWeight.Bold, color = OrangePrimary, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "KONTI YA NYIR'UBUBIKO" else "MASTER OWNER ACCOUNT",
                            fontFamily = GopherFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Amazina ya Nyir'ububiko *" else "Owner Full Name *", fontFamily = GopherFontFamily) },
                        placeholder = { Text("Jean Paul Mugisha") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(Localization.get("username_or_phone", language) + " *", fontFamily = GopherFontFamily) },
                        placeholder = { Text("admin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // 4-Digit Fast PIN with Auto-Generator
                    Column {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text("4-Digit Fast PIN *", fontFamily = GopherFontFamily) },
                            placeholder = { Text("1234") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = OrangePrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                onClick = {
                                    pin = (1000 + Random.nextInt(9000)).toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = OrangePrimary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (language == AppLanguage.KINYARWANDA) "Kora PIN mu buryo bwikora" else "Auto-Generate PIN",
                                        fontFamily = GopherFontFamily,
                                        fontSize = 11.sp,
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(Localization.get("password", language) + " *", fontFamily = GopherFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (shopName.isBlank()) {
                                errorMessage = if (language == AppLanguage.KINYARWANDA) "Injiza izina ry'ububiko" else "Please enter your shop name."
                            } else if (ownerName.isBlank()) {
                                errorMessage = if (language == AppLanguage.KINYARWANDA) "Injiza amazina ya nyir'ububiko" else "Please enter owner full name."
                            } else if (username.isBlank()) {
                                errorMessage = if (language == AppLanguage.KINYARWANDA) "Injiza username" else "Please enter a username."
                            } else if (pin.length != 4) {
                                errorMessage = if (language == AppLanguage.KINYARWANDA) "PIN igomba kuba imibare 4" else "PIN must be exactly 4 digits."
                            } else if (password.length < 4) {
                                errorMessage = if (language == AppLanguage.KINYARWANDA) "Ijambobanga rigomba kuba nibura inyuguti 4" else "Password must be at least 4 characters."
                            } else {
                                errorMessage = null
                                onRegister(
                                    shopName.trim(),
                                    currencyCode.trim(),
                                    currencySymbol.trim(),
                                    address.trim(),
                                    phone.trim(),
                                    ownerName.trim(),
                                    username.trim().lowercase(),
                                    pin.trim(),
                                    password.trim()
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Kora Ububiko & Tangira" else "Create Shop & Launch",
                            fontFamily = GopherFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(Localization.get("cancel", language), fontFamily = GopherFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onInstantUnlock: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
                                color = Color(0xFF334155),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBackspace()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        "BIO" -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.22f),
                                border = BorderStroke(1.5.dp, ProfitGreen),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onInstantUnlock()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Quick Unlock",
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.2.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .size(56.dp)
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
                                        color = Color.White
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
