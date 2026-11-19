package com.example.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BranchDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.CustomerPaymentDao
import com.example.data.dao.LocalAuditLogDao
import com.example.data.dao.PaymentVerificationDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PurchaseDao
import com.example.data.dao.RedeemedVoucherDao
import com.example.data.dao.SaleDao
import com.example.data.dao.ShopProfileDao
import com.example.data.dao.StockTransferDao
import com.example.data.dao.SyncQueueDao
import com.example.data.dao.SyncTombstoneDao
import com.example.data.dao.UserDao
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.LocalAuditLog
import com.example.data.model.PaymentVerificationRequest
import com.example.data.model.Product
import com.example.data.model.PurchaseRecord
import com.example.data.model.RedeemedVoucher
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncTombstone
import com.example.data.model.User
import com.example.util.CryptoKeyManager
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

@Database(
    entities = [
        ShopProfile::class,
        Product::class,
        Customer::class,
        Sale::class,
        SaleItem::class,
        SyncQueueItem::class,
        User::class,
        CustomerPayment::class,
        Branch::class,
        PurchaseRecord::class,
        StockTransfer::class,
        RedeemedVoucher::class,
        PaymentVerificationRequest::class,
        LocalAuditLog::class,
        SyncTombstone::class
    ],
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun saleDao(): SaleDao
    abstract fun shopProfileDao(): ShopProfileDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun userDao(): UserDao
    abstract fun branchDao(): BranchDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun stockTransferDao(): StockTransferDao
    abstract fun redeemedVoucherDao(): RedeemedVoucherDao
    abstract fun paymentVerificationDao(): PaymentVerificationDao
    abstract fun localAuditLogDao(): LocalAuditLogDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao

    companion object {
        const val DATABASE_NAME = "beboss_database.db"
        private const val TAG = "AppDatabase"
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return@synchronized it }
                val appContext = context.applicationContext
                val builder = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)

                if (!CryptoKeyManager.isRobolectricJvm()) {
                    System.loadLibrary("sqlcipher")
                    val passphrase = CryptoKeyManager.getOrCreateDatabasePassphrase(appContext)
                    migrateLegacyPlaintextDatabaseIfNeeded(appContext, passphrase)
                    val factory = SupportOpenHelperFactory(passphrase)
                    builder.openHelperFactory(factory)
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private fun migrateLegacyPlaintextDatabaseIfNeeded(context: Context, passphrase: ByteArray) {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists() || dbFile.length() < SQLITE_HEADER.size) return

            val header = ByteArray(SQLITE_HEADER.size)
            dbFile.inputStream().use { it.read(header) }
            if (!header.contentEquals(SQLITE_HEADER)) {
                // Already SQLCipher-encrypted
                return
            }

            Log.i(TAG, "Migrating legacy unencrypted database to SQLCipher AES-256...")
            val encryptedTmp = File(dbFile.parentFile, "$DATABASE_NAME.enc_tmp")
            if (encryptedTmp.exists()) encryptedTmp.delete()

            val plainDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            try {
                val hexKey = passphrase.joinToString("") { "%02x".format(it) }
                plainDb.rawExecSQL("ATTACH DATABASE '${encryptedTmp.absolutePath}' AS encrypted KEY \"x'$hexKey'\";")
                plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                plainDb.rawExecSQL("DETACH DATABASE encrypted;")
            } finally {
                plainDb.close()
            }

            if (encryptedTmp.exists() && encryptedTmp.length() > 0L) {
                File(dbFile.parentFile, "$DATABASE_NAME-wal").delete()
                File(dbFile.parentFile, "$DATABASE_NAME-shm").delete()
                dbFile.delete()
                if (!encryptedTmp.renameTo(dbFile)) {
                    throw IllegalStateException("Unable to finalize encrypted database migration.")
                }
            }
        }
    }
}
