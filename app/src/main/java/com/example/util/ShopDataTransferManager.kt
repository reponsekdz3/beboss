package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Branch
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.PurchaseRecord
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.StockTransfer
import com.example.data.model.User
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class ShopImportSummary(
    val shopName: String,
    val exportedAt: Long,
    val exportedBy: String,
    val userCount: Int,
    val productCount: Int,
    val customerCount: Int,
    val saleCount: Int,
    val paymentCount: Int,
    val users: List<User>,
    val products: List<Product>,
    val customers: List<Customer>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>,
    val payments: List<CustomerPayment>,
    val shopProfile: ShopProfile?,
    val branches: List<Branch> = emptyList(),
    val stockTransfers: List<StockTransfer> = emptyList(),
    val purchases: List<PurchaseRecord> = emptyList(),
    val branchScopeName: String? = null
)

object ShopDataTransferManager {

    private const val SHARE_TOKEN_PREFIX = "BBSHARE1:"
    private const val SHARE_HMAC_KEY = "BeBoss_Branch_DataShare_Integrity_2026"

    /**
     * Creates full JSON Object for shop & multi-branch data.
     * Supports optional branchIdFilter for exporting a single branch's operational snapshot.
     */
    fun createExportJson(
        profile: ShopProfile,
        users: List<User>,
        products: List<Product>,
        customers: List<Customer>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        payments: List<CustomerPayment>,
        exportedByUser: User?,
        branches: List<Branch> = emptyList(),
        stockTransfers: List<StockTransfer> = emptyList(),
        purchases: List<PurchaseRecord> = emptyList(),
        branchIdFilter: String? = null
    ): JSONObject {
        val activeFilter = branchIdFilter?.takeIf { it.isNotBlank() && it != "ALL" }
        val scopedBranch = if (activeFilter != null) branches.firstOrNull { it.id == activeFilter } else null

        val filteredProducts = if (activeFilter == null) products else products.filter { it.branchId == activeFilter || it.branchId.isBlank() }
        val filteredSales = if (activeFilter == null) sales else sales.filter { it.branchId == activeFilter }
        val filteredSaleIds = filteredSales.map { it.id }.toSet()
        val filteredSaleItems = if (activeFilter == null) saleItems else saleItems.filter { filteredSaleIds.contains(it.saleId) }
        val filteredPayments = if (activeFilter == null) payments else payments.filter { it.branchId == activeFilter }
        val filteredPurchases = if (activeFilter == null) purchases else purchases.filter { it.branchId == activeFilter }
        val filteredTransfers = if (activeFilter == null) stockTransfers else stockTransfers.filter {
            it.fromBranchId == activeFilter || it.toBranchId == activeFilter
        }
        val filteredBranches = if (activeFilter == null) branches else branches.filter { it.id == activeFilter }

        val root = JSONObject()

        // 1. Metadata
        val meta = JSONObject().apply {
            put("app", "BeBoss")
            put("version", "2.0")
            put("format", "beboss_branch_datashare_v2")
            put("timestamp", System.currentTimeMillis())
            put("exportedBy", exportedByUser?.name ?: "Owner")
            put("exportedByRole", exportedByUser?.role?.name ?: "OWNER")
            if (scopedBranch != null) {
                put("branchScopeId", scopedBranch.id)
                put("branchScopeName", scopedBranch.name)
            }
        }
        root.put("metadata", meta)

        // 2. Shop Profile
        val shopObj = JSONObject().apply {
            put("id", profile.id)
            put("shopName", profile.shopName)
            put("name", profile.name)
            put("phone", profile.phone)
            put("email", profile.email)
            put("address", profile.address)
            put("currencyCode", profile.currencyCode)
            put("currencySymbol", profile.currencySymbol)
            put("taxRate", profile.taxRate)
            put("tinNumber", profile.tinNumber)
            put("receiptFooter", profile.receiptFooter)
            put("isOnlineSyncEnabled", profile.isOnlineSyncEnabled)
            put("backendServerUrl", profile.backendServerUrl)
            put("lastSyncedAt", profile.lastSyncedAt)
            put("createdAt", profile.createdAt)
            put("updatedAt", profile.updatedAt)
        }
        root.put("shopProfile", shopObj)

        // 2.5 Branches
        val branchesArr = JSONArray()
        filteredBranches.forEach { b ->
            branchesArr.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("code", b.code)
                put("address", b.address)
                put("phone", b.phone)
                put("managerUserId", b.managerUserId)
                put("managerName", b.managerName)
                put("isMainBranch", b.isMainBranch)
                put("isActive", b.isActive)
                put("colorHex", b.colorHex)
                put("createdAt", b.createdAt)
                put("updatedAt", b.updatedAt)
            })
        }
        root.put("branches", branchesArr)

        // 3. Collaborators / Users
        val usersArr = JSONArray()
        users.forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("username", u.username)
                put("email", u.email)
                put("phone", u.phone)
                put("pinHash", u.pinHash)
                put("password", u.password)
                put("role", u.role.name)
                put("assignedBranchId", u.assignedBranchId)
                put("assignedBranchName", u.assignedBranchName)
                put("profileColorHex", u.profileColorHex)
                put("isActive", u.isActive)
                put("canSellPOS", u.canSellPOS)
                put("canApplyDiscounts", u.canApplyDiscounts)
                put("canManageInventory", u.canManageInventory)
                put("canViewCostAndProfit", u.canViewCostAndProfit)
                put("canViewAnalytics", u.canViewAnalytics)
                put("canManageCustomers", u.canManageCustomers)
                put("canCollectDebt", u.canCollectDebt)
                put("canDeleteRecords", u.canDeleteRecords)
                put("canExportReports", u.canExportReports)
                put("canManageCollaborators", u.canManageCollaborators)
                put("canManageShopSettings", u.canManageShopSettings)
                put("canExportImportData", u.canExportImportData)
                put("lastLoginAt", u.lastLoginAt)
                put("createdAt", u.createdAt)
                put("updatedAt", u.updatedAt)
            })
        }
        root.put("users", usersArr)

        // 4. Products
        val productsArr = JSONArray()
        filteredProducts.forEach { p ->
            productsArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category)
                put("barcode", p.barcode)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("quantityInStock", p.quantityInStock)
                put("lowStockThreshold", p.lowStockThreshold)
                put("unit", p.unit)
                put("branchId", p.branchId)
                put("isDeleted", p.isDeleted)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("products", productsArr)

        // 5. Customers
        val customersArr = JSONArray()
        customers.forEach { c ->
            customersArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("email", c.email)
                put("address", c.address)
                put("city", c.city)
                put("taxIdOrNin", c.taxIdOrNin)
                put("category", c.category)
                put("creditLimit", c.creditLimit)
                put("debtBalance", c.debtBalance)
                put("notes", c.notes)
                put("isDeleted", c.isDeleted)
                put("createdAt", c.createdAt)
                put("updatedAt", c.updatedAt)
            })
        }
        root.put("customers", customersArr)

        // 6. Sales
        val salesArr = JSONArray()
        filteredSales.forEach { s ->
            salesArr.put(JSONObject().apply {
                put("id", s.id)
                put("receiptNumber", s.receiptNumber)
                put("customerId", s.customerId)
                put("customerName", s.customerName)
                put("totalAmount", s.totalAmount)
                put("totalCost", s.totalCost)
                put("totalProfit", s.totalProfit)
                put("discountAmount", s.discountAmount)
                put("paymentMethod", s.paymentMethod)
                put("amountPaid", s.amountPaid)
                put("paymentStatus", s.paymentStatus)
                put("notes", s.notes)
                put("branchId", s.branchId)
                put("branchName", s.branchName)
                put("cashierId", s.cashierId)
                put("cashierName", s.cashierName)
                put("saleDate", s.saleDate)
                put("synced", s.synced)
                put("updatedAt", s.updatedAt)
            })
        }
        root.put("sales", salesArr)

        // 7. Sale Items
        val itemsArr = JSONArray()
        filteredSaleItems.forEach { i ->
            itemsArr.put(JSONObject().apply {
                put("id", i.id)
                put("saleId", i.saleId)
                put("productId", i.productId)
                put("productName", i.productName)
                put("category", i.category)
                put("unit", i.unit)
                put("quantitySold", i.quantitySold)
                put("costPriceAtSale", i.costPriceAtSale)
                put("unitPriceAtSale", i.unitPriceAtSale)
                put("subtotal", i.subtotal)
                put("profit", i.profit)
            })
        }
        root.put("saleItems", itemsArr)

        // 8. Customer Payments
        val paymentsArr = JSONArray()
        filteredPayments.forEach { py ->
            paymentsArr.put(JSONObject().apply {
                put("id", py.id)
                put("customerId", py.customerId)
                put("customerName", py.customerName)
                put("amount", py.amount)
                put("previousDebt", py.previousDebt)
                put("remainingDebt", py.remainingDebt)
                put("paymentMethod", py.paymentMethod)
                put("notes", py.notes)
                put("receiptNumber", py.receiptNumber)
                put("paymentDate", py.paymentDate)
                put("recordedBy", py.recordedBy)
                put("branchId", py.branchId)
                put("branchName", py.branchName)
            })
        }
        root.put("customerPayments", paymentsArr)

        // 9. Purchases
        val purchasesArr = JSONArray()
        filteredPurchases.forEach { pu ->
            purchasesArr.put(JSONObject().apply {
                put("id", pu.id)
                put("productId", pu.productId)
                put("productName", pu.productName)
                put("category", pu.category)
                put("supplierName", pu.supplierName)
                put("supplierPhone", pu.supplierPhone)
                put("quantityPurchased", pu.quantityPurchased)
                put("unitCostPrice", pu.unitCostPrice)
                put("totalPurchaseCost", pu.totalPurchaseCost)
                put("sellingPriceAtPurchase", pu.sellingPriceAtPurchase)
                put("paymentStatus", pu.paymentStatus)
                put("invoiceNumber", pu.invoiceNumber)
                put("branchId", pu.branchId)
                put("branchName", pu.branchName)
                put("purchasedByUserId", pu.purchasedByUserId)
                put("purchasedByName", pu.purchasedByName)
                put("notes", pu.notes)
                put("purchaseDate", pu.purchaseDate)
                put("isSynced", pu.isSynced)
                put("createdAt", pu.createdAt)
            })
        }
        root.put("purchases", purchasesArr)

        // 10. Stock Transfers
        val transfersArr = JSONArray()
        filteredTransfers.forEach { tr ->
            transfersArr.put(JSONObject().apply {
                put("id", tr.id)
                put("transferNumber", tr.transferNumber)
                put("productId", tr.productId)
                put("productName", tr.productName)
                put("fromBranchId", tr.fromBranchId)
                put("fromBranchName", tr.fromBranchName)
                put("toBranchId", tr.toBranchId)
                put("toBranchName", tr.toBranchName)
                put("quantity", tr.quantity)
                put("unit", tr.unit)
                put("transferDate", tr.transferDate)
                put("status", tr.status)
                put("notes", tr.notes)
                put("transferredBy", tr.transferredBy)
                put("synced", tr.synced)
                put("createdAt", tr.createdAt)
            })
        }
        root.put("stockTransfers", transfersArr)

        return root
    }

    /**
     * Generates a GZIP-compressed, HMAC-signed portable Branch Share Token (`BBSHARE1:...`)
     * for instant sharing via WhatsApp, SMS, Clipboard, or Nearby Share.
     */
    fun createCompressedSyncToken(json: JSONObject): String {
        val rawBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(rawBytes)
        }
        val b64Payload = base64Encode(baos.toByteArray())
        val sig8 = computeShareHmac(b64Payload).take(8).uppercase(Locale.ROOT)
        return "$SHARE_TOKEN_PREFIX$sig8:$b64Payload"
    }

    fun decodeCompressedSyncToken(token: String): String {
        val trimmed = token.trim()
        require(trimmed.startsWith(SHARE_TOKEN_PREFIX)) { "Invalid BeBoss Share Token prefix." }
        val remainder = trimmed.removePrefix(SHARE_TOKEN_PREFIX)
        val sepIndex = remainder.indexOf(':')
        require(sepIndex > 0) { "Malformed BeBoss Share Token structure." }
        val providedSig = remainder.substring(0, sepIndex).uppercase(Locale.ROOT)
        val b64Payload = remainder.substring(sepIndex + 1)
        val expectedSig = computeShareHmac(b64Payload).take(8).uppercase(Locale.ROOT)
        require(providedSig == expectedSig) { "Branch Share Token integrity check failed (truncated or tampered)." }

        val compressed = base64Decode(b64Payload)
        GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(StandardCharsets.UTF_8).use { reader ->
            return reader.readText()
        }
    }

    /**
     * Exports full or branch-scoped shop data to a `.beboss` JSON file in phone storage.
     */
    fun exportShopPackage(
        context: Context,
        profile: ShopProfile,
        users: List<User>,
        products: List<Product>,
        customers: List<Customer>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        payments: List<CustomerPayment>,
        exportedByUser: User?,
        branches: List<Branch> = emptyList(),
        stockTransfers: List<StockTransfer> = emptyList(),
        purchases: List<PurchaseRecord> = emptyList(),
        branchIdFilter: String? = null
    ): File {
        val root = createExportJson(
            profile = profile,
            users = users,
            products = products,
            customers = customers,
            sales = sales,
            saleItems = saleItems,
            payments = payments,
            exportedByUser = exportedByUser,
            branches = branches,
            stockTransfers = stockTransfers,
            purchases = purchases,
            branchIdFilter = branchIdFilter
        )

        val backupDir = File(context.getExternalFilesDir(null), "beboss_backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanShopName = profile.shopName.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val branchSuffix = branchIdFilter?.takeIf { it.isNotBlank() && it != "ALL" }?.let { "_${it.take(8)}" }.orEmpty()
        val fileName = "${cleanShopName}${branchSuffix}_Backup_${timeStampStr}.beboss"
        val targetFile = File(backupDir, fileName)

        FileOutputStream(targetFile).use { fos ->
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        return targetFile
    }

    fun parseShopPackageFile(file: File): ShopImportSummary {
        val raw = FileInputStream(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parseShopPackageString(raw)
    }

    /**
     * Parses either raw JSON or a compressed `BBSHARE1:` token into a structured ShopImportSummary.
     */
    fun parseShopPackageString(input: String): ShopImportSummary {
        val trimmed = input.trim()
        val jsonString = if (trimmed.startsWith(SHARE_TOKEN_PREFIX)) {
            decodeCompressedSyncToken(trimmed)
        } else {
            trimmed
        }

        val root = JSONObject(jsonString)

        val meta = root.optJSONObject("metadata")
        val exportedAt = meta?.optLong("timestamp") ?: System.currentTimeMillis()
        val exportedBy = meta?.optString("exportedBy") ?: "Owner"
        val branchScopeName = meta?.optString("branchScopeName")?.takeIf { it.isNotBlank() }

        // Shop Profile
        var shopProfile: ShopProfile? = null
        if (root.has("shopProfile")) {
            val sObj = root.getJSONObject("shopProfile")
            shopProfile = ShopProfile(
                id = sObj.optLong("id", 1L),
                shopName = sObj.optString("shopName", "My Shop"),
                name = sObj.optString("name", ""),
                phone = sObj.optString("phone", ""),
                email = sObj.optString("email", ""),
                address = sObj.optString("address", ""),
                currencyCode = sObj.optString("currencyCode", "RWF"),
                currencySymbol = sObj.optString("currencySymbol", "FRw"),
                taxRate = sObj.optDouble("taxRate", 0.0),
                tinNumber = sObj.optString("tinNumber", ""),
                receiptFooter = sObj.optString("receiptFooter", "Thank you for your business!"),
                isOnlineSyncEnabled = sObj.optBoolean("isOnlineSyncEnabled", true),
                backendServerUrl = sObj.optString("backendServerUrl", "https://api.beboss.app/v1"),
                lastSyncedAt = sObj.optLong("lastSyncedAt", 0L),
                createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        // Branches
        val branches = mutableListOf<Branch>()
        if (root.has("branches")) {
            val bArr = root.getJSONArray("branches")
            for (i in 0 until bArr.length()) {
                val b = bArr.getJSONObject(i)
                branches.add(
                    Branch(
                        id = b.optString("id"),
                        name = b.optString("name", "Branch"),
                        code = b.optString("code", "BR-01"),
                        address = b.optString("address", "Kigali, Rwanda"),
                        phone = b.optString("phone", ""),
                        managerUserId = b.optString("managerUserId", ""),
                        managerName = b.optString("managerName", ""),
                        isMainBranch = b.optBoolean("isMainBranch", false),
                        isActive = b.optBoolean("isActive", true),
                        colorHex = b.optString("colorHex", "#FF6B1A"),
                        createdAt = b.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = b.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Users
        val users = mutableListOf<User>()
        if (root.has("users")) {
            val uArr = root.getJSONArray("users")
            for (i in 0 until uArr.length()) {
                val u = uArr.getJSONObject(i)
                val roleName = u.optString("role", "CASHIER")
                val role = try { UserRole.valueOf(roleName) } catch (_: Exception) { UserRole.CASHIER }
                users.add(
                    User(
                        id = u.optString("id"),
                        name = u.optString("name"),
                        username = u.optString("username"),
                        email = u.optString("email", ""),
                        phone = u.optString("phone", ""),
                        pinHash = u.optString("pinHash", ""),
                        password = u.optString("password", ""),
                        role = role,
                        assignedBranchId = u.optString("assignedBranchId", ""),
                        assignedBranchName = u.optString("assignedBranchName", "All Branches"),
                        profileColorHex = u.optString("profileColorHex", "#FF6B1A"),
                        isActive = u.optBoolean("isActive", true),
                        canSellPOS = u.optBoolean("canSellPOS", true),
                        canApplyDiscounts = u.optBoolean("canApplyDiscounts", role != UserRole.CASHIER),
                        canManageInventory = u.optBoolean("canManageInventory", role != UserRole.CASHIER),
                        canViewCostAndProfit = u.optBoolean("canViewCostAndProfit", role != UserRole.CASHIER),
                        canViewAnalytics = u.optBoolean("canViewAnalytics", role != UserRole.CASHIER),
                        canManageCustomers = u.optBoolean("canManageCustomers", true),
                        canCollectDebt = u.optBoolean("canCollectDebt", role != UserRole.CASHIER),
                        canDeleteRecords = u.optBoolean("canDeleteRecords", role == UserRole.OWNER),
                        canExportReports = u.optBoolean("canExportReports", role != UserRole.CASHIER),
                        canManageCollaborators = u.optBoolean("canManageCollaborators", role == UserRole.OWNER),
                        canManageShopSettings = u.optBoolean("canManageShopSettings", role == UserRole.OWNER),
                        canExportImportData = u.optBoolean("canExportImportData", role == UserRole.OWNER),
                        lastLoginAt = u.optLong("lastLoginAt", System.currentTimeMillis()),
                        createdAt = u.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = u.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Products
        val products = mutableListOf<Product>()
        if (root.has("products")) {
            val pArr = root.getJSONArray("products")
            for (i in 0 until pArr.length()) {
                val p = pArr.getJSONObject(i)
                products.add(
                    Product(
                        id = p.optString("id"),
                        name = p.optString("name"),
                        category = p.optString("category", "General"),
                        barcode = p.optString("barcode", ""),
                        costPrice = p.optDouble("costPrice", 0.0),
                        sellingPrice = p.optDouble("sellingPrice", 0.0),
                        quantityInStock = p.optDouble("quantityInStock", 0.0),
                        lowStockThreshold = p.optDouble("lowStockThreshold", 5.0),
                        unit = p.optString("unit", "pcs"),
                        branchId = p.optString("branchId", "main_branch"),
                        isDeleted = p.optBoolean("isDeleted", false),
                        createdAt = p.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Customers
        val customers = mutableListOf<Customer>()
        if (root.has("customers")) {
            val cArr = root.getJSONArray("customers")
            for (i in 0 until cArr.length()) {
                val c = cArr.getJSONObject(i)
                customers.add(
                    Customer(
                        id = c.optString("id"),
                        name = c.optString("name"),
                        phone = c.optString("phone", ""),
                        email = c.optString("email", ""),
                        address = c.optString("address", ""),
                        city = c.optString("city", "Kigali"),
                        taxIdOrNin = c.optString("taxIdOrNin", ""),
                        category = c.optString("category", "Regular Customer"),
                        creditLimit = c.optDouble("creditLimit", 500000.0),
                        notes = c.optString("notes", ""),
                        debtBalance = c.optDouble("debtBalance", 0.0),
                        isDeleted = c.optBoolean("isDeleted", false),
                        createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = c.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Sales
        val sales = mutableListOf<Sale>()
        if (root.has("sales")) {
            val sArr = root.getJSONArray("sales")
            for (i in 0 until sArr.length()) {
                val s = sArr.getJSONObject(i)
                sales.add(
                    Sale(
                        id = s.optString("id"),
                        receiptNumber = s.optString("receiptNumber"),
                        customerId = s.optString("customerId").ifBlank { null },
                        customerName = s.optString("customerName", "Walk-in Customer"),
                        saleDate = s.optLong("saleDate", s.optLong("timestamp", System.currentTimeMillis())),
                        totalAmount = s.optDouble("totalAmount", 0.0),
                        totalCost = s.optDouble("totalCost", 0.0),
                        totalProfit = s.optDouble("totalProfit", 0.0),
                        discountAmount = s.optDouble("discountAmount", 0.0),
                        paymentMethod = s.optString("paymentMethod", "CASH"),
                        paymentStatus = s.optString("paymentStatus", "PAID"),
                        amountPaid = s.optDouble("amountPaid", 0.0),
                        notes = s.optString("notes", ""),
                        branchId = s.optString("branchId", "main_branch"),
                        branchName = s.optString("branchName", "Main Branch"),
                        cashierId = s.optString("cashierId", ""),
                        cashierName = s.optString("cashierName", "Shop Operator"),
                        synced = s.optBoolean("synced", false),
                        updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Sale Items
        val saleItems = mutableListOf<SaleItem>()
        if (root.has("saleItems")) {
            val iArr = root.getJSONArray("saleItems")
            for (i in 0 until iArr.length()) {
                val item = iArr.getJSONObject(i)
                saleItems.add(
                    SaleItem(
                        id = item.optString("id"),
                        saleId = item.optString("saleId"),
                        productId = item.optString("productId"),
                        productName = item.optString("productName"),
                        category = item.optString("category", "General"),
                        unit = item.optString("unit", "pcs"),
                        quantitySold = item.optDouble("quantitySold", item.optDouble("quantity", 1.0)),
                        costPriceAtSale = item.optDouble("costPriceAtSale", item.optDouble("unitCostPrice", 0.0)),
                        unitPriceAtSale = item.optDouble("unitPriceAtSale", item.optDouble("unitSellingPrice", 0.0)),
                        subtotal = item.optDouble("subtotal", item.optDouble("totalPrice", 0.0)),
                        profit = item.optDouble("profit", 0.0)
                    )
                )
            }
        }

        // Payments
        val payments = mutableListOf<CustomerPayment>()
        if (root.has("customerPayments")) {
            val pyArr = root.getJSONArray("customerPayments")
            for (i in 0 until pyArr.length()) {
                val py = pyArr.getJSONObject(i)
                payments.add(
                    CustomerPayment(
                        id = py.optString("id"),
                        customerId = py.optString("customerId"),
                        customerName = py.optString("customerName", "Customer"),
                        amount = py.optDouble("amount", py.optDouble("amountPaid", 0.0)),
                        previousDebt = py.optDouble("previousDebt", 0.0),
                        remainingDebt = py.optDouble("remainingDebt", 0.0),
                        paymentMethod = py.optString("paymentMethod", "Cash"),
                        notes = py.optString("notes", ""),
                        receiptNumber = py.optString("receiptNumber", "PAY-${System.currentTimeMillis().toString().takeLast(6)}"),
                        paymentDate = py.optLong("paymentDate", py.optLong("timestamp", System.currentTimeMillis())),
                        recordedBy = py.optString("recordedBy", "Shop Owner"),
                        branchId = py.optString("branchId", "main_branch"),
                        branchName = py.optString("branchName", "Main Branch")
                    )
                )
            }
        }

        // Purchases
        val purchases = mutableListOf<PurchaseRecord>()
        if (root.has("purchases")) {
            val puArr = root.getJSONArray("purchases")
            for (i in 0 until puArr.length()) {
                val pu = puArr.getJSONObject(i)
                purchases.add(
                    PurchaseRecord(
                        id = pu.optString("id"),
                        productId = pu.optString("productId"),
                        productName = pu.optString("productName"),
                        category = pu.optString("category", "General"),
                        supplierName = pu.optString("supplierName", "Local Wholesale Supplier"),
                        supplierPhone = pu.optString("supplierPhone", ""),
                        quantityPurchased = pu.optDouble("quantityPurchased", 1.0),
                        unitCostPrice = pu.optDouble("unitCostPrice", 0.0),
                        totalPurchaseCost = pu.optDouble("totalPurchaseCost", 0.0),
                        sellingPriceAtPurchase = pu.optDouble("sellingPriceAtPurchase", 0.0),
                        paymentStatus = pu.optString("paymentStatus", "PAID_CASH"),
                        invoiceNumber = pu.optString("invoiceNumber", ""),
                        branchId = pu.optString("branchId", "main_branch"),
                        branchName = pu.optString("branchName", "Main Store"),
                        purchasedByUserId = pu.optString("purchasedByUserId", ""),
                        purchasedByName = pu.optString("purchasedByName", "Shop Manager"),
                        notes = pu.optString("notes", ""),
                        purchaseDate = pu.optLong("purchaseDate", System.currentTimeMillis()),
                        isSynced = pu.optBoolean("isSynced", false),
                        createdAt = pu.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Stock Transfers
        val transfers = mutableListOf<StockTransfer>()
        if (root.has("stockTransfers")) {
            val trArr = root.getJSONArray("stockTransfers")
            for (i in 0 until trArr.length()) {
                val tr = trArr.getJSONObject(i)
                transfers.add(
                    StockTransfer(
                        id = tr.optString("id"),
                        transferNumber = tr.optString("transferNumber"),
                        productId = tr.optString("productId"),
                        productName = tr.optString("productName"),
                        fromBranchId = tr.fromBranchIdOrOpt(tr),
                        fromBranchName = tr.optString("fromBranchName"),
                        toBranchId = tr.optString("toBranchId"),
                        toBranchName = tr.optString("toBranchName"),
                        quantity = tr.optDouble("quantity", 0.0),
                        unit = tr.optString("unit", "pcs"),
                        transferDate = tr.optLong("transferDate", System.currentTimeMillis()),
                        status = tr.optString("status", "COMPLETED"),
                        notes = tr.optString("notes", ""),
                        transferredBy = tr.optString("transferredBy", "Store Operator"),
                        synced = tr.optBoolean("synced", false),
                        createdAt = tr.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return ShopImportSummary(
            shopName = shopProfile?.shopName ?: "Shop",
            exportedAt = exportedAt,
            exportedBy = exportedBy,
            userCount = users.size,
            productCount = products.size,
            customerCount = customers.size,
            saleCount = sales.size,
            paymentCount = payments.size,
            users = users,
            products = products,
            customers = customers,
            sales = sales,
            saleItems = saleItems,
            payments = payments,
            shopProfile = shopProfile,
            branches = branches,
            stockTransfers = transfers,
            purchases = purchases,
            branchScopeName = branchScopeName
        )
    }

    private fun JSONObject.fromBranchIdOrOpt(tr: JSONObject): String = tr.optString("fromBranchId")

    fun listLocalBackups(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "beboss_backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.extension == "beboss" || file.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun shareBackupFile(context: Context, file: File, shopName: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BeBoss Shop Data Package - $shopName")
            putExtra(Intent.EXTRA_TEXT, "Offline Shop Data Package for $shopName (${file.name}). Open in BeBoss app to restore or sync.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Shop Data Package via..."))
    }

    fun shareShopPackage(context: Context, file: File) {
        shareBackupFile(context, file, "Shop")
    }

    /**
     * Shares the bundled BeBoss Web Admin Platform HTML (`admin_portal.html`) so an admin can
     * open it in any desktop/mobile browser or deploy it statically.
     */
    fun exportAndShareAdminWebPortal(context: Context) {
        val outDir = File(context.cacheDir, "admin_web")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = File(outDir, "BeBoss_Admin_Web_Portal.html")
        context.assets.open("admin_portal.html").use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BeBoss Admin Verification Web Platform")
            putExtra(Intent.EXTRA_TEXT, "Open BeBoss_Admin_Web_Portal.html in Chrome/Edge to verify MoMo payments in Supabase and generate BB2 unlock codes.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open / Share BeBoss Admin Web Platform"))
    }

    fun buildCollaboratorInviteText(shopProfile: ShopProfile, worker: User): String {
        val perms = mutableListOf<String>()
        if (worker.canSellPOS) perms.add("POS Checkout")
        if (worker.canManageInventory) perms.add("Inventory & Stock")
        if (worker.canManageCustomers) perms.add("Customer Debt & Ledger")
        if (worker.canViewAnalytics) perms.add("Financial Reports")
        if (worker.canApplyDiscounts) perms.add("Custom Discounts")

        return """
            Hello ${worker.name},
            You have been granted access to *${shopProfile.shopName}* on BeBoss POS!

            *Your Access Credentials:*
            • Role: ${worker.role.displayName}
            • Username: ${worker.username}
            • Fast Unlock PIN: *${worker.pinHash}*
            • Assigned Branch: ${worker.assignedBranchName}
            • Assigned Permissions: ${perms.joinToString(", ")}

            Please keep your PIN safe. You can now log in directly on the store terminal.
            — ${shopProfile.name.ifBlank { "Store Management" }}
        """.trimIndent()
    }

    private fun computeShareHmac(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(SHARE_HMAC_KEY.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        return SecurityUtils.bytesToHex(mac.doFinal(data.toByteArray(StandardCharsets.UTF_8)))
    }

    private val b64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun base64Encode(data: ByteArray): String {
        val sb = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i++].toInt() and 0xFF
            val hasB1 = i < data.size
            val b1 = if (hasB1) data[i++].toInt() and 0xFF else 0
            val hasB2 = i < data.size
            val b2 = if (hasB2) data[i++].toInt() and 0xFF else 0

            val triple = (b0 shl 16) or (b1 shl 8) or b2
            sb.append(b64Chars[(triple ushr 18) and 0x3F])
            sb.append(b64Chars[(triple ushr 12) and 0x3F])
            sb.append(if (hasB1) b64Chars[(triple ushr 6) and 0x3F] else '=')
            sb.append(if (hasB2) b64Chars[triple and 0x3F] else '=')
        }
        return sb.toString()
    }

    private fun base64Decode(input: String): ByteArray {
        val clean = input.filter { !it.isWhitespace() }
        val baos = ByteArrayOutputStream((clean.length * 3) / 4)
        var i = 0
        while (i + 3 < clean.length) {
            val c0 = b64Chars.indexOf(clean[i++])
            val c1 = b64Chars.indexOf(clean[i++])
            val ch2 = clean[i++]
            val ch3 = clean[i++]
            val c2 = if (ch2 == '=') 0 else b64Chars.indexOf(ch2)
            val c3 = if (ch3 == '=') 0 else b64Chars.indexOf(ch3)
            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
            baos.write((triple ushr 16) and 0xFF)
            if (ch2 != '=') baos.write((triple ushr 8) and 0xFF)
            if (ch3 != '=') baos.write(triple and 0xFF)
        }
        return baos.toByteArray()
    }
}
