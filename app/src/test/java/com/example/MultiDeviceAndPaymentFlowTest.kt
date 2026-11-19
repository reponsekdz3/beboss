package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.model.ShopProfile
import com.example.util.AdminPaymentConfig
import com.example.util.CloudSyncScheduler
import com.example.util.OfflineSubscriptionManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiDeviceAndPaymentFlowTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OfflineSubscriptionManager.saveDynamicPaymentConfig(context, AdminPaymentConfig())
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {
        OfflineSubscriptionManager.saveDynamicPaymentConfig(context, AdminPaymentConfig())
    }

    @Test
    fun `dynamic admin payment config updates MTN merchant and phone fallback USSD and Airtel USSD`() {
        val merchantConfig = AdminPaymentConfig(
            adminWhatsAppNumber = "+250788000111",
            mtnMoMoCode = "998877",
            mtnMoMoPhone = "0788112233",
            mtnRecipientName = "Kigali Tech Ltd",
            airtelMoneyPhone = "0733998877",
            airtelRecipientName = "Kigali Tech Ltd",
            perDeviceFeeRwf = 2000,
            updatedAt = 1_760_000_000_000L
        )
        OfflineSubscriptionManager.saveDynamicPaymentConfig(context, merchantConfig)

        assertEquals(
            "*182*8*1*998877*7000#",
            OfflineSubscriptionManager.buildMtnUssdCode(7000)
        )
        assertEquals(
            "*500*1*1*0733998877*7000#",
            OfflineSubscriptionManager.buildAirtelUssdCode(7000)
        )
        assertEquals("+250788000111", OfflineSubscriptionManager.getCurrentPaymentConfig().adminWhatsAppNumber)

        // When merchant code is blank, MTN USSD must fall back cleanly to P2P *182*1*1*<phone>*<amt>#
        OfflineSubscriptionManager.saveDynamicPaymentConfig(
            context,
            merchantConfig.copy(mtnMoMoCode = "")
        )
        assertEquals(
            "*182*1*1*0788112233*9000#",
            OfflineSubscriptionManager.buildMtnUssdCode(9000)
        )
    }

    @Test
    fun `payment proof WhatsApp message includes username, registered phone, sender phone and payer full name`() {
        val profile = ShopProfile(
            name = "Jean Claude",
            phone = "+250788554433",
            shopName = "BeBoss Kigali Electronics"
        )

        val message = OfflineSubscriptionManager.buildPaymentProofWhatsAppMessage(
            shopProfile = profile,
            ownerUsername = "jclaude",
            registeredPhone = "+250788554433",
            provider = "MTN MoMo",
            ussdCode = "*182*8*1*054321*7000#",
            amountRwf = 7000,
            durationMonths = 1,
            branchCount = 1,
            workerCount = 1,
            deviceCount = 2,
            payerPhone = "0789112233",
            payerFullName = "Marie Rose Mukamana",
            telcoReference = "FT251109.1234.A12345",
            requestId = "req-12345678"
        )

        assertTrue(message.contains("*Account Username:* jclaude"))
        assertTrue(message.contains("*Registered Shop Phone:* +250788554433"))
        assertTrue(message.contains("*Sender MoMo Phone:* 0789112233"))
        assertTrue(message.contains("*Names on Sender Phone:* Marie Rose Mukamana"))
        assertTrue(message.contains("*USSD Dialed:* *182*8*1*054321*7000#"))
        assertTrue(message.contains("*Amount Paid:* 7000 FRw"))
        assertTrue(message.contains("2 Device(s)"))
        assertTrue(message.contains("FT251109.1234.A12345"))
    }

    @Test
    fun `CloudSyncScheduler enqueues periodic and immediate workers with CONNECTED network constraint`() {
        CloudSyncScheduler.schedulePeriodicSync(context)
        CloudSyncScheduler.enqueueImmediateSync(context)

        val workManager = WorkManager.getInstance(context)

        val periodicInfos = workManager.getWorkInfosForUniqueWork(CloudSyncScheduler.PERIODIC_WORK_NAME).get()
        assertEquals(1, periodicInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, periodicInfos.first().state)
        assertEquals(NetworkType.CONNECTED, periodicInfos.first().constraints.requiredNetworkType)

        val immediateInfos = workManager.getWorkInfosForUniqueWork(CloudSyncScheduler.IMMEDIATE_WORK_NAME).get()
        assertEquals(1, immediateInfos.size)
        assertEquals(NetworkType.CONNECTED, immediateInfos.first().constraints.requiredNetworkType)
    }
}
