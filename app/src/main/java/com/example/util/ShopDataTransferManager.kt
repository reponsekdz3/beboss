package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val shopProfile: ShopProfile?
)

object ShopDataTransferManager {

    /**
     * Creates full JSON Object for shop data.
     */
    fun createExportJson(
        profile: ShopProfile,
        users: List<User>,
        products: List<Product>,
        customers: List<Customer>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        payments: List<CustomerPayment>,
        exportedByUser: User?
    ): JSONObject {
        val root = JSONObject()

        // 1. Metadata
        val meta = JSONObject().apply {
            put("app", "BeBoss")
            put("version", "1.0")
            put("format", "beboss_offline_package_v1")
            put("timestamp", System.currentTimeMillis())
            put("exportedBy", exportedByUser?.name ?: "Owner")
            put("exportedByRole", exportedByUser?.role?.name ?: "OWNER")
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
            put("receiptFooter", profile.receiptFooter)
            put("isOnlineSyncEnabled", profile.isOnlineSyncEnabled)
            put("backendServerUrl", profile.backendServerUrl)
            put("lastSyncedAt", profile.lastSyncedAt)
            put("createdAt", profile.createdAt)
            put("updatedAt", profile.updatedAt)
        }
        root.put("shopProfile", shopObj)

        // 3. Collaborators / Users
        val usersArr = JSONArray()
        users.forEach { u ->
            val uObj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("username", u.username)
                put("email", u.email)
                put("phone", u.phone)
                put("pinHash", u.pinHash)
                put("password", u.password)
                put("role", u.role.name)
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
            }
            usersArr.put(uObj)
        }
        root.put("users", usersArr)

        // 4. Products
        val productsArr = JSONArray()
        products.forEach { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category)
                put("barcode", p.barcode)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("quantityInStock", p.quantityInStock)
                put("lowStockThreshold", p.lowStockThreshold)
                put("unit", p.unit)
                put("isDeleted", p.isDeleted)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            productsArr.put(pObj)
        }
        root.put("products", productsArr)

        // 5. Customers
        val customersArr = JSONArray()
        customers.forEach { c ->
            val cObj = JSONObject().apply {
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
            }
            customersArr.put(cObj)
        }
        root.put("customers", customersArr)

        // 6. Sales
        val salesArr = JSONArray()
        sales.forEach { s ->
            val sObj = JSONObject().apply {
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
                put("receiptNumber", s.receiptNumber)
                put("branchId", s.branchId)
                put("branchName", s.branchName)
                put("cashierId", s.cashierId)
                put("cashierName", s.cashierName)
                put("saleDate", s.saleDate)
                put("synced", s.synced)
                put("updatedAt", s.updatedAt)
            }
            salesArr.put(sObj)
        }
        root.put("sales", salesArr)

        // 7. Sale Items
        val itemsArr = JSONArray()
        saleItems.forEach { i ->
            val iObj = JSONObject().apply {
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
            }
            itemsArr.put(iObj)
        }
        root.put("saleItems", itemsArr)

        // 8. Customer Payments
        val paymentsArr = JSONArray()
        payments.forEach { py ->
            val pyObj = JSONObject().apply {
                put("id", py.id)
                put("customerId", py.customerId)
                put("customerName", py.customerName)
                put("amount", py.amount)
                put("paymentMethod", py.paymentMethod)
                put("notes", py.notes)
                put("receiptNumber", py.receiptNumber)
                put("paymentDate", py.paymentDate)
            }
            paymentsArr.put(pyObj)
        }
        root.put("customerPayments", paymentsArr)

        return root
    }

    /**
     * Exports full shop data to a local `.beboss` JSON file in phone storage.
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
        exportedByUser: User?
    ): File {
        val root = createExportJson(
            profile = profile,
            users = users,
            products = products,
            customers = customers,
            sales = sales,
            saleItems = saleItems,
            payments = payments,
            exportedByUser = exportedByUser
        )

        // Save to Phone Storage
        val backupDir = File(context.getExternalFilesDir(null), "beboss_backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanShopName = profile.shopName.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val fileName = "${cleanShopName}_Backup_${timeStampStr}.beboss"
        val targetFile = File(backupDir, fileName)

        FileOutputStream(targetFile).use { fos ->
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        return targetFile
    }

    /**
     * Reads and parses a `.beboss` or `.json` file from phone storage.
     */
    fun parseShopPackageFile(file: File): ShopImportSummary {
        val jsonStr = FileInputStream(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parseShopPackageString(jsonStr)
    }

    /**
     * Parses raw JSON string into a structured ShopImportSummary.
     */
    fun parseShopPackageString(jsonString: String): ShopImportSummary {
        val root = JSONObject(jsonString)

        val meta = root.optJSONObject("metadata")
        val exportedAt = meta?.optLong("timestamp") ?: System.currentTimeMillis()
        val exportedBy = meta?.optString("exportedBy") ?: "Owner"

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
                receiptFooter = sObj.optString("receiptFooter", "Thank you for your business!"),
                isOnlineSyncEnabled = sObj.optBoolean("isOnlineSyncEnabled", true),
                backendServerUrl = sObj.optString("backendServerUrl", "https://api.beboss.app/v1"),
                lastSyncedAt = sObj.optLong("lastSyncedAt", 0L),
                createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        // Users
        val users = mutableListOf<User>()
        if (root.has("users")) {
            val uArr = root.getJSONArray("users")
            for (i in 0 until uArr.length()) {
                val u = uArr.getJSONObject(i)
                val roleName = u.optString("role", "CASHIER")
                val role = try { UserRole.valueOf(roleName) } catch (e: Exception) { UserRole.CASHIER }
                users.add(
                    User(
                        id = u.optString("id"),
                        name = u.optString("name"),
                        username = u.optString("username"),
                        email = u.optString("email", ""),
                        phone = u.optString("phone", ""),
                        pinHash = u.optString("pinHash", "1234"),
                        password = u.optString("password", "admin123"),
                        role = role,
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
                val catName = c.optString("category", "Regular Customer")
                customers.add(
                    Customer(
                        id = c.optString("id"),
                        name = c.optString("name"),
                        phone = c.optString("phone", ""),
                        email = c.optString("email", ""),
                        address = c.optString("address", ""),
                        city = c.optString("city", "Kigali"),
                        taxIdOrNin = c.optString("taxIdOrNin", ""),
                        category = catName,
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
                        paymentMethod = py.optString("paymentMethod", "Cash"),
                        notes = py.optString("notes", ""),
                        receiptNumber = py.optString("receiptNumber", "PAY-${System.currentTimeMillis().toString().takeLast(6)}"),
                        paymentDate = py.optLong("paymentDate", py.optLong("timestamp", System.currentTimeMillis()))
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
            shopProfile = shopProfile
        )
    }

    /**
     * Lists existing local backups stored in Phone Storage.
     */
    fun listLocalBackups(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "beboss_backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.extension == "beboss" || file.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Shares a backup file via Android Share Intent.
     */
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
     * Generates a worker invitation / credentials text for WhatsApp/SMS.
     */
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
            • Assigned Permissions: ${perms.joinToString(", ")}

            Please keep your PIN safe. You can now log in directly on the store terminal.
            — ${shopProfile.name.ifBlank { "Store Management" }}
        """.trimIndent()
    }
}
