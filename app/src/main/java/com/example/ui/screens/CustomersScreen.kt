package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.CustomerCategory
import com.example.data.model.CustomerPayment
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.ContactsHelper
import com.example.util.PdfReportGenerator
import com.example.util.ReceiptGenerator
import java.util.UUID

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    totalOutstandingDebt: Double,
    shopProfile: ShopProfile,
    allSales: List<Sale> = emptyList(),
    allPayments: List<CustomerPayment> = emptyList(),
    onSaveCustomer: (Customer) -> Unit,
    onRecordPayment: (String, Double) -> Unit,
    onDeleteCustomer: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyDebt by remember { mutableStateOf(false) }

    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerForPayment by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val filtered = customers.filter { c ->
        val matchesDebt = !filterOnlyDebt || c.debtBalance > 0
        val matchesSearch = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery, ignoreCase = true) ||
                c.category.contains(searchQuery, ignoreCase = true) ||
                c.city.contains(searchQuery, ignoreCase = true)
        matchesDebt && matchesSearch
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Outstanding Debt KPI Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (totalOutstandingDebt > 0) Color(0xFFFEE2E2) else Color.White),
                    border = if (totalOutstandingDebt > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)) else null,
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL OUTSTANDING CUSTOMER DEBT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalOutstandingDebt > 0) LossRed else InkMedium,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ReceiptGenerator.formatMoney(totalOutstandingDebt, shopProfile),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (totalOutstandingDebt > 0) LossRed else ProfitGreen
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (totalOutstandingDebt > 0) LossRed else ProfitGreen,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (totalOutstandingDebt > 0) Icons.Default.MoneyOff else Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search and Filter Bar
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customer, phone, category...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = InkMedium) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = InkMedium)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !filterOnlyDebt,
                                onClick = { filterOnlyDebt = false },
                                label = { Text("All (${customers.size})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.White)
                            )

                            val debtCount = customers.count { it.debtBalance > 0 }
                            FilterChip(
                                selected = filterOnlyDebt,
                                onClick = { filterOnlyDebt = true },
                                label = { Text("Owes Debt ($debtCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LossRed, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }

            // Customers List
            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = InkMedium, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No customers found", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { isAddingNew = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add First Customer")
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        shopProfile = shopProfile,
                        onEdit = { customerToEdit = customer },
                        onRecordPayment = { customerForPayment = customer },
                        onDelete = { customerToDelete = customer },
                        onSendWhatsApp = {
                            if (customer.phone.isNotBlank()) {
                                sendWhatsAppReminder(context, customer, shopProfile)
                            } else {
                                Toast.makeText(context, "No phone number saved for ${customer.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onGeneratePdfStatement = {
                            try {
                                val customerSales = allSales.filter { it.customerId == customer.id }
                                val customerPaymentsList = allPayments.filter { it.customerId == customer.id }
                                val pdfFile = PdfReportGenerator.generateCustomerStatementPdf(
                                    context = context,
                                    customer = customer,
                                    sales = customerSales,
                                    payments = customerPaymentsList,
                                    profile = shopProfile
                                )
                                PdfReportGenerator.sharePdf(context, pdfFile, "Statement - ${customer.name}")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error generating statement: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCallPhone = {
                            if (customer.phone.isNotBlank()) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                context.startActivity(dialIntent)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // FAB to Add Customer
        FloatingActionButton(
            onClick = { isAddingNew = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = OrangePrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Customer", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Add / Edit Customer Dialog
    if (isAddingNew || customerToEdit != null) {
        CustomerFormDialog(
            initialCustomer = customerToEdit,
            shopProfile = shopProfile,
            onSave = { c ->
                onSaveCustomer(c)
                isAddingNew = false
                customerToEdit = null
            },
            onDismiss = {
                isAddingNew = false
                customerToEdit = null
            }
        )
    }

    // Record Debt Payment Dialog
    if (customerForPayment != null) {
        RecordDebtPaymentDialog(
            customer = customerForPayment!!,
            shopProfile = shopProfile,
            onConfirm = { amount ->
                onRecordPayment(customerForPayment!!.id, amount)
                customerForPayment = null
            },
            onDismiss = { customerForPayment = null }
        )
    }

    // Delete Confirmation Dialog
    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${customerToDelete!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(customerToDelete!!.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun sendWhatsAppReminder(context: Context, customer: Customer, shopProfile: ShopProfile) {
    val message = buildString {
        append("Mwiriwe/Hello ${customer.name},\n\n")
        append("This is a reminder from *${shopProfile.shopName}*.\n")
        if (customer.debtBalance > 0) {
            append("Outstanding Debt Balance: *${ReceiptGenerator.formatMoney(customer.debtBalance, shopProfile)}*\n\n")
            append("Please settle via MoMo / Cash when convenient. Murakoze cyane!")
        } else {
            append("Thank you for your valued partnership and regular patronage! Murakoze cyane.")
        }
    }

    try {
        val cleanPhone = customer.phone.replace(" ", "").replace("-", "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to standard SMS
        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:${customer.phone}?body=${Uri.encode(message)}")
        }
        context.startActivity(smsIntent)
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    shopProfile: ShopProfile,
    onEdit: () -> Unit,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onGeneratePdfStatement: () -> Unit,
    onCallPhone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (customer.debtBalance > 0) Color(0xFFFEE2E2) else OrangeLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (customer.debtBalance > 0) LossRed else OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = customer.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.category,
                                fontSize = 11.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (customer.city.isNotBlank()) {
                                Text(
                                    text = " • ${customer.city}",
                                    fontSize = 11.sp,
                                    color = InkMedium
                                )
                            }
                        }
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                fontSize = 12.sp,
                                color = InkMedium
                            )
                        }
                    }
                }

                // Debt Badge
                if (customer.debtBalance > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("Owes Debt", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LossRed)
                            Text(
                                text = ReceiptGenerator.formatMoney(customer.debtBalance, shopProfile),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = LossRed
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "No Debt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (customer.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${customer.notes}",
                    fontSize = 11.sp,
                    color = InkMedium,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Call, WhatsApp, PDF Statement, Record Payment, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customer.debtBalance > 0) {
                    Button(
                        onClick = onRecordPayment,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // WhatsApp
                Surface(
                    onClick = onSendWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = "WhatsApp", tint = ProfitGreen, modifier = Modifier.size(18.dp))
                    }
                }

                // PDF Statement
                Surface(
                    onClick = onGeneratePdfStatement,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = "PDF Statement", tint = DarkNavy, modifier = Modifier.size(18.dp))
                    }
                }

                // Call
                if (customer.phone.isNotBlank()) {
                    Surface(
                        onClick = onCallPhone,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF0E6),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Edit
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = InkDark, modifier = Modifier.size(18.dp))
                    }
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormDialog(
    initialCustomer: Customer?,
    shopProfile: ShopProfile,
    onSave: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var email by remember { mutableStateOf(initialCustomer?.email ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var city by remember { mutableStateOf(initialCustomer?.city ?: "Kigali") }
    var category by remember { mutableStateOf(initialCustomer?.category ?: CustomerCategory.REGULAR.displayName) }
    var creditLimitStr by remember { mutableStateOf(initialCustomer?.creditLimit?.toInt()?.toString() ?: "500000") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var debtStr by remember { mutableStateOf(if (initialCustomer != null && initialCustomer.debtBalance > 0) "${initialCustomer.debtBalance.toInt()}" else "") }

    var categoryExpanded by remember { mutableStateOf(false) }

    // Contact picker launcher from device address book
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val contact = ContactsHelper.extractContactDetails(context, uri)
            if (contact != null) {
                name = contact.name
                if (contact.phone.isNotBlank()) phone = contact.phone
                if (contact.email.isNotBlank()) email = contact.email
                Toast.makeText(context, "Imported ${contact.name} from Phonebook!", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialCustomer != null) "Edit Customer Profile" else "Add New Customer",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Import from Phonebook Contacts
                OutlinedButton(
                    onClick = { contactPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Fill from Device Contacts", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    placeholder = { Text("e.g. Jeanne Uwase") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / WhatsApp Number") },
                    placeholder = { Text("+250 788 123 456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        CustomerCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    category = cat.displayName
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Town") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = debtStr,
                        onValueChange = { debtStr = it },
                        label = { Text("Initial Debt (${shopProfile.currencySymbol})") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Physical Address / Sector") },
                    placeholder = { Text("e.g. Kimironko Market, Stall 12") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Preferences") },
                    placeholder = { Text("e.g. Always prefers morning delivery") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val c = Customer(
                                id = initialCustomer?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                city = city.trim().ifBlank { "Kigali" },
                                category = category,
                                creditLimit = creditLimitStr.toDoubleOrNull() ?: 500000.0,
                                notes = notes.trim(),
                                debtBalance = debtStr.toDoubleOrNull() ?: 0.0,
                                createdAt = initialCustomer?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(c)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotBlank()
                ) {
                    Text("Save Customer Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecordDebtPaymentDialog(
    customer: Customer,
    shopProfile: ShopProfile,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf("${customer.debtBalance.toInt()}") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Record Debt Payment",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customer: ${customer.name}",
                    fontSize = 13.sp,
                    color = OrangePrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Outstanding Debt:", fontSize = 13.sp, color = LossRed)
                        Text(
                            ReceiptGenerator.formatMoney(customer.debtBalance, shopProfile),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LossRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Payment Received (${shopProfile.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick buttons: Full, Half
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { amountStr = "${customer.debtBalance.toInt()}" },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text("Full (${customer.debtBalance.toInt()})", fontSize = 12.sp, color = ProfitGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }

                    if (customer.debtBalance > 1) {
                        Surface(
                            onClick = { amountStr = "${(customer.debtBalance / 2).toInt()}" },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6)
                        ) {
                            Text("Half (${(customer.debtBalance / 2).toInt()})", fontSize = 12.sp, color = InkDark, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsed = amountStr.toDoubleOrNull() ?: 0.0
                            if (parsed > 0) {
                                onConfirm(parsed)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
