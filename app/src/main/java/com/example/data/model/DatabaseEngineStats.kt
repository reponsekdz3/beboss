package com.example.data.model

data class DatabaseEngineStats(
    val productCount: Int = 0,
    val salesCount: Int = 0,
    val saleItemsCount: Int = 0,
    val customerCount: Int = 0,
    val paymentsCount: Int = 0,
    val purchasesCount: Int = 0,
    val branchesCount: Int = 0,
    val usersCount: Int = 0,
    val syncQueueCount: Int = 0,
    val totalRecordsCount: Int = 0,
    val fileSizeBytes: Long = 0L,
    val fileSizeFormatted: String = "0 KB",
    val journalMode: String = "WAL",
    val pageSize: Long = 4096L,
    val pageCount: Long = 0L,
    val integrityStatus: String = "OK",
    val lastOptimizedAt: Long = System.currentTimeMillis()
)

data class DatabaseMaintenanceResult(
    val success: Boolean,
    val message: String,
    val durationMs: Long,
    val stats: DatabaseEngineStats? = null
)
