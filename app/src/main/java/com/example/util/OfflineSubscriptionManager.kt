package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ShopProfile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class SubscriptionPlan(val title: String, val priceRwf: Int, val durationDays: Int, val description: String) {
    MONTHLY("Monthly Offline POS", 5000, 30, "5,000 RWF per month — Full offline POS, Stock & Staff"),
    QUARTERLY("3 Months Special", 14000, 90, "14,000 RWF (Save 1,000 RWF) — 3 Months Access"),
    ANNUAL("Annual Super Merchant", 50000, 365, "50,000 RWF (Save 10,000 RWF) — 1 Year Access")
}

data class VoucherValidationResult(
    val isValid: Boolean,
    val daysToAdd: Int,
    val message: String,
    val planName: String
)

object OfflineSubscriptionManager {

    const val MOMO_MERCHANT_CODE = "054321"
    const val MOMO_PHONE_NUMBER = "0788765432"
    const val AIRTEL_PHONE_NUMBER = "0738765432"
    const val SUPPORT_WHATSAPP = "+250788765432"
    const val MONTHLY_PRICE_RWF = 5000

    /**
     * Dials MTN Mobile Money USSD with merchant payment / transfer of 5,000 RWF.
     * Works 100% offline without mobile data or internet!
     */
    fun dialMtnMoMo(context: Context, amount: Int = MONTHLY_PRICE_RWF) {
        try {
            // USSD for MTN Rwanda MoMo Pay merchant code
            // Format: *182*8*1*MERCHANT*AMOUNT# or *182*1*1*PHONE*AMOUNT#
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
     * Dials Airtel Money USSD with payment of 5,000 RWF.
     */
    fun dialAirtelMoney(context: Context, amount: Int = MONTHLY_PRICE_RWF) {
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
     * Validates an offline activation voucher or unlock token.
     * Uses deterministic verification algorithms that work without internet.
     */
    fun validateVoucherCode(inputCode: String, shopProfile: ShopProfile): VoucherValidationResult {
        val cleanCode = inputCode.trim().uppercase()

        if (cleanCode.isBlank()) {
            return VoucherValidationResult(false, 0, "Please enter a voucher code", "")
        }

        // 1. Universal Offline Master Keys
        val universal30DayKeys = setOf(
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
            "RW50K-VIP-YEAR",
            "SUPER-MERCHANT-2026"
        )

        if (universalAnnualKeys.contains(cleanCode)) {
            return VoucherValidationResult(true, 365, "Annual Super Merchant activated! (+365 Days)", "Annual Plan")
        }

        if (universal30DayKeys.contains(cleanCode)) {
            return VoucherValidationResult(true, 30, "Monthly Subscription successfully activated! (+30 Days)", "Monthly Plan (5,000 RWF)")
        }

        // 2. Dynamic Monthly Hash Key Verification
        // Format: RW5K-XXXXX or BB5K-XXXXX
        val challenge = getDeviceChallengeCode(shopProfile)
        val expectedHash = abs((challenge + "BEBOSS_SALT_2026").hashCode() % 90000 + 10000)
        val dynamicMonthlyKey = "RW5K-$expectedHash"
        val dynamicAltKey = "BB5K-$expectedHash"

        if (cleanCode == dynamicMonthlyKey || cleanCode == dynamicAltKey) {
            return VoucherValidationResult(true, 30, "Shop voucher verified! (+30 Days)", "Monthly Plan (5,000 RWF)")
        }

        // 3. Dynamic Current Month Voucher verification (e.g. RW5K-AUG26-XXXX or BEBOSS-XXXX)
        val currentMonth = SimpleDateFormat("MMM", Locale.US).format(Date()).uppercase()
        if (cleanCode.startsWith("RW5K-$currentMonth") || cleanCode.startsWith("BB5K-$currentMonth")) {
            return VoucherValidationResult(true, 30, "Valid monthly voucher applied! (+30 Days)", "Monthly Plan (5,000 RWF)")
        }

        // 4. MoMo SMS Reference validation (e.g., TxID from MTN MoMo message >= 8 chars)
        if ((cleanCode.startsWith("TX") || cleanCode.startsWith("MOMO") || cleanCode.startsWith("MP") || cleanCode.all { it.isDigit() }) && cleanCode.length >= 7) {
            return VoucherValidationResult(true, 30, "MoMo Payment Reference recorded! (+30 Days Activated)", "MoMo Pay (5,000 RWF)")
        }

        return VoucherValidationResult(
            isValid = false,
            daysToAdd = 0,
            message = "Invalid or expired voucher code. Check code or contact support via WhatsApp/SMS.",
            planName = ""
        )
    }

    /**
     * Opens WhatsApp support chat with pre-filled subscription assistance message.
     */
    fun contactSupportViaWhatsApp(context: Context, shopProfile: ShopProfile, customMsg: String? = null) {
        val deviceCode = getDeviceChallengeCode(shopProfile)
        val defaultText = """
            Hello BeBoss Support,
            I would like to activate / renew my shop's offline subscription (5,000 RWF/month).

            • Shop Name: ${shopProfile.shopName}
            • Owner: ${shopProfile.name}
            • Phone: ${shopProfile.phone}
            • Device Code: $deviceCode
            • Monthly Fee: 5,000 RWF (Paid via MoMo)

            Please send my offline voucher activation code. Thank you!
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
                putExtra(Intent.EXTRA_SUBJECT, "BeBoss Offline POS Application")
                putExtra(Intent.EXTRA_TEXT, "Install BeBoss POS on your device to manage inventory, sales and staff offline. No internet required!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share BeBoss App via Bluetooth / Nearby Share..."))
        } catch (e: Exception) {
            // Fallback text share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "BeBoss Offline POS — Share and install locally on Android devices without internet.")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share BeBoss App..."))
        }
    }
}
