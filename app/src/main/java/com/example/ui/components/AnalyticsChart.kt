package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyAnalyticsPoint
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMedium
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ProfitGreen

@Composable
fun AnalyticsChart(
    points: List<DailyAnalyticsPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = "Revenue & Profit Trends",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkDark
                )

                // Chart Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(OrangePrimary, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Revenue", fontSize = 11.sp, color = InkMedium)

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.size(8.dp).background(ProfitGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Profit", fontSize = 11.sp, color = InkMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sales data in this period", fontSize = 13.sp, color = InkMedium)
                }
            } else {
                val maxVal = points.maxOfOrNull { maxOf(it.revenue, it.profit) }?.coerceAtLeast(100.0) ?: 100.0

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 8.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val groupCount = points.size
                    val slotWidth = width / groupCount
                    val barWidth = (slotWidth * 0.32f).coerceAtMost(28f)
                    val spacing = 4f

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = height * (i.toFloat() / gridLines)
                        drawLine(
                            color = Color(0xFFF3F4F6),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw bars
                    points.forEachIndexed { index, pt ->
                        val centerX = (index * slotWidth) + (slotWidth / 2)

                        // Revenue bar (Orange)
                        val revHeight = ((pt.revenue / maxVal) * height).toFloat()
                        val revX = centerX - barWidth - (spacing / 2)
                        val revY = height - revHeight

                        drawRoundRect(
                            color = OrangePrimary,
                            topLeft = Offset(revX, revY),
                            size = Size(barWidth, revHeight.coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Profit bar (Green)
                        val profHeight = ((pt.profit.coerceAtLeast(0.0) / maxVal) * height).toFloat()
                        val profX = centerX + (spacing / 2)
                        val profY = height - profHeight

                        drawRoundRect(
                            color = ProfitGreen,
                            topLeft = Offset(profX, profY),
                            size = Size(barWidth, profHeight.coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // X-axis label
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            pt.dateLabel,
                            centerX,
                            height + 28f,
                            paint
                        )
                    }
                }
            }
        }
    }
}
