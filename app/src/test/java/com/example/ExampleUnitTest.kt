package com.example

import com.example.data.model.ShopProfile
import com.example.util.OfflineSubscriptionManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `subscription pricing - 1 branch single store tier`() {
        val breakdown = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 1,
            workerCount = 1,
            durationMonths = 1
        )
        assertEquals(5000, breakdown.branchBasePrice)
        assertEquals(0, breakdown.workerFee)
        assertEquals(5000, breakdown.totalPayable)
    }

    @Test
    fun `subscription pricing - 2 branches dual store tier is 10k`() {
        val breakdown = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 2,
            workerCount = 1,
            durationMonths = 1
        )
        assertEquals(10000, breakdown.branchBasePrice)
        assertEquals(0, breakdown.workerFee)
        assertEquals(10000, breakdown.totalPayable)
    }

    @Test
    fun `subscription pricing - 3 or more branches enterprise tier is 20k`() {
        val breakdown = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 3,
            workerCount = 1,
            durationMonths = 1
        )
        assertEquals(20000, breakdown.branchBasePrice)
        assertEquals(0, breakdown.workerFee)
        assertEquals(20000, breakdown.totalPayable)
    }

    @Test
    fun `subscription pricing - staff addon calculates properly`() {
        // 3 workers -> +2,000 FRw
        val breakdown3Staff = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 2,
            workerCount = 3,
            durationMonths = 1
        )
        assertEquals(10000, breakdown3Staff.branchBasePrice)
        assertEquals(2000, breakdown3Staff.workerFee)
        assertEquals(12000, breakdown3Staff.totalPayable)

        // 5 workers -> +4,000 FRw
        val breakdown5Staff = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 1,
            workerCount = 5,
            durationMonths = 1
        )
        assertEquals(5000, breakdown5Staff.branchBasePrice)
        assertEquals(4000, breakdown5Staff.workerFee)
        assertEquals(9000, breakdown5Staff.totalPayable)
    }

    @Test
    fun `voucher validation - dual branch 10k key`() {
        val dummyProfile = ShopProfile(shopName = "Kigali Mart", phone = "0788112233")
        val result = OfflineSubscriptionManager.validateVoucherCode(
            inputCode = "RW10K-2026-ACTIVE",
            shopProfile = dummyProfile,
            branchCount = 2,
            workerCount = 2
        )
        assertTrue(result.isValid)
        assertEquals(30, result.daysToAdd)
        assertEquals(10000, result.verifiedAmount)
    }

    @Test
    fun `voucher validation - multi branch 20k key`() {
        val dummyProfile = ShopProfile(shopName = "Kigali Mart", phone = "0788112233")
        val result = OfflineSubscriptionManager.validateVoucherCode(
            inputCode = "RW20K-2026-ACTIVE",
            shopProfile = dummyProfile,
            branchCount = 4,
            workerCount = 5
        )
        assertTrue(result.isValid)
        assertEquals(30, result.daysToAdd)
        assertEquals(20000, result.verifiedAmount)
    }
}

