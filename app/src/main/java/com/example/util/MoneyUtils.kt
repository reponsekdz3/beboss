package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

/**
 * Exact integer-cents / BigDecimal currency math utilities preventing floating-point
 * rounding drift across POS line totals, discounts, VAT, customer debts, and P&L.
 */
object MoneyUtils {

    private const val SCALE_FACTOR = 100.0

    fun toCents(amount: Double): Long {
        if (!amount.isFinite() || amount <= 0.0) return 0L
        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun toSignedCents(amount: Double): Long {
        if (!amount.isFinite()) return 0L
        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun centsToDouble(cents: Long): Double = cents / SCALE_FACTOR

    fun multiplyPriceQuantityCents(unitPrice: Double, quantity: Double): Long {
        if (!unitPrice.isFinite() || !quantity.isFinite() || unitPrice <= 0.0 || quantity <= 0.0) return 0L
        return BigDecimal.valueOf(unitPrice)
            .multiply(BigDecimal.valueOf(quantity))
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun roundMoney(amount: Double): Double {
        if (!amount.isFinite()) return 0.0
        return (amount * SCALE_FACTOR).roundToLong() / SCALE_FACTOR
    }
}
