package com.example.util

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

data class RateLimitDecision(
    val allowed: Boolean,
    val remainingSeconds: Long = 0L,
    val failedAttempts: Int = 0,
    val message: String = ""
)

/**
 * Exponential backoff rate limiter protecting PIN, Password, and Unlock Code endpoints
 * against automated brute-force attempts. Persists across app restarts via SharedPreferences.
 */
object AuthRateLimiter {

    private const val PREFS_NAME = "beboss_auth_rate_limiter"
    private const val FREE_ATTEMPTS = 3
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 3_600_000L

    private data class Bucket(
        var failures: Int = 0,
        var lockedUntilMs: Long = 0L
    )

    private val memoryBuckets = ConcurrentHashMap<String, Bucket>()

    fun checkAllowed(
        key: String,
        context: Context? = null,
        nowMs: Long = System.currentTimeMillis()
    ): RateLimitDecision {
        val bucket = loadBucket(key, context)
        if (bucket.lockedUntilMs > nowMs) {
            val remainingSec = ((bucket.lockedUntilMs - nowMs) / 1000L).coerceAtLeast(1L)
            return RateLimitDecision(
                allowed = false,
                remainingSeconds = remainingSec,
                failedAttempts = bucket.failures,
                message = "Too many failed attempts (${bucket.failures}). Try again in ${remainingSec}s."
            )
        }
        return RateLimitDecision(allowed = true, failedAttempts = bucket.failures)
    }

    fun recordFailure(
        key: String,
        context: Context? = null,
        nowMs: Long = System.currentTimeMillis()
    ): RateLimitDecision {
        val bucket = loadBucket(key, context)
        bucket.failures += 1
        if (bucket.failures > FREE_ATTEMPTS) {
            val exponent = (bucket.failures - FREE_ATTEMPTS - 1).coerceIn(0, 8)
            val delayMs = min(MAX_LOCKOUT_MS, BASE_LOCKOUT_MS * (1L shl exponent))
            bucket.lockedUntilMs = nowMs + delayMs
        }
        saveBucket(key, bucket, context)

        val remainingSec = if (bucket.lockedUntilMs > nowMs) {
            ((bucket.lockedUntilMs - nowMs) / 1000L).coerceAtLeast(1L)
        } else 0L

        val msg = if (remainingSec > 0L) {
            "Locked for ${remainingSec}s after ${bucket.failures} failed attempts."
        } else {
            val left = (FREE_ATTEMPTS - bucket.failures).coerceAtLeast(0)
            if (left > 0) "$left attempt(s) remaining before temporary lockout." else ""
        }
        return RateLimitDecision(
            allowed = remainingSec == 0L,
            remainingSeconds = remainingSec,
            failedAttempts = bucket.failures,
            message = msg
        )
    }

    fun recordSuccess(key: String, context: Context? = null) {
        memoryBuckets.remove(key)
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove("fail_$key")
            ?.remove("until_$key")
            ?.apply()
    }

    fun clearAll(context: Context? = null) {
        memoryBuckets.clear()
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.clear()
            ?.apply()
    }

    private fun loadBucket(key: String, context: Context?): Bucket {
        memoryBuckets[key]?.let { return it }
        val bucket = if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Bucket(
                failures = prefs.getInt("fail_$key", 0),
                lockedUntilMs = prefs.getLong("until_$key", 0L)
            )
        } else {
            Bucket()
        }
        memoryBuckets[key] = bucket
        return bucket
    }

    private fun saveBucket(key: String, bucket: Bucket, context: Context?) {
        memoryBuckets[key] = bucket
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putInt("fail_$key", bucket.failures)
            ?.putLong("until_$key", bucket.lockedUntilMs)
            ?.apply()
    }
}
