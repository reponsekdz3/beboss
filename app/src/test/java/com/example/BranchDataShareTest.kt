package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Branch
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
import com.example.util.ShopDataTransferManager
import com.example.util.ShopImportSummary
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
}
