package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.CustomerCategory
import com.example.data.model.CustomerPayment
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
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
                put("supplier", p.supplier)
                put("description", p.description)
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
                put("totalDebt", c.totalDebt)
                put("creditLimit", c.creditLimit)
                put("category", c.category.name)
                put("notes", c.notes)
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
                put("customerId", s.customerId ?: "")
                put("customerName", s.customerName ?: "")
                put("totalAmount", s.totalAmount)
                put("subtotalAmount", s.subtotalAmount)
                put("discountAmount", s.discountAmount)
                put("taxAmount", s.taxAmount)
                put("amountPaid", s.amountPaid)
                put("changeGiven", s.changeGiven)
                put("paymentMethod", s.paymentMethod.name)
                put("paymentStatus", s.paymentStatus.name)
                put("cashierName", s.cashierName)
                put("notes", s.notes)
                put("timestamp", s.timestamp)
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
                put("quantity", i.quantity)
                put("unitCostPrice", i.unitCostPrice)
                put("unitSellingPrice", i.unitSellingPrice)
                put("totalPrice", i.totalPrice)
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
                put("amountPaid", py.amountPaid)
                put("paymentMethod", py.paymentMethod.name)
                put("notes", py.notes)
                put("recordedBy", py.recordedBy)
                put("timestamp", py.timestamp)
            }
            paymentsArr.put(pyObj)
        }
        root.put("customerPayments", paymentsArr)

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
                id = sObj.optInt("id", 1),
                shopName = sObj.optString("shopName", "My Shop"),
                name = sObj.optString("name", ""),
                phone = sObj.optString("phone", ""),
                email = sObj.optString("email", ""),
                address = sObj.optString("address", ""),
                currencyCode = sObj.optString("currencyCode", "RWF"),
                currencySymbol = sObj.optString("currencySymbol", "FRw"),
                taxRate = sObj.optDouble("taxRate", 0.0),
                receiptFooter = sObj.optString("receiptFooter", "Thank you for your business!")
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
                        canExportImportData = u.optBoolean("canExportImportData", role == UserRole.OWNER)
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
                        supplier = p.optString("supplier", ""),
                        description = p.optString("description", ""),
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
                val catName = c.optString("category", "RETAIL")
                val cat = try { CustomerCategory.valueOf(catName) } catch (e: Exception) { CustomerCategory.RETAIL }
                customers.add(
                    Customer(
                        id = c.optString("id"),
                        name = c.optString("name"),
                        phone = c.optString("phone", ""),
                        email = c.optString("email", ""),
                        address = c.optString("address", ""),
                        totalDebt = c.optDouble("totalDebt", 0.0),
                        creditLimit = c.optDouble("creditLimit", 0.0),
                        category = cat,
                        notes = c.optString("notes", ""),
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
                val pMethod = try { PaymentMethod.valueOf(s.optString("paymentMethod", "CASH")) } catch (e: Exception) { PaymentMethod.CASH }
                val pStatus = try { PaymentStatus.valueOf(s.optString("paymentStatus", "PAID")) } catch (e: Exception) { PaymentStatus.PAID }
                sales.add(
                    Sale(
                        id = s.optString("id"),
                        receiptNumber = s.optString("receiptNumber"),
                        customerId = s.optString("customerId").ifBlank { null },
                        customerName = s.optString("customerName").ifBlank { null },
                        totalAmount = s.optDouble("totalAmount", 0.0),
                        subtotalAmount = s.optDouble("subtotalAmount", 0.0),
                        discountAmount = s.optDouble("discountAmount", 0.0),
                        taxAmount = s.optDouble("taxAmount", 0.0),
                        amountPaid = s.optDouble("amountPaid", 0.0),
                        changeGiven = s.optDouble("changeGiven", 0.0),
                        paymentMethod = pMethod,
                        paymentStatus = pStatus,
                        cashierName = s.optString("cashierName", "Staff"),
                        notes = s.optString("notes", ""),
                        timestamp = s.optLong("timestamp", System.currentTimeMillis())
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
                        quantity = item.optDouble("quantity", 1.0),
                        unitCostPrice = item.optDouble("unitCostPrice", 0.0),
                        unitSellingPrice = item.optDouble("unitSellingPrice", 0.0),
                        totalPrice = item.optDouble("totalPrice", 0.0)
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
                val pMethod = try { PaymentMethod.valueOf(py.optString("paymentMethod", "CASH")) } catch (e: Exception) { PaymentMethod.CASH }
                payments.add(
                    CustomerPayment(
                        id = py.optString("id"),
                        customerId = py.optString("customerId"),
                        amountPaid = py.optDouble("amountPaid", 0.0),
                        paymentMethod = pMethod,
                        notes = py.optString("notes", ""),
                        recordedBy = py.optString("recordedBy", "Staff"),
                        timestamp = py.optLong("timestamp", System.currentTimeMillis())
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
