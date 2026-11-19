package com.example.util

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Thread-safe 64-bit Hybrid Logical Clock (HLC) for causal multi-branch synchronization.
 * Upper 48 bits encode physical epoch millis; lower 16 bits encode logical sequence counter.
 */
object HlcClock {

    private val lastHlc = AtomicLong(pack(ClockGuard.effectiveNowMs(), 0))

    fun pack(physicalMs: Long, logical: Int): Long {
        val maskedLogical = (logical and 0xFFFF).toLong()
        return (physicalMs shl 16) or maskedLogical
    }

    fun physicalOf(hlc: Long): Long = hlc ushr 16

    fun logicalOf(hlc: Long): Int = (hlc and 0xFFFFL).toInt()

    /**
     * Advances HLC for a local mutation event.
     */
    fun tick(nowMs: Long = ClockGuard.effectiveNowMs()): Long {
        while (true) {
            val prev = lastHlc.get()
            val prevPhys = physicalOf(prev)
            val prevLogic = logicalOf(prev)
            val next = if (nowMs > prevPhys) {
                pack(nowMs, 0)
            } else {
                pack(prevPhys, (prevLogic + 1).coerceAtMost(0xFFFF))
            }
            if (lastHlc.compareAndSet(prev, next)) return next
        }
    }

    /**
     * Merges a remote HLC timestamp on receipt so future local events stay causally ordered after it.
     */
    fun observe(remoteHlc: Long, nowMs: Long = ClockGuard.effectiveNowMs()): Long {
        if (remoteHlc <= 0L) return tick(nowMs)
        while (true) {
            val prev = lastHlc.get()
            val prevPhys = physicalOf(prev)
            val remotePhys = physicalOf(remoteHlc)
            val maxPhys = max(nowMs, max(prevPhys, remotePhys))
            val nextLogic = when {
                maxPhys == prevPhys && maxPhys == remotePhys ->
                    (max(logicalOf(prev), logicalOf(remoteHlc)) + 1).coerceAtMost(0xFFFF)
                maxPhys == prevPhys ->
                    (logicalOf(prev) + 1).coerceAtMost(0xFFFF)
                maxPhys == remotePhys ->
                    (logicalOf(remoteHlc) + 1).coerceAtMost(0xFFFF)
                else -> 0
            }
            val next = pack(maxPhys, nextLogic)
            if (lastHlc.compareAndSet(prev, next)) return next
        }
    }

    /**
     * Deterministic total-order comparator between a local and incoming entity version.
     * Returns > 0 if incoming wins over local, < 0 if local wins, 0 if identical.
     */
    fun compareVersions(
        localHlc: Long,
        localVersion: Long,
        localUpdatedAt: Long,
        localDeviceId: String,
        remoteHlc: Long,
        remoteVersion: Long,
        remoteUpdatedAt: Long,
        remoteDeviceId: String
    ): Int {
        // If both records carry real HLC clocks, compare causally
        if (localHlc > 0L && remoteHlc > 0L && localHlc != remoteHlc) {
            return remoteHlc.compareTo(localHlc)
        }
        // Monotonic per-record version comparison when timestamps tie or clocks drift
        if (remoteVersion != localVersion && (localHlc == 0L || remoteHlc == 0L)) {
            if (remoteUpdatedAt >= localUpdatedAt || remoteVersion > localVersion + 1) {
                return remoteVersion.compareTo(localVersion)
            }
        }
        // Wall-clock timestamp comparison
        if (remoteUpdatedAt != localUpdatedAt) {
            return remoteUpdatedAt.compareTo(localUpdatedAt)
        }
        // Fallback version comparison
        if (remoteVersion != localVersion) {
            return remoteVersion.compareTo(localVersion)
        }
        // Deterministic node-ID tie breaker so all branches converge identically
        return remoteDeviceId.compareTo(localDeviceId)
    }
}
