package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.DatabaseMigrations
import com.example.data.model.LocalAuditLog
import com.example.data.model.SyncTombstone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private class LegacyV9SqliteHelper(context: Context, dbName: String) :
        SQLiteOpenHelper(context, dbName, null, 9) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE shop_profile (
                    id INTEGER PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT NOT NULL,
                    shopName TEXT NOT NULL,
                    address TEXT NOT NULL,
                    currencyCode TEXT NOT NULL,
                    currencySymbol TEXT NOT NULL,
                    taxRate REAL NOT NULL,
                    tinNumber TEXT NOT NULL,
                    receiptFooter TEXT NOT NULL,
                    receiptHeader TEXT NOT NULL,
                    dailySalesTarget REAL NOT NULL,
                    monthlyRevenueTarget REAL NOT NULL,
                    targetMarginPercent REAL NOT NULL,
                    cashRegisterFloat REAL NOT NULL,
                    receiptPaperWidthMm INTEGER NOT NULL,
                    showLogoOnReceipt INTEGER NOT NULL,
                    showTaxOnReceipt INTEGER NOT NULL,
                    showBarcodeOnReceipt INTEGER NOT NULL,
                    autoPrintReceiptOnSale INTEGER NOT NULL,
                    soundFeedbackEnabled INTEGER NOT NULL,
                    hapticFeedbackEnabled INTEGER NOT NULL,
                    lowStockThresholdDefault REAL NOT NULL,
                    isOnlineSyncEnabled INTEGER NOT NULL,
                    backendServerUrl TEXT NOT NULL,
                    lastSyncedAt INTEGER NOT NULL,
                    subscriptionStatus TEXT NOT NULL,
                    monthlyFeeRwf INTEGER NOT NULL,
                    subscriptionExpiresAt INTEGER NOT NULL,
                    trialStartedAt INTEGER NOT NULL,
                    lastPaymentRef TEXT NOT NULL,
                    lastPaymentAmount INTEGER NOT NULL,
                    lastPaymentDate INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE products (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    costPrice REAL NOT NULL,
                    sellingPrice REAL NOT NULL,
                    quantityInStock REAL NOT NULL,
                    lowStockThreshold REAL NOT NULL,
                    unit TEXT NOT NULL,
                    barcode TEXT NOT NULL,
                    branchId TEXT NOT NULL,
                    isDeleted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE customers (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT NOT NULL,
                    address TEXT NOT NULL,
                    city TEXT NOT NULL,
                    taxIdOrNin TEXT NOT NULL,
                    category TEXT NOT NULL,
                    creditLimit REAL NOT NULL,
                    notes TEXT NOT NULL,
                    debtBalance REAL NOT NULL,
                    isDeleted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE sales (
                    id TEXT PRIMARY KEY NOT NULL,
                    customerId TEXT,
                    customerName TEXT NOT NULL,
                    saleDate INTEGER NOT NULL,
                    totalAmount REAL NOT NULL,
                    totalCost REAL NOT NULL,
                    totalProfit REAL NOT NULL,
                    discountAmount REAL NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    paymentStatus TEXT NOT NULL,
                    amountPaid REAL NOT NULL,
                    notes TEXT NOT NULL,
                    receiptNumber TEXT NOT NULL,
                    branchId TEXT NOT NULL,
                    branchName TEXT NOT NULL,
                    cashierId TEXT NOT NULL,
                    cashierName TEXT NOT NULL,
                    synced INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE sale_items (
                    id TEXT PRIMARY KEY NOT NULL,
                    saleId TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    productName TEXT NOT NULL,
                    category TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    quantitySold REAL NOT NULL,
                    costPriceAtSale REAL NOT NULL,
                    unitPriceAtSale REAL NOT NULL,
                    subtotal REAL NOT NULL,
                    profit REAL NOT NULL,
                    branchId TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE sync_queue (
                    id TEXT PRIMARY KEY NOT NULL,
                    tableName TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    deviceId TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    syncedAt INTEGER,
                    status TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE users (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    username TEXT NOT NULL,
                    email TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    pinHash TEXT NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    assignedBranchId TEXT NOT NULL,
                    assignedBranchName TEXT NOT NULL,
                    profileColorHex TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    canSellPOS INTEGER NOT NULL,
                    canApplyDiscounts INTEGER NOT NULL,
                    canManageInventory INTEGER NOT NULL,
                    canViewCostAndProfit INTEGER NOT NULL,
                    canViewAnalytics INTEGER NOT NULL,
                    canManageCustomers INTEGER NOT NULL,
                    canCollectDebt INTEGER NOT NULL,
                    canDeleteRecords INTEGER NOT NULL,
                    canExportReports INTEGER NOT NULL,
                    canManageCollaborators INTEGER NOT NULL,
                    canManageShopSettings INTEGER NOT NULL,
                    canExportImportData INTEGER NOT NULL,
                    lastLoginAt INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE customer_payments (
                    id TEXT PRIMARY KEY NOT NULL,
                    customerId TEXT NOT NULL,
                    customerName TEXT NOT NULL,
                    amount REAL NOT NULL,
                    previousDebt REAL NOT NULL,
                    remainingDebt REAL NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    receiptNumber TEXT NOT NULL,
                    paymentDate INTEGER NOT NULL,
                    recordedBy TEXT NOT NULL,
                    branchId TEXT NOT NULL,
                    branchName TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE branches (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    code TEXT NOT NULL,
                    address TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    managerUserId TEXT NOT NULL,
                    managerName TEXT NOT NULL,
                    isMainBranch INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    colorHex TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE purchases (
                    id TEXT PRIMARY KEY NOT NULL,
                    productId TEXT NOT NULL,
                    productName TEXT NOT NULL,
                    category TEXT NOT NULL,
                    supplierName TEXT NOT NULL,
                    supplierPhone TEXT NOT NULL,
                    quantityPurchased REAL NOT NULL,
                    unitCostPrice REAL NOT NULL,
                    totalPurchaseCost REAL NOT NULL,
                    sellingPriceAtPurchase REAL NOT NULL,
                    paymentStatus TEXT NOT NULL,
                    invoiceNumber TEXT NOT NULL,
                    branchId TEXT NOT NULL,
                    branchName TEXT NOT NULL,
                    purchasedByUserId TEXT NOT NULL,
                    purchasedByName TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    purchaseDate INTEGER NOT NULL,
                    isSynced INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE stock_transfers (
                    id TEXT PRIMARY KEY NOT NULL,
                    transferNumber TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    productName TEXT NOT NULL,
                    fromBranchId TEXT NOT NULL,
                    fromBranchName TEXT NOT NULL,
                    toBranchId TEXT NOT NULL,
                    toBranchName TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    transferDate INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    transferredBy TEXT NOT NULL,
                    synced INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE redeemed_vouchers (
                    id TEXT PRIMARY KEY NOT NULL,
                    voucherCode TEXT NOT NULL,
                    voucherHash TEXT NOT NULL,
                    planName TEXT NOT NULL,
                    daysAdded INTEGER NOT NULL,
                    verifiedAmount INTEGER NOT NULL,
                    redeemedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE payment_verification_requests (
                    id TEXT PRIMARY KEY NOT NULL,
                    shopName TEXT NOT NULL,
                    ownerUsername TEXT NOT NULL,
                    ownerPhone TEXT NOT NULL,
                    deviceChallenge TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    ussdCode TEXT NOT NULL,
                    payerPhone TEXT NOT NULL,
                    amountRwf INTEGER NOT NULL,
                    durationMonths INTEGER NOT NULL,
                    branchCount INTEGER NOT NULL,
                    workerCount INTEGER NOT NULL,
                    telcoReference TEXT NOT NULL,
                    status TEXT NOT NULL,
                    generatedUnlockCode TEXT NOT NULL,
                    syncedToSupabase INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // Seed representative v9 rows to confirm non-destructive migration
            val now = 1_760_000_000_000L
            db.execSQL(
                """
                INSERT INTO shop_profile VALUES (
                    1, 'Jean Paul', '+250788123456', 'owner@beboss.rw', 'BeBoss Kigali',
                    'Kigali', 'RWF', 'FRw', 18.0, '102030', 'Thanks', 'BEBOSS',
                    150000.0, 4500000.0, 25.0, 20000.0, 58, 1, 1, 1, 0, 1, 1, 5.0,
                    1, 'https://api.beboss.app/v1', 0, 'ACTIVE', 5000, $now, $now,
                    'REF-9', 5000, $now, $now, $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO branches VALUES (
                    'main_branch', 'Kigali HQ', 'HQ-01', 'KN 4 Ave', '+250788123456',
                    'u_owner', 'Jean Paul', 1, 1, '#FF6B1A', $now, $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO products VALUES (
                    'p_v9_1', 'Arabica Coffee 500g', 'Beverages', 2200.0, 3500.0, 14.0,
                    5.0, 'pcs', '5901234123457', 'main_branch', 0, $now, $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO customers VALUES (
                    'c_v9_1', 'Aline Uwimana', '0788999000', '', 'Remera', 'Kigali',
                    '', 'Regular Customer', 500000.0, '', 2500.0, 0, $now, $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO sales VALUES (
                    's_v9_1', 'c_v9_1', 'Aline Uwimana', $now, 3500.0, 2200.0, 1300.0,
                    0.0, 'CASH', 'PAID', 3500.0, '', 'RC-V9-01', 'main_branch',
                    'Kigali HQ', 'u_owner', 'Jean Paul', 1, $now
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO sale_items VALUES (
                    'si_v9_1', 's_v9_1', 'p_v9_1', 'Arabica Coffee 500g', 'Beverages',
                    'pcs', 1.0, 2200.0, 3500.0, 3500.0, 1300.0, 'main_branch'
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    @Test
    fun `migration 9 to 10 preserves data adds causal columns indices and audit logs table`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v9_to_v10_test.db"
        context.deleteDatabase(dbName)

        // Create and populate exact v9 schema
        LegacyV9SqliteHelper(context, dbName).use { helper ->
            helper.writableDatabase.close()
        }

        // Open with Room v10 + ALL_MIGRATIONS; RoomOpenHelper verifies every column and index
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            // Verify integrity_check passes cleanly
            val sqliteDb = roomDb.openHelper.writableDatabase
            sqliteDb.query("PRAGMA integrity_check").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ok", cursor.getString(0))
            }

            // Verify all expected v10 performance indices exist
            val expectedIndices = setOf(
                "index_products_barcode",
                "index_products_branchId",
                "index_products_updatedAt",
                "index_customers_phone",
                "index_customers_debtBalance",
                "index_sales_customerId",
                "index_sales_branchId",
                "index_sales_saleDate",
                "index_sales_synced",
                "index_sale_items_saleId",
                "index_sale_items_productId",
                "index_customer_payments_customerId",
                "index_customer_payments_branchId",
                "index_customer_payments_paymentDate",
                "index_purchases_productId",
                "index_purchases_branchId",
                "index_stock_transfers_productId",
                "index_stock_transfers_fromBranchId",
                "index_stock_transfers_toBranchId",
                "index_sync_queue_status_nextAttemptAt",
                "index_local_audit_logs_createdAt",
                "index_local_audit_logs_action",
                "index_sync_tombstones_tableName_deletedAt"
            )
            val actualIndices = mutableSetOf<String>()
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
                while (cursor.moveToNext()) {
                    actualIndices.add(cursor.getString(0))
                }
            }
            for (idx in expectedIndices) {
                assertTrue("Missing v10 index: $idx", actualIndices.contains(idx))
            }

            // Verify v9 rows survived intact with default syncVersion = 1L
            val profile = roomDb.shopProfileDao().getShopProfileDirect()
            assertNotNull(profile)
            assertEquals("BeBoss Kigali", profile?.shopName)
            assertEquals(1L, profile?.syncVersion)

            val product = roomDb.productDao().getProductById("p_v9_1")
            assertNotNull(product)
            assertEquals("Arabica Coffee 500g", product?.name)
            assertEquals(14.0, product?.quantityInStock ?: 0.0, 0.001)
            assertEquals(1L, product?.syncVersion)

            val customer = roomDb.customerDao().getCustomerById("c_v9_1")
            assertNotNull(customer)
            assertEquals(2500.0, customer?.debtBalance ?: 0.0, 0.001)

            val sale = roomDb.saleDao().getSaleById("s_v9_1")
            assertNotNull(sale)
            assertEquals("RC-V9-01", sale?.receiptNumber)

            // Verify new local_audit_logs table is operational
            roomDb.localAuditLogDao().insertLog(
                LocalAuditLog(
                    id = "log_v10_1",
                    actorName = "System",
                    actorRole = "SYSTEM",
                    action = "MIGRATION_VERIFIED",
                    targetId = dbName,
                    details = "v9->v10 passed"
                )
            )
            val logs = roomDb.localAuditLogDao().getRecentLogsDirect(10)
            assertEquals(1, logs.size)
            assertEquals("MIGRATION_VERIFIED", logs.first().action)

            // Verify new v11 sync_tombstones table is operational
            roomDb.syncTombstoneDao().insertTombstone(
                SyncTombstone(
                    id = SyncTombstone.keyFor("products", "p_v9_deleted"),
                    tableName = "products",
                    recordId = "p_v9_deleted",
                    deletedAt = 1_760_000_050_000L
                )
            )
            val tombstones = roomDb.syncTombstoneDao().getAllTombstonesList()
            assertEquals(1, tombstones.size)
            assertEquals("p_v9_deleted", tombstones.first().recordId)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
