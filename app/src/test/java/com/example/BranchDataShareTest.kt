package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Branch
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.PurchaseRecord
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BeBossRepository
import com.example.util.HlcClock
import com.example.util.MoneyUtils
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
import com.example.util.SyncPayloadValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BranchDataShareTest {

    @Test
    fun `compressed BBSHARE1 token round-trip preserves branches transfers and purchases`() {
        val profile = ShopProfile(id = 1, name = "Owner", shopName = "BeBoss Kigali", phone = "+250788111222")
        val branches = listOf(
            Branch(id = "main_branch", name = "HQ", code = "HQ-01", isMainBranch = true),
            Branch(id = "br_kimironko", name = "Kimironko", code = "KM-02", isMainBranch = false)
        )
        val users = listOf(
            User(
                id = "u1",
                name = "Alice",
                username = "alice",
                pinHash = "1234",
                role = UserRole.MANAGER,
                assignedBranchId = "br_kimironko",
                assignedBranchName = "Kimironko"
            )
        )
        val products = listOf(
            Product(id = "p1", name = "Coffee", branchId = "main_branch", costPrice = 1000.0, sellingPrice = 1500.0, quantityInStock = 10.0),
            Product(id = "p2", name = "Tea", branchId = "br_kimironko", costPrice = 500.0, sellingPrice = 900.0, quantityInStock = 20.0)
        )
        val customers = listOf(Customer(id = "c1", name = "Jean", phone = "0788000111", debtBalance = 1500.0))
        val sales = listOf(
            Sale(id = "s1", receiptNumber = "RC-01", totalAmount = 900.0, totalCost = 500.0, totalProfit = 400.0, amountPaid = 900.0, branchId = "br_kimironko", branchName = "Kimironko")
        )
        val saleItems = listOf(
            SaleItem(id = "si1", saleId = "s1", productId = "p2", productName = "Tea", quantitySold = 1.0, unitPriceAtSale = 900.0, costPriceAtSale = 500.0, subtotal = 900.0, profit = 400.0, branchId = "br_kimironko")
        )
        val payments = listOf(
            CustomerPayment(id = "pay1", customerId = "c1", customerName = "Jean", amount = 500.0, branchId = "br_kimironko", branchName = "Kimironko")
        )
        val transfers = listOf(
            StockTransfer(
                id = "tr1",
                productId = "p1",
                productName = "Coffee",
                fromBranchId = "main_branch",
                fromBranchName = "HQ",
                toBranchId = "br_kimironko",
                toBranchName = "Kimironko",
                quantity = 5.0,
                unit = "pcs"
            )
        )
        val purchases = listOf(
            PurchaseRecord(
                id = "pur1",
                productId = "p2",
                productName = "Tea",
                quantityPurchased = 20.0,
                unitCostPrice = 500.0,
                totalPurchaseCost = 10000.0,
                supplierName = "Rwanda Tea Co",
                branchId = "br_kimironko",
                branchName = "Kimironko"
            )
        )

        val json = ShopDataTransferManager.createExportJson(
            profile = profile,
            users = users,
            products = products,
            customers = customers,
            sales = sales,
            saleItems = saleItems,
            payments = payments,
            exportedByUser = users.first(),
            branches = branches,
            stockTransfers = transfers,
            purchases = purchases,
            branchIdFilter = "br_kimironko"
        )

        val token = ShopDataTransferManager.createCompressedSyncToken(json)
        assertTrue(token.startsWith("BBSHARE1:"))

        val parsed = ShopDataTransferManager.parseShopPackageString(token)
        assertEquals("Kimironko", parsed.branchScopeName)
        assertEquals(1, parsed.branches.size)
        assertEquals("br_kimironko", parsed.branches.first().id)
        assertEquals(1, parsed.productCount)
        assertEquals("p2", parsed.products.first().id)
        assertEquals(1, parsed.saleCount)
        assertEquals(1, parsed.saleItems.size)
        assertEquals(1, parsed.stockTransfers.size)
        assertEquals(1, parsed.purchases.size)
        assertEquals("br_kimironko", parsed.users.first().assignedBranchId)
        assertEquals("", parsed.users.first().pinHash)
        assertEquals("", parsed.users.first().password)
    }

    @Test
    fun `tampered BBSHARE1 token is rejected by HMAC integrity check`() {
        val profile = ShopProfile(id = 1, name = "Owner", shopName = "BeBoss Kigali", phone = "+250788111222")
        val json = ShopDataTransferManager.createExportJson(
            profile = profile,
            users = emptyList(),
            products = emptyList(),
            customers = emptyList(),
            sales = emptyList(),
            saleItems = emptyList(),
            payments = emptyList(),
            exportedByUser = null
        )
        val token = ShopDataTransferManager.createCompressedSyncToken(json)
        val parts = token.split(":", limit = 3)
        val tampered = "BBSHARE1:00000000:${parts[2]}"

        try {
            ShopDataTransferManager.parseShopPackageString(tampered)
            fail("Expected tampered BBSHARE1 token to throw IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("integrity", ignoreCase = true) == true)
        }
    }

    @Test
    fun `LWW branch merge keeps newer local products and preserves longest active subscription`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = BeBossRepository(db)
            val now = System.currentTimeMillis()
            val localExpiry = now + 30L * 24 * 60 * 60 * 1000
            val olderRemoteExpiry = now + 5L * 24 * 60 * 60 * 1000

            db.shopProfileDao().insertOrUpdateProfile(
                ShopProfile(
                    id = 1,
                    name = "Jean Paul",
                    shopName = "Kigali Store",
                    phone = "+250788111222",
                    subscriptionExpiresAt = localExpiry,
                    subscriptionStatus = "ACTIVE",
                    updatedAt = now
                )
            )
            db.productDao().insertProduct(
                Product(
                    id = "p_local_newer",
                    name = "Coffee Local",
                    sellingPrice = 2500.0,
                    quantityInStock = 18.0,
                    updatedAt = now + 5000
                )
            )
            db.productDao().insertProduct(
                Product(
                    id = "p_remote_newer",
                    name = "Rice Old",
                    sellingPrice = 1000.0,
                    quantityInStock = 5.0,
                    updatedAt = now - 5000
                )
            )

            val summary = ShopImportSummary(
                shopName = "Kigali Store",
                exportedAt = now,
                exportedBy = "BranchManager",
                userCount = 0,
                productCount = 3,
                customerCount = 0,
                saleCount = 1,
                paymentCount = 0,
                users = emptyList(),
                customers = emptyList(),
                saleItems = emptyList(),
                payments = emptyList(),
                shopProfile = ShopProfile(
                    id = 1,
                    name = "Jean Paul",
                    shopName = "Kigali Store",
                    phone = "+250788111222",
                    subscriptionExpiresAt = olderRemoteExpiry,
                    subscriptionStatus = "ACTIVE",
                    updatedAt = now + 1000
                ),
                products = listOf(
                    // Stale remote update must NOT overwrite newer local product
                    Product(
                        id = "p_local_newer",
                        name = "Coffee Stale",
                        sellingPrice = 1900.0,
                        quantityInStock = 2.0,
                        updatedAt = now - 2000
                    ),
                    // Newer remote update MUST overwrite stale local product
                    Product(
                        id = "p_remote_newer",
                        name = "Rice Updated",
                        sellingPrice = 1350.0,
                        quantityInStock = 40.0,
                        updatedAt = now + 8000
                    ),
                    // Brand new sibling branch product MUST be added
                    Product(
                        id = "p_sibling_new",
                        name = "Sugar Sibling",
                        branchId = "br_kimironko",
                        sellingPrice = 1600.0,
                        quantityInStock = 12.0,
                        updatedAt = now
                    )
                ),
                sales = listOf(
                    Sale(
                        id = "sale_sibling_1",
                        receiptNumber = "RC-BR-01",
                        totalAmount = 3200.0,
                        totalCost = 2200.0,
                        totalProfit = 1000.0,
                        amountPaid = 3200.0,
                        branchId = "br_kimironko",
                        branchName = "Kimironko",
                        updatedAt = now
                    )
                )
            )

            val result = repo.mergeShopDataPackageLww(summary)

            // Verify subscription kept the longer local expiry
            val savedProfile = db.shopProfileDao().getShopProfileDirect()!!
            assertEquals(localExpiry, savedProfile.subscriptionExpiresAt)

            // Verify LWW conflict resolution on products
            val localKept = db.productDao().getProductById("p_local_newer")!!
            assertEquals("Coffee Local", localKept.name)
            assertEquals(18.0, localKept.quantityInStock, 0.01)

            val remoteWon = db.productDao().getProductById("p_remote_newer")!!
            assertEquals("Rice Updated", remoteWon.name)
            assertEquals(40.0, remoteWon.quantityInStock, 0.01)

            val addedSibling = db.productDao().getProductById("p_sibling_new")!!
            assertEquals("br_kimironko", addedSibling.branchId)

            val addedSale = db.saleDao().getSaleById("sale_sibling_1")!!
            assertEquals(3200.0, addedSale.totalAmount, 0.01)
            assertEquals(2, result.mergedProducts)
            assertEquals(1, result.mergedSales)
            assertEquals(1, result.conflictsResolved)
        } finally {
            db.close()
        }
    }

    @Test
    fun `SyncPayloadValidator rejects negative numeric fields and malformed payloads`() {
        val maliciousJson = """
            {
              "metadata": {"version": "3.0"},
              "products": [
                {"id": "p_bad", "name": "Fraud Item", "costPrice": -500, "sellingPrice": 1000, "quantityInStock": 5}
              ]
            }
        """.trimIndent()

        try {
            SyncPayloadValidator.validateAndParseRoot(maliciousJson)
            fail("Expected negative costPrice to fail validation")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("costPrice", ignoreCase = true) == true)
        }

        try {
            SyncPayloadValidator.validateAndParseRoot("{\"unknownSection\": {}}")
            fail("Expected unrecognized schema to fail validation")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("Unrecognized", ignoreCase = true) == true)
        }
    }

    @Test
    fun `HlcClock orders causal updates even when wall clocks skew`() {
        val earlierRemoteHlc = HlcClock.pack(physicalMs = 1_760_000_100_000L, logical = 1)
        val skewedLocalOldClock = HlcClock.tick(nowMs = 1_760_000_000_000L)
        // Simulate receiving earlierRemoteHlc on a device whose wall clock is behind
        val observedHlc = HlcClock.observe(earlierRemoteHlc, nowMs = 1_760_000_000_000L)
        assertTrue(HlcClock.physicalOf(observedHlc) >= HlcClock.physicalOf(earlierRemoteHlc))

        val cmp = HlcClock.compareVersions(
            localHlc = skewedLocalOldClock,
            localVersion = 2L,
            localUpdatedAt = 1_760_000_999_000L,
            localDeviceId = "dev_A",
            remoteHlc = observedHlc,
            remoteVersion = 3L,
            remoteUpdatedAt = 1_760_000_000_000L,
            remoteDeviceId = "dev_B"
        )
        // Remote observed HLC wins causally even if its raw wall clock is older
        assertTrue(cmp > 0)

        // Tie-breaker determinism when HLC, version, and timestamps match identically
        val tieBreakForward = HlcClock.compareVersions(
            localHlc = observedHlc,
            localVersion = 3L,
            localUpdatedAt = 1000L,
            localDeviceId = "dev_A",
            remoteHlc = observedHlc,
            remoteVersion = 3L,
            remoteUpdatedAt = 1000L,
            remoteDeviceId = "dev_B"
        )
        val tieBreakReverse = HlcClock.compareVersions(
            localHlc = observedHlc,
            localVersion = 3L,
            localUpdatedAt = 1000L,
            localDeviceId = "dev_B",
            remoteHlc = observedHlc,
            remoteVersion = 3L,
            remoteUpdatedAt = 1000L,
            remoteDeviceId = "dev_A"
        )
        assertTrue(tieBreakForward > 0)
        assertTrue(tieBreakReverse < 0)
    }

    @Test
    fun `tombstone prevents stale branch merge from resurrecting deleted product`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = BeBossRepository(db)
            val olderHlc = HlcClock.tick(System.currentTimeMillis() - 10_000L)
            val product = Product(
                id = "p_del",
                name = "Obsolete Item",
                costPrice = 400.0,
                sellingPrice = 700.0,
                quantityInStock = 10.0,
                updatedAt = System.currentTimeMillis() - 10_000L,
                hlcTimestamp = olderHlc
            )
            db.productDao().insertProduct(product)

            // Owner deletes product -> creates durable tombstone
            val owner = User(id = "u_owner", name = "Owner", username = "owner", role = UserRole.OWNER, isActive = true)
            repo.deleteProduct("p_del", actor = owner)

            val tombstones = repo.getAllTombstonesList()
            assertEquals(1, tombstones.size)
            assertEquals("p_del", tombstones.first().recordId)

            // Stale remote branch packet still contains "p_del" with isDeleted = false
            val staleSummary = ShopImportSummary(
                shopName = "BeBoss Test",
                exportedAt = System.currentTimeMillis(),
                exportedBy = "PeerBranch",
                userCount = 0,
                productCount = 1,
                customerCount = 0,
                saleCount = 0,
                paymentCount = 0,
                users = emptyList(),
                products = listOf(product.copy(isDeleted = false)),
                customers = emptyList(),
                sales = emptyList(),
                saleItems = emptyList(),
                payments = emptyList(),
                shopProfile = null
            )

            repo.mergeShopDataPackageLww(staleSummary)

            val reloaded = db.productDao().getProductById("p_del")
            assertEquals(true, reloaded?.isDeleted ?: true)
        } finally {
            db.close()
        }
    }

    @Test
    fun `atomic sale checkout and sale deletion preserve exact cents inventory and debt`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = BeBossRepository(db)
            val prod = Product(
                id = "p_coffee",
                name = "Espresso Beans",
                costPrice = 12.34,
                sellingPrice = 19.99,
                quantityInStock = 10.0
            )
            val cust = Customer(
                id = "c_alice",
                name = "Alice",
                phone = "0788123456",
                debtBalance = 5.0
            )
            db.productDao().insertProduct(prod)
            db.customerDao().insertCustomer(cust)

            // 3 * 19.99 = 59.97; discount 0.97 -> 59.00; paid 20.00 -> unpaid debt +39.00 (total 44.00)
            assertEquals(5997L, MoneyUtils.multiplyPriceQuantityCents(19.99, 3.0))

            val sale = repo.processSale(
                items = listOf(CartItem(product = prod, quantity = 3.0)),
                customerId = "c_alice",
                customerName = "Alice",
                discountAmount = 0.97,
                paymentMethod = "PARTIAL",
                amountPaid = 20.00,
                notes = "Test atomic checkout"
            )

            assertEquals(59.00, sale.totalAmount, 0.001)
            assertEquals(37.02, sale.totalCost, 0.001)
            assertEquals(21.98, sale.totalProfit, 0.001)
            assertEquals("PARTIAL", sale.paymentStatus)

            assertEquals(7.0, db.productDao().getProductById("p_coffee")!!.quantityInStock, 0.001)
            assertEquals(44.0, db.customerDao().getCustomerById("c_alice")!!.debtBalance, 0.001)

            // Deleting the sale as owner must restock (+3.0 -> 10.0), reduce debt (-39.0 -> 5.0), and emit tombstone
            val owner = User(id = "u_owner", name = "Owner", username = "owner", role = UserRole.OWNER, isActive = true)
            repo.deleteSale(sale.id, actor = owner)

            assertEquals(10.0, db.productDao().getProductById("p_coffee")!!.quantityInStock, 0.001)
            assertEquals(5.0, db.customerDao().getCustomerById("c_alice")!!.debtBalance, 0.001)
            assertTrue(repo.getAllTombstonesList().any { it.tableName == "sales" && it.recordId == sale.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun `domain RBAC blocks cashier without delete permission and logs security audit`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = BeBossRepository(db)
            val cashier = User(
                id = "u_cashier",
                name = "Eric Cashier",
                username = "eric",
                role = UserRole.CASHIER,
                isActive = true,
                canDeleteRecords = false
            )
            db.saleDao().insertSale(
                Sale(id = "s_protected", receiptNumber = "RC-99", totalAmount = 1500.0)
            )

            try {
                repo.deleteSale("s_protected", actor = cashier)
                fail("Expected SecurityException for cashier without delete permission")
            } catch (expected: SecurityException) {
                assertTrue(expected.message?.contains("Access denied") == true)
            }

            // Sale must remain intact and RBAC_DENIED must be logged
            assertEquals("RC-99", db.saleDao().getSaleById("s_protected")?.receiptNumber)
            val logs = db.localAuditLogDao().getRecentLogsDirect(10)
            assertTrue(logs.any { it.action == "RBAC_DENIED" && it.targetId == "s_protected" })
        } finally {
            db.close()
        }
    }
}
