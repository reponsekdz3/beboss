package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingQueueFlow(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingQueueDirect(): List<SyncQueueItem>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueItem)

    @Update
    suspend fun updateItem(item: SyncQueueItem)

    @Query("UPDATE sync_queue SET status = 'SYNCED', syncedAt = :syncedAt WHERE id = :id")
    suspend fun markItemSynced(id: String, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'SYNCED', syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markAllSynced(ids: List<String>, syncedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun purgeSyncedItems()

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}
