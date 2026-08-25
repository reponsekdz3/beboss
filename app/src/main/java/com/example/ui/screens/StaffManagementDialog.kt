package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.PdfReportGenerator
import com.example.util.ShopDataTransferManager
import java.util.UUID

@Composable
fun StaffManagementDialog(
    allUsers: List<User>,
    currentUser: User?,
    shopProfile: ShopProfile,
    onSaveUser: (User) -> Unit,
    onDeleteUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Staff & Role Access",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                            Text(
                                text = "${allUsers.size} Registered Collaborators",
                                fontSize = 11.sp,
                                color = InkMedium
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsers) { user ->
                        val isSelf = currentUser?.id == user.id
                        val activePermsCount = listOf(
                            user.canSellPOS, user.canApplyDiscounts, user.canManageInventory,
                            user.canViewCostAndProfit, user.canViewAnalytics, user.canManageCustomers,
                            user.canCollectDebt, user.canDeleteRecords, user.canExportReports,
                            user.canManageCollaborators, user.canManageShopSettings
                        ).count { it }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
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
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = InkDark
                                                )
                                                if (isSelf) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(You)", fontSize = 10.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                text = "${user.role.displayName} • PIN: ${user.pinHash}",
                                                fontSize = 11.sp,
                                                color = InkMedium
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (activePermsCount >= 8) Color(0xFFDCFCE7) else OrangeLight
                                    ) {
                                        Text(
                                            text = "$activePermsCount / 11 Perms",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activePermsCount >= 8) ProfitGreen else OrangePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // WhatsApp Credentials Invite
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFDCFCE7),
                                            modifier = Modifier.clickable {
                                                val msg = ShopDataTransferManager.buildCollaboratorInviteText(shopProfile, user)
                                                PdfReportGenerator.sendWhatsAppDirect(context, user.phone, msg)
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text("WhatsApp Invite", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                            }
                                        }

                                        // PDF Access Pass
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = OrangeLight,
                                            modifier = Modifier.clickable {
                                                try {
                                                    val pdf = PdfReportGenerator.generateStaffBadgePdf(context, user, shopProfile)
                                                    PdfReportGenerator.sharePdf(context, pdf, "Access Pass - ${user.name}")
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error generating pass: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Icon(Icons.Default.Badge, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("PDF Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                                            }
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { userToEdit = user },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DarkNavy, modifier = Modifier.size(16.dp))
                                        }
                                        if (!isSelf) {
                                            IconButton(
                                                onClick = { onDeleteUser(user.id) },
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { isAddingNew = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Collaborator Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (isAddingNew || userToEdit != null) {
        StaffFormDialog(
            initialUser = userToEdit,
            onSave = { u ->
                onSaveUser(u)
                isAddingNew = false
                userToEdit = null
            },
            onDismiss = {
                isAddingNew = false
                userToEdit = null
            }
        )
    }
}

@Composable
fun StaffFormDialog(
    initialUser: User?,
    onSave: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialUser?.name ?: "") }
    var username by remember { mutableStateOf(initialUser?.username ?: "") }
    var phone by remember { mutableStateOf(initialUser?.phone ?: "") }
    var pin by remember { mutableStateOf(initialUser?.pinHash ?: "") }
    var role by remember { mutableStateOf(initialUser?.role ?: UserRole.CASHIER) }

    // Granular permissions
    var canSellPOS by remember { mutableStateOf(initialUser?.canSellPOS ?: true) }
    var canApplyDiscounts by remember { mutableStateOf(initialUser?.canApplyDiscounts ?: (role != UserRole.CASHIER)) }
    var canManageInventory by remember { mutableStateOf(initialUser?.canManageInventory ?: (role != UserRole.CASHIER)) }
    var canViewCostAndProfit by remember { mutableStateOf(initialUser?.canViewCostAndProfit ?: (role != UserRole.CASHIER)) }
    var canViewAnalytics by remember { mutableStateOf(initialUser?.canViewAnalytics ?: (role != UserRole.CASHIER)) }
    var canManageCustomers by remember { mutableStateOf(initialUser?.canManageCustomers ?: true) }
    var canCollectDebt by remember { mutableStateOf(initialUser?.canCollectDebt ?: (role != UserRole.CASHIER)) }
    var canDeleteRecords by remember { mutableStateOf(initialUser?.canDeleteRecords ?: (role == UserRole.OWNER)) }
    var canExportReports by remember { mutableStateOf(initialUser?.canExportReports ?: (role != UserRole.CASHIER)) }
    var canManageCollaborators by remember { mutableStateOf(initialUser?.canManageCollaborators ?: (role == UserRole.OWNER)) }
    var canManageShopSettings by remember { mutableStateOf(initialUser?.canManageShopSettings ?: (role == UserRole.OWNER)) }
    var canExportImportData by remember { mutableStateOf(initialUser?.canExportImportData ?: (role == UserRole.OWNER)) }

    fun applyRoleDefaults(r: UserRole) {
        role = r
        val template = User.defaultForRole("tmp", "tmp", r)
        canSellPOS = template.canSellPOS
        canApplyDiscounts = template.canApplyDiscounts
        canManageInventory = template.canManageInventory
        canViewCostAndProfit = template.canViewCostAndProfit
        canViewAnalytics = template.canViewAnalytics
        canManageCustomers = template.canManageCustomers
        canCollectDebt = template.canCollectDebt
        canDeleteRecords = template.canDeleteRecords
        canExportReports = template.canExportReports
        canManageCollaborators = template.canManageCollaborators
        canManageShopSettings = template.canManageShopSettings
        canExportImportData = template.canExportImportData
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialUser != null) "Edit Collaborator" else "New Collaborator Account",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Worker Full Name *") },
                    placeholder = { Text("e.g. Eric Manzi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username *") },
                        placeholder = { Text("eric") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Fast PIN (4 Digits) *") },
                        placeholder = { Text("1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (for WhatsApp Invite)") },
                    placeholder = { Text("+250 788 123 456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("1. Quick Role Preset:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserRole.values().forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { applyRoleDefaults(r) },
                            label = { Text(r.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (r) {
                                    UserRole.OWNER -> OrangePrimary
                                    UserRole.MANAGER -> Color(0xFF2563EB)
                                    UserRole.CASHIER -> ProfitGreen
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(10.dp))

                Text("2. Fine-Grained Permissions Customizer:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Text("Customize exact permissions allowed for this staff member.", fontSize = 11.sp, color = InkMedium)

                Spacer(modifier = Modifier.height(8.dp))

                PermissionToggleItem("POS Checkout & Sales Terminal", "Can create sales & generate receipts", canSellPOS) { canSellPOS = it }
                PermissionToggleItem("Apply Custom Discounts", "Can enter discounts during POS checkout", canApplyDiscounts) { canApplyDiscounts = it }
                PermissionToggleItem("Manage Inventory & Stock Levels", "Can add, edit items and restock quantity", canManageInventory) { canManageInventory = it }
                PermissionToggleItem("View Wholesale Cost & Margins", "Show cost prices and net profit margins", canViewCostAndProfit) { canViewCostAndProfit = it }
                PermissionToggleItem("View Analytics & Financial Reports", "Access P&L summaries and revenue dashboard", canViewAnalytics) { canViewAnalytics = it }
                PermissionToggleItem("Manage Customers & Credit", "Create customer records and credit limits", canManageCustomers) { canManageCustomers = it }
                PermissionToggleItem("Collect Debt & Record Repayments", "Accept partial debt settlements from customers", canCollectDebt) { canCollectDebt = it }
                PermissionToggleItem("Delete Records", "Can permanently delete products or sales", canDeleteRecords) { canDeleteRecords = it }
                PermissionToggleItem("Export PDF Reports & Statements", "Export official PDF tax invoices & valuation reports", canExportReports) { canExportReports = it }
                PermissionToggleItem("Manage Other Staff Accounts", "Create or edit collaborator PINs and access", canManageCollaborators) { canManageCollaborators = it }
                PermissionToggleItem("Manage Shop Profile & Settings", "Change shop name, currency, tax rates", canManageShopSettings) { canManageShopSettings = it }
                PermissionToggleItem("Phone Storage Data Backup & Sync", "Export/Import complete offline shop data package", canExportImportData) { canExportImportData = it }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank() && pin.isNotBlank()) {
                            val u = User(
                                id = initialUser?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                username = username.ifBlank { name.lowercase().replace(" ", "") },
                                phone = phone.trim(),
                                pinHash = pin.trim(),
                                role = role,
                                profileColorHex = when (role) {
                                    UserRole.OWNER -> "#FF6B1A"
                                    UserRole.MANAGER -> "#2563EB"
                                    UserRole.CASHIER -> "#10B981"
                                },
                                canSellPOS = canSellPOS,
                                canApplyDiscounts = canApplyDiscounts,
                                canManageInventory = canManageInventory,
                                canViewCostAndProfit = canViewCostAndProfit,
                                canViewAnalytics = canViewAnalytics,
                                canManageCustomers = canManageCustomers,
                                canCollectDebt = canCollectDebt,
                                canDeleteRecords = canDeleteRecords,
                                canExportReports = canExportReports,
                                canManageCollaborators = canManageCollaborators,
                                canManageShopSettings = canManageShopSettings,
                                canExportImportData = canExportImportData
                            )
                            onSave(u)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = name.isNotBlank() && pin.isNotBlank()
                ) {
                    Text("Save Collaborator & Permissions", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
            Text(subtitle, fontSize = 10.5.sp, color = InkMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrangePrimary
            ),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
