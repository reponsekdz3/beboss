package com.example.util

import android.util.Log
import com.example.BuildConfig

/**
 * Structured logging helper with automatic PII / secret scrubbing and release build suppression.
 * Never logs PINs, passwords, Bearer tokens, API keys, BB2 codes, or raw DataShare packets.
 */
object BeBossLogger {

    private val secretPatterns = listOf(
        Regex("""(?i)(pin|password|passcode|pinHash|secret|token|apikey|authorization|x-shop-sync-key)\s*[:=]\s*["']?([^\s,"'}\]]+)"""),
        Regex("""BB2-[A-Z0-9-]+"""),
        Regex("""BBSHARE1:[A-Za-z0-9+/=:-]+"""),
        Regex("""pbkdf2:[0-9]+:[a-fA-F0-9]+:[a-fA-F0-9]+"""),
        Regex("""Bearer\s+[A-Za-z0-9._-]+""")
    )

    fun scrub(message: String): String {
        var sanitized = message
        secretPatterns.forEachIndexed { idx, regex ->
            sanitized = when (idx) {
                0 -> regex.replace(sanitized) { mr -> "${mr.groupValues[1]}=[REDACTED]" }
                else -> regex.replace(sanitized, "[REDACTED]")
            }
        }
        return sanitized
    }

    fun d(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        safeLog(Log.DEBUG, tag, scrub(message), null)
    }

    fun i(tag: String, message: String) {
        safeLog(Log.INFO, tag, scrub(message), null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val summary = if (throwable != null) {
            "${scrub(message)} (${throwable::class.java.simpleName}: ${scrub(throwable.message.orEmpty())})"
        } else {
            scrub(message)
        }
        safeLog(Log.WARN, tag, summary, null)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val summary = if (throwable != null) {
            "${scrub(message)} (${throwable::class.java.simpleName}: ${scrub(throwable.message.orEmpty())})"
        } else {
            scrub(message)
        }
        safeLog(Log.ERROR, tag, summary, null)
    }

    private fun safeLog(priority: Int, tag: String, msg: String, tr: Throwable?) {
        try {
            when (priority) {
                Log.DEBUG -> Log.d(tag, msg)
                Log.INFO -> Log.i(tag, msg)
                Log.WARN -> if (tr != null) Log.w(tag, msg, tr) else Log.w(tag, msg)
                Log.ERROR -> if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
            }
        } catch (_: Throwable) {
            // Ignored on plain JVM unit tests
        }
    }
}
