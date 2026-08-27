package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Branch
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import java.util.UUID

@Composable
fun BranchManagementDialog(
    branches: List<Branch>,
    allUsers: List<User> = emptyList(),
    selectedBranchId: String = "ALL",
    language: AppLanguage = AppLanguage.ENGLISH,
    onSelectActiveBranch: (String) -> Unit = {},
    onSaveBranch: (Branch) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var branchToEdit by remember { mutableStateOf<Branch?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var branchToDelete by remember { mutableStateOf<Branch?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
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
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Gucunga Amashami y'Ubucuruzi" else "Store Branches & Outlets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "${branches.size} Amashami Akora • Ihuza rya POS" else "${branches.size} Active Multi-Store Outlets",
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

                // Add Branch button
                Button(
                    onClick = {
                        branchToEdit = null
                        isAddingNew = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.KINYARWANDA) "Ongeraho Ishami Rishya" else "Create New Shop Branch",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Branch List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(branches) { branch ->
                        val assignedStaff = allUsers.filter { it.assignedBranchId == branch.id || (branch.isMainBranch && it.assignedBranchId.isBlank()) }
                        val isCurrentActive = selectedBranchId == branch.id

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (branch.isMainBranch) Color(0xFFFFF7ED) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (branch.isMainBranch) OrangePrimary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (branch.isActive) ProfitGreen else Color.Gray,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = branch.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = DarkNavy
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = branch.code,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkNavy,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    if (branch.isMainBranch) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = OrangePrimary
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = if (language == AppLanguage.KINYARWANDA) "Ishami Rikuru" else "Main HQ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = InkMedium, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(branch.address.ifBlank { "Kigali, Rwanda" }, fontSize = 11.5.sp, color = InkMedium)
                                    }

                                    if (branch.phone.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${branch.phone}"))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(branch.phone, fontSize = 11.5.sp, color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (language == AppLanguage.KINYARWANDA) "Umuyobozi" else "Manager"}: ${branch.managerName.ifBlank { "Store Operator" }}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkNavy
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${assignedStaff.size} ${if (language == AppLanguage.KINYARWANDA) "Abakozi" else "Staff"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onSelectActiveBranch(branch.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isCurrentActive) OrangePrimary.copy(alpha = 0.12f) else Color.Transparent
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isCurrentActive) OrangePrimary else InkMedium,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCurrentActive) (if (language == AppLanguage.KINYARWANDA) "Ishami Rikoreshwa" else "Active POS Terminal")
                                                else (if (language == AppLanguage.KINYARWANDA) "Hitamo iri Shami" else "Select for POS"),
                                            fontSize = 11.sp,
                                            color = if (isCurrentActive) OrangePrimary else InkMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = {
                                                branchToEdit = branch
                                                isAddingNew = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (language == AppLanguage.KINYARWANDA) "Hindura" else "Edit", fontSize = 11.5.sp)
                                        }

                                        if (!branch.isMainBranch) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { branchToDelete = branch },
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

    // Add / Edit Branch Form Dialog
    if (isAddingNew || branchToEdit != null) {
        val branch = branchToEdit
        BranchFormDialog(
            initialBranch = branch,
            branchesCount = branches.size,
            allUsers = allUsers,
            language = language,
            onSave = { newOrUpdated ->
                onSaveBranch(newOrUpdated)
                isAddingNew = false
                branchToEdit = null
            },
            onDismiss = {
                isAddingNew = false
                branchToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (branchToDelete != null) {
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text(if (language == AppLanguage.KINYARWANDA) "Gusiba Ishami" else "Delete Branch Outlet", fontWeight = FontWeight.Bold) },
            text = { Text(if (language == AppLanguage.KINYARWANDA) "Wizera neza ko ushaka gusiba ishami '${branchToDelete!!.name}'?" else "Are you sure you want to remove '${branchToDelete!!.name}'? Its historic sales will remain archived.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBranch(branchToDelete!!.id)
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == AppLanguage.KINYARWANDA) "Siba" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { branchToDelete = null }) {
                    Text(if (language == AppLanguage.KINYARWANDA) "Kureka" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchFormDialog(
    initialBranch: Branch?,
    branchesCount: Int,
    allUsers: List<User> = emptyList(),
    language: AppLanguage = AppLanguage.ENGLISH,
    onSave: (Branch) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialBranch?.name ?: "") }
    var code by remember { mutableStateOf(initialBranch?.code ?: "BR-0${branchesCount + 1}") }
    var address by remember { mutableStateOf(initialBranch?.address ?: "Kigali, Rwanda") }
    var phone by remember { mutableStateOf(initialBranch?.phone ?: "+250 ") }
    var managerUserId by remember { mutableStateOf(initialBranch?.managerUserId ?: "") }
    var managerName by remember { mutableStateOf(initialBranch?.managerName ?: "") }
    var isMain by remember { mutableStateOf(initialBranch?.isMainBranch ?: false) }
    var isActive by remember { mutableStateOf(initialBranch?.isActive ?: true) }
    var managerDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialBranch == null) {
                            if (language == AppLanguage.KINYARWANDA) "Ongeraho Ishami Rishya" else "Create New Shop Branch"
                        } else {
                            if (language == AppLanguage.KINYARWANDA) "Hindura Amakuru y'Ishami" else "Edit Branch Outlet"
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
                    onValueChange = { name = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Ishami *" else "Branch Outlet Name *") },
                    placeholder = { Text("e.g. Kimironko Market Sector 4") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Kode y'Ishami" else "Branch Code") },
                    placeholder = { Text("BR-01") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Telefoni y'Ishami" else "Branch Phone Number") },
                    placeholder = { Text("+250 788 123 456") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (language == AppLanguage.KINYARWANDA) "Aho Riherereye (Aderesi) *" else "Physical Address / Location *") },
                    placeholder = { Text("e.g. Nyarugenge Market Shop #42") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Manager Selector Dropdown or Manual Input
                if (allUsers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = managerDropdownExpanded,
                        onExpandedChange = { managerDropdownExpanded = !managerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = managerName.ifBlank { "Select Registered Manager" },
                            onValueChange = { managerName = it },
                            label = { Text(if (language == AppLanguage.KINYARWANDA) "Umuyobozi w'Ishami" else "Assigned Branch Manager") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = managerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = managerDropdownExpanded,
                            onDismissRequest = { managerDropdownExpanded = false }
                        ) {
                            allUsers.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.role.displayName})") },
                                    onClick = {
                                        managerUserId = u.id
                                        managerName = u.name
                                        managerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Umuyobozi w'Ishami" else "Branch Manager / Supervisor") },
                        placeholder = { Text("e.g. Aline Uwase") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Is Main HQ Branch switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ishami Rikuru ry'Ububiko (HQ)" else "Designate as Main Store HQ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = DarkNavy
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Iri rizaba ishami ry'ibanze ry'ibaruramari ryose" else "Primary hub for central reporting and default inventory",
                                fontSize = 10.5.sp,
                                color = InkMedium
                            )
                        }
                        Switch(
                            checked = isMain,
                            onCheckedChange = { isMain = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (language == AppLanguage.KINYARWANDA) "Kureka" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val newOrUpdated = Branch(
                                    id = initialBranch?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    code = code.trim().ifBlank { "BR-01" },
                                    address = address.trim().ifBlank { "Kigali, Rwanda" },
                                    phone = phone.trim(),
                                    managerUserId = managerUserId,
                                    managerName = managerName.trim(),
                                    isMainBranch = isMain,
                                    isActive = isActive,
                                    createdAt = initialBranch?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(newOrUpdated)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (language == AppLanguage.KINYARWANDA) "Bika Ishami" else "Save Branch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
