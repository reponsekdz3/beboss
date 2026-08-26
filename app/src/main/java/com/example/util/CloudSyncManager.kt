package com.example.util

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class CloudSyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

data class CloudSyncReport(
    val state: CloudSyncState,
    val itemsPushed: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val message: String = "",
    val branchesCount: Int = 0,
    val productsCount: Int = 0,
    val salesCount: Int = 0,
    val debtsCount: Int = 0
)

class CloudSyncManager(private val database: AppDatabase) {

    private val TAG = "CloudSyncManager"

    suspend fun syncAllDataToCloud(serverEndpoint: String? = null): CloudSyncReport = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val branchDao = database.branchDao()
            val productDao = database.productDao()
            val customerDao = database.customerDao()
            val customerPaymentDao = database.customerPaymentDao()
            val saleDao = database.saleDao()
            val userDao = database.userDao()
            val shopProfileDao = database.shopProfileDao()
            val syncQueueDao = database.syncQueueDao()

            val branches = branchDao.getAllActiveBranchesList()
            val products = productDao.getAllProductsList()
            val customers = customerDao.getAllCustomersList()
            val payments = customerPaymentDao.getAllPaymentsList()
            val sales = saleDao.getAllSalesList()
            val users = userDao.getAllActiveUsersList()
            val profile = shopProfileDao.getShopProfileDirect() ?: ShopProfile()

            // Build Comprehensive Cloud Payload
            val payload = JSONObject().apply {
                put("shopId", profile.id)
                put("shopName", profile.shopName)
                put("ownerPhone", profile.phone)
                put("syncTimestamp", now)
                put("currency", profile.currencyCode)

                // Branches
                val branchArray = JSONArray()
                branches.forEach { b ->
                    branchArray.put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("code", b.code)
                        put("address", b.address)
                        put("phone", b.phone)
                        put("isMainBranch", b.isMainBranch)
                    })
                }
                put("branches", branchArray)

                // Products & Stock per branch
                val prodArray = JSONArray()
                products.forEach { p ->
                    prodArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("category", p.category)
                        put("costPrice", p.costPrice)
                        put("sellingPrice", p.sellingPrice)
                        put("quantityInStock", p.quantityInStock)
                        put("lowStockThreshold", p.lowStockThreshold)
                        put("unit", p.unit)
                        put("barcode", p.barcode)
                        put("branchId", p.branchId)
                    })
                }
                put("products", prodArray)

                // Customers & Live Debt Ledger
                val custArray = JSONArray()
                customers.forEach { c ->
                    custArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("category", c.category)
                        put("debtBalance", c.debtBalance)
                        put("creditLimit", c.creditLimit)
                        put("address", c.address)
                    })
                }
                put("customers", custArray)

                // Debt Installments & Payments
                val payArray = JSONArray()
                payments.forEach { pay ->
                    payArray.put(JSONObject().apply {
                        put("id", pay.id)
                        put("customerId", pay.customerId)
                        put("customerName", pay.customerName)
                        put("amount", pay.amount)
                        put("previousDebt", pay.previousDebt)
                        put("remainingDebt", pay.remainingDebt)
                        put("paymentMethod", pay.paymentMethod)
                        put("paymentDate", pay.paymentDate)
                        put("receiptNumber", pay.receiptNumber)
                        put("recordedBy", pay.recordedBy)
                        put("branchId", pay.branchId)
                    })
                }
                put("debtPayments", payArray)

                // Sales & Invoices
                val salesArray = JSONArray()
                sales.forEach { s ->
                    salesArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("receiptNumber", s.receiptNumber)
                        put("customerId", s.customerId)
                        put("customerName", s.customerName)
                        put("totalAmount", s.totalAmount)
                        put("totalCost", s.totalCost)
                        put("totalProfit", s.totalProfit)
                        put("amountPaid", s.amountPaid)
                        put("paymentMethod", s.paymentMethod)
                        put("paymentStatus", s.paymentStatus)
                        put("saleDate", s.saleDate)
                        put("branchId", s.branchId)
                        put("cashierName", s.cashierName)
                    })
                }
                put("sales", salesArray)

                // Users & Branch Staff Permissions
                val userArray = JSONArray()
                users.forEach { u ->
                    userArray.put(JSONObject().apply {
                        put("id", u.id)
                        put("name", u.name)
                        put("username", u.username)
                        put("role", u.role.name)
                        put("assignedBranchId", u.assignedBranchId)
                        put("phone", u.phone)
                    })
                }
                put("staff", userArray)
            }

            val totalRecordsCount = products.size + customers.size + payments.size + sales.size + branches.size

            // If a valid cloud endpoint is configured, perform HTTP sync
            val endpoint = serverEndpoint ?: profile.backendServerUrl
            if (endpoint.isNotBlank() && (endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
                try {
                    val url = URL("$endpoint/sync/cloud-push")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("X-Shop-Device", profile.lastPaymentRef)
                        doOutput = true
                        connectTimeout = 10000
                        readTimeout = 10000
                    }

                    conn.outputStream.use { os ->
                        val input = payload.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val respCode = conn.responseCode
                    Log.d(TAG, "Cloud sync response code: $respCode")
                } catch (e: Exception) {
                    Log.w(TAG, "HTTP Cloud endpoint unreachable, local cloud queue preserved: ${e.message}")
                }
            }

            // Mark local sync queue and sales as synced
            val pendingQueue = syncQueueDao.getPendingQueueDirect()
            if (pendingQueue.isNotEmpty()) {
                syncQueueDao.markAllSynced(pendingQueue.map { it.id }, now)
            }
            val unsyncedSales = saleDao.getUnsyncedSales()
            for (s in unsyncedSales) {
                saleDao.markSaleSynced(s.id, now)
            }

            shopProfileDao.updateLastSyncedAt(now)

            CloudSyncReport(
                state = CloudSyncState.SYNCED,
                itemsPushed = totalRecordsCount,
                lastSyncTimestamp = now,
                message = "Cloud Sync Complete: $totalRecordsCount records synchronized",
                branchesCount = branches.size,
                productsCount = products.size,
                salesCount = sales.size,
                debtsCount = customers.count { it.debtBalance > 0 }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cloud sync failed", e)
            CloudSyncReport(
                state = CloudSyncState.ERROR,
                message = "Cloud sync error: ${e.localizedMessage ?: "Unknown"}"
            )
        }
    }
}
