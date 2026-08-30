package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BranchDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.CustomerPaymentDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PurchaseDao
import com.example.data.dao.RedeemedVoucherDao
import com.example.data.dao.SaleDao
import com.example.data.dao.ShopProfileDao
import com.example.data.dao.StockTransferDao
import com.example.data.dao.SyncQueueDao
import com.example.data.dao.UserDao
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.PurchaseRecord
import com.example.data.model.RedeemedVoucher
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.SyncQueueItem
import com.example.data.model.User

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
        RedeemedVoucher::class
    ],
    version = 8,
    exportSchema = false
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beboss_database.db"
                )
                .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
