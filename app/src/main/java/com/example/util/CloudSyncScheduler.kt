package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules WorkManager network-constrained periodic and immediate background cloud sync
 * so offline registrations, sales, stock, payment proofs, and admin direct unlocks sync
 * automatically whenever internet connectivity appears — even when the app is closed.
 */
object CloudSyncScheduler {

    private const val TAG = "CloudSyncScheduler"
    const val PERIODIC_WORK_NAME = "beboss_periodic_cloud_sync"
    const val IMMEDIATE_WORK_NAME = "beboss_immediate_cloud_sync"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<BeBossCloudSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "schedulePeriodicSync skipped: ${e.message}")
        }
    }

    fun enqueueImmediateSync(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<BeBossCloudSyncWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "enqueueImmediateSync skipped: ${e.message}")
        }
    }
}
