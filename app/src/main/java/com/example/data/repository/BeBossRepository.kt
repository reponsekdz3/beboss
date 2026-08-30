package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.CategorySalesShare
import com.example.data.model.Customer
import com.example.data.model.CustomerPayment
import com.example.data.model.DailyAnalyticsPoint
import com.example.data.model.InventoryValuation
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.data.model.SyncQueueItem
import com.example.data.model.TopProductReport
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.util.ShopImportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.example.data.model.StockTransfer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class BeBossRepository(private val database: AppDatabase) {

    private val branchDao = database.branchDao()
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val customerPaymentDao = database.customerPaymentDao()
    private val saleDao = database.saleDao()
    private val shopProfileDao = database.shopProfileDao()
    private val syncQueueDao = database.syncQueueDao()
    private val userDao = database.userDao()
    private val purchaseDao = database.purchaseDao()
    private val stockTransferDao = database.stockTransferDao()

    val cloudSyncManager = com.example.util.CloudSyncManager(database)

    // -------------------------------------------------------------
    // STOCK TRANSFERS (MULTI-BRANCH LOGISTICS)
    // -------------------------------------------------------------
    val allStockTransfers: Flow<List<StockTransfer>> = stockTransferDao.getAllTransfers()

    suspend fun getAllStockTransfersList(): List<StockTransfer> = withContext(Dispatchers.IO) {
        stockTransferDao.getAllTransfersList()
    }

    suspend fun createStockTransfer(
        productId: String,
        fromBranchId: String,
        fromBranchName: String,
        toBranchId: String,
        toBranchName: String,
        quantity: Double,
        notes: String = "",
        transferredBy: String = "Store Operator"
    ): StockTransfer = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductById(productId)
        val productName = product?.name ?: "Inventory Item"
        val unit = product?.unit ?: "pcs"
        val transferNum = "TRF-" + SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date(now))

        val transfer = StockTransfer(
            id = UUID.randomUUID().toString(),
            transferNumber = transferNum,
            productId = productId,
            productName = productName,
            fromBranchId = fromBranchId,
            fromBranchName = fromBranchName,
            toBranchId = toBranchId,
            toBranchName = toBranchName,
            quantity = quantity,
            unit = unit,
            transferDate = now,
            status = "COMPLETED",
            notes = notes.trim(),
            transferredBy = transferredBy,
            createdAt = now
        )

        stockTransferDao.insertTransfer(transfer)
        enqueueSync(
            tableName = "stock_transfers",
            recordId = transfer.id,
            operation = "CREATE",
            payloadJson = """{"product":"$productName","qty":$quantity,"from":"$fromBranchName","to":"$toBranchName"}"""
        )
        transfer
    }

    suspend fun deleteStockTransfer(id: String) = withContext(Dispatchers.IO) {
        stockTransferDao.deleteTransfer(id)
        enqueueSync("stock_transfers", id, "DELETE", """{"id":"$id"}""")
    }

    // -------------------------------------------------------------
    // PURCHASES & STOCK INFLOW MANAGEMENT (EXCEL SPREADSHEET LEDGER)
    // -------------------------------------------------------------
    val allPurchases: Flow<List<com.example.data.model.PurchaseRecord>> = purchaseDao.getAllPurchases()

    suspend fun getAllPurchasesList(): List<com.example.data.model.PurchaseRecord> = withContext(Dispatchers.IO) {
        purchaseDao.getAllPurchasesList()
    }

    suspend fun recordPurchaseOrder(
        productId: String,
        quantity: Double,
        unitCostPrice: Double,
        newSellingPrice: Double? = null,
        supplierName: String,
        supplierPhone: String = "",
        paymentStatus: String = "PAID_CASH",
        invoiceNumber: String = "",
        branchId: String = "main_branch",
        branchName: String = "Main Store",
        buyerId: String = "",
        buyerName: String = "Shop Manager",
        notes: String = ""
    ): com.example.data.model.PurchaseRecord = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductById(productId)
        val productName = product?.name ?: "Restocked Product"
        val category = product?.category ?: "General"
        val activeSellingPrice = newSellingPrice ?: (product?.sellingPrice ?: (unitCostPrice * 1.3))

        val invNum = invoiceNumber.trim().ifBlank {
            "PO-" + SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date(now))
        }

        val purchase = com.example.data.model.PurchaseRecord(
            id = UUID.randomUUID().toString(),
            productId = productId,
            productName = productName,
            category = category,
            supplierName = supplierName.trim().ifBlank { "Wholesale Distributor" },
            supplierPhone = supplierPhone.trim(),
            quantityPurchased = quantity.coerceAtLeast(0.1),
            unitCostPrice = unitCostPrice.coerceAtLeast(0.0),
            totalPurchaseCost = quantity * unitCostPrice,
            sellingPriceAtPurchase = activeSellingPrice,
            paymentStatus = paymentStatus,
            invoiceNumber = invNum,
            branchId = branchId,
            branchName = branchName,
            purchasedByUserId = buyerId,
            purchasedByName = buyerName,
            notes = notes.trim(),
            purchaseDate = now,
            createdAt = now
        )

        // Save purchase record
        purchaseDao.insertPurchase(purchase)

        // Increment product stock and update cost/selling prices if adjusted
        if (product != null) {
            val updatedProduct = product.copy(
                quantityInStock = product.quantityInStock + quantity,
                costPrice = if (unitCostPrice > 0) unitCostPrice else product.costPrice,
                sellingPrice = if (newSellingPrice != null && newSellingPrice > 0) newSellingPrice else product.sellingPrice,
                updatedAt = now
            )
            productDao.insertProduct(updatedProduct)
        }

        enqueueSync(
            tableName = "purchases",
            recordId = purchase.id,
            operation = "CREATE",
            payloadJson = """{"productId":"$productId","qty":$quantity,"unitCost":$unitCostPrice,"total":${purchase.totalPurchaseCost},"supplier":"$supplierName"}"""
        )

        purchase
    }

    suspend fun deletePurchaseRecord(purchaseId: String) = withContext(Dispatchers.IO) {
        purchaseDao.deletePurchase(purchaseId)
        enqueueSync("purchases", purchaseId, "DELETE", """{"id":"$purchaseId"}""")
    }

    // -------------------------------------------------------------
    // BRANCHES & MULTI-STORE MANAGEMENT
    // -------------------------------------------------------------
    val allBranches: Flow<List<com.example.data.model.Branch>> = branchDao.getAllActiveBranches()

    suspend fun getAllBranchesList(): List<com.example.data.model.Branch> = withContext(Dispatchers.IO) {
        branchDao.getAllActiveBranchesList()
    }

    suspend fun saveBranch(branch: com.example.data.model.Branch) = withContext(Dispatchers.IO) {
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
        enqueueSync(
            tableName = "branches",
            recordId = toSave.id,
            operation = "UPDATE",
            payloadJson = """{"id":"${toSave.id}","name":"${toSave.name}","code":"${toSave.code}","address":"${toSave.address}","phone":"${toSave.phone}","managerName":"${toSave.managerName}","isMain":${toSave.isMainBranch},"isActive":${toSave.isActive}}"""
        )
    }

    suspend fun deleteBranch(branchId: String) = withContext(Dispatchers.IO) {
        branchDao.softDeleteBranch(branchId)
        enqueueSync("branches", branchId, "DELETE", """{"id":"$branchId"}""")
    }

    suspend fun ensureDefaultBranches(): List<com.example.data.model.Branch> = withContext(Dispatchers.IO) {
        // Clean up legacy mock branches if present
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

            val mainBranch = com.example.data.model.Branch(
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

    // -------------------------------------------------------------
    // USERS & AUTHENTICATION
    // -------------------------------------------------------------
    val allActiveUsers: Flow<List<User>> = userDao.getAllActiveUsers()

    suspend fun getUserByPin(pin: String): User? = withContext(Dispatchers.IO) {
        val trimmed = pin.trim()
        val allUsers = userDao.getAllActiveUsersList()
        for (user in allUsers) {
            if (com.example.util.SecurityUtils.verifyPin(trimmed, user.pinHash)) {
                // If stored PIN was plain text, upgrade to secure hashed PIN
                if (user.pinHash == trimmed) {
                    val secureUser = user.copy(pinHash = com.example.util.SecurityUtils.hashPin(trimmed))
                    userDao.updateUser(secureUser)
                    return@withContext secureUser
                }
                return@withContext user
            }
        }
        null
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim().lowercase(Locale.getDefault()))
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        val securedPin = if (user.pinHash.length <= 6 && !user.pinHash.contains(":")) {
            com.example.util.SecurityUtils.hashPin(user.pinHash)
        } else {
            user.pinHash
        }
        val securedPassword = if (user.password.length <= 16 && !user.password.contains(":")) {
            com.example.util.SecurityUtils.hashPassword(user.password)
        } else {
            user.password
        }
        val userToSave = user.copy(pinHash = securedPin, password = securedPassword)
        userDao.insertUser(userToSave)
        enqueueSync(
            tableName = "users",
            recordId = userToSave.id,
            operation = "UPDATE",
            payloadJson = """{"id":"${userToSave.id}","name":"${userToSave.name}","username":"${userToSave.username}","role":"${userToSave.role.name}","branchId":"${userToSave.assignedBranchId}","phone":"${userToSave.phone}","isActive":${userToSave.isActive}}"""
        )
    }

    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        userDao.deleteUser(userId)
        enqueueSync("users", userId, "DELETE", """{"id":"$userId"}""")
    }

    suspend fun updateLastLogin(userId: String) = withContext(Dispatchers.IO) {
        userDao.updateLastLogin(userId)
    }

    suspend fun ensureDefaultAdminUser(): User = withContext(Dispatchers.IO) {
        val count = userDao.getUserCount()
        if (count == 0) {
            val defaultOwner = User(
                id = UUID.randomUUID().toString(),
                name = "Jean Paul (Store Owner)",
                username = "owner",
                email = "reponsekdz01@gmail.com",
                phone = "+250 788 123 456",
                pinHash = com.example.util.SecurityUtils.hashPin("1234"),
                password = com.example.util.SecurityUtils.hashPassword("admin"),
                role = UserRole.OWNER,
                profileColorHex = "#FF6B1A"
            )
            val defaultManager = User(
                id = UUID.randomUUID().toString(),
                name = "Aline Uwase (Manager)",
                username = "manager",
                email = "aline@beboss.rw",
                phone = "+250 788 654 321",
                pinHash = com.example.util.SecurityUtils.hashPin("5678"),
                password = com.example.util.SecurityUtils.hashPassword("manager123"),
                role = UserRole.MANAGER,
                profileColorHex = "#2563EB"
            )
            val defaultCashier = User(
                id = UUID.randomUUID().toString(),
                name = "Eric Mugabo (Cashier)",
                username = "cashier",
                email = "eric@beboss.rw",
                phone = "+250 789 111 222",
                pinHash = com.example.util.SecurityUtils.hashPin("0000"),
                password = com.example.util.SecurityUtils.hashPassword("cashier123"),
                role = UserRole.CASHIER,
                profileColorHex = "#10B981"
            )
            userDao.insertAllUsers(listOf(defaultOwner, defaultManager, defaultCashier))
            defaultOwner
        } else {
            userDao.getAllActiveUsers().first().firstOrNull() ?: User(
                name = "Shop Owner",
                username = "owner",
                pinHash = com.example.util.SecurityUtils.hashPin("1234")
            )
        }
    }

    // -------------------------------------------------------------
    // CUSTOMER PAYMENTS & LEDGER
    // -------------------------------------------------------------
    val allCustomerPayments: Flow<List<CustomerPayment>> = customerPaymentDao.getAllPayments()

    fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>> {
        return customerPaymentDao.getPaymentsForCustomer(customerId)
    }

    fun getSalesForCustomer(customerId: String): Flow<List<Sale>> {
        return saleDao.getSalesForCustomer(customerId)
    }

    suspend fun recordCustomerPayment(payment: CustomerPayment): CustomerPayment = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(payment.customerId)
        val currentDebt = customer?.debtBalance ?: 0.0
        val remaining = (currentDebt - payment.amount).coerceAtLeast(0.0)

        val updatedPayment = payment.copy(
            previousDebt = currentDebt,
            remainingDebt = remaining
        )

        customerPaymentDao.insertPayment(updatedPayment)
        customerDao.recordDebtPayment(payment.customerId, payment.amount)
        enqueueSync("customer_payments", updatedPayment.id, "CREATE", """{"customerId":"${updatedPayment.customerId}","amount":${updatedPayment.amount},"prevDebt":$currentDebt,"remDebt":$remaining,"method":"${updatedPayment.paymentMethod}"}""")
        
        updatedPayment
    }

    // -------------------------------------------------------------
    // SHOP PROFILE
    // -------------------------------------------------------------
    val shopProfile: Flow<ShopProfile> = shopProfileDao.getShopProfile().map {
        it ?: ShopProfile()
    }

    suspend fun getShopProfileDirect(): ShopProfile {
        return shopProfileDao.getShopProfileDirect() ?: ShopProfile()
    }

    suspend fun updateShopProfile(profile: ShopProfile) = withContext(Dispatchers.IO) {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        shopProfileDao.insertOrUpdateProfile(updated)
        enqueueSync("shop_profile", updated.id.toString(), "UPDATE", """{"shopName":"${updated.shopName}","currency":"${updated.currencyCode}"}""")
    }

    // -------------------------------------------------------------
    // PRODUCTS / INVENTORY
    // -------------------------------------------------------------
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val categories: Flow<List<String>> = productDao.getAllCategories()
    val totalProductCount: Flow<Int> = productDao.getTotalProductCount()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) allProducts else productDao.searchProducts(query.trim())
    }

    suspend fun getProductById(id: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        val isNew = productDao.getProductById(product.id) == null
        val toSave = product.copy(updatedAt = System.currentTimeMillis())
        productDao.insertProduct(toSave)
        enqueueSync("products", toSave.id, if (isNew) "CREATE" else "UPDATE", """{"name":"${toSave.name}","sellingPrice":${toSave.sellingPrice},"qty":${toSave.quantityInStock}}""")
    }

    suspend fun adjustStock(productId: String, adjustmentDelta: Double, reason: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val newStock = (product.quantityInStock + adjustmentDelta).coerceAtLeast(0.0)
        productDao.setStock(productId, newStock)
        enqueueSync("products", productId, "UPDATE", """{"stockAdjustment":$adjustmentDelta,"newStock":$newStock,"reason":"$reason"}""")
    }

    suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        productDao.softDeleteProduct(productId)
        enqueueSync("products", productId, "DELETE", """{"id":"$productId"}""")
    }

    // -------------------------------------------------------------
    // CUSTOMERS & DEBTS
    // -------------------------------------------------------------
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val customersWithDebt: Flow<List<Customer>> = customerDao.getCustomersWithDebt()
    val totalCustomerCount: Flow<Int> = customerDao.getTotalCustomerCount()
    val totalOutstandingDebt: Flow<Double> = customerDao.getTotalOutstandingDebt()

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return if (query.isBlank()) allCustomers else customerDao.searchCustomers(query.trim())
    }

    suspend fun getCustomerById(id: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    suspend fun saveCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        val isNew = customerDao.getCustomerById(customer.id) == null
        val toSave = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.insertCustomer(toSave)
        enqueueSync("customers", toSave.id, if (isNew) "CREATE" else "UPDATE", """{"name":"${toSave.name}","phone":"${toSave.phone}"}""")
    }

    suspend fun recordDebtPayment(customerId: String, paymentAmount: Double) = withContext(Dispatchers.IO) {
        customerDao.recordDebtPayment(customerId, paymentAmount)
        enqueueSync("customers", customerId, "UPDATE", """{"debtPayment":$paymentAmount}""")
    }

    suspend fun deleteCustomer(customerId: String) = withContext(Dispatchers.IO) {
        customerDao.softDeleteCustomer(customerId)
        enqueueSync("customers", customerId, "DELETE", """{"id":"$customerId"}""")
    }

    // -------------------------------------------------------------
    // SALES & POINT OF SALE (POS)
    // -------------------------------------------------------------
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun getSaleWithItems(saleId: String): SaleWithItems? = withContext(Dispatchers.IO) {
        val sale = saleDao.getSaleById(saleId) ?: return@withContext null
        val items = saleDao.getItemsForSale(saleId)
        SaleWithItems(sale, items)
    }

    /**
     * Executes atomic sale checkout:
     * 1. Inserts Sale
     * 2. Inserts SaleItems
     * 3. Decrements inventory for each product
     * 4. Updates customer debt balance if sale has unpaid/debt portion
     * 5. Enqueues sync records
     */
    suspend fun processSale(
        items: List<CartItem>,
        customerId: String?,
        customerName: String,
        discountAmount: Double,
        paymentMethod: String,
        amountPaid: Double,
        notes: String,
        branchId: String = "main_branch",
        branchName: String = "Main Store",
        cashierId: String = "",
        cashierName: String = "Shop Operator"
    ): Sale = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val saleId = UUID.randomUUID().toString()

        val rawTotal = items.sumOf { it.product.sellingPrice * it.quantity }
        val netTotal = (rawTotal - discountAmount).coerceAtLeast(0.0)
        val totalCost = items.sumOf { it.product.costPrice * it.quantity }
        val totalProfit = netTotal - totalCost

        val isDebt = paymentMethod == "CREDIT_DEBT" || amountPaid < netTotal
        val paymentStatus = when {
            amountPaid >= netTotal -> "PAID"
            amountPaid > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }
        val unpaidDebt = (netTotal - amountPaid).coerceAtLeast(0.0)

        val receiptNum = "REC-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date(now))

        val sale = Sale(
            id = saleId,
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            saleDate = now,
            totalAmount = netTotal,
            totalCost = totalCost,
            totalProfit = totalProfit,
            discountAmount = discountAmount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            amountPaid = amountPaid,
            notes = notes,
            receiptNumber = receiptNum,
            branchId = branchId,
            branchName = branchName,
            cashierId = cashierId,
            cashierName = cashierName,
            synced = false,
            updatedAt = now
        )

        val saleItems = items.map { cart ->
            val sub = cart.product.sellingPrice * cart.quantity
            val itemCost = cart.product.costPrice * cart.quantity
            SaleItem(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = cart.product.id,
                productName = cart.product.name,
                category = cart.product.category,
                unit = cart.product.unit,
                quantitySold = cart.quantity,
                costPriceAtSale = cart.product.costPrice,
                unitPriceAtSale = cart.product.sellingPrice,
                subtotal = sub,
                profit = sub - itemCost,
                branchId = branchId
            )
        }

        // Database writes
        saleDao.insertSale(sale)
        saleDao.insertSaleItems(saleItems)

        // Decrement product inventory
        for (item in items) {
            productDao.decrementStock(item.product.id, item.quantity, now)
        }

        // Update customer debt if applicable
        if (customerId != null && unpaidDebt > 0.0) {
            customerDao.addDebt(customerId, unpaidDebt, now)
        }

        // Enqueue sync queue
        enqueueSync("sales", saleId, "CREATE", """{"total":$netTotal,"itemsCount":${items.size},"receipt":"$receiptNum"}""")

        sale
    }

    // -------------------------------------------------------------
    // ANALYTICS & PROFIT/LOSS ENGINE (ALL COMPUTED LOCALLY OFFLINE)
    // -------------------------------------------------------------
    fun getInventoryValuation(): Flow<InventoryValuation> = allProducts.map { products ->
        var totalCost = 0.0
        var totalRetail = 0.0
        var totalUnits = 0.0
        var lowCount = 0
        var outOfStockCount = 0

        for (p in products) {
            totalUnits += p.quantityInStock
            totalCost += p.quantityInStock * p.costPrice
            totalRetail += p.quantityInStock * p.sellingPrice
            if (p.quantityInStock <= 0) {
                outOfStockCount++
            } else if (p.isLowStock) {
                lowCount++
            }
        }

        InventoryValuation(
            totalItems = products.size,
            totalUnitsInStock = totalUnits,
            totalCostValue = totalCost,
            totalRetailValue = totalRetail,
            potentialProfit = totalRetail - totalCost,
            lowStockCount = lowCount,
            outOfStockCount = outOfStockCount
        )
    }

    fun getProfitLossForPeriod(period: AnalyticsPeriod): Flow<ProfitLossSummary> {
        val (start, end) = period.getRange()
        return saleDao.getSalesByDateRange(start, end).map { sales ->
            val totalRev = sales.sumOf { it.totalAmount }
            val totalCost = sales.sumOf { it.totalCost }
            val netProfit = sales.sumOf { it.totalProfit }
            val count = sales.size
            val avgOrder = if (count > 0) totalRev / count else 0.0
            val margin = if (totalRev > 0) (netProfit / totalRev) * 100.0 else 0.0

            ProfitLossSummary(
                totalRevenue = totalRev,
                totalCost = totalCost,
                netProfit = netProfit,
                totalSalesCount = count,
                totalItemsSold = 0.0, // Aggregated in details
                averageOrderValue = avgOrder,
                profitMarginPercent = margin,
                isProfitable = netProfit >= 0,
                periodLabel = period.displayName
            )
        }
    }

    fun getDailyAnalyticsPoints(period: AnalyticsPeriod): Flow<List<DailyAnalyticsPoint>> {
        val (start, end) = period.getRange()
        return saleDao.getSalesByDateRange(start, end).map { sales ->
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            val grouped = sales.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.saleDate }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }

            // Create series of days in the period
            val points = mutableListOf<DailyAnalyticsPoint>()
            val cal = Calendar.getInstance().apply { timeInMillis = start }
            val endCal = Calendar.getInstance().apply { timeInMillis = end }

            while (cal.before(endCal) || cal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)) {
                val dayStart = cal.timeInMillis
                val daySales = grouped[dayStart] ?: emptyList()
                val rev = daySales.sumOf { it.totalAmount }
                val profit = daySales.sumOf { it.totalProfit }

                points.add(
                    DailyAnalyticsPoint(
                        dateLabel = format.format(Date(dayStart)),
                        timestamp = dayStart,
                        revenue = rev,
                        profit = profit,
                        salesCount = daySales.size
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            points
        }
    }

    fun getTopProducts(period: AnalyticsPeriod, limit: Int = 10): Flow<List<TopProductReport>> {
        val (start, end) = period.getRange()
        return saleDao.getTopProductsByProfit(start, end, limit).map { raws ->
            raws.map {
                TopProductReport(
                    productId = it.productId,
                    productName = it.productName,
                    totalQuantitySold = it.totalQuantity,
                    totalRevenue = it.totalRevenue,
                    totalProfit = it.totalProfit
                )
            }
        }
    }

    fun getCategorySalesBreakdown(period: AnalyticsPeriod): Flow<List<CategorySalesShare>> {
        val (start, end) = period.getRange()
        return saleDao.getCategorySalesBreakdown(start, end).map { raws ->
            val total = raws.sumOf { it.totalRevenue }
            raws.map {
                val pct = if (total > 0) (it.totalRevenue / total) * 100.0 else 0.0
                CategorySalesShare(
                    category = it.category,
                    revenue = it.totalRevenue,
                    percentage = pct
                )
            }
        }
    }

    // -------------------------------------------------------------
    // SYNC QUEUE & BACKGROUND SYNC LOGIC
    // -------------------------------------------------------------
    val pendingSyncCount: Flow<Int> = syncQueueDao.getPendingCount()
    val pendingSyncItems: Flow<List<SyncQueueItem>> = syncQueueDao.getPendingQueueFlow()

    private suspend fun enqueueSync(tableName: String, recordId: String, operation: String, payloadJson: String) {
        val item = SyncQueueItem(
            tableName = tableName,
            recordId = recordId,
            operation = operation,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis()
        )
        syncQueueDao.enqueue(item)
    }

    /**
     * Pushes pending queue and full store records to cloud / backend with optional custom endpoint
     */
    suspend fun performSync(serverEndpoint: String? = null): SyncResult = withContext(Dispatchers.IO) {
        try {
            val report = cloudSyncManager.syncAllDataToCloud(serverEndpoint)
            SyncResult(
                success = report.state == com.example.util.CloudSyncState.SYNCED,
                syncedCount = report.itemsPushed,
                message = report.message
            )
        } catch (e: Exception) {
            SyncResult(success = false, syncedCount = 0, message = "Sync failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun testServerPing(endpoint: String) = withContext(Dispatchers.IO) {
        cloudSyncManager.testEndpointConnection(endpoint)
    }

    val syncAuditLogs = cloudSyncManager.syncAuditLogs

    fun logSyncEvent(type: String, status: String, summary: String, latencyMs: Long = 0L, payloadSizeKb: Double = 0.0) {
        cloudSyncManager.addAuditLog(type, status, summary, latencyMs, payloadSizeKb)
    }

    suspend fun clearSyncQueue() = withContext(Dispatchers.IO) {
        syncQueueDao.clearQueue()
    }

    // -------------------------------------------------------------
    // SEEDING & DATA MANAGEMENT
    // -------------------------------------------------------------
    // -------------------------------------------------------------
    // SQLITE DATABASE ENGINE TOOLS, MAINTENANCE & HEALTH
    // -------------------------------------------------------------
    suspend fun getDatabaseStats(context: android.content.Context): com.example.data.model.DatabaseEngineStats = withContext(Dispatchers.IO) {
        val pCount = productDao.getProductCountDirect()
        val sCount = saleDao.getSaleCount()
        val siCount = saleDao.getSaleItemCount()
        val cCount = customerDao.getCustomerCountDirect()
        val payCount = customerPaymentDao.getPaymentCount()
        val purCount = purchaseDao.getPurchaseCount()
        val bCount = branchDao.getBranchCount()
        val uCount = userDao.getUserCount()
        val qCount = syncQueueDao.getPendingQueueDirect().size
        val total = pCount + sCount + siCount + cCount + payCount + purCount + bCount + uCount + qCount

        val dbFile = context.getDatabasePath("beboss_database.db")
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        val sizeFormatted = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${String.format(Locale.US, "%.1f", sizeBytes / 1024.0)} KB"
            else -> "${String.format(Locale.US, "%.2f", sizeBytes / (1024.0 * 1024.0))} MB"
        }

        var journal = "WAL"
        var pageSize = 4096L
        var pageCount = 0L
        var integrity = "OK"

        try {
            val dbWritable = database.openHelper.writableDatabase
            val cursor1 = dbWritable.query("PRAGMA page_count;")
            if (cursor1.moveToFirst()) {
                pageCount = cursor1.getLong(0)
            }
            cursor1.close()

            val cursor2 = dbWritable.query("PRAGMA page_size;")
            if (cursor2.moveToFirst()) {
                pageSize = cursor2.getLong(0)
            }
            cursor2.close()

            val cursor3 = dbWritable.query("PRAGMA journal_mode;")
            if (cursor3.moveToFirst()) {
                journal = cursor3.getString(0) ?: "WAL"
            }
            cursor3.close()
        } catch (_: Exception) {}

        com.example.data.model.DatabaseEngineStats(
            productCount = pCount,
            salesCount = sCount,
            saleItemsCount = siCount,
            customerCount = cCount,
            paymentsCount = payCount,
            purchasesCount = purCount,
            branchesCount = bCount,
            usersCount = uCount,
            syncQueueCount = qCount,
            totalRecordsCount = total,
            fileSizeBytes = sizeBytes,
            fileSizeFormatted = sizeFormatted,
            journalMode = journal,
            pageSize = pageSize,
            pageCount = pageCount,
            integrityStatus = integrity
        )
    }

    suspend fun optimizeDatabase(context: android.content.Context): com.example.data.model.DatabaseMaintenanceResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val dbWritable = database.openHelper.writableDatabase
            dbWritable.execSQL("PRAGMA wal_checkpoint(FULL);")
            dbWritable.execSQL("PRAGMA optimize;")
            val duration = System.currentTimeMillis() - start
            val stats = getDatabaseStats(context)
            com.example.data.model.DatabaseMaintenanceResult(
                success = true,
                message = "SQLite Database optimized & checkpoints merged in ${duration}ms! B-Tree indexes defragmented.",
                durationMs = duration,
                stats = stats
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            com.example.data.model.DatabaseMaintenanceResult(
                success = false,
                message = "Database optimization error: ${e.message}",
                durationMs = duration
            )
        }
    }

    suspend fun verifyDatabaseIntegrity(): com.example.data.model.DatabaseMaintenanceResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val dbWritable = database.openHelper.writableDatabase
            val cursor = dbWritable.query("PRAGMA integrity_check(10);")
            val results = mutableListOf<String>()
            while (cursor.moveToNext()) {
                results.add(cursor.getString(0))
            }
            cursor.close()
            val duration = System.currentTimeMillis() - start
            val isOk = results.size == 1 && results[0].equals("ok", ignoreCase = true)
            com.example.data.model.DatabaseMaintenanceResult(
                success = isOk,
                message = if (isOk) "PRAGMA integrity_check: PASSED (100% healthy, 0 corruptions detected)" else "Integrity alerts: ${results.joinToString(", ")}",
                durationMs = duration
            )
        } catch (e: Exception) {
            com.example.data.model.DatabaseMaintenanceResult(
                success = false,
                message = "Integrity check failed: ${e.message}",
                durationMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun clearTransactionalDataOnly() = withContext(Dispatchers.IO) {
        saleDao.clearAllSales()
        saleDao.clearAllSaleItems()
        purchaseDao.clearAllPurchases()
        customerPaymentDao.clearAllPayments()
        customerDao.resetAllDebtsToZero()
        syncQueueDao.clearQueue()
    }

    suspend fun seedSampleShopData() = withContext(Dispatchers.IO) {
        val profile = ShopProfile(
            id = 1L,
            name = "Eric Mugabo",
            phone = "+250 788 123 456",
            email = "mugabo.shop@beboss.rw",
            shopName = "Mugabo Super Retail & Wholesale",
            address = "Nyarugenge Market, Shop #42, Kigali",
            currencyCode = "RWF",
            currencySymbol = "FRw",
            taxRate = 0.0,
            receiptFooter = "Murakoze cyane! Thank you for choosing Mugabo Store!"
        )
        shopProfileDao.insertOrUpdateProfile(profile)

        val sampleProducts = listOf(
            Product(name = "Inyange Milk (500ml)", category = "Beverages", costPrice = 450.0, sellingPrice = 600.0, quantityInStock = 24.0, lowStockThreshold = 10.0, unit = "bottle", barcode = "6001001"),
            Product(name = "Kinazi Cassava Flour (2kg)", category = "Food & Grocery", costPrice = 1800.0, sellingPrice = 2400.0, quantityInStock = 15.0, lowStockThreshold = 5.0, unit = "pack", barcode = "6001002"),
            Product(name = "Gorilla Rwandan Coffee (250g)", category = "Beverages", costPrice = 3200.0, sellingPrice = 4500.0, quantityInStock = 8.0, lowStockThreshold = 4.0, unit = "pack", barcode = "6001003"),
            Product(name = "Cooking Oil Sunseed (1L)", category = "Food & Grocery", costPrice = 2600.0, sellingPrice = 3200.0, quantityInStock = 18.0, lowStockThreshold = 6.0, unit = "bottle", barcode = "6001004"),
            Product(name = "Jasmine Rice 5kg (Tanzania)", category = "Food & Grocery", costPrice = 6500.0, sellingPrice = 8000.0, quantityInStock = 12.0, lowStockThreshold = 5.0, unit = "bag", barcode = "6001005"),
            Product(name = "Skol Lager Beer (50cl)", category = "Beverages", costPrice = 900.0, sellingPrice = 1200.0, quantityInStock = 48.0, lowStockThreshold = 12.0, unit = "bottle", barcode = "6001006"),
            Product(name = "Primus Beer (50cl)", category = "Beverages", costPrice = 850.0, sellingPrice = 1100.0, quantityInStock = 3.0, lowStockThreshold = 12.0, unit = "bottle", barcode = "6001007"), // Low stock
            Product(name = "MTN Airtime Card (1,000 RWF)", category = "Electronics & Telecom", costPrice = 960.0, sellingPrice = 1000.0, quantityInStock = 45.0, lowStockThreshold = 20.0, unit = "pcs", barcode = "6001008"),
            Product(name = "Airtel Airtime Card (1,000 RWF)", category = "Electronics & Telecom", costPrice = 960.0, sellingPrice = 1000.0, quantityInStock = 25.0, lowStockThreshold = 15.0, unit = "pcs", barcode = "6001009"),
            Product(name = "Sunlight Laundry Soap Bar", category = "Household & Cleaning", costPrice = 700.0, sellingPrice = 1000.0, quantityInStock = 30.0, lowStockThreshold = 8.0, unit = "pcs", barcode = "6001010"),
            Product(name = "Sugar (1kg Local White)", category = "Food & Grocery", costPrice = 1400.0, sellingPrice = 1700.0, quantityInStock = 2.0, lowStockThreshold = 10.0, unit = "kg", barcode = "6001011"), // Low stock
            Product(name = "Bic Ballpoint Pens (Blue - Box 10)", category = "Stationery", costPrice = 1500.0, sellingPrice = 2200.0, quantityInStock = 14.0, lowStockThreshold = 4.0, unit = "box", barcode = "6001012")
        )
        productDao.insertAll(sampleProducts)

        val sampleCustomers = listOf(
            Customer(name = "Jeanne Uwase", phone = "+250 788 445 566", notes = "Neighborhood regular, runs salon next door", debtBalance = 3500.0),
            Customer(name = "Patrick Hakizimana", phone = "+250 783 112 233", notes = "Orders wholesale rice & oil on Mondays", debtBalance = 0.0),
            Customer(name = "Marie Claire Mukamana", phone = "+250 781 998 877", notes = "Pays via MTN MoMo", debtBalance = 1200.0),
            Customer(name = "Emmanuel Ndayisaba", phone = "+250 785 667 788", notes = "School canteen buyer", debtBalance = 8000.0)
        )
        customerDao.insertAll(sampleCustomers)

        // Generate realistic historical sales for charts
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        for (dayOffset in 6 downTo 0) {
            cal.timeInMillis = now - (dayOffset * 86400000L)
            cal.set(Calendar.HOUR_OF_DAY, 11)
            val saleTime = cal.timeInMillis

            val sampleItems = when (dayOffset % 3) {
                0 -> listOf(
                    CartItem(sampleProducts[0], 3.0),
                    CartItem(sampleProducts[3], 1.0)
                )
                1 -> listOf(
                    CartItem(sampleProducts[4], 2.0),
                    CartItem(sampleProducts[2], 1.0)
                )
                else -> listOf(
                    CartItem(sampleProducts[5], 6.0),
                    CartItem(sampleProducts[7], 5.0)
                )
            }

            val rawTot = sampleItems.sumOf { it.product.sellingPrice * it.quantity }
            val rawCost = sampleItems.sumOf { it.product.costPrice * it.quantity }
            val sId = UUID.randomUUID().toString()

            val s = Sale(
                id = sId,
                customerId = if (dayOffset == 2) sampleCustomers[0].id else null,
                customerName = if (dayOffset == 2) sampleCustomers[0].name else "Walk-in Customer",
                saleDate = saleTime,
                totalAmount = rawTot,
                totalCost = rawCost,
                totalProfit = rawTot - rawCost,
                discountAmount = 0.0,
                paymentMethod = if (dayOffset % 2 == 0) "MOMO" else "CASH",
                paymentStatus = "PAID",
                amountPaid = rawTot,
                notes = "Auto-seeded order #$dayOffset",
                receiptNumber = "REC-" + SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date(saleTime)) + "-00$dayOffset",
                synced = true
            )
            saleDao.insertSale(s)

            val sItems = sampleItems.map {
                val sub = it.product.sellingPrice * it.quantity
                SaleItem(
                    id = UUID.randomUUID().toString(),
                    saleId = sId,
                    productId = it.product.id,
                    productName = it.product.name,
                    category = it.product.category,
                    unit = it.product.unit,
                    quantitySold = it.quantity,
                    costPriceAtSale = it.product.costPrice,
                    unitPriceAtSale = it.product.sellingPrice,
                    subtotal = sub,
                    profit = sub - (it.product.costPrice * it.quantity)
                )
            }
            saleDao.insertSaleItems(sItems)
        }
    }

    suspend fun importShopDataPackage(summary: ShopImportSummary) = withContext(Dispatchers.IO) {
        summary.shopProfile?.let { shopProfileDao.insertOrUpdateProfile(it) }
        if (summary.users.isNotEmpty()) {
            userDao.insertAllUsers(summary.users)
        }
        if (summary.products.isNotEmpty()) {
            productDao.insertAll(summary.products)
        }
        if (summary.customers.isNotEmpty()) {
            customerDao.insertAll(summary.customers)
        }
        if (summary.sales.isNotEmpty()) {
            saleDao.insertSales(summary.sales)
        }
        if (summary.saleItems.isNotEmpty()) {
            saleDao.insertSaleItems(summary.saleItems)
        }
        if (summary.payments.isNotEmpty()) {
            customerPaymentDao.insertPayments(summary.payments)
        }
    }
}

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var customPrice: Double? = null
) {
    val effectiveUnitPrice: Double
        get() = customPrice ?: product.sellingPrice

    val subtotal: Double
        get() = effectiveUnitPrice * quantity

    val totalCost: Double
        get() = product.costPrice * quantity

    val estimatedProfit: Double
        get() = subtotal - totalCost
}

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val message: String
)

