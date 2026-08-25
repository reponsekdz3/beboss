package com.example.data.model

import java.util.Calendar

enum class AnalyticsPeriod(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time");

    fun getRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        return when (this) {
            TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            THIS_WEEK -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                Pair(cal.timeInMillis, now)
            }
            THIS_MONTH -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                Pair(cal.timeInMillis, now)
            }
            ALL_TIME -> {
                Pair(0L, now)
            }
        }
    }
}

data class DailyAnalyticsPoint(
    val dateLabel: String,
    val timestamp: Long,
    val revenue: Double,
    val profit: Double,
    val salesCount: Int
)

data class TopProductReport(
    val productId: String,
    val productName: String,
    val totalQuantitySold: Double,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class InventoryValuation(
    val totalItems: Int,
    val totalUnitsInStock: Double,
    val totalCostValue: Double,
    val totalRetailValue: Double,
    val potentialProfit: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int
)

data class ProfitLossSummary(
    val totalRevenue: Double,
    val totalCost: Double,
    val netProfit: Double,
    val totalSalesCount: Int,
    val totalItemsSold: Double,
    val averageOrderValue: Double,
    val profitMarginPercent: Double,
    val isProfitable: Boolean,
    val periodLabel: String
)

data class CategorySalesShare(
    val category: String,
    val revenue: Double,
    val percentage: Double
)
