package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import java.text.NumberFormat
import java.util.Locale

object BeBossNotificationManager {

    const val CHANNEL_SALES_ID = "beboss_sales_channel"
    const val CHANNEL_INVENTORY_ID = "beboss_inventory_channel"
    const val CHANNEL_DEBTS_ID = "beboss_debts_channel"
    const val CHANNEL_SYNC_ID = "beboss_sync_channel"
    const val CHANNEL_TARGETS_ID = "beboss_targets_channel"

    private const val NOTIF_ID_SALE_BASE = 1000
    private const val NOTIF_ID_INVENTORY_BASE = 2000
    private const val NOTIF_ID_DEBT_BASE = 3000
    private const val NOTIF_ID_SYNC = 4001
    private const val NOTIF_ID_TARGET = 5001
    private const val NOTIF_ID_TRANSFER_BASE = 6000

    /**
     * Initializes all modern Android Notification Channels with rich audio and vibration properties.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Sales & Cash Register Channel
            val salesChannel = NotificationChannel(
                CHANNEL_SALES_ID,
                "Sales & Cash Register",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications on completed sales, receipts, and cash float updates"
                enableLights(true)
                lightColor = Color.argb(255, 249, 115, 22) // BeBoss Orange
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 100)
            }

            // 2. Inventory & Stock Alerts Channel
            val inventoryChannel = NotificationChannel(
                CHANNEL_INVENTORY_ID,
                "Stock & Inventory Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when products reach minimum threshold or run out of stock"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            // 3. Customer Debts & Credit Repayments
            val debtsChannel = NotificationChannel(
                CHANNEL_DEBTS_ID,
                "Debts & Credit Repayments",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on customer debt payments, debt balances, and payment schedules"
                enableLights(true)
                lightColor = Color.argb(255, 22, 163, 74) // Profit Green
            }

            // 4. Multi-Branch & Cloud Sync Channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                "Multi-Branch & Cloud Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background sync logs, P2P branch data exchange, and database backups"
            }

            // 5. Daily Sales Target & Milestones Channel
            val targetsChannel = NotificationChannel(
                CHANNEL_TARGETS_ID,
                "Sales Targets & Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebrations when shop hits daily or monthly revenue targets"
                enableLights(true)
                lightColor = Color.argb(255, 255, 215, 0) // Gold
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 250)
            }

            notificationManager.createNotificationChannels(
                listOf(salesChannel, inventoryChannel, debtsChannel, syncChannel, targetsChannel)
            )
        }
    }

    private fun getLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Trigger real high-priority sale notification
     */
    fun sendSaleCompletedNotification(context: Context, sale: Sale, shopProfile: ShopProfile, items: List<com.example.data.model.SaleItem> = emptyList()) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val currency = shopProfile.currencySymbol.ifBlank { "FRw" }
        val formattedTotal = NumberFormat.getNumberInstance(Locale.US).format(sale.totalAmount)
        val itemCount = if (items.isNotEmpty()) items.sumOf { it.quantitySold.toInt() } else 1

        val receiptLabel = if (sale.receiptNumber.isNotBlank()) "Receipt #${sale.receiptNumber}" else "Sale #${sale.id.take(6)}"

        val bigText = buildString {
            append("$receiptLabel\n")
            append("Total: $formattedTotal $currency (${sale.paymentMethod})\n")
            append("Cashier: ${sale.cashierName}\n")
            if (items.isNotEmpty()) {
                append("Items: ")
                append(items.take(3).joinToString(", ") { "${it.productName} (${it.quantitySold.toInt()}x)" })
                if (items.size > 3) append(" +${items.size - 3} more")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SALES_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("💰 Sale Completed: $formattedTotal $currency")
            .setContentText("$receiptLabel • $itemCount items via ${sale.paymentMethod}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFFF97316.toInt())
            .setContentIntent(getLaunchPendingIntent(context))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIF_ID_SALE_BASE + (sale.id.hashCode() % 500),
                notification
            )
        } catch (e: SecurityException) {
            // Missing permission safely handled
        }
    }

    /**
     * Trigger urgent Low Stock Notification
     */
    fun sendLowStockNotification(
        context: Context,
        productName: String,
        currentStock: Double,
        minStock: Double = 5.0,
        unit: String = "pcs"
    ) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val stockDisplay = if (currentStock <= 0) "OUT OF STOCK" else "$currentStock $unit remaining"
        val title = if (currentStock <= 0) "🚨 Out of Stock: $productName" else "⚠️ Low Stock Alert: $productName"

        val notification = NotificationCompat.Builder(context, CHANNEL_INVENTORY_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText("$stockDisplay (Min target: $minStock $unit). Tap to restock now.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Product: $productName\nCurrent Quantity: $currentStock $unit\nThreshold: $minStock $unit\nPlease arrange a supplier purchase or transfer stock from another branch immediately."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFFDC2626.toInt())
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIF_ID_INVENTORY_BASE + (productName.hashCode() % 500),
                notification
            )
        } catch (e: SecurityException) {
            // Handled
        }
    }

    /**
     * Trigger Debt Repayment notification
     */
    fun sendDebtPaymentNotification(
        context: Context,
        customerName: String,
        amountPaid: Double,
        remainingDebt: Double,
        currencySymbol: String
    ) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val formattedPaid = NumberFormat.getNumberInstance(Locale.US).format(amountPaid)
        val formattedRemaining = NumberFormat.getNumberInstance(Locale.US).format(remainingDebt)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEBTS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("💵 Debt Payment Received: $formattedPaid $currencySymbol")
            .setContentText("From $customerName • Outstanding balance: $formattedRemaining $currencySymbol")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Customer: $customerName\nAmount Paid: $formattedPaid $currencySymbol\nRemaining Debt: $formattedRemaining $currencySymbol\nCustomer credit record has been updated successfully in SQLite ledger."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFF16A34A.toInt())
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIF_ID_DEBT_BASE + (customerName.hashCode() % 500),
                notification
            )
        } catch (e: SecurityException) {
            // Handled
        }
    }

    /**
     * Trigger Daily Sales Target Milestone celebration
     */
    fun sendDailyTargetReachedNotification(
        context: Context,
        todayTotal: Double,
        targetAmount: Double = 100000.0,
        currencySymbol: String = "FRw"
    ) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val formattedToday = NumberFormat.getNumberInstance(Locale.US).format(todayTotal)
        val formattedTarget = NumberFormat.getNumberInstance(Locale.US).format(targetAmount)

        val notification = NotificationCompat.Builder(context, CHANNEL_TARGETS_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("🎉 Daily Sales Target Reached!")
            .setContentText("Hit $formattedToday / $formattedTarget $currencySymbol today! Outstanding work!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Congratulations! Today's sales have reached $formattedToday $currencySymbol, surpassing your daily business target of $formattedTarget $currencySymbol. Keep up the high performance!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFFEAB308.toInt())
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_TARGET, notification)
        } catch (e: SecurityException) {
            // Handled
        }
    }

    /**
     * Trigger Multi-Branch Sync notification
     */
    fun sendBranchSyncNotification(context: Context, branchName: String = "Main Store", recordsCount: Int = 0) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("⚡ Multi-Branch Sync Complete")
            .setContentText("Synchronized $recordsCount records with branch '$branchName'")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_SYNC, notification)
        } catch (e: SecurityException) {
            // Handled
        }
    }

    /**
     * Trigger Stock Transfer notification
     */
    fun sendStockTransferNotification(context: Context, transfer: StockTransfer) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("📦 Stock Transferred (${transfer.transferNumber})")
            .setContentText("${transfer.quantity} ${transfer.unit} of ${transfer.productName}: ${transfer.fromBranchName} ➔ ${transfer.toBranchName}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIF_ID_TRANSFER_BASE + (transfer.id.hashCode() % 500),
                notification
            )
        } catch (e: SecurityException) {
            // Handled
        }
    }

    /**
     * Send test notification across any selected channel
     */
    fun sendTestNotification(context: Context, channelId: String = CHANNEL_SALES_ID) {
        if (!PermissionManager.hasNotificationPermission(context)) return

        val (title, text, color) = when (channelId) {
            CHANNEL_INVENTORY_ID -> Triple("⚠️ Low Stock Test Alert", "BeBoss inventory channel is active and notifying for stock thresholds.", 0xFFDC2626.toInt())
            CHANNEL_DEBTS_ID -> Triple("💵 Debt Reminder Test Alert", "BeBoss customer debt payment channel is active.", 0xFF16A34A.toInt())
            CHANNEL_TARGETS_ID -> Triple("🎉 Target Milestone Test Alert", "BeBoss sales target achievement channel is active.", 0xFFEAB308.toInt())
            CHANNEL_SYNC_ID -> Triple("⚡ Multi-Branch Sync Test Alert", "BeBoss offline P2P and cloud synchronization channel is active.", 0xFF0284C7.toInt())
            else -> Triple("💰 POS Checkout Test Notification", "BeBoss POS sales & receipt engine notification is 100% active and operational.", 0xFFF97316.toInt())
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(color)
            .setContentIntent(getLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (e: SecurityException) {
            // Handled
        }
    }
}
