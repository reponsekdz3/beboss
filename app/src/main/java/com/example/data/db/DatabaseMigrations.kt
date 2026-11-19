package com.example.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Robust non-destructive schema migrations for BeBoss database.
 * Preserves all sales, products, customers, and staff history across app upgrades.
 */
object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN isOnlineSyncEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN backendServerUrl TEXT NOT NULL DEFAULT 'https://api.beboss.app/v1'")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN subscriptionStatus TEXT NOT NULL DEFAULT 'ACTIVE'")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN monthlyFeeRwf INTEGER NOT NULL DEFAULT 5000")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN subscriptionExpiresAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN trialStartedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN lastPaymentRef TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN lastPaymentAmount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN lastPaymentDate INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_payments (
                    id TEXT PRIMARY KEY NOT NULL,
                    customerId TEXT NOT NULL,
                    customerName TEXT NOT NULL,
                    amount REAL NOT NULL,
                    previousDebt REAL NOT NULL,
                    remainingDebt REAL NOT NULL,
                    paymentMethod TEXT NOT NULL DEFAULT 'CASH',
                    paymentDate INTEGER NOT NULL,
                    receiptNumber TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    recordedBy TEXT NOT NULL,
                    branchId TEXT NOT NULL DEFAULT 'main_branch',
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS branches (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    code TEXT NOT NULL,
                    address TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    managerName TEXT NOT NULL,
                    isMainBranch INTEGER NOT NULL DEFAULT 0,
                    colorHex TEXT NOT NULL DEFAULT '#FF6B1A',
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS purchase_records (
                    id TEXT PRIMARY KEY NOT NULL,
                    productId TEXT NOT NULL,
                    productName TEXT NOT NULL,
                    category TEXT NOT NULL,
                    supplierName TEXT NOT NULL,
                    supplierPhone TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
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
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stock_transfers (
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
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS redeemed_vouchers (
                    id TEXT PRIMARY KEY NOT NULL,
                    voucherCode TEXT NOT NULL,
                    voucherHash TEXT NOT NULL,
                    planName TEXT NOT NULL,
                    daysAdded INTEGER NOT NULL,
                    verifiedAmount INTEGER NOT NULL,
                    redeemedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS payment_verification_requests (
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
                    syncedToSupabase INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val causalTables = listOf("products", "customers", "branches", "users", "sales", "shop_profile")
            for (table in causalTables) {
                db.execSQL("ALTER TABLE $table ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE $table ADD COLUMN hlcTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE $table ADD COLUMN lastModifiedDeviceId TEXT NOT NULL DEFAULT ''")
            }

            db.execSQL("ALTER TABLE sync_queue ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sync_queue ADD COLUMN nextAttemptAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sync_queue ADD COLUMN lastError TEXT NOT NULL DEFAULT ''")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_audit_logs (
                    id TEXT PRIMARY KEY NOT NULL,
                    actorName TEXT NOT NULL,
                    actorRole TEXT NOT NULL,
                    action TEXT NOT NULL,
                    targetId TEXT NOT NULL,
                    details TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // Performance & query indices matching Room @Index definitions
            db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products(barcode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_products_branchId ON products(branchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_products_updatedAt ON products(updatedAt)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_phone ON customers(phone)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_debtBalance ON customers(debtBalance)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_customerId ON sales(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_branchId ON sales(branchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_saleDate ON sales(saleDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_synced ON sales(synced)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_saleId ON sale_items(saleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_productId ON sale_items(productId)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_customerId ON customer_payments(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_branchId ON customer_payments(branchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_paymentDate ON customer_payments(paymentDate)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_productId ON purchases(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_branchId ON purchases(branchId)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_productId ON stock_transfers(productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_fromBranchId ON stock_transfers(fromBranchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transfers_toBranchId ON stock_transfers(toBranchId)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status_nextAttemptAt ON sync_queue(status, nextAttemptAt)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_local_audit_logs_createdAt ON local_audit_logs(createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_local_audit_logs_action ON local_audit_logs(action)")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_tombstones (
                    id TEXT PRIMARY KEY NOT NULL,
                    tableName TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    deletedAt INTEGER NOT NULL,
                    hlcTimestamp INTEGER NOT NULL,
                    deletedByDeviceId TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_sync_tombstones_tableName_deletedAt ON sync_tombstones(tableName, deletedAt)"
            )
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN deviceCountLimit INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN adminWhatsAppNumber TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN autoUnlockedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE shop_profile ADD COLUMN lastAppliedCloudUnlockAt INTEGER NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE payment_verification_requests ADD COLUMN payerFullName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE payment_verification_requests ADD COLUMN deviceCount INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE payment_verification_requests ADD COLUMN approvalMode TEXT NOT NULL DEFAULT ''")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS shop_devices (
                    id TEXT PRIMARY KEY NOT NULL,
                    shopSyncKey TEXT NOT NULL,
                    deviceId TEXT NOT NULL,
                    deviceName TEXT NOT NULL,
                    joinCode TEXT NOT NULL,
                    assignedUserId TEXT NOT NULL,
                    assignedUsername TEXT NOT NULL,
                    assignedFullName TEXT NOT NULL,
                    role TEXT NOT NULL,
                    branchId TEXT NOT NULL,
                    branchName TEXT NOT NULL,
                    isOwnerPrimary INTEGER NOT NULL DEFAULT 0,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    monthlyFeeAddonRwf INTEGER NOT NULL DEFAULT 2000,
                    lastSyncedAt INTEGER NOT NULL DEFAULT 0,
                    syncedToSupabase INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shop_devices_deviceId ON shop_devices(deviceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shop_devices_joinCode ON shop_devices(joinCode)")
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12
    )
}
