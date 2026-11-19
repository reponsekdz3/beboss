package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ShopDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDeviceDao {

    @Query("SELECT * FROM shop_devices ORDER BY isOwnerPrimary DESC, createdAt ASC")
    fun getAllDevices(): Flow<List<ShopDevice>>

    @Query("SELECT * FROM shop_devices ORDER BY isOwnerPrimary DESC, createdAt ASC")
    suspend fun getAllDevicesList(): List<ShopDevice>

    @Query("SELECT COUNT(*) FROM shop_devices WHERE isActive = 1")
    suspend fun getActiveDevicesCount(): Int

    @Query("SELECT * FROM shop_devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: String): ShopDevice?

    @Query("SELECT * FROM shop_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getByDeviceId(deviceId: String): ShopDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: ShopDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<ShopDevice>)

    @Query("DELETE FROM shop_devices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shop_devices")
    suspend fun clearAll()
}
