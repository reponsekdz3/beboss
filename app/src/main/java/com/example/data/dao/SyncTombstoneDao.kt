package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncTombstone

@Dao
interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstones ORDER BY deletedAt DESC")
    suspend fun getAllTombstonesList(): List<SyncTombstone>

    @Query("SELECT * FROM sync_tombstones WHERE tableName = :tableName")
    suspend fun getTombstonesForTable(tableName: String): List<SyncTombstone>

    @Query("SELECT * FROM sync_tombstones WHERE deletedAt >= :sinceMs")
    suspend fun getTombstonesSince(sinceMs: Long): List<SyncTombstone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: SyncTombstone)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tombstones: List<SyncTombstone>)

    @Query("DELETE FROM sync_tombstones WHERE id = :id")
    suspend fun deleteTombstone(id: String)

    @Query("DELETE FROM sync_tombstones")
    suspend fun clearAllTombstones()
}
