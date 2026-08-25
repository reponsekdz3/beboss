package com.example.ui.screens

import android.content.Context
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
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.CategorySalesShare
import com.example.data.model.DailyAnalyticsPoint
import com.example.data.model.ProfitLossSummary
import com.example.data.model.ShopProfile
import com.example.data.model.TopProductReport
import com.example.ui.components.AnalyticsChart
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.LossRed
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.ReceiptGenerator

@Composable
fun AnalyticsScreen(
    selectedPeriod: AnalyticsPeriod,
    summary: ProfitLossSummary,
    chartPoints: List<DailyAnalyticsPoint>,
    topProducts: List<TopProductReport>,
    categoryShares: List<CategorySalesShare>,
    shopProfile: ShopProfile,
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
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Selector Tabs
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
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
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OrangePrimary else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = period.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else InkDark
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NET PROFIT / LOSS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkMedium,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (summary.isProfitable) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
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
                            label = "Total Revenue",
                            value = ReceiptGenerator.formatMoney(summary.totalRevenue, shopProfile),
                            color = OrangePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Cost of Goods",
                            value = ReceiptGenerator.formatMoney(summary.totalCost, shopProfile),
                            color = InkDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Orders Count",
                            value = "${summary.totalSalesCount}",
                            color = Color(0xFF2563EB),
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                text = "Top Profitable Products",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (topProducts.isEmpty()) {
                        Text("No sales data in selected period.", fontSize = 13.sp, color = InkMedium)
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
                                        color = if (index == 0) OrangePrimary else Color(0xFFF3F4F6),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index == 0) Color.White else InkDark
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                                        Text("${item.totalQuantitySold.toInt()} sold • Rev: ${ReceiptGenerator.formatMoney(item.totalRevenue, shopProfile)}", fontSize = 11.sp, color = InkMedium)
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
                                HorizontalDivider(color = Color(0xFFF3F4F6))
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Category Sales Breakdown",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        categoryShares.forEach { cat ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.category, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkDark)
                                    Text(
                                        "${ReceiptGenerator.formatMoney(cat.revenue, shopProfile)} (${cat.percentage.toInt()}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.percentage / 100.0).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = OrangePrimary,
                                    trackColor = Color(0xFFF3F4F6)
                                )
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
                            val pdf = com.example.util.PdfReportGenerator.generateSalesReportPdf(
                                context = context,
                                periodName = selectedPeriod.displayName,
                                summary = summary,
                                topProducts = topProducts,
                                categoryShares = categoryShares,
                                shopProfile = shopProfile
                            )
                            com.example.util.PdfReportGenerator.sharePdf(
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
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.DarkNavy)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Official PDF Financial Report", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val reportText = buildString {
                            append("📊 ${shopProfile.shopName.uppercase()} — FINANCIAL SUMMARY\n")
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
        color = Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = InkMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
