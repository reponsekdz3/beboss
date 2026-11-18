package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.Branch
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.MoMoPaymentGateway
import com.example.util.OfflineSubscriptionManager
import com.example.util.PaymentProcessingResult
import com.example.util.ReceiptGenerator
import com.example.util.SubscriptionPriceBreakdown
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(
    shopProfile: ShopProfile,
    branches: List<Branch> = emptyList(),
    allUsers: List<User> = emptyList(),
    currentUser: User? = null,
    paymentRequests: List<PaymentVerificationRequest> = emptyList(),
    language: AppLanguage,
    onActivateVoucher: (String) -> Unit,
    onProcessDirectPayment: ((provider: String, phone: String, durationMonths: Int, onSuccess: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit) -> Unit)? = null,
    onSubmitPaymentVerification: ((provider: String, payerPhone: String, username: String, registeredPhone: String, durationMonths: Int, branchCount: Int, workerCount: Int, telcoRef: String, onDone: (PaymentVerificationRequest, Boolean) -> Unit) -> Unit)? = null,
    onCheckCloudApproval: () -> Unit = {},
    onGrantEmergencyGrace: () -> Unit,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val ownerAccount = remember(currentUser, allUsers) {
        currentUser ?: allUsers.firstOrNull { it.role.name == "OWNER" } ?: allUsers.firstOrNull()
    }

    val initialBranchCount = branches.size.coerceAtLeast(1)
    val initialWorkerCount = allUsers.size.coerceAtLeast(1)
    var selectedBranchCount by remember { mutableIntStateOf(initialBranchCount) }
    var selectedWorkerCount by remember { mutableIntStateOf(initialWorkerCount) }
    var selectedDurationMonths by remember { mutableIntStateOf(1) }

    val breakdown = remember(selectedBranchCount, selectedWorkerCount, selectedDurationMonths) {
        OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = selectedBranchCount,
            workerCount = selectedWorkerCount,
            durationMonths = selectedDurationMonths
        )
    }

    var selectedPaymentProvider by remember { mutableStateOf("MTN MoMo") }
    var usernameProofInput by remember(ownerAccount?.username) { mutableStateOf(ownerAccount?.username ?: "") }
    var registeredPhoneInput by remember(ownerAccount?.phone, shopProfile.phone) {
        mutableStateOf(ownerAccount?.phone?.ifBlank { shopProfile.phone } ?: shopProfile.phone)
    }
    var payerPhoneInput by remember(shopProfile.phone) { mutableStateOf(shopProfile.phone) }
    var telcoRefInput by remember { mutableStateOf("") }

    var voucherInput by remember { mutableStateOf("") }
    var showDirectPaymentDialog by remember { mutableStateOf(false) }
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Status Card
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "BeBoss Multi-Branch Licensing",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${shopProfile.shopName} • @${ownerAccount?.username ?: "owner"} • Expires $expiryDateStr",
                        fontSize = 12.5.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Device Code", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // 2. Dynamic Branch & Staff Tier Calculator
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
                                text = "Step 1: Choose Branch & Staff Plan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shop Workers Administered:",
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { if (selectedWorkerCount > 1) selectedWorkerCount-- },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease staff count", modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = "$selectedWorkerCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            Surface(
                                onClick = { selectedWorkerCount++ },
                                shape = CircleShape,
                                color = OrangePrimary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase staff count", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair(1, "1 Month"),
                            Pair(3, "3 Mo (-10%)"),
                            Pair(12, "1 Yr (-20%)")
                        ).forEach { (months, label) ->
                            val isSelected = selectedDurationMonths == months
                            Surface(
                                onClick = { selectedDurationMonths = months },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DarkNavy else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total USSD Payable:", fontSize = 11.5.sp, color = InkMedium, fontWeight = FontWeight.SemiBold)
                                Text("${breakdown.totalPayable} FRw", fontSize = 22.sp, fontWeight = FontWeight.Black, color = DarkNavy)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = ProfitGreen.copy(alpha = 0.15f)) {
                                Text(
                                    text = "+${selectedDurationMonths * 30} Days",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Step 2 & Step 3: Pay via Real USSD + Send Account Username & Phone to WhatsApp Admin
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Step 2: Pay via USSD & Verify on WhatsApp",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "1) Dial real USSD on your phone to pay ${breakdown.totalPayable} FRw.\n2) Confirm your created Account Username & Registered Phone, then send proof to Admin WhatsApp (${OfflineSubscriptionManager.defaultSupportWhatsApp}). Admin verifies payment on their phone and generates your unlock code from the Web Dashboard.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    // Real USSD Dial Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedPaymentProvider = "MTN MoMo"
                                OfflineSubscriptionManager.dialMtnMoMo(context, breakdown.totalPayable)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dial_mtn_ussd_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
                        ) {
                            Text(
                                text = "MTN USSD *182#",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                selectedPaymentProvider = "Airtel Money"
                                OfflineSubscriptionManager.dialAirtelMoney(context, breakdown.totalPayable)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dial_airtel_ussd_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text(
                                text = "Airtel USSD *500#",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Account Proof Sent to Admin (for Unlock Code Binding):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = usernameProofInput,
                        onValueChange = { usernameProofInput = it },
                        label = { Text("Created Account Username *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_proof_username_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = registeredPhoneInput,
                        onValueChange = { registeredPhoneInput = it },
                        label = { Text("Registered Account Phone Number *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_proof_phone_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = payerPhoneInput,
                            onValueChange = { payerPhoneInput = it },
                            label = { Text("MoMo Payer Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = telcoRefInput,
                            onValueChange = { telcoRefInput = it },
                            label = { Text("MoMo SMS Ref (Optional)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    val activeUssdCode = if (selectedPaymentProvider.contains("AIRTEL", ignoreCase = true))
                        breakdown.ussdAirtelCode else breakdown.ussdMtnCode

                    Button(
                        onClick = {
                            val cleanUser = usernameProofInput.trim().ifBlank { ownerAccount?.username ?: "owner" }
                            val cleanPhone = registeredPhoneInput.trim().ifBlank { shopProfile.phone }
                            if (onSubmitPaymentVerification != null) {
                                onSubmitPaymentVerification(
                                    selectedPaymentProvider,
                                    payerPhoneInput.ifBlank { cleanPhone },
                                    cleanUser,
                                    cleanPhone,
                                    selectedDurationMonths,
                                    selectedBranchCount,
                                    selectedWorkerCount,
                                    telcoRefInput
                                ) { req, _ ->
                                    OfflineSubscriptionManager.sendPaymentProofViaWhatsApp(
                                        context = context,
                                        shopProfile = shopProfile,
                                        ownerUsername = cleanUser,
                                        registeredPhone = cleanPhone,
                                        provider = selectedPaymentProvider,
                                        ussdCode = activeUssdCode,
                                        amountRwf = breakdown.totalPayable,
                                        durationMonths = selectedDurationMonths,
                                        branchCount = selectedBranchCount,
                                        workerCount = selectedWorkerCount,
                                        telcoReference = telcoRefInput,
                                        requestId = req.id
                                    )
                                }
                            } else {
                                OfflineSubscriptionManager.sendPaymentProofViaWhatsApp(
                                    context = context,
                                    shopProfile = shopProfile,
                                    ownerUsername = cleanUser,
                                    registeredPhone = cleanPhone,
                                    provider = selectedPaymentProvider,
                                    ussdCode = activeUssdCode,
                                    amountRwf = breakdown.totalPayable,
                                    durationMonths = selectedDurationMonths,
                                    branchCount = selectedBranchCount,
                                    workerCount = selectedWorkerCount,
                                    telcoReference = telcoRefInput
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("submit_whatsapp_proof_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit & Send Username/Phone on WhatsApp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    if (onProcessDirectPayment != null) {
                        OutlinedButton(
                            onClick = { showDirectPaymentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Instant Activation with MoMo SMS Reference ID",
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 4. Step 4: Admin Unlock Code Activation + Live Cloud Verification Queue
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Step 3: Insert Admin Unlock Code",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = onCheckCloudApproval,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync Supabase Approvals", modifier = Modifier.size(15.dp), tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                        }
                    }

                    Text(
                        text = "Enter the BB2-... unlock code generated by the Admin Web Platform (sent via WhatsApp or auto-synced via Supabase):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = voucherInput,
                        onValueChange = { voucherInput = it },
                        label = { Text("Admin Unlock Code (BB2-...) or MoMo Ref") },
                        placeholder = { Text("BB2-SINGLE-30-...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_code_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (voucherInput.isNotBlank()) {
                                onActivateVoucher(voucherInput.trim())
                                voucherInput = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("activate_unlock_code_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                        enabled = voucherInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Activate Unlock Code", fontWeight = FontWeight.Bold)
                    }

                    // Live Verification Requests History (Offline Room + Cloud Sync Status)
                    if (paymentRequests.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Recent Payment Verification Requests (${paymentRequests.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        paymentRequests.take(4).forEach { req ->
                            val isApproved = req.status == "APPROVED" && req.generatedUnlockCode.isNotBlank()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isApproved) ProfitGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${req.amountRwf} FRw • ${req.provider}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isApproved) ProfitGreen.copy(alpha = 0.15f)
                                            else if (req.status == "REDEEMED") Color(0xFF3B82F6).copy(alpha = 0.15f)
                                            else OrangePrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = req.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isApproved) ProfitGreen else if (req.status == "REDEEMED") Color(0xFF2563EB) else OrangePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${req.ownerUsername} (${req.ownerPhone}) • ${if (req.syncedToSupabase) "Synced to Supabase" else "Saved Offline (Waiting Internet)"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (req.generatedUnlockCode.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = req.generatedUnlockCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ProfitGreen,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (req.status == "APPROVED") {
                                                Button(
                                                    onClick = { onActivateVoucher(req.generatedUnlockCode) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Apply Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            OfflineSubscriptionManager.contactSupportViaWhatsApp(
                                context = context,
                                shopProfile = shopProfile,
                                branches = branches,
                                allUsers = allUsers
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProfitGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp Admin Support", color = ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onGrantEmergencyGrace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
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
    }

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
            }
        )
    }

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
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { ReceiptGenerator.shareReceipt(context, latestInvoiceText) },
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
    currentUser: User? = null,
    paymentRequests: List<PaymentVerificationRequest> = emptyList(),
    language: AppLanguage,
    onActivateVoucher: (String) -> Unit,
    onProcessDirectPayment: ((provider: String, phone: String, durationMonths: Int, onSuccess: (PaymentProcessingResult, SubscriptionPriceBreakdown) -> Unit) -> Unit)? = null,
    onSubmitPaymentVerification: ((provider: String, payerPhone: String, username: String, registeredPhone: String, durationMonths: Int, branchCount: Int, workerCount: Int, telcoRef: String, onDone: (PaymentVerificationRequest, Boolean) -> Unit) -> Unit)? = null,
    onCheckCloudApproval: () -> Unit = {},
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
                currentUser = currentUser,
                paymentRequests = paymentRequests,
                language = language,
                onActivateVoucher = onActivateVoucher,
                onProcessDirectPayment = onProcessDirectPayment,
                onSubmitPaymentVerification = onSubmitPaymentVerification,
                onCheckCloudApproval = onCheckCloudApproval,
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
    onPaymentCompleted: (PaymentProcessingResult) -> Unit
) {
    var selectedProvider by remember { mutableStateOf("MTN MoMo") }
    var phoneInput by remember { mutableStateOf(shopProfile.phone) }
    var smsRefInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val activeUssdCode = if (selectedProvider.contains("AIRTEL", ignoreCase = true)) {
        breakdown.ussdAirtelCode
    } else {
        breakdown.ussdMtnCode
    }

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
                        text = "MoMo SMS Settlement",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )
                    if (!isProcessing) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = InkMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = breakdown.branchTierName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                        Text(text = breakdown.workerTierName, fontSize = 11.sp, color = InkMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total: ${breakdown.totalPayable} FRw (${breakdown.durationMonths * 30} Days)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MTN MoMo", "Airtel Money").forEach { prov ->
                        val isSelected = selectedProvider == prov
                        Surface(
                            onClick = {
                                if (!isProcessing) {
                                    selectedProvider = prov
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                if (prov == "MTN MoMo") Color(0xFFFFCC00) else Color(0xFFDC2626)
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        phoneInput = it
                        errorMessage = null
                    },
                    label = { Text("Payer Phone Number (078... / 073...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        MoMoPaymentGateway.openUssdDialer(context, activeUssdCode)
                        statusMessage = "Dialer launched for $selectedProvider ($activeUssdCode). Complete payment and paste your SMS reference ID below."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dial USSD ($activeUssdCode)", fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = smsRefInput,
                    onValueChange = {
                        smsRefInput = it
                        errorMessage = null
                    },
                    label = { Text("Telco SMS Financial Ref (e.g. MP260903...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = errorMessage ?: "", color = LossRed, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    }
                }

                if (statusMessage != null && errorMessage == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = statusMessage ?: "", color = DarkNavy, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying SMS financial reference...", fontSize = 11.5.sp, color = DarkNavy)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        errorMessage = null
                        coroutineScope.launch {
                            val pastRefs = setOfNotNull(shopProfile.lastPaymentRef)
                            val result = OfflineSubscriptionManager.processMoMoPaymentAsync(
                                context = context,
                                shopProfile = shopProfile,
                                payerPhone = phoneInput,
                                provider = selectedProvider,
                                branchCount = breakdown.branchCount,
                                workerCount = breakdown.workerCount,
                                durationMonths = breakdown.durationMonths,
                                userEnteredReference = smsRefInput.ifBlank { null },
                                alreadyUsedRefs = pastRefs
                            )
                            isProcessing = false
                            if (result.isSuccess) {
                                onPaymentCompleted(result)
                            } else {
                                errorMessage = result.message
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = !isProcessing && (phoneInput.isNotBlank() || smsRefInput.isNotBlank())
                ) {
                    Text(
                        text = if (isProcessing) "Verifying..." else "Verify SMS & Activate ${breakdown.totalPayable} FRw",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
