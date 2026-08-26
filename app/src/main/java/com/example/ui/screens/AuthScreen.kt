package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Localization

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

    val activeUser = currentUser ?: allUsers.firstOrNull()

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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Top Controls (Language & Theme toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher Badge Button
                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(language.flag, fontSize = 16.sp)
                        Text(
                            text = if (language == AppLanguage.ENGLISH) "EN" else "RW",
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
                    color = Color(0x33FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
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

            // Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = OrangePrimary,
                    modifier = Modifier.size(56.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = shopProfile.shopName.ifBlank { "BeBoss Store" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = Localization.get("security_warning", language).uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Operator Selection Bar
            if (allUsers.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "HITAMO UMUCURUZI" else "SELECT OPERATOR / CASHIER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        items(allUsers) { user ->
                            val isSelected = activeUser?.id == user.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OrangePrimary.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary) else null,
                                modifier = Modifier.clickable {
                                    onSelectUser(user)
                                    pinInput = ""
                                    onClearError()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
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
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = user.name.split(" ").firstOrNull() ?: user.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = user.role.displayName.split(" ").first(),
                                            fontSize = 9.sp,
                                            color = if (isSelected) OrangePrimary else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PIN / Credentials Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF0F172A),
                        contentColor = OrangePrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; onClearError() },
                            text = { 
                                Text(
                                    if (language == AppLanguage.KINYARWANDA) "PIN Y'Ibanga" else "Security PIN", 
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
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold
                                ) 
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (authError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33DC2626),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authError,
                                fontSize = 12.sp,
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedTab == 0) {
                        // PIN Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            for (i in 0 until 4) {
                                val filled = i < pinInput.length
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
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

                        Spacer(modifier = Modifier.height(12.dp))

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
                        // Password Form
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(Localization.get("username_or_phone", language)) },
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

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(Localization.get("password", language)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary) },
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                    onLoginWithCredentials(usernameInput.trim(), passwordInput.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.get("unlock", language), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Register New Account / Setup button
            OutlinedButton(
                onClick = onOpenRegister,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.get("create_store_account", language),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
    var businessCategory by remember { mutableStateOf("Retail & Grocery") }
    var currencySymbol by remember { mutableStateOf("FRw") }
    var currencyCode by remember { mutableStateOf("RWF") }
    var phone by remember { mutableStateOf("+250 ") }
    var address by remember { mutableStateOf("Kigali, Rwanda") }
    var ownerName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Retail & Grocery",
        "Fashion & Boutique",
        "Electronics & Phones",
        "Pharmacy & Cosmetics",
        "Hardware / Quincaillerie",
        "Restaurant & Bar",
        "Wholesale / En Gros",
        "General Merchandise"
    )

    val currencies = listOf(
        Pair("FRw", "RWF"),
        Pair("USD ($)", "USD"),
        Pair("KES (KSh)", "KES"),
        Pair("UGX (USh)", "UGX"),
        Pair("EUR (€)", "EUR")
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
                    color = Color(0x33FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(language.flag, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == AppLanguage.ENGLISH) "EN" else "RW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = onToggleTheme,
                    shape = CircleShape,
                    color = Color(0x33FFFFFF)
                ) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Surface(
                shape = CircleShape,
                color = OrangePrimary,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Localization.get("create_store_account", language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (language == AppLanguage.KINYARWANDA) 
                    "Kora konti yawe n'ububiko bwawe utangire gucuruza mu mutekano." 
                    else "Set up your store details & master credentials to get started.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x33DC2626),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "1. AMAKURU Y'UBUBIKO" else "1. STORE INFORMATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Ububiko / Isoko" else "Shop / Business Name *") },
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(Localization.get("phone_number", language)) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text(Localization.get("currency", language)) },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(Localization.get("address", language)) },
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "2. KONTI YA NYIR'UBUBIKO" else "2. MASTER OWNER ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Amazina ya Nyir'ububiko *" else "Owner Full Name *") },
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
                        label = { Text(Localization.get("username_or_phone", language) + " *") },
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text("4-Digit PIN *") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = OrangePrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(Localization.get("password", language) + " *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

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
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.KINYARWANDA) "Kora Ububiko & Tangira" else "Create Shop & Launch",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                    ) {
                        Text(Localization.get("cancel", language))
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
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIO", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    when (key) {
                        "DEL" -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF334155),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable { onBackspace() }
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
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ProfitGreen),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable { onInstantUnlock() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Quick Unlock",
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable { onDigit(key) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        fontSize = 20.sp,
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
