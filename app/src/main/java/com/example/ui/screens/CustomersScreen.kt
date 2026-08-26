package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.util.ExcelCsvExporter
import com.example.util.PdfReportGenerator
import com.example.util.PickedContactInfo
import com.example.util.ReceiptGenerator
import com.example.util.SmsHelper
import java.util.UUID

enum class CustomerSortField {
    NAME, DEBT, CREDIT_LIMIT, CATEGORY
}

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
    var sortField by remember { mutableStateOf(CustomerSortField.DEBT) }
    var sortAscending by remember { mutableStateOf(false) }

    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerForPayment by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var showBulkContactsDialog by remember { mutableStateOf(false) }

    val filtered = customers.filter { c ->
        val matchesDebt = !filterOnlyDebt || c.debtBalance > 0
        val matchesSearch = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery, ignoreCase = true) ||
                c.category.contains(searchQuery, ignoreCase = true) ||
                c.city.contains(searchQuery, ignoreCase = true)
        matchesDebt && matchesSearch
    }.let { list ->
        when (sortField) {
            CustomerSortField.NAME -> if (sortAscending) list.sortedBy { it.name.lowercase() } else list.sortedByDescending { it.name.lowercase() }
            CustomerSortField.DEBT -> if (sortAscending) list.sortedBy { it.debtBalance } else list.sortedByDescending { it.debtBalance }
            CustomerSortField.CREDIT_LIMIT -> if (sortAscending) list.sortedBy { it.creditLimit } else list.sortedByDescending { it.creditLimit }
            CustomerSortField.CATEGORY -> if (sortAscending) list.sortedBy { it.category } else list.sortedByDescending { it.category }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Outstanding Debt KPI Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (totalOutstandingDebt > 0) Color(0xFFFEE2E2) else Color.White),
                    border = if (totalOutstandingDebt > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)) else null,
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL UNCOLLECTED CUSTOMER DEBT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalOutstandingDebt > 0) LossRed else InkMedium,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = ReceiptGenerator.formatMoney(totalOutstandingDebt, shopProfile),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (totalOutstandingDebt > 0) LossRed else ProfitGreen
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (totalOutstandingDebt > 0) LossRed else ProfitGreen,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (totalOutstandingDebt > 0) Icons.Default.MoneyOff else Icons.Default.People,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action button row: Excel Export, Bulk Contacts Import
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val file = ExcelCsvExporter.exportCustomersCsv(context, customers, shopProfile)
                                        ExcelCsvExporter.shareCsvFile(context, file, "Share Customers & Debts CSV")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = OrangePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel/CSV Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }

                            OutlinedButton(
                                onClick = { showBulkContactsDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(15.dp), tint = DarkNavy)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Contacts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
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
                    Column(modifier = Modifier.padding(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customer name, phone, city...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = InkMedium) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = InkMedium)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !filterOnlyDebt,
                                onClick = { filterOnlyDebt = false },
                                label = { Text("All (${customers.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            )

                            val debtCount = customers.count { it.debtBalance > 0 }
                            FilterChip(
                                selected = filterOnlyDebt,
                                onClick = { filterOnlyDebt = true },
                                label = { Text("Owes Debt ($debtCount)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LossRed, selectedLabelColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Excel Customers Table Header
            item {
                ExcelCustomerTableHeaderRow(
                    sortField = sortField,
                    sortAscending = sortAscending,
                    currencySymbol = shopProfile.currencySymbol,
                    onSort = { field ->
                        if (sortField == field) {
                            sortAscending = !sortAscending
                        } else {
                            sortField = field
                            sortAscending = true
                        }
                    }
                )
            }

            // Excel Customers List
            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = InkMedium, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No customers found in ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkDark)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { isAddingNew = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Add Customer to Ledger")
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(filtered, key = { _, c -> c.id }) { index, customer ->
                    ExcelCustomerRow(
                        index = index + 1,
                        customer = customer,
                        shopProfile = shopProfile,
                        isEven = index % 2 == 0,
                        onRecordPayment = { customerForPayment = customer },
                        onEdit = { customerToEdit = customer },
                        onDelete = { customerToDelete = customer },
                        onSendReminder = {
                            if (customer.phone.isNotBlank()) {
                                sendDebtReminder(context, customer, shopProfile)
                            } else {
                                Toast.makeText(context, "No phone number for ${customer.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onGeneratePdf = {
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

            item { Spacer(modifier = Modifier.height(76.dp)) }
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

    // Bulk Phone Contacts Import Dialog
    if (showBulkContactsDialog) {
        BulkContactsImportDialog(
            onImport = { selectedContacts ->
                selectedContacts.forEach { contact ->
                    val newCustomer = Customer(
                        id = UUID.randomUUID().toString(),
                        name = contact.name,
                        phone = contact.phone,
                        email = contact.email,
                        category = CustomerCategory.REGULAR.displayName,
                        creditLimit = 500000.0,
                        city = "Kigali"
                    )
                    onSaveCustomer(newCustomer)
                }
                showBulkContactsDialog = false
                Toast.makeText(context, "Imported ${selectedContacts.size} contacts successfully!", Toast.LENGTH_LONG).show()
            },
            onDismiss = { showBulkContactsDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${customerToDelete!!.name}'? Outstanding debt history will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(customerToDelete!!.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
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

@Composable
private fun ExcelCustomerTableHeaderRow(
    sortField: CustomerSortField,
    sortAscending: Boolean,
    currencySymbol: String,
    onSort: (CustomerSortField) -> Unit
) {
    Surface(
        color = DarkNavy,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .weight(2.2f)
                    .clickable { onSort(CustomerSortField.NAME) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Name & Phone",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (sortField == CustomerSortField.NAME) {
                    Icon(
                        if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1.3f)
                    .clickable { onSort(CustomerSortField.DEBT) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Debt ($currencySymbol)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (sortField == CustomerSortField.DEBT) {
                    Icon(
                        if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = "Actions",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(96.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExcelCustomerRow(
    index: Int,
    customer: Customer,
    shopProfile: ShopProfile,
    isEven: Boolean,
    onRecordPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendReminder: () -> Unit,
    onGeneratePdf: () -> Unit,
    onCallPhone: () -> Unit
) {
    Surface(
        color = if (isEven) Color(0xFFF9FAFB) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0xFFE5E7EB)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // # Index
            Text(
                text = "$index",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = InkMedium,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )

            // Name + Phone + Category
            Column(
                modifier = Modifier
                    .weight(2.2f)
                    .clickable { onEdit() }
            ) {
                Text(
                    text = customer.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customer.category,
                        fontSize = 10.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = " • ${customer.phone}",
                            fontSize = 10.sp,
                            color = InkMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            // Debt Column
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.End
            ) {
                if (customer.debtBalance > 0) {
                    Text(
                        text = ReceiptGenerator.formatMoney(customer.debtBalance, shopProfile),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = LossRed,
                        maxLines = 1
                    )
                    Text(
                        text = "Unpaid Debt",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                } else {
                    Text(
                        text = "Cleared (0)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }

            // Action Icons: Pay (if debt), Reminder, Call, Edit
            Row(
                modifier = Modifier.width(96.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customer.debtBalance > 0) {
                    Surface(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(6.dp),
                        color = ProfitGreen,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Payment, contentDescription = "Pay", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                }

                Surface(
                    onClick = onSendReminder,
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, contentDescription = "Reminder", tint = ProfitGreen, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.width(3.dp))

                if (customer.phone.isNotBlank()) {
                    Surface(
                        onClick = onCallPhone,
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF0E6),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = OrangePrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                }

                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = InkDark, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun sendDebtReminder(context: Context, customer: Customer, shopProfile: ShopProfile) {
    val message = buildString {
        append("Mwiriwe/Hello ${customer.name},\n\n")
        append("This is an official statement from *${shopProfile.shopName}*.\n")
        if (customer.debtBalance > 0) {
            append("Your pending balance is *${ReceiptGenerator.formatMoney(customer.debtBalance, shopProfile)}*.\n\n")
            append("Please settle via MTN MoMo / Airtel Money / Cash at your earliest convenience. Murakoze cyane!")
        } else {
            append("Thank you for your regular patronage and timely payments! Murakoze cyane.")
        }
    }

    SmsHelper.sendSmsOrOpenIntent(context, customer.phone, message)
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
                        text = if (initialCustomer != null) "Edit Customer in Ledger" else "Add New Customer",
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
                    placeholder = { Text("e.g. Prefers morning delivery") },
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
                        .height(48.dp),
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

@Composable
fun BulkContactsImportDialog(
    onImport: (List<PickedContactInfo>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var contactsList by remember { mutableStateOf<List<PickedContactInfo>>(emptyList()) }
    var selectedContacts by remember { mutableStateOf<Set<PickedContactInfo>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsList = ContactsHelper.fetchAllPhoneContacts(context)
            selectedContacts = contactsList.toSet()
            isLoading = false
        } else {
            isLoading = false
            Toast.makeText(context, "Contacts permission denied", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (ContactsHelper.hasContactsPermission(context)) {
            contactsList = ContactsHelper.fetchAllPhoneContacts(context)
            selectedContacts = contactsList.toSet()
            isLoading = false
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
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
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import Contacts into BeBoss",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select contacts from your phone to add as shop customers:",
                    fontSize = 12.sp,
                    color = InkMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("Reading phonebook contacts...", fontSize = 13.sp, color = InkMedium)
                    }
                } else if (contactsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No contacts found in phonebook.", fontSize = 13.sp, color = InkMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(contactsList) { contact ->
                            val isSelected = selectedContacts.contains(contact)
                            Surface(
                                onClick = {
                                    selectedContacts = if (isSelected) {
                                        selectedContacts - contact
                                    } else {
                                        selectedContacts + contact
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFFF0E6) else Color(0xFFF9FAFB),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkDark)
                                        Text(contact.phone, fontSize = 11.sp, color = InkMedium)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        onClick = { onImport(selectedContacts.toList()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedContacts.isNotEmpty()
                    ) {
                        Text("Import (${selectedContacts.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
