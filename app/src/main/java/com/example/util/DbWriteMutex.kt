package com.example.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide cooperative write mutex serializing critical Room transactions
 * (POS checkout, inventory adjustments, sale deletion, and multi-branch CRDT merges)
 * to prevent concurrent lost-update stock/debt anomalies.
 */
object DbWriteMutex {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }
}
