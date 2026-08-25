package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {

    fun formatMoney(amount: Double, profile: ShopProfile): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            minimumFractionDigits = 0
        }
        return "${formatter.format(amount)} ${profile.currencySymbol}"
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Builds clean, formatted plain text receipt ideal for WhatsApp, SMS, or thermal Bluetooth receipt printers
     */
    fun generateTextReceipt(
        sale: Sale,
        items: List<SaleItem>,
        profile: ShopProfile
    ): String {
        val sb = StringBuilder()
        val line = "--------------------------------"
        val doubleLine = "================================"

        sb.append(doubleLine).append("\n")
        sb.append("   ").append(profile.shopName.uppercase(Locale.getDefault())).append("\n")
        if (profile.address.isNotBlank()) {
            sb.append("   ").append(profile.address).append("\n")
        }
        if (profile.phone.isNotBlank()) {
            sb.append("   Tel: ").append(profile.phone).append("\n")
        }
        sb.append(doubleLine).append("\n")

        sb.append("Receipt #: ").append(sale.receiptNumber).append("\n")
        sb.append("Date     : ").append(formatDate(sale.saleDate)).append("\n")
        sb.append("Customer : ").append(sale.customerName).append("\n")
        sb.append("Payment  : ").append(sale.paymentMethod).append(" (").append(sale.paymentStatus).append(")\n")
        sb.append(line).append("\n")

        sb.append(String.format("%-16s %4s %9s\n", "ITEM", "QTY", "TOTAL"))
        sb.append(line).append("\n")

        for (item in items) {
            val nameTrunc = if (item.productName.length > 15) item.productName.take(13) + ".." else item.productName
            val qtyStr = if (item.quantitySold % 1.0 == 0.0) "${item.quantitySold.toInt()}" else "${item.quantitySold}"
            val subStr = "${item.subtotal.toInt()}"
            sb.append(String.format("%-16s %4s %9s\n", nameTrunc, qtyStr, subStr))
        }

        sb.append(line).append("\n")
        sb.append(String.format("%-18s: %12s\n", "TOTAL", formatMoney(sale.totalAmount, profile)))
        if (sale.discountAmount > 0) {
            sb.append(String.format("%-18s: %12s\n", "Discount", formatMoney(sale.discountAmount, profile)))
        }
        sb.append(String.format("%-18s: %12s\n", "Paid", formatMoney(sale.amountPaid, profile)))
        val balanceDue = sale.totalAmount - sale.amountPaid
        if (balanceDue > 0) {
            sb.append(String.format("%-18s: %12s\n", "BALANCE DUE", formatMoney(balanceDue, profile)))
        }

        sb.append(doubleLine).append("\n")
        sb.append("   ").append(profile.receiptFooter).append("\n")
        sb.append("   Powered by BeBoss\n")
        sb.append(doubleLine)

        return sb.toString()
    }

    /**
     * Builds styled HTML for WebView printing / PDF generation
     */
    fun generateHtmlReceipt(
        sale: Sale,
        items: List<SaleItem>,
        profile: ShopProfile
    ): String {
        val rows = StringBuilder()
        for (item in items) {
            val qtyStr = if (item.quantitySold % 1.0 == 0.0) "${item.quantitySold.toInt()}" else "${item.quantitySold}"
            rows.append("""
                <tr>
                    <td style="padding: 6px 0; border-bottom: 1px dashed #ddd;">${item.productName}</td>
                    <td style="padding: 6px 0; border-bottom: 1px dashed #ddd; text-align: center;">$qtyStr ${item.unit}</td>
                    <td style="padding: 6px 0; border-bottom: 1px dashed #ddd; text-align: right;">${formatMoney(item.subtotal, profile)}</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0; padding: 20px; color: #111; font-size: 13px; }
                    .header { text-align: center; margin-bottom: 16px; }
                    .shop-title { font-size: 20px; font-weight: bold; color: #FF6B1A; margin-bottom: 4px; }
                    .meta-table { width: 100%; margin-bottom: 12px; font-size: 12px; }
                    .items-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
                    .totals { margin-top: 12px; border-top: 2px solid #111; padding-top: 8px; }
                    .total-row { display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 4px; }
                    .grand-total { font-size: 18px; font-weight: bold; color: #FF6B1A; }
                    .footer { text-align: center; margin-top: 24px; font-size: 11px; color: #666; border-top: 1px dashed #ccc; padding-top: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="shop-title">${profile.shopName}</div>
                    <div>${profile.address}</div>
                    <div>Tel: ${profile.phone} | ${profile.email}</div>
                </div>

                <table class="meta-table">
                    <tr><td><strong>Receipt #:</strong> ${sale.receiptNumber}</td><td style="text-align:right;"><strong>Date:</strong> ${formatDate(sale.saleDate)}</td></tr>
                    <tr><td><strong>Customer:</strong> ${sale.customerName}</td><td style="text-align:right;"><strong>Payment:</strong> ${sale.paymentMethod}</td></tr>
                </table>

                <table class="items-table">
                    <thead>
                        <tr style="border-bottom: 2px solid #111; text-align: left;">
                            <th style="padding-bottom: 6px;">Item</th>
                            <th style="padding-bottom: 6px; text-align: center;">Qty</th>
                            <th style="padding-bottom: 6px; text-align: right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rows
                    </tbody>
                </table>

                <div class="totals">
                    <div class="total-row grand-total">
                        <span>TOTAL:</span>
                        <span>${formatMoney(sale.totalAmount, profile)}</span>
                    </div>
                    <div class="total-row">
                        <span>Paid (${sale.paymentMethod}):</span>
                        <span>${formatMoney(sale.amountPaid, profile)}</span>
                    </div>
                    ${if (sale.totalAmount > sale.amountPaid) """
                    <div class="total-row" style="color: #DC2626; font-weight: bold;">
                        <span>Balance Due:</span>
                        <span>${formatMoney(sale.totalAmount - sale.amountPaid, profile)}</span>
                    </div>
                    """ else ""}
                </div>

                <div class="footer">
                    <div>${profile.receiptFooter}</div>
                    <div style="margin-top: 4px;">Created with BeBoss — Offline Business Manager</div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun shareReceipt(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via")
        context.startActivity(shareIntent)
    }
}
