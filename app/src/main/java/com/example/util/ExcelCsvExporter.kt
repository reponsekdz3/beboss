package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.InventoryValuation
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ShopProfile
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelCsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
    private val dateDisplayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Exports products inventory into an Excel-ready CSV spreadsheet.
     */
    fun exportProductsCsv(context: Context, products: List<Product>, shopProfile: ShopProfile): File {
        val fileName = "BeBoss_Inventory_${dateFormat.format(Date())}.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            // Excel UTF-8 BOM so accents & symbols render cleanly in Microsoft Excel
            writer.write("\uFEFF")
            // Header Row
            writer.append("ID,Product Name,Barcode / SKU,Category,Quantity in Stock,Unit,Cost Price (${shopProfile.currencySymbol}),Selling Price (${shopProfile.currencySymbol}),Gross Profit Margin %,Total Cost Value,Total Retail Value,Low Stock Status\n")

            products.forEach { p ->
                val margin = if (p.sellingPrice > 0) ((p.sellingPrice - p.costPrice) / p.sellingPrice * 100.0) else 0.0
                val costVal = p.quantityInStock * p.costPrice
                val retailVal = p.quantityInStock * p.sellingPrice
                val status = if (p.quantityInStock <= 0) "OUT OF STOCK" else if (p.isLowStock) "LOW STOCK" else "HEALTHY"

                writer.append("\"${escapeCsv(p.id)}\",")
                writer.append("\"${escapeCsv(p.name)}\",")
                writer.append("\"${escapeCsv(p.barcode)}\",")
                writer.append("\"${escapeCsv(p.category)}\",")
                writer.append("${p.quantityInStock},")
                writer.append("\"${escapeCsv(p.unit)}\",")
                writer.append("${p.costPrice},")
                writer.append("${p.sellingPrice},")
                writer.append(String.format(Locale.US, "%.1f%%", margin) + ",")
                writer.append("${costVal.toLong()},")
                writer.append("${retailVal.toLong()},")
                writer.append("\"$status\"\n")
            }
        }
        return file
    }

    /**
     * Exports customers and their debt balances to CSV spreadsheet.
     */
    fun exportCustomersCsv(context: Context, customers: List<Customer>, shopProfile: ShopProfile): File {
        val fileName = "BeBoss_Customers_Debts_${dateFormat.format(Date())}.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            writer.write("\uFEFF")
            writer.append("Customer ID,Full Name,Phone Number,Category,City / Address,Outstanding Debt (${shopProfile.currencySymbol}),Credit Limit (${shopProfile.currencySymbol}),Credit Status,Notes\n")

            customers.forEach { c ->
                val status = when {
                    c.debtBalance > c.creditLimit && c.creditLimit > 0 -> "LIMIT EXCEEDED"
                    c.debtBalance > 0 -> "OWING DEBT"
                    else -> "CLEARED"
                }

                writer.append("\"${escapeCsv(c.id)}\",")
                writer.append("\"${escapeCsv(c.name)}\",")
                writer.append("\"${escapeCsv(c.phone)}\",")
                writer.append("\"${escapeCsv(c.category)}\",")
                writer.append("\"${escapeCsv(c.city.ifBlank { c.address })}\",")
                writer.append("${c.debtBalance.toLong()},")
                writer.append("${c.creditLimit.toLong()},")
                writer.append("\"$status\",")
                writer.append("\"${escapeCsv(c.notes)}\"\n")
            }
        }
        return file
    }

    /**
     * Exports sales ledger to CSV spreadsheet.
     */
    fun exportSalesCsv(context: Context, sales: List<Sale>, shopProfile: ShopProfile): File {
        val fileName = "BeBoss_Sales_Ledger_${dateFormat.format(Date())}.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            writer.write("\uFEFF")
            writer.append("Invoice Ref,Date & Time,Customer Name,Discount,Net Total (${shopProfile.currencySymbol}),Amount Paid (${shopProfile.currencySymbol}),Debt Incurred (${shopProfile.currencySymbol}),Payment Method,Payment Status\n")

            sales.forEach { s ->
                val debtIncurred = (s.totalAmount - s.amountPaid).coerceAtLeast(0.0)
                writer.append("\"${escapeCsv(s.receiptNumber)}\",")
                writer.append("\"${dateDisplayFormat.format(Date(s.saleDate))}\",")
                writer.append("\"${escapeCsv(s.customerName)}\",")
                writer.append("${s.discountAmount.toLong()},")
                writer.append("${s.totalAmount.toLong()},")
                writer.append("${s.amountPaid.toLong()},")
                writer.append("${debtIncurred.toLong()},")
                writer.append("\"${escapeCsv(s.paymentMethod)}\",")
                writer.append("\"${escapeCsv(s.paymentStatus)}\"\n")
            }
        }
        return file
    }

    /**
     * Parses CSV lines to import products in bulk.
     */
    fun parseProductsCsv(csvContent: String): List<Product> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val parsedProducts = mutableListOf<Product>()
        val dataLines = if (lines.first().contains("Product Name", ignoreCase = true) || lines.first().contains("Name", ignoreCase = true)) {
            lines.drop(1)
        } else {
            lines
        }

        for (line in dataLines) {
            val tokens = splitCsvLine(line)
            if (tokens.size >= 2) {
                // Determine format: name, category, costPrice, sellingPrice, qty, unit, barcode
                val name = tokens.getOrNull(1)?.takeIf { it.isNotBlank() } ?: tokens.getOrNull(0) ?: continue
                if (name.equals("Product Name", ignoreCase = true) || name.equals("ID", ignoreCase = true)) continue

                val barcode = tokens.getOrNull(2) ?: ""
                val category = tokens.getOrNull(3)?.ifBlank { "General" } ?: "General"
                val qty = tokens.getOrNull(4)?.toDoubleOrNull() ?: 10.0
                val unit = tokens.getOrNull(5)?.ifBlank { "pcs" } ?: "pcs"
                val costPrice = tokens.getOrNull(6)?.toDoubleOrNull() ?: (tokens.getOrNull(2)?.toDoubleOrNull() ?: 0.0)
                val sellingPrice = tokens.getOrNull(7)?.toDoubleOrNull() ?: (tokens.getOrNull(3)?.toDoubleOrNull() ?: (costPrice * 1.25))

                parsedProducts.add(
                    Product(
                        id = "prod_${System.currentTimeMillis()}_${parsedProducts.size}",
                        name = name.trim(),
                        barcode = barcode.trim(),
                        category = category.trim(),
                        quantityInStock = qty,
                        unit = unit.trim(),
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        lowStockThreshold = 5.0
                    )
                )
            }
        }
        return parsedProducts
    }

    fun shareCsvFile(context: Context, file: File, title: String = "Share Spreadsheet CSV") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false

        for (c in line) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }
}
