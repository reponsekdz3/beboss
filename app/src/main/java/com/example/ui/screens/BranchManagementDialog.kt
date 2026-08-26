package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    allUsers: List<User>,
    language: AppLanguage,
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
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Gucunga Amashami" else "Shop Branches",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = InkDark
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "${branches.size} Amashami Akora" else "${branches.size} Active Branches",
                                fontSize = 12.sp,
                                color = InkMedium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        text = if (language == AppLanguage.KINYARWANDA) "Ongeraho Ishami Rishya" else "Add New Branch",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Branch List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(branches) { branch ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (branch.isMainBranch) Color(0xFFFFF7ED) else Color(0xFFF8FAFC)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
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
                                            fontSize = 15.sp,
                                            color = DarkNavy
                                        )
                                    }

                                    if (branch.isMainBranch) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = OrangePrimary
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.KINYARWANDA) "Ishami Rikuru" else "Main Branch",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = InkMedium, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(branch.address.ifBlank { "Kigali" }, fontSize = 12.sp, color = InkMedium)
                                    }

                                    if (branch.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = InkMedium, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(branch.phone, fontSize = 12.sp, color = InkMedium)
                                        }
                                    }
                                }

                                if (branch.managerName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (language == AppLanguage.KINYARWANDA) "Umuyobozi" else "Manager"}: ${branch.managerName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkNavy
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            branchToEdit = branch
                                            isAddingNew = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == AppLanguage.KINYARWANDA) "Hindura" else "Edit", fontSize = 12.sp)
                                    }

                                    if (!branch.isMainBranch) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { branchToDelete = branch },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(18.dp))
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
        var name by remember { mutableStateOf(branch?.name ?: "") }
        var code by remember { mutableStateOf(branch?.code ?: "BR-${branches.size + 1}") }
        var address by remember { mutableStateOf(branch?.address ?: "Kigali, Rwanda") }
        var phone by remember { mutableStateOf(branch?.phone ?: "+250 ") }
        var managerUserId by remember { mutableStateOf(branch?.managerUserId ?: "") }
        var managerName by remember { mutableStateOf(branch?.managerName ?: "") }
        var isMain by remember { mutableStateOf(branch?.isMainBranch ?: false) }
        var isActive by remember { mutableStateOf(branch?.isActive ?: true) }

        Dialog(onDismissRequest = {
            isAddingNew = false
            branchToEdit = null
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (branch == null) {
                            if (language == AppLanguage.KINYARWANDA) "Ongeraho Ishami Rishya" else "Create New Branch"
                        } else {
                            if (language == AppLanguage.KINYARWANDA) "Hindura Amakuru y'Ishami" else "Edit Branch Info"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Ishami *" else "Branch Name *") },
                        placeholder = { Text("e.g. Nyabugogo Branch / Kimironko") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text(if (language == AppLanguage.KINYARWANDA) "Kode y'Ishami" else "Branch Code") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(if (language == AppLanguage.KINYARWANDA) "Telefoni" else "Branch Phone") },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Aho Riherereye (Aderesi)" else "Physical Location / Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text(if (language == AppLanguage.KINYARWANDA) "Izina ry'Umuyobozi w'Ishami" else "Branch Manager / Supervisor") },
                        placeholder = { Text("e.g. Emmanuel Nshimiyimana") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Ishami Rikuru ry'Ububiko" else "Set as Main Headquarter Branch",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = DarkNavy
                            )
                            Text(
                                text = if (language == AppLanguage.KINYARWANDA) "Iri rizaba ishami ry'ibanze" else "Primary location for centralized reports",
                                fontSize = 11.sp,
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isAddingNew = false
                                branchToEdit = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (language == AppLanguage.KINYARWANDA) "Kureka" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val newOrUpdated = Branch(
                                        id = branch?.id ?: UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        code = code.trim().ifBlank { "BR-01" },
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        managerUserId = managerUserId,
                                        managerName = managerName.trim(),
                                        isMainBranch = isMain,
                                        isActive = isActive,
                                        createdAt = branch?.createdAt ?: System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    onSaveBranch(newOrUpdated)
                                    isAddingNew = false
                                    branchToEdit = null
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

    // Delete Confirmation Dialog
    if (branchToDelete != null) {
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text(if (language == AppLanguage.KINYARWANDA) "Gusiba Ishami" else "Delete Branch", fontWeight = FontWeight.Bold) },
            text = { Text(if (language == AppLanguage.KINYARWANDA) "Wizera neza ko ushaka gusiba ishami '${branchToDelete!!.name}'?" else "Are you sure you want to remove '${branchToDelete!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBranch(branchToDelete!!.id)
                        branchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == AppLanguage.KINYARWANDA) "Siba" else "Delete")
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
