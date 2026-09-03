package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RedeemedVoucher
import kotlinx.coroutines.flow.Flow

@Dao
interface RedeemedVoucherDao {
    @Query("SELECT * FROM redeemed_vouchers ORDER BY redeemedAt DESC")
    fun getAllRedeemedVouchers(): Flow<List<RedeemedVoucher>>

    @Query("SELECT voucherHash FROM redeemed_vouchers")
    suspend fun getAllRedeemedHashes(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM redeemed_vouchers WHERE voucherHash = :hash LIMIT 1)")
    suspend fun isVoucherRedeemed(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedeemedVoucher(voucher: RedeemedVoucher)

    @Query("DELETE FROM redeemed_vouchers")
    suspend fun clearAllVouchers()
}
