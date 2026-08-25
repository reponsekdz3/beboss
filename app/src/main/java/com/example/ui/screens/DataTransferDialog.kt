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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
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
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataTransferDialog(
    shopProfile: ShopProfile,
    allUsers: List<User>,
    products: List<Product>,
    customers: List<Customer>,
    sales: List<Sale>,
    customerPayments: List<CustomerPayment>,
    currentUser: User?,
    onImportPackage: (ShopImportSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var localBackups by remember { mutableStateOf(ShopDataTransferManager.listLocalBackups(context)) }
    var selectedBackupForPreview by remember { mutableStateOf<ShopImportSummary?>(null) }
    var showManualJsonInput by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var latestExportedFile by remember { mutableStateOf<File?>(null) }

    fun refreshBackups() {
        localBackups = ShopDataTransferManager.listLocalBackups(context)
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Phone Storage & Offline Sync",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                            Text(
                                text = "Export, Share & Restore Shop Packages Offline",
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

                // Action Card 1: Export Shop Data to Phone Storage
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Export Full Shop Package", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkDark)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Creates a complete offline `.beboss` data package in phone storage including all products (${products.size}), customers (${customers.size}), sales (${sales.size}), staff accounts (${allUsers.size}), and shop settings.",
                            fontSize = 11.sp,
                            color = InkMedium,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                try {
                                    val f = ShopDataTransferManager.exportShopPackage(
                                        context = context,
                                        profile = shopProfile,
                                        users = allUsers,
                                        products = products,
                                        customers = customers,
                                        sales = sales,
                                        saleItems = emptyList(), // or sale items
                                        payments = customerPayments,
                                        exportedByUser = currentUser
                                    )
                                    latestExportedFile = f
                                    refreshBackups()
                                    Toast.makeText(context, "Saved to Storage: ${f.name}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export to Phone Storage Now", fontWeight = FontWeight.Bold)
                        }

                        if (latestExportedFile != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("✓ Exported Successfully!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                        Text(latestExportedFile!!.name, fontSize = 10.sp, color = InkDark)
                                    }
                                    Button(
                                        onClick = {
                                            ShopDataTransferManager.shareBackupFile(context, latestExportedFile!!, shopProfile.shopName)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Card 2: Existing Phone Storage Backups
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("2. Saved Packages in Storage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkDark)
                            }
                            Text("${localBackups.size} Files", fontSize = 11.sp, color = InkMedium)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (localBackups.isEmpty()) {
                            Text("No saved shop packages found on device storage yet.", fontSize = 11.sp, color = InkMedium)
                        } else {
                            localBackups.take(5).forEach { backupFile ->
                                val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(backupFile.lastModified()))
                                val sizeKb = backupFile.length() / 1024

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(0.5.dp, Color(0xFFE5E7EB)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(backupFile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkDark)
                                            Text("$dateFormatted • ${sizeKb} KB", fontSize = 10.sp, color = InkMedium)
                                        }

                                        Row {
                                            // Share file
                                            IconButton(
                                                onClick = {
                                                    ShopDataTransferManager.shareBackupFile(context, backupFile, shopProfile.shopName)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                            }

                                            // Restore / Preview
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val summary = ShopDataTransferManager.parseShopPackageFile(backupFile)
                                                        selectedBackupForPreview = summary
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Cannot parse backup: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = ProfitGreen, modifier = Modifier.size(16.dp))
                                            }

                                            // Delete
                                            IconButton(
                                                onClick = {
                                                    backupFile.delete()
                                                    refreshBackups()
                                                    Toast.makeText(context, "Deleted backup file", Toast.LENGTH_SHORT).show()
                                                },
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

                Spacer(modifier = Modifier.height(14.dp))

                // Action Card 3: Import via JSON / Paste
                OutlinedButton(
                    onClick = { showManualJsonInput = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import from Raw Shop JSON Package", color = DarkNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Preview / Restore Confirmation Dialog
    if (selectedBackupForPreview != null) {
        val summary = selectedBackupForPreview!!
        val expDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(summary.exportedAt))

        AlertDialog(
            onDismissRequest = { selectedBackupForPreview = null },
            title = {
                Text("Restore Shop Package?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text("Shop: ${summary.shopName}", fontWeight = FontWeight.Bold, color = OrangePrimary, fontSize = 14.sp)
                    Text("Exported: $expDate by ${summary.exportedBy}", fontSize = 12.sp, color = InkMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Package Contents:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• ${summary.productCount} Products & Stock Quantities", fontSize = 12.sp)
                    Text("• ${summary.customerCount} Customers & Outstanding Debt", fontSize = 12.sp)
                    Text("• ${summary.saleCount} Sales Transactions", fontSize = 12.sp)
                    Text("• ${summary.paymentCount} Customer Debt Repayments", fontSize = 12.sp)
                    Text("• ${summary.userCount} Collaborator Accounts & PINs", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Are you sure you want to load and merge this data into your local database?", fontSize = 12.sp, color = InkMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportPackage(summary)
                        selectedBackupForPreview = null
                        Toast.makeText(context, "Shop Data Package Imported Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Restore & Import Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedBackupForPreview = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual JSON Import Dialog
    if (showManualJsonInput) {
        AlertDialog(
            onDismissRequest = { showManualJsonInput = false },
            title = { Text("Paste Shop Data JSON", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Paste the JSON string from a received .beboss file or worker transfer package:", fontSize = 12.sp, color = InkMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualJsonText,
                        onValueChange = { manualJsonText = it },
                        label = { Text("Raw Shop Package JSON") },
                        placeholder = { Text("{\"metadata\": ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val summary = ShopDataTransferManager.parseShopPackageString(manualJsonText.trim())
                            showManualJsonInput = false
                            selectedBackupForPreview = summary
                        } catch (e: Exception) {
                            Toast.makeText(context, "Invalid JSON format: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = manualJsonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Verify & Preview")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManualJsonInput = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
