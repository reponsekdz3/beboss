package com.example.util

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/**
 * Monotonic high-water clock guard against device clock-rollback attacks.
 * Prevents rolling the Android system clock back to artificially extend subscriptions
 * or win LWW/HLC sync comparisons.
 */
object ClockGuard {

    private const val PREFS_NAME = "beboss_monotonic_clock"
    private const val KEY_HIGH_WATER_EPOCH = "hw_epoch_ms"
    private const val KEY_ANCHOR_ELAPSED = "hw_elapsed_ms"

    private val highWaterEpoch = AtomicLong(0L)
    private val anchorElapsed = AtomicLong(0L)
    @Volatile
    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEpoch = prefs.getLong(KEY_HIGH_WATER_EPOCH, 0L)
        val now = System.currentTimeMillis()
        val elapsed = safeElapsedRealtime()
        val initialHighWater = maxOf(savedEpoch, now)
        highWaterEpoch.set(initialHighWater)
        anchorElapsed.set(elapsed)
        prefs.edit()
            .putLong(KEY_HIGH_WATER_EPOCH, initialHighWater)
            .putLong(KEY_ANCHOR_ELAPSED, elapsed)
            .apply()
        initialized = true
    }

    /**
     * Returns a monotonic timestamp that never moves backwards even if the OS wall clock is rolled back.
     */
    fun effectiveNowMs(context: Context? = null): Long {
        if (context != null && !initialized) init(context)
        val sysNow = System.currentTimeMillis()
        val prev = highWaterEpoch.get()
        val elapsedDelta = (safeElapsedRealtime() - anchorElapsed.get()).coerceAtLeast(0L)
        val monotonicEstimate = if (prev > 0L) prev + elapsedDelta else sysNow
        val candidate = maxOf(sysNow, monotonicEstimate)

        if (candidate > prev) {
            highWaterEpoch.compareAndSet(prev, candidate)
            if (context != null && candidate - prev > 5_000L) {
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_HIGH_WATER_EPOCH, candidate)
                    .apply()
            }
        }
        return candidate
    }

    /**
     * Records an external trusted server or peer timestamp high-water mark.
     */
    fun observeTrustedEpoch(trustedEpochMs: Long, context: Context? = null) {
        if (trustedEpochMs <= 0L) return
        while (true) {
            val prev = highWaterEpoch.get()
            if (trustedEpochMs <= prev) return
            if (highWaterEpoch.compareAndSet(prev, trustedEpochMs)) {
                anchorElapsed.set(safeElapsedRealtime())
                context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    ?.edit()
                    ?.putLong(KEY_HIGH_WATER_EPOCH, trustedEpochMs)
                    ?.putLong(KEY_ANCHOR_ELAPSED, anchorElapsed.get())
                    ?.apply()
                return
            }
        }
    }

    /**
     * Detects suspicious backwards clock skew (> 5 minutes earlier than monotonic high-water mark).
     */
    fun isClockRollbackDetected(): Boolean {
        val prev = highWaterEpoch.get()
        if (prev <= 0L) return false
        return System.currentTimeMillis() + 300_000L < prev
    }

    private fun safeElapsedRealtime(): Long {
        return try {
            SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            0L
        }
    }
}
