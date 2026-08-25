package com.example.ui.components

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.ReceiptGenerator

@Composable
fun ReceiptDialog(
    saleWithItems: SaleWithItems,
    shopProfile: ShopProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sale = saleWithItems.sale
    val items = saleWithItems.items

    val textReceipt = ReceiptGenerator.generateTextReceipt(sale, items, shopProfile)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = ProfitGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sale Completed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = InkDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paper Receipt Visual Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shopProfile.shopName.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OrangePrimary,
                            textAlign = TextAlign.Center
                        )
                        if (shopProfile.address.isNotBlank()) {
                            Text(
                                text = shopProfile.address,
                                fontSize = 12.sp,
                                color = InkMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (shopProfile.phone.isNotBlank()) {
                            Text(
                                text = "Tel: ${shopProfile.phone}",
                                fontSize = 12.sp,
                                color = InkMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color(0xFFD1D5DB)
                        )

                        // Meta details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Receipt #:", fontSize = 12.sp, color = InkMedium)
                            Text(sale.receiptNumber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date:", fontSize = 12.sp, color = InkMedium)
                            Text(ReceiptGenerator.formatDate(sale.saleDate), fontSize = 12.sp, color = InkDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Customer:", fontSize = 12.sp, color = InkMedium)
                            Text(sale.customerName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment:", fontSize = 12.sp, color = InkMedium)
                            Text("${sale.paymentMethod} (${sale.paymentStatus})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkDark)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color(0xFFD1D5DB)
                        )

                        // Items list
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = InkDark
                                    )
                                    Text(
                                        text = "${item.quantitySold} ${item.unit} x ${ReceiptGenerator.formatMoney(item.unitPriceAtSale, shopProfile)}",
                                        fontSize = 11.sp,
                                        color = InkMedium
                                    )
                                }
                                Text(
                                    text = ReceiptGenerator.formatMoney(item.subtotal, shopProfile),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkDark
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.5.dp,
                            color = InkDark
                        )

                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                            Text(
                                text = ReceiptGenerator.formatMoney(sale.totalAmount, shopProfile),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OrangePrimary
                            )
                        }

                        if (sale.discountAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount:", fontSize = 12.sp, color = InkMedium)
                                Text("-${ReceiptGenerator.formatMoney(sale.discountAmount, shopProfile)}", fontSize = 12.sp, color = ProfitGreen)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount Paid:", fontSize = 12.sp, color = InkMedium)
                            Text(ReceiptGenerator.formatMoney(sale.amountPaid, shopProfile), fontSize = 12.sp, color = InkDark)
                        }

                        val debt = sale.totalAmount - sale.amountPaid
                        if (debt > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Debt / Balance Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                Text(ReceiptGenerator.formatMoney(debt, shopProfile), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = shopProfile.receiptFooter,
                            fontSize = 11.sp,
                            color = InkMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Share Text/WhatsApp, Share PDF Invoice, Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val pdf = com.example.util.PdfReportGenerator.generateInvoiceReceiptPdf(
                                    context = context,
                                    sale = sale,
                                    items = items,
                                    shopProfile = shopProfile
                                )
                                com.example.util.PdfReportGenerator.sharePdf(
                                    context = context,
                                    file = pdf,
                                    title = "Invoice ${sale.receiptNumber}"
                                )
                            } catch (e: Exception) {
                                ReceiptGenerator.shareReceipt(context, textReceipt)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            printHtmlReceipt(context, ReceiptGenerator.generateHtmlReceipt(sale, items, shopProfile), sale.receiptNumber)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print POS")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(textReceipt))
                        Toast.makeText(context, "Receipt text copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Plain Text / SMS Format")
                }
            }
        }
    }
}

private fun printHtmlReceipt(context: Context, htmlContent: String, jobName: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch print service: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
