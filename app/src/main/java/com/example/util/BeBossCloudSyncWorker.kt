package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.model.UserRole
import com.example.data.repository.BeBossRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Always-on WorkManager background synchronization worker.
 * Runs automatically when network connectivity returns (even when BeBoss is closed)
 * to sync registration records, shop data, multi-device updates, payment proofs,
 * live admin WhatsApp/USSD config, and Admin direct auto-unlock status with Supabase.
 */
class BeBossCloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val appCtx = applicationContext
            OfflineSubscriptionManager.initConfigCache(appCtx)
            ClockGuard.init(appCtx)

            val db = AppDatabase.getDatabase(appCtx)
            val repository = BeBossRepository(db)
            val profile = repository.getShopProfileDirect()

            // Nothing to sync if user has not created or joined a shop yet
            if (profile.shopName.isBlank()) {
                return@withContext Result.success()
            }

            val users = db.userDao().getAllUsersDirect()
            val ownerUser = users.firstOrNull { it.role == UserRole.OWNER } ?: users.firstOrNull()
            val branches = db.branchDao().getAllActiveBranchesList()
            val deviceId = SupabaseAdminClient.getOrCreateDeviceId(appCtx)

            repository.ensureOwnerPrimaryDevice(
                deviceId = deviceId,
                shopProfile = profile,
                ownerUser = ownerUser,
                mainBranch = branches.firstOrNull { it.isMainBranch } ?: branches.firstOrNull()
            )
            val activeDevicesCount = repository.getActiveDevicesCount().coerceAtLeast(1)

            val adminResult = repository.syncPendingAdminDataWithSupabase(
                shopProfile = repository.getShopProfileDirect(),
                ownerUser = ownerUser,
                branchCount = branches.size.coerceAtLeast(1),
                workerCount = users.count { it.isActive }.coerceAtLeast(1),
                deviceCount = activeDevicesCount,
                context = appCtx
            )

            val mainBranchId = branches.firstOrNull { it.isMainBranch }?.id
                ?: branches.firstOrNull()?.id
                ?: "main_branch"

            val branchReport = repository.performSync(
                activeBranchId = mainBranchId,
                deviceId = deviceId
            )

            if (adminResult.directAutoUnlocked) {
                val updatedProf = repository.getShopProfileDirect()
                BeBossNotificationManager.sendDirectAutoUnlockNotification(
                    context = appCtx,
                    shopName = updatedProf.shopName,
                    daysRemaining = updatedProf.daysRemaining
                )
            }

            if (branchReport.state == CloudSyncState.ERROR && SupabaseAdminClient.isConfigured()) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background cloud sync worker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BeBossCloudSyncWorker"
    }
}
