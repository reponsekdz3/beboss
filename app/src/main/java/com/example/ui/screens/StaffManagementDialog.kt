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
import androidx.compose.material.icons.filled.AutoFixHigh
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
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Branch
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
import com.example.util.AppLanguage
import com.example.util.PdfReportGenerator
import com.example.util.ShopDataTransferManager
import java.util.UUID
import kotlin.random.Random

@Composable
fun StaffManagementDialog(
    allUsers: List<User>,
    currentUser: User?,
    branches: List<Branch> = emptyList(),
    shopProfile: ShopProfile,
    language: AppLanguage = AppLanguage.ENGLISH,
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
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = OrangeLight,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Abakozi & Uburenganzira" else "Staff & Workers Management",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "${allUsers.size} Abakozi Banditse" else "${allUsers.size} Registered Workers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add Worker Button
                Button(
                    onClick = {
                        userToEdit = null
                        isAddingNew = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "Ongeraho Umukozi Mushya" else "Add New Worker / Collaborator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Workers List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsers) { user ->
                        val isSelf = currentUser?.id == user.id
                        val assignedBranch = branches.firstOrNull { it.id == user.assignedBranchId }
                        val activePermsCount = listOf(
                            user.canSellPOS, user.canApplyDiscounts, user.canManageInventory,
                            user.canViewCostAndProfit, user.canViewAnalytics, user.canManageCustomers,
                            user.canCollectDebt, user.canDeleteRecords, user.canExportReports,
                            user.canManageCollaborators, user.canManageShopSettings, user.canExportImportData
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
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = DarkNavy
                                                )
                                                if (isSelf) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(You)", fontSize = 10.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                text = "${user.role.displayName} • @${user.username}",
                                                fontSize = 11.5.sp,
                                                color = InkMedium
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (user.role) {
                                            UserRole.OWNER -> OrangeLight
                                            UserRole.MANAGER -> Color(0xFFDBEAFE)
                                            UserRole.CASHIER -> Color(0xFFDCFCE7)
                                        }
                                    ) {
                                        Text(
                                            text = user.role.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (user.role) {
                                                UserRole.OWNER -> OrangePrimary
                                                UserRole.MANAGER -> Color(0xFF1D4ED8)
                                                UserRole.CASHIER -> ProfitGreen
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Branch and phone line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = assignedBranch?.name ?: (if (user.role == UserRole.OWNER) "All Branches (HQ)" else "Main Store"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkNavy
                                        )
                                    }

                                    if (user.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = InkMedium, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(user.phone, fontSize = 11.sp, color = InkMedium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))

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
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            }
        }
    }

    if (isAddingNew || userToEdit != null) {
        StaffFormDialog(
            initialUser = userToEdit,
            branches = branches,
            language = language,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFormDialog(
    initialUser: User?,
    branches: List<Branch> = emptyList(),
    language: AppLanguage = AppLanguage.ENGLISH,
    onSave: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialUser?.name ?: "") }
    var username by remember { mutableStateOf(initialUser?.username ?: "") }
    var phone by remember { mutableStateOf(initialUser?.phone ?: "") }
    var pin by remember { mutableStateOf(if (initialUser != null && initialUser.pinHash.length <= 6) initialUser.pinHash else "") }
    var showPin by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(initialUser?.role ?: UserRole.CASHIER) }
    var selectedBranchId by remember { mutableStateOf(initialUser?.assignedBranchId ?: (branches.firstOrNull { it.isMainBranch }?.id ?: "")) }
    var branchDropdownExpanded by remember { mutableStateOf(false) }

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        text = if (initialUser != null) {
                            if (language == AppLanguage.KINYARWANDA) "Hindura Amakuru y'Umukozi" else "Edit Worker Profile"
                        } else {
                            if (language == AppLanguage.KINYARWANDA) "Ongeraho Umukozi Mushya" else "New Worker Account"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (username.isBlank() || initialUser == null) {
                            username = it.lowercase().replace(" ", "").filter { ch -> ch.isLetterOrDigit() }
                        }
                    },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Amazina y'Umukozi *" else "Worker Full Name *") },
                    placeholder = { Text("e.g. Jean Pierre Mugabo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ryo Kwinjira *" else "Username *") },
                    placeholder = { Text("e.g. jean") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "PIN yo Kwinjira *" else "Login PIN *") },
                    placeholder = { Text(if (initialUser != null) "Unchanged" else "1234") },
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle PIN",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Quick Generate PIN button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "⚡ Kora PIN nshya mu buryo bwikora" else "⚡ Auto-Generate 4-Digit PIN",
                        fontSize = 11.5.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                pin = String.format("%04d", Random.nextInt(1000, 9999))
                                showPin = true
                            }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Nimero ya Telefoni (WhatsApp)" else "Worker Phone / WhatsApp") },
                    placeholder = { Text("+250 788 123 456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Assigned Branch Dropdown
                if (branches.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = branchDropdownExpanded,
                        onExpandedChange = { branchDropdownExpanded = !branchDropdownExpanded }
                    ) {
                        val currentBranchName = branches.firstOrNull { it.id == selectedBranchId }?.name 
                            ?: (if (selectedBranchId.isBlank()) "All Branches (Master HQ)" else "Select Branch")

                        OutlinedTextField(
                            value = currentBranchName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (language == AppLanguage.KINYARWANDA) "Ishami Akoreramo" else "Assigned Work Branch") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = branchDropdownExpanded,
                            onDismissRequest = { branchDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Branches (HQ / Master Access)") },
                                onClick = {
                                    selectedBranchId = ""
                                    branchDropdownExpanded = false
                                }
                            )
                            branches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text("${b.name} (${b.code})") },
                                    onClick = {
                                        selectedBranchId = b.id
                                        branchDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (language == AppLanguage.KINYARWANDA) "1. Hitamo Inshingano (Role):" else "1. Quick Role Preset:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

                Text(
                    text = if (language == AppLanguage.KINYARWANDA) "2. Uburenganzira Bwihariye bw'Umukozi:" else "2. Fine-Grained Permissions Customizer:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (language == AppLanguage.KINYARWANDA) "Hitamo neza ibyo uyu mukozi yemerewe gukora muri porogaramu." else "Customize exact capabilities allowed for this staff member.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                val finalBranch = branches.firstOrNull { it.id == selectedBranchId }
                val isFormValid = name.isNotBlank() && (pin.isNotBlank() || initialUser != null)

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val finalPin = if (pin.isNotBlank()) pin.trim() else (initialUser?.pinHash ?: "1234")
                            val u = User(
                                id = initialUser?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                username = username.ifBlank { name.lowercase().replace(" ", "") },
                                phone = phone.trim(),
                                pinHash = finalPin,
                                role = role,
                                assignedBranchId = selectedBranchId,
                                assignedBranchName = finalBranch?.name ?: "Main Store",
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
                    enabled = isFormValid
                ) {
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "Bika Umukozi n'Uburenganzira" else "Save Worker & Permissions",
                        fontWeight = FontWeight.Bold
                    )
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
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
