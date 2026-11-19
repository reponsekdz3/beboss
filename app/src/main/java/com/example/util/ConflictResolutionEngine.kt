package com.example.util

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.LocalAuditLog
import com.example.data.model.Sale
import com.example.data.model.SyncTombstone
import com.example.data.repository.BranchMergeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified causal + ledger-CRDT conflict resolution engine shared across
 * DataShare imports, P2P LAN sync, and Cloud multi-branch sync.
 *
 * Design guarantees:
 * 1. Mutable master entities (ShopProfile, Branch, User, Product, Customer, Sale) resolve via
 *    Hybrid Logical Clock + monotonic syncVersion + wall-clock + node tie-breaker.
 * 2. Durable Deletion Tombstones (`sync_tombstones`) prevent deleted records from resurrecting
 *    across offline/online multi-branch merges.
 * 3. Redacted/blank credentials from peer exports NEVER overwrite existing local PBKDF2 hashes.
 * 4. Immutable ledger events (SaleItem, CustomerPayment, PurchaseRecord, StockTransfer) act as a
 *    grow-only set CRDT — deduplicated by ID and never dropped or mutated.
 * 5. Serialized via `DbWriteMutex` inside a single Room transaction so concurrent syncs/checkouts stay consistent.
 */
object ConflictResolutionEngine {

    suspend fun mergePackage(
        database: AppDatabase,
        summary: ShopImportSummary,
        sourceLabel: String = "DataShare"
    ): BranchMergeResult = withContext(Dispatchers.IO) {
        val branchDao = database.branchDao()
        val userDao = database.userDao()
        val productDao = database.productDao()
        val customerDao = database.customerDao()
        val saleDao = database.saleDao()
        val paymentDao = database.customerPaymentDao()
        val purchaseDao = database.purchaseDao()
        val transferDao = database.stockTransferDao()
        val profileDao = database.shopProfileDao()
        val auditDao = database.localAuditLogDao()
        val tombstoneDao = database.syncTombstoneDao()

        DbWriteMutex.withLock {
            database.withTransaction {
                var mergedBranches = 0
                var mergedUsers = 0
                var mergedProducts = 0
                var mergedCustomers = 0
                var mergedSales = 0
                var mergedSaleItems = 0
                var mergedPayments = 0
                var mergedPurchases = 0
                var mergedTransfers = 0
                var conflicts = 0

                // 0. Merge durable deletion tombstones first
                val tombstoneMap = tombstoneDao.getAllTombstonesList().associateBy { it.id }.toMutableMap()
                val tombstonesToInsert = mutableListOf<SyncTombstone>()
                for (remoteTomb in summary.tombstones) {
                    if (remoteTomb.hlcTimestamp > 0L) HlcClock.observe(remoteTomb.hlcTimestamp)
                    val key = SyncTombstone.keyFor(remoteTomb.tableName, remoteTomb.recordId)
                    val normalized = remoteTomb.copy(id = key)
                    val existingTomb = tombstoneMap[key]
                    if (existingTomb == null ||
                        normalized.deletedAt > existingTomb.deletedAt ||
                        normalized.hlcTimestamp > existingTomb.hlcTimestamp
                    ) {
                        tombstoneMap[key] = normalized
                        tombstonesToInsert.add(normalized)
                    }
                }
                if (tombstonesToInsert.isNotEmpty()) {
                    tombstoneDao.insertAll(tombstonesToInsert)
                }

                fun isDeletedByTombstone(table: String, recordId: String, entityUpdatedAt: Long, entityHlc: Long): Boolean {
                    val tomb = tombstoneMap[SyncTombstone.keyFor(table, recordId)] ?: return false
                    return when {
                        tomb.hlcTimestamp > 0L && entityHlc > 0L -> tomb.hlcTimestamp >= entityHlc
                        else -> tomb.deletedAt >= entityUpdatedAt
                    }
                }

                // 1. ShopProfile merge: preserve longest active subscription & causal metadata
                summary.shopProfile?.let { remoteProf ->
                    HlcClock.observe(remoteProf.hlcTimestamp)
                    val localProf = profileDao.getShopProfileDirect()
                    if (localProf == null) {
                        profileDao.insertOrUpdateProfile(remoteProf.copy(id = 1L))
                    } else {
                        val bestExpiry = maxOf(localProf.subscriptionExpiresAt, remoteProf.subscriptionExpiresAt)
                        val now = ClockGuard.effectiveNowMs()
                        val bestStatus = if (bestExpiry > now) "ACTIVE" else localProf.subscriptionStatus
                        val winner = HlcClock.compareVersions(
                            localProf.hlcTimestamp, localProf.syncVersion, localProf.updatedAt, localProf.lastModifiedDeviceId,
                            remoteProf.hlcTimestamp, remoteProf.syncVersion, remoteProf.updatedAt, remoteProf.lastModifiedDeviceId
                        )
                        val mergedProf = if (winner > 0) {
                            remoteProf.copy(
                                id = 1L,
                                subscriptionExpiresAt = bestExpiry,
                                subscriptionStatus = bestStatus,
                                lastPaymentRef = if (localProf.lastPaymentDate >= remoteProf.lastPaymentDate) localProf.lastPaymentRef else remoteProf.lastPaymentRef,
                                lastPaymentAmount = if (localProf.lastPaymentDate >= remoteProf.lastPaymentDate) localProf.lastPaymentAmount else remoteProf.lastPaymentAmount,
                                lastPaymentDate = maxOf(localProf.lastPaymentDate, remoteProf.lastPaymentDate),
                                isOnlineSyncEnabled = localProf.isOnlineSyncEnabled,
                                syncVersion = maxOf(localProf.syncVersion, remoteProf.syncVersion),
                                hlcTimestamp = maxOf(localProf.hlcTimestamp, remoteProf.hlcTimestamp)
                            )
                        } else {
                            localProf.copy(
                                subscriptionExpiresAt = bestExpiry,
                                subscriptionStatus = bestStatus
                            )
                        }
                        profileDao.insertOrUpdateProfile(mergedProf)
                    }
                }

                // 2. Branches
                val existingBranches = branchDao.getAllActiveBranchesList().associateBy { it.id }
                val branchesToWrite = mutableListOf<com.example.data.model.Branch>()
                for (remoteBranch in summary.branches) {
                    HlcClock.observe(remoteBranch.hlcTimestamp)
                    if (isDeletedByTombstone("branches", remoteBranch.id, remoteBranch.updatedAt, remoteBranch.hlcTimestamp)) {
                        continue
                    }
                    val local = existingBranches[remoteBranch.id]
                    if (local == null) {
                        branchesToWrite.add(remoteBranch)
                        mergedBranches++
                    } else if (HlcClock.compareVersions(
                            local.hlcTimestamp, local.syncVersion, local.updatedAt, local.lastModifiedDeviceId,
                            remoteBranch.hlcTimestamp, remoteBranch.syncVersion, remoteBranch.updatedAt, remoteBranch.lastModifiedDeviceId
                        ) > 0
                    ) {
                        branchesToWrite.add(remoteBranch)
                        mergedBranches++
                        conflicts++
                    }
                }
                if (branchesToWrite.isNotEmpty()) branchDao.insertAllBranches(branchesToWrite)
                for ((_, tomb) in tombstoneMap) {
                    if (tomb.tableName == "branches") {
                        val local = existingBranches[tomb.recordId]
                        if (local != null && isDeletedByTombstone("branches", local.id, local.updatedAt, local.hlcTimestamp)) {
                            branchDao.softDeleteBranch(local.id, tomb.deletedAt)
                        }
                    }
                }

                // 3. Users — NEVER overwrite existing credentials with blank/redacted peer hashes
                val existingUsers = userDao.getAllUsersDirect().associateBy { it.id }
                val usersToWrite = mutableListOf<com.example.data.model.User>()
                for (remoteUser in summary.users) {
                    HlcClock.observe(remoteUser.hlcTimestamp)
                    if (isDeletedByTombstone("users", remoteUser.id, remoteUser.updatedAt, remoteUser.hlcTimestamp)) {
                        continue
                    }
                    val local = existingUsers[remoteUser.id]
                    if (local == null) {
                        val safeRemote = remoteUser.copy(
                            pinHash = remoteUser.pinHash.takeIf { it.startsWith("pbkdf2:") }.orEmpty(),
                            password = remoteUser.password.takeIf { it.startsWith("pbkdf2:") }.orEmpty()
                        )
                        usersToWrite.add(safeRemote)
                        mergedUsers++
                    } else if (HlcClock.compareVersions(
                            local.hlcTimestamp, local.syncVersion, local.updatedAt, local.lastModifiedDeviceId,
                            remoteUser.hlcTimestamp, remoteUser.syncVersion, remoteUser.updatedAt, remoteUser.lastModifiedDeviceId
                        ) > 0
                    ) {
                        usersToWrite.add(
                            remoteUser.copy(
                                pinHash = remoteUser.pinHash.takeIf { it.startsWith("pbkdf2:") } ?: local.pinHash,
                                password = remoteUser.password.takeIf { it.startsWith("pbkdf2:") } ?: local.password
                            )
                        )
                        mergedUsers++
                        conflicts++
                    }
                }
                if (usersToWrite.isNotEmpty()) userDao.insertAllUsers(usersToWrite)
                for ((_, tomb) in tombstoneMap) {
                    if (tomb.tableName == "users") {
                        val local = existingUsers[tomb.recordId]
                        if (local != null && isDeletedByTombstone("users", local.id, local.updatedAt, local.hlcTimestamp)) {
                            userDao.deleteUser(local.id)
                        }
                    }
                }

                // 4. Products (respecting deletion tombstones)
                val existingProducts = productDao.getAllProductsList().associateBy { it.id }
                val productsToWrite = mutableListOf<com.example.data.model.Product>()
                for (remoteProd in summary.products) {
                    HlcClock.observe(remoteProd.hlcTimestamp)
                    if (isDeletedByTombstone("products", remoteProd.id, remoteProd.updatedAt, remoteProd.hlcTimestamp)) {
                        continue
                    }
                    val local = existingProducts[remoteProd.id]
                    if (local == null) {
                        productsToWrite.add(remoteProd)
                        mergedProducts++
                    } else if (HlcClock.compareVersions(
                            local.hlcTimestamp, local.syncVersion, local.updatedAt, local.lastModifiedDeviceId,
                            remoteProd.hlcTimestamp, remoteProd.syncVersion, remoteProd.updatedAt, remoteProd.lastModifiedDeviceId
                        ) > 0
                    ) {
                        productsToWrite.add(remoteProd)
                        mergedProducts++
                        conflicts++
                    }
                }
                if (productsToWrite.isNotEmpty()) productDao.insertAll(productsToWrite)
                for ((_, tomb) in tombstoneMap) {
                    if (tomb.tableName == "products") {
                        val local = existingProducts[tomb.recordId]
                        if (local != null && isDeletedByTombstone("products", local.id, local.updatedAt, local.hlcTimestamp)) {
                            productDao.softDeleteProduct(local.id, tomb.deletedAt)
                        }
                    }
                }

                // 5. Customers (respecting deletion tombstones)
                val existingCustomers = customerDao.getAllCustomersList().associateBy { it.id }
                val customersToWrite = mutableListOf<com.example.data.model.Customer>()
                for (remoteCust in summary.customers) {
                    HlcClock.observe(remoteCust.hlcTimestamp)
                    if (isDeletedByTombstone("customers", remoteCust.id, remoteCust.updatedAt, remoteCust.hlcTimestamp)) {
                        continue
                    }
                    val local = existingCustomers[remoteCust.id]
                    if (local == null) {
                        customersToWrite.add(remoteCust)
                        mergedCustomers++
                    } else if (HlcClock.compareVersions(
                            local.hlcTimestamp, local.syncVersion, local.updatedAt, local.lastModifiedDeviceId,
                            remoteCust.hlcTimestamp, remoteCust.syncVersion, remoteCust.updatedAt, remoteCust.lastModifiedDeviceId
                        ) > 0
                    ) {
                        customersToWrite.add(remoteCust)
                        mergedCustomers++
                        conflicts++
                    }
                }
                if (customersToWrite.isNotEmpty()) customerDao.insertAll(customersToWrite)
                for ((_, tomb) in tombstoneMap) {
                    if (tomb.tableName == "customers") {
                        val local = existingCustomers[tomb.recordId]
                        if (local != null && isDeletedByTombstone("customers", local.id, local.updatedAt, local.hlcTimestamp)) {
                            customerDao.softDeleteCustomer(local.id, tomb.deletedAt)
                        }
                    }
                }

                // 6. Sales (respecting deletion tombstones)
                val existingSales = saleDao.getAllSalesList().associateBy { it.id }
                val salesToWrite = mutableListOf<Sale>()
                for (remoteSale in summary.sales) {
                    HlcClock.observe(remoteSale.hlcTimestamp)
                    if (isDeletedByTombstone("sales", remoteSale.id, remoteSale.updatedAt, remoteSale.hlcTimestamp)) {
                        continue
                    }
                    val local = existingSales[remoteSale.id]
                    if (local == null) {
                        salesToWrite.add(remoteSale)
                        mergedSales++
                    } else if (HlcClock.compareVersions(
                            local.hlcTimestamp, local.syncVersion, local.updatedAt, local.lastModifiedDeviceId,
                            remoteSale.hlcTimestamp, remoteSale.syncVersion, remoteSale.updatedAt, remoteSale.lastModifiedDeviceId
                        ) > 0
                    ) {
                        salesToWrite.add(remoteSale)
                        mergedSales++
                        conflicts++
                    }
                }
                if (salesToWrite.isNotEmpty()) saleDao.insertSales(salesToWrite)
                for ((_, tomb) in tombstoneMap) {
                    if (tomb.tableName == "sales") {
                        val local = existingSales[tomb.recordId]
                        if (local != null && isDeletedByTombstone("sales", local.id, local.updatedAt, local.hlcTimestamp)) {
                            saleDao.deleteSaleItems(local.id)
                            saleDao.deleteSale(local.id)
                        }
                    }
                }

                // 7. Immutable Ledger Events (Grow-Only Set CRDT deduplicated by ID)
                val existingItemIds = saleDao.getAllSaleItemsList().map { it.id }.toSet()
                val missingItems = summary.saleItems.filterNot {
                    existingItemIds.contains(it.id) || tombstoneMap.containsKey(SyncTombstone.keyFor("sales", it.saleId))
                }
                if (missingItems.isNotEmpty()) {
                    saleDao.insertSaleItems(missingItems)
                    mergedSaleItems = missingItems.size
                }

                val existingPaymentIds = paymentDao.getAllPaymentsList().map { it.id }.toSet()
                val missingPayments = summary.payments.filterNot { existingPaymentIds.contains(it.id) }
                if (missingPayments.isNotEmpty()) {
                    paymentDao.insertPayments(missingPayments)
                    mergedPayments = missingPayments.size
                }

                val existingPurchaseIds = purchaseDao.getAllPurchasesList().map { it.id }.toSet()
                val missingPurchases = summary.purchases.filterNot { existingPurchaseIds.contains(it.id) }
                if (missingPurchases.isNotEmpty()) {
                    purchaseDao.insertPurchases(missingPurchases)
                    mergedPurchases = missingPurchases.size
                }

                val existingTransferIds = transferDao.getAllTransfersList().map { it.id }.toSet()
                val missingTransfers = summary.stockTransfers.filterNot {
                    existingTransferIds.contains(it.id) || tombstoneMap.containsKey(SyncTombstone.keyFor("stock_transfers", it.id))
                }
                if (missingTransfers.isNotEmpty()) {
                    transferDao.insertTransfers(missingTransfers)
                    mergedTransfers = missingTransfers.size
                }

                val result = BranchMergeResult(
                    mergedBranches = mergedBranches,
                    mergedUsers = mergedUsers,
                    mergedProducts = mergedProducts,
                    mergedCustomers = mergedCustomers,
                    mergedSales = mergedSales,
                    mergedSaleItems = mergedSaleItems,
                    mergedPayments = mergedPayments,
                    mergedPurchases = mergedPurchases,
                    mergedTransfers = mergedTransfers,
                    conflictsResolved = conflicts
                )

                if (result.totalMergedCount > 0 || conflicts > 0 || tombstonesToInsert.isNotEmpty()) {
                    auditDao.insertLog(
                        LocalAuditLog(
                            actorName = summary.exportedBy,
                            actorRole = "SYNC_ENGINE",
                            action = "DATASHARE_MERGE",
                            targetId = sourceLabel,
                            details = "Merged ${result.totalMergedCount} record(s), ${tombstonesToInsert.size} tombstone(s), resolved $conflicts causal conflict(s)."
                        )
                    )
                }
                if (conflicts > 0) {
                    auditDao.insertLog(
                        LocalAuditLog(
                            actorName = summary.exportedBy,
                            actorRole = "SYNC_CONFLICT",
                            action = "SYNC_CONFLICT",
                            targetId = sourceLabel,
                            details = "Resolved $conflicts causal HLC conflict(s) (Branches=$mergedBranches, Users=$mergedUsers, Products=$mergedProducts, Customers=$mergedCustomers, Sales=$mergedSales)."
                        )
                    )
                }

                result
            }
        }
    }
}
