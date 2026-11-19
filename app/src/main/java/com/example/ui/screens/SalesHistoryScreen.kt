package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.ReceiptGenerator

@Composable
fun SalesHistoryScreen(
    sales: List<Sale>,
    shopProfile: ShopProfile,
    language: AppLanguage = AppLanguage.ENGLISH,
    onOpenReceipt: (String) -> Unit,
    onDeleteSale: ((String, String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("All") }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var refundReasonInput by remember { mutableStateOf("") }
    var showSealVerifierDialog by remember { mutableStateOf(false) }
    var verifyReceiptNumberInput by remember { mutableStateOf("") }
    var verifySealInput by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val filtered = sales.filter { s ->
        val matchesMethod = selectedPaymentMethod == "All" || s.paymentMethod.equals(selectedPaymentMethod, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                s.customerName.contains(searchQuery, ignoreCase = true) ||
                s.receiptNumber.contains(searchQuery, ignoreCase = true)
        matchesMethod && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Search & Filter
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(Localization.get("search", language) + " # / " + Localization.get("customer", language), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = InkMedium) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = InkMedium)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )

                    Surface(
                        onClick = {
                            verificationResult = null
                            showSealVerifierDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = ProfitGreen.copy(alpha = 0.14f),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("verify_receipt_seal_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = Localization.get("verify_receipt_seal", language), tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("inspect", language), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val methods = listOf("All", "CASH", "MOMO", "SPLIT", "CARD", "CREDIT_DEBT")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(methods, key = { it }) { method ->
                        val isSelected = selectedPaymentMethod == method
                        val methodLabel = when (method) {
                            "All" -> Localization.get("all", language)
                            "CASH" -> Localization.get("cash", language)
                            "MOMO" -> Localization.get("momo", language)
                            "SPLIT" -> Localization.get("split_payment", language)
                            "CARD" -> Localization.get("card", language)
                            "CREDIT_DEBT" -> Localization.get("credit_debt", language)
                            else -> method
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(methodLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // List of Sales
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = InkMedium, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No sales history found", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkDark)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { sale ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReceipt(sale.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(OrangeLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = sale.customerName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = InkDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sale.receiptNumber} • ${ReceiptGenerator.formatDate(sale.saleDate)}",
                                        fontSize = 11.sp,
                                        color = InkMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF3F4F6)
                                        ) {
                                            Text(
                                                text = "${sale.paymentMethod} (${sale.paymentStatus})",
                                                fontSize = 10.sp,
                                                color = InkDark,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = ReceiptGenerator.formatMoney(sale.totalAmount, shopProfile),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = InkDark
                                    )
                                    Text(
                                        text = "+${ReceiptGenerator.formatMoney(sale.totalProfit, shopProfile)} profit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ProfitGreen
                                    )
                                }

                                if (onDeleteSale != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { saleToDelete = sale },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete / Void Sale",
                                            tint = LossRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
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

    if (saleToDelete != null) {
        val quickReasons = listOf("Customer Return", "Pricing Error", "Duplicate Entry", "Damaged Goods")
        AlertDialog(
            onDismissRequest = {
                saleToDelete = null
                refundReasonInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("refund_void_sale", language), fontWeight = FontWeight.Bold, color = LossRed)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Receipt #${saleToDelete!!.receiptNumber} (${ReceiptGenerator.formatMoney(saleToDelete!!.totalAmount, shopProfile)})\n• Stock will be atomically restored\n• Credit debt will be recalculated\n• Owner/Manager approval & audit entry recorded.",
                        fontSize = 12.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickReasons) { preset ->
                            FilterChip(
                                selected = refundReasonInput == preset,
                                onClick = { refundReasonInput = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = refundReasonInput,
                        onValueChange = { refundReasonInput = it },
                        label = { Text("Mandatory Refund / Void Reason *") },
                        placeholder = { Text("Enter audit reason...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refund_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = saleToDelete!!.id
                        val reason = refundReasonInput.trim()
                        saleToDelete = null
                        refundReasonInput = ""
                        onDeleteSale?.invoke(id, reason)
                    },
                    enabled = refundReasonInput.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    modifier = Modifier.testTag("confirm_refund_sale_btn")
                ) {
                    Text("Approve Refund & Void", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    saleToDelete = null
                    refundReasonInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Offline Receipt Seal Cryptographic Verifier Dialog
    if (showSealVerifierDialog) {
        AlertDialog(
            onDismissRequest = { showSealVerifierDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ProfitGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("verify_receipt_seal", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Verify any printed or shared BeBoss receipt offline against its tamper-evident SHA-256 BB-seal.",
                        fontSize = 12.sp,
                        color = InkMedium
                    )
                    OutlinedTextField(
                        value = verifyReceiptNumberInput,
                        onValueChange = {
                            verifyReceiptNumberInput = it
                            verificationResult = null
                        },
                        label = { Text("Receipt Number (e.g. REC-...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = verifySealInput,
                        onValueChange = {
                            verifySealInput = it.uppercase()
                            verificationResult = null
                        },
                        label = { Text("Printed Seal (e.g. BB-A1B2C3D4E5)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (verificationResult != null) {
                        val (valid, msg) = verificationResult!!
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = (if (valid) ProfitGreen else LossRed).copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (valid) ProfitGreen else LossRed,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val matched = sales.firstOrNull {
                            it.receiptNumber.equals(verifyReceiptNumberInput.trim(), ignoreCase = true)
                        }
                        verificationResult = if (matched == null) {
                            false to "NOT FOUND: No local sale matches receipt '${verifyReceiptNumberInput.trim()}'."
                        } else if (ReceiptGenerator.verifyOfflineReceiptSeal(matched, shopProfile, verifySealInput)) {
                            true to "AUTHENTIC: Receipt #${matched.receiptNumber} (${ReceiptGenerator.formatMoney(matched.totalAmount, shopProfile)}) matches cryptographic seal."
                        } else {
                            false to "TAMPERED / MISMATCH: Seal does not match local sale #${matched.receiptNumber}!"
                        }
                    },
                    enabled = verifyReceiptNumberInput.isNotBlank() && verifySealInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                ) {
                    Text("Verify Offline", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSealVerifierDialog = false }) {
                    Text(Localization.get("close", language))
                }
            }
        )
    }
}
