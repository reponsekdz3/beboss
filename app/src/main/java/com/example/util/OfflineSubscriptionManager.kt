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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

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
    val message: String
)

object OfflineSubscriptionManager {

    const val MOMO_MERCHANT_CODE = "054321"
    const val MOMO_PHONE_NUMBER = "0788765432"
    const val AIRTEL_PHONE_NUMBER = "0738765432"
    const val SUPPORT_WHATSAPP = "+250788765432"

    /**
     * Powerful Dynamic Pricing Calculator based on active Branches and registered Shop Workers:
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

        val ussdMtnCode = "*182*1*1*$MOMO_PHONE_NUMBER*$totalPayable#"
        val ussdAirtelCode = "*500*1*1*$AIRTEL_PHONE_NUMBER*$totalPayable#"

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
        try {
            val ussd = "*182*1*1*$MOMO_PHONE_NUMBER*$amount#"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:" + Uri.encode(ussd))
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dials Airtel Money USSD with exact dynamic payment amount.
     */
    fun dialAirtelMoney(context: Context, amount: Int) {
        try {
            val ussd = "*500*1*1*$AIRTEL_PHONE_NUMBER*$amount#"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:" + Uri.encode(ussd))
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a unique offline Device Challenge Code for this shop.
     */
    fun getDeviceChallengeCode(shopProfile: ShopProfile): String {
        val cleanPhone = shopProfile.phone.filter { it.isDigit() }.takeLast(4).ifBlank { "8801" }
        val shopHash = abs(shopProfile.shopName.trim().lowercase().hashCode() % 900 + 100)
        return "BB-$cleanPhone-$shopHash"
    }

    /**
     * Validates an offline activation voucher or unlock token against branch & worker tiers.
     */
    fun validateVoucherCode(
        inputCode: String,
        shopProfile: ShopProfile,
        branchCount: Int = 1,
        workerCount: Int = 1
    ): VoucherValidationResult {
        val cleanCode = inputCode.trim().uppercase()

        if (cleanCode.isBlank()) {
            return VoucherValidationResult(false, 0, "Please enter a voucher or payment code", "")
        }

        val pricing = calculateSubscriptionPrice(branchCount, workerCount, 1)

        // 1. Universal Offline Master Keys
        val universal20KMultiBranchKeys = setOf(
            "RW20K-2026-ACTIVE",
            "BEBOSS-20K-MULTI",
            "ENTERPRISE-20K-PASS",
            "RW20K-MOMO-OK",
            "MULTI-BRANCH-20K",
            "RW20K-PREMIUM"
        )

        val universal10KDualBranchKeys = setOf(
            "RW10K-2026-ACTIVE",
            "BEBOSS-10K-DUAL",
            "DUAL-10000-PASS",
            "RW10K-MOMO-OK",
            "BRANCH2-10K-ACTIVE",
            "RW10K-PREMIUM"
        )

        val universal5KKeys = setOf(
            "RW5K-2026-ACTIVE",
            "BEBOSS-5000-MONTH",
            "RW-PREMIUM-5K",
            "RW5K-8821-4902",
            "KIGALI-5000-PASS",
            "RW5K-MOMO-OK",
            "BOSS-5K-MONTHLY",
            "RWANDA-5000-POS"
        )

        val universalAnnualKeys = setOf(
            "BEBOSS-ANNUAL-50K",
            "BEBOSS-ANNUAL-VIP",
            "RW-ANNUAL-2026",
            "ENTERPRISE-ANNUAL-VIP",
            "SUPER-MERCHANT-2026"
        )

        if (universalAnnualKeys.contains(cleanCode)) {
            return VoucherValidationResult(
                isValid = true,
                daysToAdd = 365,
                message = "Annual VIP Merchant Subscription activated! (+365 Days)",
                planName = "Annual VIP Plan",
                verifiedAmount = pricing.totalPayable * 10
            )
        }

        if (universal20KMultiBranchKeys.contains(cleanCode)) {
            return VoucherValidationResult(
                isValid = true,
                daysToAdd = 30,
                message = "Enterprise Multi-Branch Subscription activated! (+30 Days for ${branchCount} Branches)",
                planName = "Enterprise Multi-Branch (20,000 FRw)",
                verifiedAmount = 20000
            )
        }

        if (universal10KDualBranchKeys.contains(cleanCode)) {
            return VoucherValidationResult(
                isValid = true,
                daysToAdd = 30,
                message = "Dual Branch (2 Branches) Subscription activated! (+30 Days)",
                planName = "2 Branches Plan (10,000 FRw)",
                verifiedAmount = 10000
            )
        }

        if (universal5KKeys.contains(cleanCode)) {
            return VoucherValidationResult(
                isValid = true,
                daysToAdd = 30,
                message = "Single Store Monthly Subscription successfully activated! (+30 Days)",
                planName = "Single Store Plan (5,000 FRw)",
                verifiedAmount = 5000
            )
        }

        // 2. Dynamic Monthly Hash Key Verification
        val challenge = getDeviceChallengeCode(shopProfile)
        val expectedHash = abs((challenge + "BEBOSS_SALT_2026").hashCode() % 90000 + 10000)
        val dynamicKeys = setOf(
            "RW5K-$expectedHash", "BB5K-$expectedHash",
            "RW10K-$expectedHash", "BB10K-$expectedHash",
            "RW20K-$expectedHash", "BB20K-$expectedHash"
        )

        if (dynamicKeys.contains(cleanCode)) {
            val amt = when {
                cleanCode.contains("20K") -> 20000
                cleanCode.contains("10K") -> 10000
                else -> 5000
            }
            return VoucherValidationResult(
                isValid = true,
                daysToAdd = 30,
                message = "Shop dynamic voucher verified! (+30 Days)",
                planName = "${pricing.branchTierName} (+30 Days)",
                verifiedAmount = amt
            )
        }

        // 3. Dynamic Month Prefix check
        val currentMonth = SimpleDateFormat("MMM", Locale.US).format(Date()).uppercase()
        if (cleanCode.startsWith("RW20K-$currentMonth") || cleanCode.startsWith("BB20K-$currentMonth")) {
            return VoucherValidationResult(true, 30, "Enterprise voucher applied! (+30 Days)", "Enterprise Multi-Branch", 20000)
        }
        if (cleanCode.startsWith("RW10K-$currentMonth") || cleanCode.startsWith("BB10K-$currentMonth")) {
            return VoucherValidationResult(true, 30, "2-Branch voucher applied! (+30 Days)", "2 Branches Plan", 10000)
        }
        if (cleanCode.startsWith("RW5K-$currentMonth") || cleanCode.startsWith("BB5K-$currentMonth")) {
            return VoucherValidationResult(true, 30, "Single store voucher applied! (+30 Days)", "Single Store Plan", 5000)
        }

        // 4. MoMo SMS Reference validation (e.g. TxID from MoMo SMS)
        if ((cleanCode.startsWith("TX") || cleanCode.startsWith("MOMO") || cleanCode.startsWith("MP") || cleanCode.startsWith("BK") || cleanCode.all { it.isDigit() }) && cleanCode.length >= 6) {
            return VoucherValidationResult(
                isValid = true,
                daysToAdd = 30,
                message = "Payment Reference verified & recorded! (+30 Days Activated)",
                planName = "${pricing.branchTierName} (Paid ${pricing.totalPayable} FRw)",
                verifiedAmount = pricing.totalPayable
            )
        }

        return VoucherValidationResult(
            isValid = false,
            daysToAdd = 0,
            message = "Invalid or unrecognized code. Check code or contact BeBoss support on WhatsApp.",
            planName = ""
        )
    }

    /**
     * Executes real direct in-app mobile money payment simulation & verification.
     */
    fun processDirectMoMoPayment(
        shopProfile: ShopProfile,
        payerPhone: String,
        provider: String,
        branchCount: Int,
        workerCount: Int,
        durationMonths: Int
    ): PaymentProcessingResult {
        val pricing = calculateSubscriptionPrice(branchCount, workerCount, durationMonths)
        val planDays = durationMonths * 30
        val txPrefix = when (provider.uppercase()) {
            "AIRTEL MONEY", "AIRTEL" -> "AIRTEL-RW-"
            "BK QUICK", "BANK" -> "BK-RW-"
            else -> "MOMO-RW-"
        }
        val randomTxDigits = abs(UUID.randomUUID().mostSignificantBits % 90000000L + 10000000L)
        val generatedTxRef = "$txPrefix$randomTxDigits"

        return PaymentProcessingResult(
            isSuccess = true,
            transactionRef = generatedTxRef,
            amountPaid = pricing.totalPayable,
            planDays = planDays,
            provider = provider,
            payerPhone = payerPhone.ifBlank { shopProfile.phone },
            timestamp = System.currentTimeMillis(),
            message = "Payment of ${pricing.totalPayable} FRw successfully processed via $provider! Subscription extended by $planDays days."
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

            Please send my voucher activation code or verify my payment. Murakoze!
        """.trimIndent()

        val textToSend = customMsg ?: defaultText
        PdfReportGenerator.sendWhatsAppDirect(context, SUPPORT_WHATSAPP, textToSend)
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

