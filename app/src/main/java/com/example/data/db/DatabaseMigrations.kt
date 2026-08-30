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

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8
    )
}
