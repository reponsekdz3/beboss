package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BeBossRepository
import com.example.ui.viewmodel.coordinators.AuthSessionCoordinator
import com.example.util.AppLanguage
import com.example.util.AuthRateLimiter
import com.example.util.ReceiptGenerator
import com.example.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthSecurityTest {

    @Test
    fun `auth rate limiter locks after 3 failures and resets after cooldown or success`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AuthRateLimiter.clearAll(context)
        val key = "pin:test_user"
        val t0 = 1_760_000_000_000L

        assertTrue(AuthRateLimiter.checkAllowed(key, context, t0).allowed)
        assertTrue(AuthRateLimiter.recordFailure(key, context, t0 + 100).allowed) // 1
        assertTrue(AuthRateLimiter.recordFailure(key, context, t0 + 200).allowed) // 2
        assertTrue(AuthRateLimiter.recordFailure(key, context, t0 + 300).allowed) // 3

        val fourth = AuthRateLimiter.recordFailure(key, context, t0 + 400) // 4 -> triggers 30s lock
        assertFalse(fourth.allowed)
        assertTrue(fourth.remainingSeconds >= 29L)
        assertFalse(AuthRateLimiter.checkAllowed(key, context, t0 + 5_000L).allowed)

        // Allowed again once lockout window expires
        assertTrue(AuthRateLimiter.checkAllowed(key, context, t0 + 35_000L).allowed)

        // Successful auth clears bucket immediately
        AuthRateLimiter.recordSuccess(key, context)
        assertEquals(0, AuthRateLimiter.checkAllowed(key, context, t0 + 35_000L).failedAttempts)
    }

    @Test
    fun `biometric unlock with verifiedByHardware false stays locked and logs audit`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        try {
            val repo = BeBossRepository(db)
            val owner = User(
                id = "u_owner",
                name = "Owner",
                username = "owner",
                pinHash = SecurityUtils.hashPin("4829"),
                password = SecurityUtils.hashPassword("Owner2026!"),
                role = UserRole.OWNER,
                isActive = true
            )
            db.userDao().insertUser(owner)

            val prefs = app.getSharedPreferences("auth_test_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("active_user_id", owner.id).apply()

            val coord = AuthSessionCoordinator(
                application = app,
                prefs = prefs,
                db = db,
                repository = repo,
                scope = scope,
                language = MutableStateFlow(AppLanguage.ENGLISH),
                emitSnackbar = {}
            )
            coord.selectLockedOperator(owner)
            coord.unlockWithBiometric(verifiedByHardware = false)

            assertTrue(coord.isLocked.value)
            assertNotNull(coord.authError.value)
            delay(150)

            val audits = db.localAuditLogDao().getRecentLogsDirect(10)
            assertTrue(audits.any { it.action == "UNLOCK_BIOMETRIC_REJECTED" })
        } finally {
            scope.cancel()
            db.close()
        }
    }

    @Test
    fun `receipt offline seal verifies untampered sale and detects total or receipt tampering`() {
        val profile = ShopProfile(
            id = 1L,
            shopName = "Kigali Coffee Hub",
            tinNumber = "102938475",
            taxRate = 18.0,
            showTaxOnReceipt = true,
            currencySymbol = "FRw"
        )
        val sale = Sale(
            id = "sale_001",
            receiptNumber = "RC-2026-001",
            customerName = "Alice",
            saleDate = 1_760_000_000_000L,
            totalAmount = 11_800.0,
            amountPaid = 11_800.0,
            branchId = "main_branch",
            cashierId = "u_1"
        )
        val items = listOf(
            SaleItem(
                id = "item_1",
                saleId = sale.id,
                productId = "p_1",
                productName = "Roasted Beans",
                quantitySold = 2.0,
                unitPriceAtSale = 5_900.0,
                subtotal = 11_800.0
            )
        )

        val seal = ReceiptGenerator.computeOfflineReceiptSeal(sale, profile)
        assertTrue(seal.startsWith("BB-"))
        assertTrue(ReceiptGenerator.verifyOfflineReceiptSeal(sale, profile, seal))

        val tamperedTotal = sale.copy(totalAmount = 1_800.0)
        assertFalse(ReceiptGenerator.verifyOfflineReceiptSeal(tamperedTotal, profile, seal))

        val tax = ReceiptGenerator.computeTaxIncludedAmount(11_800.0, 18.0)
        assertEquals(1_800.0, tax, 0.01)

        val text = ReceiptGenerator.generateTextReceipt(sale, items, profile)
        assertTrue(text.contains("TIN: 102938475"))
        assertTrue(text.contains("Incl. Tax (18.0%)"))
        assertTrue(text.contains("Verify Seal: $seal"))
    }
}
