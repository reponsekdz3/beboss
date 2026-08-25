package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopProfile
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(
    shopProfile: ShopProfile,
    language: AppLanguage,
    onActivateVoucher: (String) -> Unit,
    onGrantEmergencyGrace: () -> Unit,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var voucherInput by remember { mutableStateOf("") }
    var voucherError by remember { mutableStateOf<String?>(null) }
    var voucherSuccess by remember { mutableStateOf<String?>(null) }

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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BeBoss Offline Merchant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${shopProfile.shopName} • 5,000 RWF / Month",
                    fontSize = 14.sp,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isActive) "Your store access is active until $expiryDateStr. All offline sales, receipts, inventory, and staff access are fully operational."
                           else "Your 14-day trial or monthly subscription has ended. Pay 5,000 RWF via MTN MoMo / Airtel Money to continue managing your shop offline.",
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

        // 2. Direct USSD Offline Payment (5,000 RWF)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dialpad, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Pay Monthly Fee (5,000 RWF)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap below to dial directly on your phone — no internet or mobile data required:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // MTN MoMo Button
                Button(
                    onClick = {
                        OfflineSubscriptionManager.dialMtnMoMo(context, 5000)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🟡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MTN MoMo Pay (5,000 RWF)",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Airtel Money Button
                Button(
                    onClick = {
                        OfflineSubscriptionManager.dialAirtelMoney(context, 5000)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔴", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Airtel Money (5,000 RWF)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Merchant Details Note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Merchant Code: ${OfflineSubscriptionManager.MOMO_MERCHANT_CODE} (BeBoss POS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MTN / Airtel Phone: 0788 765 432 / 0738 765 432",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Offline Voucher / MoMo SMS Reference Verification
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. Enter Voucher or MoMo TxID",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your activation token or the Transaction ID from your MoMo SMS:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = voucherInput,
                    onValueChange = {
                        voucherInput = it
                        voucherError = null
                    },
                    label = { Text("Voucher Token or MoMo TxID") },
                    placeholder = { Text("e.g. RW5K-2026-ACTIVE or TxID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (voucherError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(voucherError!!, color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (voucherSuccess != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(voucherSuccess!!, color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val result = OfflineSubscriptionManager.validateVoucherCode(voucherInput, shopProfile)
                        if (result.isValid) {
                            onActivateVoucher(voucherInput)
                            voucherSuccess = "${result.message} (${result.planName})"
                            voucherError = null
                            voucherInput = ""
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        } else {
                            voucherError = result.message
                            voucherSuccess = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activate & Unlock +30 Days", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. WhatsApp & Offline Support
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Instant Support & Offline Codes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "If you need an immediate monthly token or voucher code, send a quick message to our Rwanda support desk:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            OfflineSubscriptionManager.contactSupportViaWhatsApp(context, shopProfile)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🟢 WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }

                    OutlinedButton(
                        onClick = {
                            OfflineSubscriptionManager.shareAppApkOffline(context)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share APK", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGrantEmergencyGrace,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                    ) {
                        Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("grant_grace_period", language), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPaywallDialog(
    shopProfile: ShopProfile,
    language: AppLanguage,
    onActivateVoucher: (String) -> Unit,
    onGrantEmergencyGrace: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = LossRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.get("subscription_expired", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            SubscriptionScreen(
                shopProfile = shopProfile,
                language = language,
                onActivateVoucher = onActivateVoucher,
                onGrantEmergencyGrace = onGrantEmergencyGrace
            )
        },
        confirmButton = {}
    )
}
