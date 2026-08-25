package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.ReceiptGenerator
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    shopProfile: ShopProfile,
    currentUser: User? = null,
    allUsers: List<User> = emptyList(),
    products: List<Product> = emptyList(),
    customers: List<Customer> = emptyList(),
    sales: List<Sale> = emptyList(),
    customerPayments: List<CustomerPayment> = emptyList(),
    pendingSyncCount: Int = 0,
    isSyncing: Boolean = false,
    language: AppLanguage,
    isDarkTheme: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSaveProfile: (ShopProfile) -> Unit,
    onSyncNow: () -> Unit = {},
    onSaveUser: (User) -> Unit = {},
    onDeleteUser: (String) -> Unit = {},
    onImportPackage: (ShopImportSummary) -> Unit = {},
    onLockApp: () -> Unit = {},
    onSwitchUser: (User) -> Unit = {}
) {
    val context = LocalContext.current
    var shopName by remember(shopProfile.shopName) { mutableStateOf(shopProfile.shopName) }
    var ownerName by remember(shopProfile.name) { mutableStateOf(shopProfile.name) }
    var phone by remember(shopProfile.phone) { mutableStateOf(shopProfile.phone) }
    var address by remember(shopProfile.address) { mutableStateOf(shopProfile.address) }
    var receiptFooter by remember(shopProfile.receiptFooter) { mutableStateOf(shopProfile.receiptFooter) }
    var currencyCode by remember(shopProfile.currencyCode) { mutableStateOf(shopProfile.currencyCode) }
    var currencySymbol by remember(shopProfile.currencySymbol) { mutableStateOf(shopProfile.currencySymbol) }

    var currencyExpanded by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showDataTransferDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Language & Theme Quick Switch Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Localization.get("display_and_language", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleLanguage() }
                            .padding(vertical = 6.dp),
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
                                    Icon(Icons.Default.Language, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.get("language", language),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${language.flag} ${language.displayName}",
                                    fontSize = 12.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OrangePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (language == AppLanguage.ENGLISH) "Switch to Kinyarwanda" else "Hindura mu Cyongereza",
                                fontSize = 11.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Theme Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTheme() }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0x33FFD166) else Color(0x33334155),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFFFFD166) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.get("app_theme", language),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDarkTheme) Localization.get("dark_mode", language) else Localization.get("light_mode", language),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary
                            )
                        )
                    }
                }
            }
        }

        // 2. Active User & Role Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = OrangeLight,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.name ?: "Active Staff User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Role: ${currentUser?.role?.displayName ?: "Owner"}",
                                    fontSize = 12.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = onLockApp,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("lock_terminal", language), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStaffDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("staff_management", language), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showDataTransferDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("data_backup", language), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Shop Profile Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("shop_profile", language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(Localization.get("shop_name", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(Localization.get("owner_name", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(Localization.get("phone_number", language)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = !currencyExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "$currencySymbol ($currencyCode)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(Localization.get("currency", language)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                listOf(
                                    Triple("FRw", "RWF", "Rwandan Franc"),
                                    Triple("$", "USD", "US Dollar"),
                                    Triple("KSh", "KES", "Kenyan Shilling"),
                                    Triple("USh", "UGX", "Ugandan Shilling"),
                                    Triple("€", "EUR", "Euro")
                                ).forEach { (sym, code, name) ->
                                    DropdownMenuItem(
                                        text = { Text("$sym - $name ($code)") },
                                        onClick = {
                                            currencySymbol = sym
                                            currencyCode = code
                                            currencyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(Localization.get("address", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text(Localization.get("receipt_footer", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSaveProfile(
                                shopProfile.copy(
                                    shopName = shopName.trim(),
                                    name = ownerName.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    receiptFooter = receiptFooter.trim(),
                                    currencyCode = currencyCode,
                                    currencySymbol = currencySymbol
                                )
                            )
                            Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("save_shop_profile", language), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Staff Dialog
    if (showStaffDialog) {
        StaffManagementDialog(
            allUsers = allUsers,
            currentUser = currentUser,
            language = language,
            onDismiss = { showStaffDialog = false },
            onSaveUser = onSaveUser,
            onDeleteUser = onDeleteUser,
            onSwitchUser = onSwitchUser
        )
    }

    // Data Transfer Dialog
    if (showDataTransferDialog) {
        DataTransferDialog(
            shopProfile = shopProfile,
            users = allUsers,
            products = products,
            customers = customers,
            sales = sales,
            customerPayments = customerPayments,
            language = language,
            onDismiss = { showDataTransferDialog = false },
            onImportPackage = onImportPackage
        )
    }
}

@Composable
private fun StaffManagementDialog(
    allUsers: List<User>,
    currentUser: User?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSaveUser: (User) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSwitchUser: (User) -> Unit
) {
    var showAddUserForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.CASHIER) }
    var userError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Localization.get("staff_management", language), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!showAddUserForm) {
                    allUsers.forEach { user ->
                        val isSelf = currentUser?.id == user.id
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
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
                                        Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${user.role.displayName} • PIN: ${user.pinHash}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (!isSelf && user.role != UserRole.OWNER) {
                                    IconButton(onClick = { onDeleteUser(user.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showAddUserForm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.get("add_staff", language))
                    }
                } else {
                    Text(Localization.get("add_staff", language), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OrangePrimary)

                    if (userError != null) {
                        Text(userError!!, color = Color(0xFFDC2626), fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                            label = { Text("4-Digit PIN *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Password *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(UserRole.CASHIER, UserRole.MANAGER).forEach { role ->
                            OutlinedButton(
                                onClick = { newRole = role },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (newRole == role) OrangePrimary.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(role.displayName, fontSize = 11.sp, fontWeight = if (newRole == role) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddUserForm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Localization.get("cancel", language))
                        }
                        Button(
                            onClick = {
                                if (newName.isBlank() || newUsername.isBlank() || newPin.length != 4 || newPassword.isBlank()) {
                                    userError = "Please fill all fields properly (PIN must be 4 digits)"
                                } else {
                                    val newUser = User(
                                        id = UUID.randomUUID().toString(),
                                        name = newName.trim(),
                                        username = newUsername.trim().lowercase(),
                                        phone = newPhone.trim(),
                                        pinHash = newPin.trim(),
                                        password = newPassword.trim(),
                                        role = newRole,
                                        profileColorHex = if (newRole == UserRole.MANAGER) "#2563EB" else "#10B981"
                                    )
                                    onSaveUser(newUser)
                                    showAddUserForm = false
                                    newName = ""
                                    newUsername = ""
                                    newPin = ""
                                    newPassword = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Localization.get("save", language))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showAddUserForm) {
                OutlinedButton(onClick = onDismiss) {
                    Text(Localization.get("close", language))
                }
            }
        }
    )
}

@Composable
private fun DataTransferDialog(
    shopProfile: ShopProfile,
    users: List<User>,
    products: List<Product>,
    customers: List<Customer>,
    sales: List<Sale>,
    customerPayments: List<CustomerPayment>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onImportPackage: (ShopImportSummary) -> Unit
) {
    val context = LocalContext.current
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Localization.get("data_backup", language), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Localization.get("backup_description", language),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Export Button
                Button(
                    onClick = {
                        val exportedFile = ShopDataTransferManager.exportShopPackage(
                            context = context,
                            profile = shopProfile,
                            users = users,
                            products = products,
                            customers = customers,
                            sales = sales,
                            saleItems = emptyList(),
                            payments = customerPayments,
                            exportedByUser = users.firstOrNull()
                        )
                        ShopDataTransferManager.shareBackupFile(context, exportedFile, shopProfile.shopName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("export_json_backup", language))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = Localization.get("restore_database", language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = { Text("Paste JSON Backup Data") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                if (importError != null) {
                    Text(importError!!, color = Color(0xFFDC2626), fontSize = 11.sp)
                }

                if (importSuccess != null) {
                    Text(importSuccess!!, color = ProfitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (importJsonText.isBlank()) {
                            importError = "Please paste valid JSON backup text."
                        } else {
                            try {
                                val summary = ShopDataTransferManager.parseShopPackageString(importJsonText)
                                onImportPackage(summary)
                                importError = null
                                importSuccess = "Successfully restored ${summary.products.size} products & ${summary.sales.size} sales!"
                                importJsonText = ""
                            } catch (e: Exception) {
                                importError = "JSON Parsing failed: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("restore_database", language))
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(Localization.get("close", language))
            }
        }
    )
}
