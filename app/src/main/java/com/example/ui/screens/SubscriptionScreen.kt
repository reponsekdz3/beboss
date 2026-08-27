package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Branch
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.OfflineSubscriptionManager
import com.example.util.PaymentProcessingResult
import com.example.util.ReceiptGenerator
import com.example.util.SubscriptionPriceBreakdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(
    shopProfile: ShopProfile,
    branches: List<Branch> = emptyList(),
    allUsers: List<User> = emptyList(),
    language: AppLanguage,
    onActivateVoucher: (String) -> Unit,
    onProcessDirectPayment: ((provider: String, phone: String, durationMonths: Int, onSuccess: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit) -> Unit)? = null,
    onGrantEmergencyGrace: () -> Unit,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Configurable branch & staff pricing calculator state
    val initialBranchCount = branches.size.coerceAtLeast(1)
    val initialWorkerCount = allUsers.size.coerceAtLeast(1)
    var selectedBranchCount by remember { mutableIntStateOf(initialBranchCount) }
    var selectedWorkerCount by remember { mutableIntStateOf(initialWorkerCount) }
    var selectedDurationMonths by remember { mutableIntStateOf(1) } // 1, 3, 12

    // Dynamic Price Calculation
    val breakdown = remember(selectedBranchCount, selectedWorkerCount, selectedDurationMonths) {
        OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = selectedBranchCount,
            workerCount = selectedWorkerCount,
            durationMonths = selectedDurationMonths
        )
    }

    var voucherInput by remember { mutableStateOf("") }
    var showDirectPaymentDialog by remember { mutableStateOf(false) }
    var selectedPaymentProvider by remember { mutableStateOf("MTN MoMo") }
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var latestInvoiceText by remember { mutableStateOf("") }

    val daysLeft = shopProfile.daysRemaining
    val isActive = shopProfile.isSubscriptionActive
    val deviceCode = OfflineSubscriptionManager.getDeviceChallengeCode(shopProfile)
    val expiryDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(shopProfile.subscriptionExpiresAt))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Subscription Hero Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official Brand Logo
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(54.dp),
                    shadowElevation = 4.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beboss_app_logo_1787833759468),
                        contentDescription = "BeBoss Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isActive) ProfitGreen else LossRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isActive) ProfitGreen else LossRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isActive) "${Localization.get("active_subscription", language)} ($daysLeft ${Localization.get("days_left", language)})"
                                   else Localization.get("subscription_expired", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) ProfitGreen else LossRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "BeBoss Multi-Branch Merchant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${shopProfile.shopName} • ${branches.size.coerceAtLeast(1)} Branches • ${allUsers.size.coerceAtLeast(1)} Staff",
                    fontSize = 13.sp,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isActive) "Store access is fully active until $expiryDateStr. Multi-branch inventory, cashier POS, debts, and cloud sync are operational."
                           else "Subscription expired. Choose your branch and staff tier below to renew instantly via MoMo, Airtel, or offline voucher.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Shop Device Challenge Code Tag
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Device Code", deviceCode))
                        Toast.makeText(context, "Copied Device Code: $deviceCode", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${Localization.get("device_code", language)}: $deviceCode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 2. Dynamic Pricing Engine (Branches + Workers Administration)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
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
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Branch & Staff Tier Calculator",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OrangePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Dynamic Rates",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Branch Count Selector (1 Branch = 5k, 2 Branches = 10k, 3+ Branches = 20k)
                Text(
                    text = "1. Active Branches Managed in App:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(1, "1 Branch", "5,000 FRw"),
                        Triple(2, "2 Branches", "10,000 FRw"),
                        Triple(3, "3+ Branches", "20,000 FRw")
                    ).forEach { (count, label, priceTag) ->
                        val isSelected = (count == 1 && selectedBranchCount == 1) ||
                                         (count == 2 && selectedBranchCount == 2) ||
                                         (count == 3 && selectedBranchCount >= 3)
                        Surface(
                            onClick = { selectedBranchCount = count },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) OrangePrimary else Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = priceTag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color(0xFFFFE082) else OrangePrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Worker / Staff Count Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "2. Shop Workers / Cashiers Administered:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = breakdown.workerTierName,
                            fontSize = 11.sp,
                            color = OrangePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Plus / Minus Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { if (selectedWorkerCount > 1) selectedWorkerCount-- },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = "$selectedWorkerCount",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Surface(
                            onClick = { selectedWorkerCount++ },
                            shape = CircleShape,
                            color = OrangePrimary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Billing Cycle Duration: 1 Month, 3 Months (-10%), 1 Year (-20%)
                Text(
                    text = "3. Select Billing Cycle:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(1, "1 Month (Standard)"),
                        Pair(3, "3 Months (Save 10%)"),
                        Pair(12, "1 Year (Save 20%)")
                    ).forEach { (months, label) ->
                        val isSelected = selectedDurationMonths == months
                        Surface(
                            onClick = { selectedDurationMonths = months },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) DarkNavy else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Total Summary Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Payable Fee:",
                                    fontSize = 12.sp,
                                    color = InkMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${breakdown.totalPayable} FRw",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DarkNavy
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ProfitGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${selectedDurationMonths * 30} Days Access",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = OrangePrimary.copy(alpha = 0.2f))

                        Text(
                            text = "• Branch Base: ${breakdown.branchBasePrice} FRw  • Staff Fee: ${breakdown.workerFee} FRw/mo",
                            fontSize = 11.sp,
                            color = InkDark
                        )
                        if (breakdown.discountAmount > 0) {
                            Text(
                                text = "• Discount Applied: -${breakdown.discountAmount} FRw (${breakdown.discountPercent}% OFF)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }
        }

        // 3. Instant Payment Actions (MoMo / Airtel / Card / USSD)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay & Activate Instantly",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose direct in-app mobile money push or dial offline USSD:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Direct In-App Push Modal Button
                Button(
                    onClick = { showDirectPaymentDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pay ${breakdown.totalPayable} FRw via MoMo / Airtel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick USSD Offline Buttons (MTN & Airtel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            OfflineSubscriptionManager.dialMtnMoMo(context, breakdown.totalPayable)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
                    ) {
                        Text(
                            text = "MTN Dial *182#",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            OfflineSubscriptionManager.dialAirtelMoney(context, breakdown.totalPayable)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text(
                            text = "Airtel Dial *500#",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Support Help Button
                OutlinedButton(
                    onClick = {
                        OfflineSubscriptionManager.contactSupportViaWhatsApp(
                            context = context,
                            shopProfile = shopProfile,
                            branches = branches,
                            allUsers = allUsers
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProfitGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contact BeBoss WhatsApp Support",
                        color = ProfitGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 4. Offline Voucher & SMS Reference Code Activation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Activation Voucher or TxID",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paste voucher code from agent or MoMo SMS transaction ID to activate offline:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = voucherInput,
                    onValueChange = { voucherInput = it },
                    label = { Text("Code (e.g. RW10K-2026-ACTIVE or MoMo TxID)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Keys Helper Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("RW10K-2026-ACTIVE", "RW20K-2026-ACTIVE", "BEBOSS-ANNUAL-VIP").forEach { sampleKey ->
                        Surface(
                            onClick = { voucherInput = sampleKey },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = sampleKey.take(12) + "...",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (voucherInput.isNotBlank()) {
                            onActivateVoucher(voucherInput.trim())
                            voucherInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                    enabled = voucherInput.isNotBlank()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify & Activate Voucher", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Emergency 3-Day Grace Button
                OutlinedButton(
                    onClick = onGrantEmergencyGrace,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                ) {
                    Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp), tint = LossRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Emergency 3-Day Grace Extension", fontSize = 12.sp, color = LossRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Direct MoMo Payment Interactive Simulation Modal
    if (showDirectPaymentDialog) {
        DirectMoMoPaymentDialog(
            shopProfile = shopProfile,
            breakdown = breakdown,
            onDismiss = { showDirectPaymentDialog = false },
            onPaymentCompleted = { result ->
                showDirectPaymentDialog = false
                val invoice = ReceiptGenerator.generateSubscriptionInvoice(
                    profile = shopProfile,
                    breakdown = breakdown,
                    txRef = result.transactionRef,
                    provider = result.provider,
                    payerPhone = result.payerPhone,
                    planDays = result.planDays
                )
                latestInvoiceText = invoice
                showInvoiceDialog = true
            },
            onProcessDirectPayment = onProcessDirectPayment
        )
    }

    // Digital Invoice / Receipt Certificate Viewer
    if (showInvoiceDialog) {
        AlertDialog(
            onDismissRequest = { showInvoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Official Subscription Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = latestInvoiceText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ReceiptGenerator.shareReceipt(context, latestInvoiceText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Print Invoice")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showInvoiceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SubscriptionPaywallDialog(
    shopProfile: ShopProfile,
    branches: List<Branch> = emptyList(),
    allUsers: List<User> = emptyList(),
    language: AppLanguage,
    onActivateVoucher: (String) -> Unit,
    onProcessDirectPayment: ((provider: String, phone: String, durationMonths: Int, onSuccess: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit) -> Unit)? = null,
    onGrantEmergencyGrace: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissible when expired */ },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = LossRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.get("subscription_expired", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = LossRed
                )
            }
        },
        text = {
            SubscriptionScreen(
                shopProfile = shopProfile,
                branches = branches,
                allUsers = allUsers,
                language = language,
                onActivateVoucher = onActivateVoucher,
                onProcessDirectPayment = onProcessDirectPayment,
                onGrantEmergencyGrace = onGrantEmergencyGrace
            )
        },
        confirmButton = {}
    )
}

@Composable
private fun DirectMoMoPaymentDialog(
    shopProfile: ShopProfile,
    breakdown: SubscriptionPriceBreakdown,
    onDismiss: () -> Unit,
    onPaymentCompleted: (PaymentProcessingResult) -> Unit,
    onProcessDirectPayment: ((provider: String, phone: String, durationMonths: Int, onSuccess: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit) -> Unit)?
) {
    var selectedProvider by remember { mutableStateOf("MTN MoMo") }
    var phoneInput by remember { mutableStateOf(shopProfile.phone) }
    var isProcessing by remember { mutableStateOf(false) }
    var stepMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pay Subscription Online",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    if (!isProcessing) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = InkMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount to pay badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Plan: ${breakdown.branchTierName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                        Text(
                            text = "Staff: ${breakdown.workerTierName}",
                            fontSize = 11.sp,
                            color = InkMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total: ${breakdown.totalPayable} FRw (${breakdown.durationMonths * 30} Days)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Provider Selection
                Text("Select Payment Gateway:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MTN MoMo", "Airtel Money", "BK Quick").forEach { prov ->
                        val isSelected = selectedProvider == prov
                        Surface(
                            onClick = { if (!isProcessing) selectedProvider = prov },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                when (prov) {
                                    "MTN MoMo" -> Color(0xFFFFCC00)
                                    "Airtel Money" -> Color(0xFFDC2626)
                                    else -> DarkNavy
                                }
                            } else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = prov,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    if (prov == "MTN MoMo") Color(0xFF0F172A) else Color.White
                                } else InkDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Account Phone Number (+250...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing
                )

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = ProfitGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stepMessage,
                                fontSize = 12.sp,
                                color = DarkNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        coroutineScope.launch {
                            stepMessage = "Connecting to $selectedProvider gateway..."
                            delay(1000)
                            stepMessage = "Prompting PIN on $phoneInput..."
                            delay(1200)
                            stepMessage = "Payment confirmed! Extending shop access..."
                            delay(800)

                            if (onProcessDirectPayment != null) {
                                onProcessDirectPayment(
                                    selectedProvider,
                                    phoneInput,
                                    breakdown.durationMonths
                                ) { result, _ ->
                                    onPaymentCompleted(result)
                                }
                            } else {
                                val result = OfflineSubscriptionManager.processDirectMoMoPayment(
                                    shopProfile = shopProfile,
                                    payerPhone = phoneInput,
                                    provider = selectedProvider,
                                    branchCount = breakdown.branchCount,
                                    workerCount = breakdown.workerCount,
                                    durationMonths = breakdown.durationMonths
                                )
                                onPaymentCompleted(result)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = !isProcessing && phoneInput.isNotBlank()
                ) {
                    Text(
                        text = if (isProcessing) "Processing Payment..." else "Authorize & Pay ${breakdown.totalPayable} FRw",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
