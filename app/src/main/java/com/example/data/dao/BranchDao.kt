package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Branch
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {

    @Query("SELECT * FROM branches WHERE isActive = 1 ORDER BY isMainBranch DESC, name ASC")
    fun getAllActiveBranches(): Flow<List<Branch>>

    @Query("SELECT * FROM branches WHERE isActive = 1 ORDER BY isMainBranch DESC, name ASC")
    suspend fun getAllActiveBranchesList(): List<Branch>

    @Query("SELECT * FROM branches WHERE id = :id LIMIT 1")
    suspend fun getBranchById(id: String): Branch?

    @Query("SELECT * FROM branches WHERE isMainBranch = 1 LIMIT 1")
    suspend fun getMainBranch(): Branch?

    @Query("SELECT COUNT(*) FROM branches WHERE isActive = 1")
    suspend fun getBranchCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: Branch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBranches(branches: List<Branch>)

    @Update
    suspend fun updateBranch(branch: Branch)

    @Query("UPDATE branches SET isActive = 0, updatedAt = :timestamp WHERE id = :branchId")
    suspend fun softDeleteBranch(branchId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM branches WHERE id = :branchId")
    suspend fun deleteBranchPermanently(branchId: String)
}
