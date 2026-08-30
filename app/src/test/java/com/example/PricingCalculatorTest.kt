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
}
