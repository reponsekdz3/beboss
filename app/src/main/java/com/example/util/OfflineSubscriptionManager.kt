package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Branch
import com.example.data.model.ShopProfile
import com.example.data.model.User
import java.io.File

data class SubscriptionPriceBreakdown(
    val branchCount: Int,
    val branchBasePrice: Int,
    val branchTierName: String,
    val workerCount: Int,
    val workerFee: Int,
    val workerTierName: String,
    val monthlySubtotal: Int,
    val durationMonths: Int,
    val discountPercent: Int,
    val discountAmount: Int,
    val totalPayable: Int,
    val ussdMtnCode: String,
    val ussdAirtelCode: String
)

data class VoucherValidationResult(
    val isValid: Boolean,
    val daysToAdd: Int,
    val message: String,
    val planName: String,
    val verifiedAmount: Int = 0
)

data class PaymentProcessingResult(
    val isSuccess: Boolean,
    val transactionRef: String,
    val amountPaid: Int,
    val planDays: Int,
    val provider: String,
    val payerPhone: String,
    val timestamp: Long,
    val message: String,
    val isPending: Boolean = false,
    val financialId: String? = null
)

object OfflineSubscriptionManager {

    // Dynamic defaults (can be overridden via ShopProfile settings)
    var defaultMomoMerchantCode = "054321"
    var defaultMomoPhoneNumber = "0788765432"
    var defaultAirtelPhoneNumber = "0738765432"
    var defaultSupportWhatsApp = "+250788765432"

    /**
     * Dynamic Pricing Calculator based on active Branches and registered Shop Workers:
     * - 1 Branch: 5,000 RWF
     * - 2 Branches: 10,000 RWF
     * - >2 Branches (3+): 20,000 RWF
     * - Worker Administration:
     *   * 1 worker: Included (+0 RWF)
     *   * 2-3 workers: +2,000 RWF/mo
     *   * 4-5 workers: +4,000 RWF/mo
     *   * 6+ workers: +4,000 + (workers - 5) * 1,000 RWF/mo
     */
    fun calculateSubscriptionPrice(
        branchCount: Int,
        workerCount: Int,
        durationMonths: Int = 1
    ): SubscriptionPriceBreakdown {
        val effectiveBranches = branchCount.coerceAtLeast(1)
        val effectiveWorkers = workerCount.coerceAtLeast(1)

        // 1. Branch Base Tier
        val (branchBasePrice, branchTierName) = when {
            effectiveBranches == 1 -> Pair(5000, "Single Store Plan (5,000 FRw/mo)")
            effectiveBranches == 2 -> Pair(10000, "2 Branches Multi-Store Plan (10,000 FRw/mo)")
            else -> Pair(20000, "Enterprise Multi-Branch ($effectiveBranches Branches - 20,000 FRw/mo)")
        }

        // 2. Staff / Worker Administration Addon
        val (workerFee, workerTierName) = when {
            effectiveWorkers <= 1 -> Pair(0, "Solo Operator (Included)")
            effectiveWorkers in 2..3 -> Pair(2000, "Small Team ($effectiveWorkers Workers • +2,000 FRw/mo)")
            effectiveWorkers in 4..5 -> Pair(4000, "Medium Team ($effectiveWorkers Workers • +4,000 FRw/mo)")
            else -> {
                val addon = 4000 + (effectiveWorkers - 5) * 1000
                Pair(addon, "Large Team ($effectiveWorkers Workers • +$addon FRw/mo)")
            }
        }

        val monthlySubtotal = branchBasePrice + workerFee
        val rawTotal = monthlySubtotal * durationMonths

        val discountPercent = when (durationMonths) {
            3 -> 10
            12 -> 20
            else -> 0
        }
        val discountAmount = (rawTotal * discountPercent) / 100
        val totalPayable = rawTotal - discountAmount

        val ussdMtnCode = "*182*8*1*$defaultMomoMerchantCode*$totalPayable#"
        val ussdAirtelCode = "*500*1*1*$defaultAirtelPhoneNumber*$totalPayable#"

        return SubscriptionPriceBreakdown(
            branchCount = effectiveBranches,
            branchBasePrice = branchBasePrice,
            branchTierName = branchTierName,
            workerCount = effectiveWorkers,
            workerFee = workerFee,
            workerTierName = workerTierName,
            monthlySubtotal = monthlySubtotal,
            durationMonths = durationMonths,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            totalPayable = totalPayable,
            ussdMtnCode = ussdMtnCode,
            ussdAirtelCode = ussdAirtelCode
        )
    }

    /**
     * Dials MTN Mobile Money USSD with exact dynamic payment amount.
     */
    fun dialMtnMoMo(context: Context, amount: Int) {
        PaymentGatewayManager.dialMtnMoMo(context, amount, defaultMomoMerchantCode, defaultMomoPhoneNumber)
    }

    /**
     * Dials Airtel Money USSD with exact dynamic payment amount.
     */
    fun dialAirtelMoney(context: Context, amount: Int) {
        PaymentGatewayManager.dialAirtelMoney(context, amount, defaultAirtelPhoneNumber)
    }

    /**
     * Generates a unique offline Device Challenge Code for this shop.
     */
    fun getDeviceChallengeCode(shopProfile: ShopProfile): String {
        return SubscriptionSecurityManager.computeDeviceChallenge(shopProfile)
    }

    /**
     * Validates an offline activation voucher or unlock token against branch & worker tiers
     * using cryptographic HMAC-SHA256 signature verification and device binding.
     */
    fun validateVoucherCode(
        inputCode: String,
        shopProfile: ShopProfile,
        branchCount: Int = 1,
        workerCount: Int = 1
    ): VoucherValidationResult {
        return SubscriptionSecurityManager.verifyVoucherToken(
            voucherInput = inputCode,
            shopProfile = shopProfile,
            branchCount = branchCount,
            workerCount = workerCount
        )
    }

    /**
     * Executes authentic mobile money payment verification.
     * Validates phone format, authentic Telco SMS references, anti-replay,
     * and strictly rejects unconfirmed or invalid requests without fake approvals.
     */
    fun processDirectMoMoPayment(
        shopProfile: ShopProfile,
        payerPhone: String,
        provider: String,
        branchCount: Int,
        workerCount: Int,
        durationMonths: Int,
        smsTransactionReference: String? = null,
        alreadyUsedRefs: Set<String> = emptySet()
    ): PaymentProcessingResult {
        val pricing = calculateSubscriptionPrice(branchCount, workerCount, durationMonths)
        val planDays = durationMonths * 30
        val effectivePayer = if (payerPhone.isNotBlank()) payerPhone else shopProfile.phone
        val timestamp = System.currentTimeMillis()

        // 1. Validate Rwanda phone number
        val (phoneValid, normalizedPhone) = MoMoPaymentGateway.validateRwandaPhoneNumber(effectivePayer, provider)
        if (!phoneValid) {
            return PaymentProcessingResult(
                isSuccess = false,
                isPending = false,
                transactionRef = "",
                amountPaid = 0,
                planDays = 0,
                provider = provider,
                payerPhone = effectivePayer,
                timestamp = timestamp,
                message = normalizedPhone
            )
        }

        // 2. If a Telco SMS Reference ID is provided, verify it authentically
        if (!smsTransactionReference.isNullOrBlank()) {
            val verifyRes = MoMoPaymentGateway.verifyTelcoTransactionReference(
                rawReference = smsTransactionReference,
                provider = provider,
                amount = pricing.totalPayable,
                alreadyUsedRefs = alreadyUsedRefs
            )
            return PaymentProcessingResult(
                isSuccess = verifyRes.isSuccess,
                isPending = verifyRes.isPending,
                transactionRef = verifyRes.transactionReference,
                financialId = verifyRes.financialId,
                amountPaid = if (verifyRes.isSuccess) pricing.totalPayable else 0,
                planDays = if (verifyRes.isSuccess) planDays else 0,
                provider = provider,
                payerPhone = normalizedPhone,
                timestamp = timestamp,
                message = verifyRes.message
            )
        }

        // 3. If no reference is provided and no automated gateway is available:
        // Mark as PENDING_CONFIRMATION so the user can complete USSD on their phone,
        // rather than falsely granting instant access.
        val ussdCode = when {
            provider.contains("AIRTEL", ignoreCase = true) -> pricing.ussdAirtelCode
            else -> pricing.ussdMtnCode
        }

        return PaymentProcessingResult(
            isSuccess = false,
            isPending = true,
            transactionRef = "PENDING-$provider-${timestamp.toString().takeLast(6)}",
            financialId = null,
            amountPaid = 0,
            planDays = 0,
            provider = provider,
            payerPhone = normalizedPhone,
            timestamp = timestamp,
            message = "USSD Prompt sent for $provider. Dial $ussdCode or approve the prompt on $normalizedPhone, then enter the SMS reference ID."
        )
    }

    /**
     * Executes asynchronous payment verification using real gateway endpoints.
     */
    suspend fun processMoMoPaymentAsync(
        context: Context,
        shopProfile: ShopProfile,
        payerPhone: String,
        provider: String,
        branchCount: Int,
        workerCount: Int,
        durationMonths: Int,
        userEnteredReference: String? = null,
        alreadyUsedRefs: Set<String> = emptySet()
    ): PaymentProcessingResult {
        val pricing = calculateSubscriptionPrice(branchCount, workerCount, durationMonths)
        val planDays = durationMonths * 30
        val timestamp = System.currentTimeMillis()

        val gatewayRes = MoMoPaymentGateway.executePaymentRequest(
            context = context,
            shopProfile = shopProfile,
            payerPhone = payerPhone,
            provider = provider,
            amount = pricing.totalPayable,
            description = "BeBoss ${pricing.branchTierName} ($durationMonths mo)",
            userEnteredReference = userEnteredReference,
            alreadyUsedRefs = alreadyUsedRefs
        )

        return PaymentProcessingResult(
            isSuccess = gatewayRes.isSuccess,
            isPending = gatewayRes.isPending,
            transactionRef = gatewayRes.transactionReference,
            financialId = gatewayRes.financialId,
            amountPaid = if (gatewayRes.isSuccess) pricing.totalPayable else 0,
            planDays = if (gatewayRes.isSuccess) planDays else 0,
            provider = provider,
            payerPhone = gatewayRes.payerPhone,
            timestamp = timestamp,
            message = gatewayRes.message
        )
    }

    /**
     * Opens WhatsApp support chat with pre-filled subscription assistance message itemizing branches and staff.
     */
    fun contactSupportViaWhatsApp(
        context: Context,
        shopProfile: ShopProfile,
        branches: List<Branch>,
        allUsers: List<User>,
        customMsg: String? = null
    ) {
        val deviceCode = getDeviceChallengeCode(shopProfile)
        val pricing = calculateSubscriptionPrice(branches.size, allUsers.size, 1)

        val defaultText = """
            Hello BeBoss Official Support,
            I want to activate / renew my shop's subscription:

            • Shop Name: ${shopProfile.shopName}
            • Owner: ${shopProfile.name} (${shopProfile.phone})
            • Active Branches: ${branches.size} (${pricing.branchTierName})
            • Registered Staff: ${allUsers.size} (${pricing.workerTierName})
            • Monthly Calculated Fee: ${pricing.totalPayable} FRw
            • Device Challenge Code: $deviceCode

            Please send my cryptographic voucher activation code or verify my payment. Murakoze!
        """.trimIndent()

        val textToSend = customMsg ?: defaultText
        PdfReportGenerator.sendWhatsAppDirect(context, defaultSupportWhatsApp, textToSend)
    }

    /**
     * Shares the BeBoss POS app (APK) offline via Bluetooth, Nearby Share, or File Transfer.
     */
    fun shareAppApkOffline(context: Context) {
        try {
            val appInfo = context.applicationInfo
            val originalApk = File(appInfo.sourceDir)
            val cacheApk = File(context.cacheDir, "BeBoss_Offline_POS.apk")

            originalApk.copyTo(cacheApk, overwrite = true)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheApk
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BeBoss Multi-Branch POS")
                putExtra(Intent.EXTRA_TEXT, "Install BeBoss POS on your device to manage inventory, branches, sales, and staff. Works 100% offline with auto-cloud sync!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share BeBoss App via Bluetooth / Share..."))
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "BeBoss Multi-Branch POS — Local Android management for shops and multi-branch stores.")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share BeBoss App..."))
        }
    }
}
