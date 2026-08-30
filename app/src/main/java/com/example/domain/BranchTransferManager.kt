package com.example.domain

import com.example.data.dao.BranchDao
import com.example.data.dao.ProductDao
import com.example.data.dao.ShopProfileDao
import com.example.data.dao.StockTransferDao
import com.example.data.dao.SyncQueueDao
import com.example.data.model.Branch
import com.example.data.model.StockTransfer
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Domain Manager handling Multi-Branch Stores and Inter-Branch Stock Transfers.
 */
class BranchTransferManager(
    private val branchDao: BranchDao,
    private val stockTransferDao: StockTransferDao,
    private val productDao: ProductDao,
    private val shopProfileDao: ShopProfileDao,
    private val syncQueueDao: SyncQueueDao
) {
    val allBranches: Flow<List<Branch>> = branchDao.getAllActiveBranches()
    val allStockTransfers: Flow<List<StockTransfer>> = stockTransferDao.getAllTransfers()

    suspend fun getAllBranchesList(): List<Branch> = withContext(Dispatchers.IO) {
        branchDao.getAllActiveBranchesList()
    }

    suspend fun saveBranch(branch: Branch) = withContext(Dispatchers.IO) {
        val toSave = branch.copy(updatedAt = System.currentTimeMillis())
        if (toSave.isMainBranch) {
            val allOtherBranches = branchDao.getAllActiveBranchesList()
            for (b in allOtherBranches) {
                if (b.id != toSave.id && b.isMainBranch) {
                    branchDao.insertBranch(b.copy(isMainBranch = false, updatedAt = System.currentTimeMillis()))
                }
            }
        }
        branchDao.insertBranch(toSave)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "branches",
                recordId = toSave.id,
                operation = "UPDATE",
                payloadJson = """{"id":"${toSave.id}","name":"${toSave.name}","code":"${toSave.code}","address":"${toSave.address}","phone":"${toSave.phone}","managerName":"${toSave.managerName}","isMain":${toSave.isMainBranch},"isActive":${toSave.isActive}}"""
            )
        )
    }

    suspend fun deleteBranch(branchId: String) = withContext(Dispatchers.IO) {
        branchDao.softDeleteBranch(branchId)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "branches",
                recordId = branchId,
                operation = "DELETE",
                payloadJson = """{"id":"$branchId"}"""
            )
        )
    }

    suspend fun ensureDefaultBranches(): List<Branch> = withContext(Dispatchers.IO) {
        val mockIds = setOf("branch_kimironko", "branch_nyabugogo")
        val currentBranches = branchDao.getAllActiveBranchesList()
        currentBranches.filter { it.id in mockIds }.forEach { mockBranch ->
            branchDao.softDeleteBranch(mockBranch.id)
        }

        val count = branchDao.getBranchCount()
        if (count == 0) {
            val profile = shopProfileDao.getShopProfileDirect()
            val shopName = profile?.shopName?.ifBlank { "Main Store" } ?: "Main Store"
            val address = profile?.address?.ifBlank { "Kigali, Rwanda" } ?: "Kigali, Rwanda"
            val phone = profile?.phone ?: ""
            val managerName = profile?.name ?: "Shop Owner"

            val mainBranch = Branch(
                id = "main_branch",
                name = "$shopName (HQ)",
                code = "HQ-01",
                address = address,
                phone = phone,
                managerName = managerName,
                isMainBranch = true,
                colorHex = "#FF6B1A"
            )
            val initialList = listOf(mainBranch)
            branchDao.insertAllBranches(initialList)
            initialList
        } else {
            branchDao.getAllActiveBranchesList()
        }
    }

    suspend fun recordStockTransfer(
        productId: String,
        fromBranchId: String,
        fromBranchName: String,
        toBranchId: String,
        toBranchName: String,
        quantity: Double,
        notes: String,
        transferredBy: String = "Store Manager"
    ): StockTransfer = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductById(productId)
        val prodName = product?.name ?: "Stock Transfer Item"
        val unit = product?.unit ?: "pcs"

        val transferNumber = "TRF-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date(now))

        val transfer = StockTransfer(
            id = UUID.randomUUID().toString(),
            transferNumber = transferNumber,
            productId = productId,
            productName = prodName,
            fromBranchId = fromBranchId,
            fromBranchName = fromBranchName,
            toBranchId = toBranchId,
            toBranchName = toBranchName,
            quantity = quantity.coerceAtLeast(0.0),
            unit = unit,
            transferDate = now,
            status = "COMPLETED",
            notes = notes.trim(),
            transferredBy = transferredBy,
            createdAt = now
        )

        stockTransferDao.insertTransfer(transfer)

        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "stock_transfers",
                recordId = transfer.id,
                operation = "CREATE",
                payloadJson = """{"transferNum":"$transferNumber","productId":"$productId","qty":$quantity,"from":"$fromBranchId","to":"$toBranchId"}"""
            )
        )

        transfer
    }

    suspend fun deleteStockTransfer(transferId: String) = withContext(Dispatchers.IO) {
        stockTransferDao.deleteTransfer(transferId)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "stock_transfers",
                recordId = transferId,
                operation = "DELETE",
                payloadJson = """{"id":"$transferId"}"""
            )
        )
    }
}
