package com.example

import com.example.util.OfflineSubscriptionManager
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingCalculatorTest {

    @Test
    fun testSingleBranchSoloOperatorPrice() {
        val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 1,
            workerCount = 1,
            durationMonths = 1
        )

        assertEquals(5000, pricing.branchBasePrice)
        assertEquals(0, pricing.workerFee)
        assertEquals(5000, pricing.totalPayable)
    }

    @Test
    fun testTwoBranchesSmallTeamPrice() {
        val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 2,
            workerCount = 3,
            durationMonths = 1
        )

        assertEquals(10000, pricing.branchBasePrice)
        assertEquals(2000, pricing.workerFee)
        assertEquals(12000, pricing.totalPayable)
    }

    @Test
    fun testEnterpriseMultiBranchLargeTeamWithAnnualDiscount() {
        // 4 branches, 7 workers, 12 months
        // Base = 20,000, Worker = 4000 + 2*1000 = 6000 => monthly 26,000
        // 12 months raw = 312,000. 20% discount = 62,400 => total 249,600
        val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 4,
            workerCount = 7,
            durationMonths = 12
        )

        assertEquals(20000, pricing.branchBasePrice)
        assertEquals(6000, pricing.workerFee)
        assertEquals(26000, pricing.monthlySubtotal)
        assertEquals(20, pricing.discountPercent)
        assertEquals(249600, pricing.totalPayable)
    }

    @Test
    fun testMultiDeviceAddOnCharges2000PerExtraDevice() {
        // 1 branch (5,000), 1 worker (0), 3 devices (1 primary free + 2 extra * 2,000 = 4,000)
        // Monthly subtotal = 9,000 FRw
        val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 1,
            workerCount = 1,
            durationMonths = 1,
            deviceCount = 3
        )

        assertEquals(3, pricing.deviceCount)
        assertEquals(2, pricing.extraDevicesCount)
        assertEquals(4000, pricing.deviceFee)
        assertEquals(9000, pricing.monthlySubtotal)
        assertEquals(9000, pricing.totalPayable)
    }

    @Test
    fun testMultiDeviceWithQuarterlyDiscount() {
        // 2 branches (10,000) + 3 workers (2,000) + 2 devices (1 extra * 2,000 = 2,000)
        // Monthly = 14,000; 3 months raw = 42,000; 10% discount = 4,200 => 37,800
        val pricing = OfflineSubscriptionManager.calculateSubscriptionPrice(
            branchCount = 2,
            workerCount = 3,
            durationMonths = 3,
            deviceCount = 2
        )

        assertEquals(1, pricing.extraDevicesCount)
        assertEquals(2000, pricing.deviceFee)
        assertEquals(14000, pricing.monthlySubtotal)
        assertEquals(10, pricing.discountPercent)
        assertEquals(4200, pricing.discountAmount)
        assertEquals(37800, pricing.totalPayable)
    }
}
