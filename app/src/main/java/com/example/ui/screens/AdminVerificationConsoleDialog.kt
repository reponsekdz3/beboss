package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ShopProfile
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.OfflineSubscriptionManager
import com.example.util.PdfReportGenerator
import com.example.util.SupabaseAdminClient
import com.example.util.SupabaseAdminSession
import com.example.util.SupabaseAuditLogDto
import com.example.util.SupabaseBranchSnapshotDto
import com.example.util.SupabasePaymentRequestDto
import com.example.util.SupabaseShopAccountDto
import com.example.util.SupabaseShopUserDto
import com.example.util.SupabaseUnlockCodeDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminVerificationConsoleDialog(
    shopProfile: ShopProfile,
    adminSession: SupabaseAdminSession?,
    adminAuthError: String?,
    isAdminAuthenticating: Boolean,
    paymentRequests: List<SupabasePaymentRequestDto>,
    shopAccounts: List<SupabaseShopAccountDto>,
    shopUsers: List<SupabaseShopUserDto> = emptyList(),
    branchSnapshots: List<SupabaseBranchSnapshotDto> = emptyList(),
    unlockCodes: List<SupabaseUnlockCodeDto>,
    auditLogs: List<SupabaseAuditLogDto>,
    isLoading: Boolean,
    onSignInAdmin: (email: String, password: String) -> Unit,
    onSignUpAdmin: (email: String, password: String, fullName: String) -> Unit,
    onSignOutAdmin: () -> Unit,
    onRefresh: () -> Unit,
    onApprovePayment: (SupabasePaymentRequestDto, (Boolean, String) -> Unit) -> Unit,
    onRejectPayment: (SupabasePaymentRequestDto, String) -> Unit,
    onIssueManualCode: (username: String, registeredPhone: String, tierCode: String, days: Int, amountRwf: Int, onDone: (Boolean, String) -> Unit) -> Unit,
    onRevokeCode: (SupabaseUnlockCodeDto) -> Unit,
    onExtendShopSubscription: (SupabaseShopAccountDto, Int) -> Unit = { _, _ -> },
    onSetShopStatus: (SupabaseShopAccountDto, String) -> Unit = { _, _ -> },
    onDeleteShopAccount: (SupabaseShopAccountDto) -> Unit = {},
    onUpdateShopUserRole: (SupabaseShopUserDto, String) -> Unit = { _, _ -> },
    onToggleShopUserActive: (SupabaseShopUserDto, Boolean) -> Unit = { _, _ -> },
    onDeleteShopUser: (SupabaseShopUserDto) -> Unit = {},
    onDeleteBranchSnapshot: (SupabaseBranchSnapshotDto) -> Unit = {},
    onShareWebPortal: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var lastGeneratedCode by remember { mutableStateOf<String?>(null) }
    var lastTargetPhone by remember { mutableStateOf("") }
    var lastTargetUsername by remember { mutableStateOf("") }
    var lastTargetAmount by remember { mutableIntStateOf(0) }
    var lastTargetDays by remember { mutableIntStateOf(30) }

    // Single Master Admin Auth Form State (Email locked strictly to MASTER_ADMIN_EMAIL)
    var isRegisterMode by remember { mutableStateOf(false) }
    val masterEmail = SupabaseAdminClient.MASTER_ADMIN_EMAIL
    var adminPassword by remember { mutableStateOf("") }
    var adminFullName by remember { mutableStateOf("BeBoss Master Admin") }
    var showAdminPassword by remember { mutableStateOf(false) }

    // Search filters for Users & Shops
    var userSearchQuery by remember { mutableStateOf("") }
    var shopSearchQuery by remember { mutableStateOf("") }

    // Reject Modal State
    var requestToReject by remember { mutableStateOf<SupabasePaymentRequestDto?>(null) }
    var rejectReasonInput by remember { mutableStateOf("Payment reference not found on MoMo statement") }

    // Manual Generator Form State
    var manualUsername by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualTier by remember { mutableStateOf("SINGLE") }
    var manualDaysText by remember { mutableStateOf("30") }
    var manualAmountText by remember { mutableStateOf("5000") }

    val dateFormatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 700.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = OrangePrimary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BeBoss Master Admin Console",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (adminSession != null) {
                                    "Master Session: ${adminSession.email}"
                                } else {
                                    "Single Master Admin: $masterEmail"
                                },
                                fontSize = 11.sp,
                                color = if (adminSession != null) ProfitGreen else OrangePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close admin console")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (adminSession == null) {
                    // SINGLE MASTER ADMIN AUTH GATE
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isRegisterMode = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_auth_signin_tab"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (!isRegisterMode) OrangePrimary.copy(alpha = 0.14f) else Color.Transparent
                                    )
                                ) {
                                    Text("Master Sign In", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { isRegisterMode = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_auth_signup_tab"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isRegisterMode) OrangePrimary.copy(alpha = 0.14f) else Color.Transparent
                                    )
                                ) {
                                    Text("First-Time Setup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Text(
                                text = if (isRegisterMode) {
                                    "Initialize the single authorized Supabase Master Admin account ($masterEmail) in admin_profiles."
                                } else {
                                    "Sign in with the Master Administrator credentials for $masterEmail to manage all shops, staff users, subscriptions, and unlock codes."
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = adminFullName,
                                    onValueChange = { adminFullName = it },
                                    label = { Text("Master Admin Name *") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangePrimary) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_fullname_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = masterEmail,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Authorized Master Admin Email (Locked)") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OrangePrimary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_email_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it },
                                label = { Text("Admin Password *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangePrimary) },
                                trailingIcon = {
                                    IconButton(onClick = { showAdminPassword = !showAdminPassword }) {
                                        Icon(
                                            imageVector = if (showAdminPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (showAdminPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (!adminAuthError.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LossRed.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = adminAuthError,
                                        color = LossRed,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (isRegisterMode) {
                                        onSignUpAdmin(masterEmail, adminPassword, adminFullName.trim())
                                    } else {
                                        onSignInAdmin(masterEmail, adminPassword)
                                    }
                                },
                                enabled = !isAdminAuthenticating && adminPassword.length >= 6 && (!isRegisterMode || adminFullName.isNotBlank()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_auth_submit_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                if (isAdminAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticating...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRegisterMode) "Initialize Master Admin & Enter" else "Sign In as Master Admin",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onShareWebPortal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_export_portal_unauth_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangePrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Web Admin Portal (admin_portal.html)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }
                        }
                    }
                } else {
                    // AUTHENTICATED MASTER ADMIN WORKSPACE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_sync_supabase_btn")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync All", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onShareWebPortal,
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Web Portal", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSignOutAdmin,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("admin_signout_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out admin", tint = LossRed, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val pendingCount = paymentRequests.count { it.status == "PENDING_VERIFICATION" }
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = OrangePrimary,
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Queue ($pendingCount/${paymentRequests.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Users (${shopUsers.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Shops (${shopAccounts.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Issue Code", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("Codes (${unlockCodes.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            text = { Text("Branches (${branchSnapshots.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 6,
                            onClick = { selectedTab = 6 },
                            text = { Text("Audit (${auditLogs.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (lastGeneratedCode != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ProfitGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Latest Issued Unlock Code (@$lastTargetUsername)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                Text(
                                    text = lastGeneratedCode!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("BeBoss Unlock Code", lastGeneratedCode!!))
                                            Toast.makeText(context, "Unlock code copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val msg = OfflineSubscriptionManager.buildAdminUnlockReplyWhatsAppMessage(
                                                shopName = shopProfile.shopName.ifBlank { "BeBoss Shop" },
                                                username = lastTargetUsername,
                                                unlockCode = lastGeneratedCode!!,
                                                daysAdded = lastTargetDays,
                                                tierName = "$manualTier ($lastTargetAmount FRw)"
                                            )
                                            PdfReportGenerator.sendWhatsAppDirect(context, lastTargetPhone, msg)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    when (selectedTab) {
                        0 -> {
                            // Payment Verification Queue
                            if (paymentRequests.isEmpty()) {
                                Text(
                                    text = "No Supabase payment requests found yet. Pull 'Sync All' or switch to 'Issue Code'.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                paymentRequests.forEach { req ->
                                    val isApproved = req.status == "APPROVED" && req.generatedUnlockCode.isNotBlank()
                                    val isRejected = req.status == "REJECTED"
                                    val expectedFee = OfflineSubscriptionManager.calculateSubscriptionPrice(
                                        branchCount = req.branchCount.coerceAtLeast(1),
                                        workerCount = req.workerCount.coerceAtLeast(1),
                                        durationMonths = req.durationMonths.coerceAtLeast(1)
                                    ).totalPayable
                                    val feeOk = req.amountRwf >= expectedFee

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isApproved -> ProfitGreen.copy(alpha = 0.45f)
                                                isRejected -> LossRed.copy(alpha = 0.45f)
                                                else -> OrangePrimary.copy(alpha = 0.4f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(req.shopName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (feeOk) ProfitGreen.copy(alpha = 0.14f) else WarningAmber.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = if (feeOk) "FEE OK" else "CHECK ($expectedFee)",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (feeOk) ProfitGreen else WarningAmber,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = req.status,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = when {
                                                            isApproved -> ProfitGreen
                                                            isRejected -> LossRed
                                                            else -> OrangePrimary
                                                        }
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "User: @${req.ownerUsername} • Reg Phone: ${req.ownerPhone}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${req.provider} • ${req.amountRwf} FRw (${req.durationMonths * 30}d, ${req.branchCount} br / ${req.workerCount} stf)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Payer: ${req.payerPhone} • Ref: ${req.telcoReference.ifBlank { "WhatsApp Proof" }}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (isApproved) {
                                                Text(
                                                    text = "Code: ${req.generatedUnlockCode}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ProfitGreen
                                                )
                                            } else if (isRejected && req.rejectionReason.isNotBlank()) {
                                                Text(
                                                    text = "Rejected: ${req.rejectionReason}",
                                                    fontSize = 11.sp,
                                                    color = LossRed,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        onApprovePayment(req) { _, code ->
                                                            lastGeneratedCode = code
                                                            lastTargetUsername = req.ownerUsername
                                                            lastTargetPhone = req.ownerPhone
                                                            lastTargetAmount = req.amountRwf
                                                            lastTargetDays = req.durationMonths * 30
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1.4f)
                                                        .heightIn(min = 48.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (isApproved) DarkNavy else ProfitGreen)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isApproved) "Re-send Code" else "Verify & Issue BB2",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                if (!isApproved && !isRejected) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            requestToReject = req
                                                            rejectReasonInput = "MoMo transaction reference mismatch"
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp),
                                                        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.6f))
                                                    ) {
                                                        Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // ALL SHOP USERS DIRECTORY & ROLE / ACCESS MANAGEMENT
                            OutlinedTextField(
                                value = userSearchQuery,
                                onValueChange = { userSearchQuery = it },
                                label = { Text("Search users by name, @username, shop or role") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("admin_user_search_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            val filteredUsers = shopUsers.filter { u ->
                                val q = userSearchQuery.trim().lowercase(Locale.ROOT)
                                q.isBlank() ||
                                    u.fullName.lowercase(Locale.ROOT).contains(q) ||
                                    u.username.lowercase(Locale.ROOT).contains(q) ||
                                    u.shopName.lowercase(Locale.ROOT).contains(q) ||
                                    u.role.lowercase(Locale.ROOT).contains(q) ||
                                    u.phone.contains(q)
                            }

                            if (filteredUsers.isEmpty()) {
                                Text(
                                    text = "No users synced yet. Shops automatically upload staff profiles (without passwords) when syncing online.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                filteredUsers.forEach { u ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (u.isActive) MaterialTheme.colorScheme.outlineVariant else LossRed.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${u.fullName} (@${u.username})",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${u.shopName} • ${u.assignedBranchName} • ${u.phone.ifBlank { "No phone" }}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = (if (u.isActive) ProfitGreen else LossRed).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (u.isActive) u.role else "SUSPENDED",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (u.isActive) ProfitGreen else LossRed,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            // Role Switcher Row
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("OWNER", "MANAGER", "CASHIER").forEach { roleOpt ->
                                                    val isCurrent = u.role == roleOpt
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (!isCurrent) onUpdateShopUserRole(u, roleOpt)
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 40.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            containerColor = if (isCurrent) OrangePrimary.copy(alpha = 0.16f) else Color.Transparent
                                                        )
                                                    ) {
                                                        Text(
                                                            text = roleOpt,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCurrent) OrangePrimary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            // Suspend/Activate & Delete Row
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { onToggleShopUserActive(u, !u.isActive) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (u.isActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                        contentDescription = null,
                                                        tint = if (u.isActive) WarningAmber else ProfitGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = if (u.isActive) "Suspend" else "Activate",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (u.isActive) WarningAmber else ProfitGreen
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { onDeleteShopUser(u) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp),
                                                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.55f))
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // ALL SHOPS LICENSING & STATUS MANAGEMENT
                            OutlinedTextField(
                                value = shopSearchQuery,
                                onValueChange = { shopSearchQuery = it },
                                label = { Text("Filter shops by name, @owner or phone") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            val filteredShops = shopAccounts.filter { s ->
                                val q = shopSearchQuery.trim().lowercase(Locale.ROOT)
                                q.isBlank() ||
                                    s.shopName.lowercase(Locale.ROOT).contains(q) ||
                                    s.ownerUsername.lowercase(Locale.ROOT).contains(q) ||
                                    s.ownerPhone.contains(q)
                            }

                            if (filteredShops.isEmpty()) {
                                Text(
                                    text = "No Supabase shop accounts synced yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                filteredShops.forEach { acc ->
                                    val daysLeft = ((acc.subscriptionExpiresAt - System.currentTimeMillis()) / 86_400_000L).toInt()
                                    val isSuspended = acc.subscriptionStatus == "SUSPENDED"
                                    val statusColor = when {
                                        isSuspended -> LossRed
                                        acc.subscriptionStatus == "ACTIVE" && daysLeft > 0 -> ProfitGreen
                                        else -> WarningAmber
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Default.Store, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("${acc.shopName} (@${acc.ownerUsername})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                Text(
                                                    text = if (isSuspended) "SUSPENDED" else "${acc.subscriptionStatus} (${daysLeft.coerceAtLeast(0)}d)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = statusColor
                                                )
                                            }
                                            Text(
                                                text = "Phone: ${acc.ownerPhone} • ${acc.branchCount} br / ${acc.workerCount} stf • ${acc.monthlyFeeRwf} FRw/mo",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { onExtendShopSubscription(acc, 30) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                                                ) {
                                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("+30d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        manualUsername = acc.ownerUsername
                                                        manualPhone = acc.ownerPhone
                                                        manualAmountText = acc.monthlyFeeRwf.toString()
                                                        selectedTab = 3
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("BB2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        onSetShopStatus(acc, if (isSuspended) "ACTIVE" else "SUSPENDED")
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isSuspended) "Activate" else "Suspend",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSuspended) ProfitGreen else WarningAmber
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onDeleteShopAccount(acc) },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete shop", tint = LossRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Manual BB2 Unlock Code Generator
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualUsername,
                                    onValueChange = { manualUsername = it },
                                    label = { Text("Shop Owner Created Username *") },
                                    placeholder = { Text("e.g. jeanpaul") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = manualPhone,
                                    onValueChange = { manualPhone = it },
                                    label = { Text("Owner Registered Phone *") },
                                    placeholder = { Text("+250 788 123 456") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("SINGLE", "MULTI", "PRO").forEach { tier ->
                                        val selected = manualTier == tier
                                        OutlinedButton(
                                            onClick = {
                                                manualTier = tier
                                                manualAmountText = when (tier) {
                                                    "SINGLE" -> "5000"
                                                    "MULTI" -> "10000"
                                                    else -> "20000"
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (selected) OrangePrimary.copy(alpha = 0.15f) else Color.Transparent
                                            )
                                        ) {
                                            Text(tier, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) OrangePrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = manualDaysText,
                                        onValueChange = { manualDaysText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Days") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = manualAmountText,
                                        onValueChange = { manualAmountText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Amount (FRw)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val days = manualDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                                        val amt = manualAmountText.toIntOrNull() ?: 5000
                                        onIssueManualCode(manualUsername.trim(), manualPhone.trim(), manualTier, days, amt) { _, code ->
                                            lastGeneratedCode = code
                                            lastTargetUsername = manualUsername.trim()
                                            lastTargetPhone = manualPhone.trim()
                                            lastTargetAmount = amt
                                            lastTargetDays = days
                                        }
                                    },
                                    enabled = manualUsername.isNotBlank() && manualPhone.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_generate_manual_bb2_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Account-Bound BB2 Unlock Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        4 -> {
                            // Unlock Codes Ledger with Revoke
                            if (unlockCodes.isEmpty()) {
                                Text(
                                    text = "No BB2 unlock codes recorded in Supabase yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                unlockCodes.forEach { codeDto ->
                                    val isRevoked = codeDto.status == "REVOKED"
                                    val isRedeemed = codeDto.status == "REDEEMED"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                isRevoked -> LossRed.copy(alpha = 0.4f)
                                                isRedeemed -> ProfitGreen.copy(alpha = 0.4f)
                                                else -> MaterialTheme.colorScheme.outlineVariant
                                            }
                                        ),
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
                                                Text(
                                                    text = codeDto.unlockCode,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRevoked) LossRed else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "@${codeDto.shopUsername} • ${codeDto.tierCode} (${codeDto.planDays}d / ${codeDto.amountRwf} FRw) • ${codeDto.status}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!isRevoked) {
                                                OutlinedButton(
                                                    onClick = { onRevokeCode(codeDto) },
                                                    modifier = Modifier.heightIn(min = 48.dp),
                                                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.5f))
                                                ) {
                                                    Icon(Icons.Default.Block, contentDescription = "Revoke code", tint = LossRed, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Revoke", fontSize = 10.5.sp, color = LossRed, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        5 -> {
                            // Branch Sync Snapshots Monitor & Cleanup
                            if (branchSnapshots.isEmpty()) {
                                Text(
                                    text = "No cloud branch snapshots recorded yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                branchSnapshots.forEach { snap ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("${snap.shopName} • ${snap.branchName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Text(
                                                    text = "${snap.recordsSummary} • Device: ${snap.deviceId} • ${dateFormatter.format(Date(snap.updatedAt))}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteBranchSnapshot(snap) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove branch snapshot", tint = LossRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Admin Audit Trail
                            if (auditLogs.isEmpty()) {
                                Text(
                                    text = "No audit events recorded yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                auditLogs.forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.History, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(entry.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = OrangePrimary)
                                                }
                                                Text(
                                                    text = dateFormatter.format(Date(entry.createdAt)),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${entry.adminEmail} • ${entry.details}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
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
    }

    if (requestToReject != null) {
        AlertDialog(
            onDismissRequest = { requestToReject = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject Payment Claim", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rejecting claim from @${requestToReject!!.ownerUsername} (${requestToReject!!.amountRwf} FRw).",
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectPayment(requestToReject!!, rejectReasonInput.trim())
                        requestToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Confirm Reject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { requestToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
