package com.example

import android.app.Application
import com.example.util.BeBossNotificationManager
import com.example.util.ClockGuard
import com.example.util.CloudSyncScheduler
import com.example.util.OfflineSubscriptionManager

class BeBossApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ClockGuard.init(this)
        OfflineSubscriptionManager.initConfigCache(this)
        BeBossNotificationManager.createNotificationChannels(this)
        CloudSyncScheduler.schedulePeriodicSync(this)
    }
}
