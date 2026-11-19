package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.TrendingUp
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.CategorySalesShare
import com.example.data.model.DailyAnalyticsPoint
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.ShopProfile
import com.example.data.model.TopProductReport
import com.example.ui.components.AnalyticsChart
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.PdfReportGenerator
import com.example.util.ReceiptGenerator
import kotlin.math.ceil

@Composable
fun AnalyticsScreen(
    selectedPeriod: AnalyticsPeriod,
    summary: ProfitLossSummary,
    chartPoints: List<DailyAnalyticsPoint>,
    topProducts: List<TopProductReport>,
    categoryShares: List<CategorySalesShare>,
    products: List<Product> = emptyList(),
    shopProfile: ShopProfile,
    language: AppLanguage = AppLanguage.ENGLISH,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    val context = LocalContext.current
    val periods = listOf(
        AnalyticsPeriod.TODAY,
        AnalyticsPeriod.THIS_WEEK,
        AnalyticsPeriod.THIS_MONTH,
        AnalyticsPeriod.ALL_TIME
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Selector Tabs
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEach { period: AnalyticsPeriod ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            onClick = { onPeriodSelected(period) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OrangePrimary else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val periodLabel = when (period) {
                                    AnalyticsPeriod.TODAY -> Localization.get("period_today", language)
                                    AnalyticsPeriod.THIS_WEEK -> Localization.get("period_this_week", language)
                                    AnalyticsPeriod.THIS_MONTH -> Localization.get("period_this_month", language)
                                    AnalyticsPeriod.ALL_TIME -> Localization.get("period_all_time", language)
                                }
                                Text(
                                    text = periodLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hero Profit & Loss Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.get("gross_profit", language).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (summary.isProfitable) ProfitGreen.copy(alpha = 0.14f) else LossRed.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (summary.isProfitable) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (summary.isProfitable) ProfitGreen else LossRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${summary.profitMarginPercent.toInt()}% Margin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.isProfitable) ProfitGreen else LossRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = (if (summary.netProfit >= 0) "+" else "") + ReceiptGenerator.formatMoney(summary.netProfit, shopProfile),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (summary.isProfitable) ProfitGreen else LossRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Metric Tiles: Revenue, Cost, Orders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            label = Localization.get("gross_revenue", language),
                            value = ReceiptGenerator.formatMoney(summary.totalRevenue, shopProfile),
                            color = OrangePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = Localization.get("cost_of_goods", language),
                            value = ReceiptGenerator.formatMoney(summary.totalCost, shopProfile),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = Localization.get("today_orders", language),
                            value = "${summary.totalSalesCount}",
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Timeline Trend Chart
        item {
            AnalyticsChart(
                points = chartPoints,
                currencySymbol = shopProfile.currencySymbol
            )
        }

        // Top Selling Products Leaderboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("top_selling_products", language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (topProducts.isEmpty()) {
                        Text("No sales data in selected period.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        topProducts.take(5).forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (index == 0) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index == 0) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${item.totalQuantitySold.toInt()} sold • Rev: ${ReceiptGenerator.formatMoney(item.totalRevenue, shopProfile)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Text(
                                    text = "+${ReceiptGenerator.formatMoney(item.totalProfit, shopProfile)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                            if (index < topProducts.take(5).size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }

        // Category Share Breakdown
        if (categoryShares.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("category_sales_share", language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        categoryShares.forEach { cat ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.category, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${ReceiptGenerator.formatMoney(cat.revenue, shopProfile)} (${cat.percentage.toInt()}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.percentage / 100.0).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = OrangePrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Demand Forecasting & Reorder Suggestions (Moving Average Velocity)
        item {
            val periodDays = when (selectedPeriod) {
                AnalyticsPeriod.TODAY -> 1.0
                AnalyticsPeriod.THIS_WEEK -> 7.0
                AnalyticsPeriod.THIS_MONTH -> 30.0
                AnalyticsPeriod.ALL_TIME -> 30.0
            }
            val soldByName = topProducts.associateBy { it.productName.lowercase() }
            val forecasts = products
                .filter { !it.isDeleted }
                .mapNotNull { product ->
                    val soldQty = soldByName[product.name.lowercase()]?.totalQuantitySold ?: 0.0
                    val dailyVelocity = soldQty / periodDays
                    val daysCover = if (dailyVelocity > 0.0) product.quantityInStock / dailyVelocity else Double.POSITIVE_INFINITY
                    val needsReorder = product.isLowStock || (dailyVelocity > 0.0 && daysCover < 10.0)
                    if (!needsReorder) return@mapNotNull null
                    val targetStock = maxOf(
                        ceil(dailyVelocity * 14.0),
                        product.lowStockThreshold * 2.0,
                        5.0
                    )
                    val reorderQty = (targetStock - product.quantityInStock).coerceAtLeast(1.0).toInt()
                    val estCost = reorderQty * product.costPrice
                    Triple(product, daysCover, reorderQty to estCost)
                }
                .sortedBy { it.second }
                .take(5)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = Localization.get("demand_forecast_title", language),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Computed from ${selectedPeriod.displayName.lowercase()} sales velocity & stock cover",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (forecasts.isEmpty()) {
                        Text(
                            text = "All active inventory items have healthy stock cover for the next 10+ days.",
                            fontSize = 12.sp,
                            color = ProfitGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        forecasts.forEachIndexed { idx, (product, daysCover, reorderPair) ->
                            val (reorderQty, estCost) = reorderPair
                            val coverText = if (daysCover.isInfinite()) "Low stock threshold" else "${daysCover.toInt().coerceAtLeast(0)}d stock cover"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "On hand: ${product.quantityInStock.toInt()} ${product.unit} • $coverText",
                                        fontSize = 11.sp,
                                        color = if (product.quantityInStock <= 0 || daysCover < 3.0) LossRed else WarningAmber
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Order +$reorderQty ${product.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary
                                    )
                                    Text(
                                        text = "Est. ${ReceiptGenerator.formatMoney(estCost, shopProfile)}",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (idx < forecasts.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }

        // Share & PDF Export Report Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        try {
                            val pdf = PdfReportGenerator.generateSalesReportPdf(
                                context = context,
                                periodName = selectedPeriod.displayName,
                                summary = summary,
                                topProducts = topProducts,
                                categoryShares = categoryShares,
                                shopProfile = shopProfile
                            )
                            PdfReportGenerator.sharePdf(
                                context = context,
                                file = pdf,
                                title = "Share Financial PDF Report (${selectedPeriod.displayName})"
                            )
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Could not generate PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Localization.get("export_pdf_report", language), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val reportText = buildString {
                            append("${shopProfile.shopName.uppercase()} — FINANCIAL SUMMARY\n")
                            append("Period: ${selectedPeriod.displayName}\n")
                            append("--------------------------------\n")
                            append("Total Sales Revenue : ${ReceiptGenerator.formatMoney(summary.totalRevenue, shopProfile)}\n")
                            append("Cost of Goods Sold  : ${ReceiptGenerator.formatMoney(summary.totalCost, shopProfile)}\n")
                            append("NET PROFIT / LOSS   : ${ReceiptGenerator.formatMoney(summary.netProfit, shopProfile)}\n")
                            append("Profit Margin       : ${summary.profitMarginPercent.toInt()}%\n")
                            append("Total Transactions  : ${summary.totalSalesCount}\n")
                            append("--------------------------------\n")
                            append("Generated on-device via BeBoss POS\n")
                        }
                        ReceiptGenerator.shareReceipt(context, reportText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = OrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Text Summary to WhatsApp / SMS", fontWeight = FontWeight.SemiBold, color = OrangePrimary)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
