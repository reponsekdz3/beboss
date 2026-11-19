package com.example.ui.viewmodel.coordinators

import android.app.Application
import android.content.Context
import android.content.Intent
import com.example.data.model.Branch
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.ProfitLossSummary
import com.example.data.model.Sale
import com.example.data.model.SaleWithItems
import com.example.data.model.ShopProfile
import com.example.data.model.User
import com.example.data.repository.BeBossRepository
import com.example.ui.viewmodel.CartState
import com.example.util.AppLanguage
import com.example.util.BeBossNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosCartCoordinator(
    private val application: Application,
    private val repository: BeBossRepository,
    private val scope: CoroutineScope,
    private val language: StateFlow<AppLanguage>,
    private val currentUser: StateFlow<User?>,
    private val selectedBranchId: StateFlow<String>,
    private val branches: StateFlow<List<Branch>>,
    private val shopProfile: StateFlow<ShopProfile>,
    private val allSales: StateFlow<List<Sale>>,
    private val todaySummary: StateFlow<ProfitLossSummary>,
    private val lowStockProducts: StateFlow<List<Product>>,
    private val emitSnackbar: suspend (String) -> Unit,
    private val onMutationSuccess: () -> Unit
) {
    private val cartPrefs by lazy {
        application.getSharedPreferences("beboss_cart_state", Context.MODE_PRIVATE)
    }

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    private val _activeReceiptSale = MutableStateFlow<SaleWithItems?>(null)
    val activeReceiptSale: StateFlow<SaleWithItems?> = _activeReceiptSale.asStateFlow()

    init {
        restoreCartDraft()
    }

    private fun updateAndPersistCart(transform: (CartState) -> CartState) {
        val next = transform(_cartState.value)
        _cartState.value = next
        persistCartDraft(next)
    }

    private fun persistCartDraft(state: CartState) {
        try {
            if (state.items.isEmpty() && state.selectedCustomerId == null && state.discountAmount == 0.0 && state.notes.isBlank()) {
                cartPrefs.edit().remove("draft_json").apply()
                return
            }
            val arr = JSONArray()
            state.items.forEach { item ->
                arr.put(JSONObject().apply {
                    put("productId", item.product.id)
                    put("quantity", item.quantity)
                    if (item.customPrice != null) put("customPrice", item.customPrice)
                })
            }
            val root = JSONObject().apply {
                put("items", arr)
                put("selectedCustomerId", state.selectedCustomerId ?: JSONObject.NULL)
                put("selectedCustomerName", state.selectedCustomerName)
                put("discountAmount", state.discountAmount)
                put("paymentMethod", state.paymentMethod)
                put("amountPaidInput", state.amountPaidInput)
                put("notes", state.notes)
            }
            cartPrefs.edit().putString("draft_json", root.toString()).apply()
        } catch (_: Exception) {
            // Non-fatal persistence best-effort
        }
    }

    private fun restoreCartDraft() {
        val raw = cartPrefs.getString("draft_json", null) ?: return
        scope.launch(Dispatchers.IO) {
            try {
                if (_cartState.value.items.isNotEmpty()) return@launch
                val root = JSONObject(raw)
                val itemsArr = root.optJSONArray("items") ?: JSONArray()
                val restoredItems = mutableListOf<CartItem>()
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    val prodId = obj.optString("productId")
                    val qty = obj.optDouble("quantity", 0.0)
                    if (prodId.isBlank() || qty <= 0.0) continue
                    val prod = repository.getProductById(prodId) ?: continue
                    if (prod.isDeleted) continue
                    val customPrice = if (obj.has("customPrice") && !obj.isNull("customPrice")) {
                        obj.optDouble("customPrice")
                    } else null
                    restoredItems.add(CartItem(product = prod, quantity = qty, customPrice = customPrice))
                }
                if (restoredItems.isNotEmpty() || !root.isNull("selectedCustomerId")) {
                    _cartState.value = CartState(
                        items = restoredItems,
                        selectedCustomerId = if (root.isNull("selectedCustomerId")) null else root.optString("selectedCustomerId"),
                        selectedCustomerName = root.optString("selectedCustomerName", "Walk-in Customer"),
                        discountAmount = root.optDouble("discountAmount", 0.0).coerceAtLeast(0.0),
                        paymentMethod = root.optString("paymentMethod", "CASH"),
                        amountPaidInput = root.optString("amountPaidInput", ""),
                        notes = root.optString("notes", "")
                    )
                }
            } catch (_: Exception) {
                cartPrefs.edit().remove("draft_json").apply()
            }
        }
    }

    fun addToCart(product: Product, quantity: Double = 1.0) {
        updateAndPersistCart { state ->
            val current = state.items.toMutableList()
            val index = current.indexOfFirst { it.product.id == product.id }
            if (index >= 0) {
                val existing = current[index]
                current[index] = existing.copy(quantity = existing.quantity + quantity)
            } else {
                current.add(CartItem(product = product, quantity = quantity))
            }
            state.copy(items = current)
        }
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Double) {
        updateAndPersistCart { state ->
            val current = state.items.toMutableList()
            val index = current.indexOfFirst { it.product.id == productId }
            if (index >= 0) {
                if (newQuantity <= 0) {
                    current.removeAt(index)
                } else {
                    current[index] = current[index].copy(quantity = newQuantity)
                }
                state.copy(items = current)
            } else state
        }
    }

    fun removeCartItem(productId: String) {
        updateAndPersistCart { state ->
            state.copy(items = state.items.filterNot { it.product.id == productId })
        }
    }

    fun clearCart() {
        _cartState.value = CartState()
        cartPrefs.edit().remove("draft_json").apply()
    }

    fun setCartCustomer(customer: Customer?) {
        updateAndPersistCart { state ->
            state.copy(
                selectedCustomerId = customer?.id,
                selectedCustomerName = customer?.name ?: "Walk-in Customer"
            )
        }
    }

    fun setCartPaymentMethod(method: String) {
        updateAndPersistCart { it.copy(paymentMethod = method) }
    }

    fun setCartDiscount(discount: Double) {
        updateAndPersistCart { it.copy(discountAmount = discount.coerceAtLeast(0.0)) }
    }

    fun setCartAmountPaidInput(input: String) {
        updateAndPersistCart { it.copy(amountPaidInput = input) }
    }

    fun setCartNotes(notes: String) {
        updateAndPersistCart { it.copy(notes = notes) }
    }

    fun checkoutSale() {
        val state = _cartState.value
        if (state.items.isEmpty()) return

        scope.launch {
            try {
                val user = currentUser.value
                val branch = branches.value.firstOrNull { it.id == selectedBranchId.value }
                    ?: branches.value.firstOrNull { it.isMainBranch }
                    ?: Branch()

                val sale = repository.processSale(
                    items = state.items,
                    customerId = state.selectedCustomerId,
                    customerName = state.selectedCustomerName,
                    discountAmount = state.discountAmount,
                    paymentMethod = state.paymentMethod,
                    amountPaid = state.effectiveAmountPaid,
                    notes = state.notes,
                    branchId = branch.id,
                    branchName = branch.name,
                    cashierId = user?.id ?: "",
                    cashierName = user?.name ?: "Shop Operator"
                )
                val saleWithItems = repository.getSaleWithItems(sale.id)
                _activeReceiptSale.value = saleWithItems
                clearCart()

                BeBossNotificationManager.sendSaleCompletedNotification(application, sale, shopProfile.value)

                val currentLowStock = repository.getProductById(state.items.firstOrNull()?.product?.id ?: "")
                if (currentLowStock != null && currentLowStock.isLowStock) {
                    BeBossNotificationManager.sendLowStockNotification(
                        application,
                        currentLowStock.name,
                        currentLowStock.quantityInStock,
                        currentLowStock.lowStockThreshold,
                        currentLowStock.unit
                    )
                }

                val msg = if (language.value == AppLanguage.KINYARWANDA)
                    "Igurisha ryakozwe neza! Inyemezabuguzi irasohotse."
                else "Sale recorded successfully! Receipt ready."
                emitSnackbar(msg)
                onMutationSuccess()
            } catch (e: Exception) {
                emitSnackbar("Error processing sale: ${e.message}")
            }
        }
    }

    fun performQuickCustomSale(description: String, amount: Double, paymentMethod: String, isDirectCheckout: Boolean = true) {
        if (amount <= 0) return
        val itemName = description.trim().ifBlank { "Custom Quick Item" }
        val branch = branches.value.firstOrNull { it.id == selectedBranchId.value }
            ?: branches.value.firstOrNull { it.isMainBranch }
            ?: Branch()
        val user = currentUser.value

        val customProduct = Product(
            id = "custom_${System.currentTimeMillis()}",
            name = itemName,
            category = "Quick Sale",
            costPrice = amount * 0.7,
            sellingPrice = amount,
            quantityInStock = 999.0,
            unit = "item",
            branchId = branch.id
        )

        if (!isDirectCheckout) {
            addToCart(customProduct, 1.0)
            scope.launch {
                emitSnackbar("Added '$itemName' (${amount.toInt()} RWF) to POS Cart.")
            }
            return
        }

        scope.launch {
            try {
                val sale = repository.processSale(
                    items = listOf(CartItem(product = customProduct, quantity = 1.0, customPrice = amount)),
                    customerId = null,
                    customerName = "Quick Cash Customer",
                    discountAmount = 0.0,
                    paymentMethod = paymentMethod,
                    amountPaid = amount,
                    notes = "Quick POS Sale: $itemName",
                    branchId = branch.id,
                    branchName = branch.name,
                    cashierId = user?.id ?: "",
                    cashierName = user?.name ?: "Shop Operator"
                )
                val saleWithItems = repository.getSaleWithItems(sale.id)
                _activeReceiptSale.value = saleWithItems
                onMutationSuccess()
                val msg = if (language.value == AppLanguage.KINYARWANDA)
                    "Igurisha ry'ako kanya (${amount.toInt()} FRw) ryakozwe neza!"
                else "Quick sale (${amount.toInt()} RWF) completed! Receipt ready."
                emitSnackbar(msg)
            } catch (e: Exception) {
                emitSnackbar("Quick sale error: ${e.message}")
            }
        }
    }

    fun openReceipt(saleId: String) {
        scope.launch {
            _activeReceiptSale.value = repository.getSaleWithItems(saleId)
        }
    }

    fun openLastReceipt() {
        val last = allSales.value.firstOrNull()
        if (last != null) {
            openReceipt(last.id)
        } else {
            scope.launch {
                emitSnackbar("No recent sales to display yet.")
            }
        }
    }

    fun closeReceipt() {
        _activeReceiptSale.value = null
    }

    fun shareDailyWhatsAppSummary(context: Context) {
        val shop = shopProfile.value
        val summary = todaySummary.value
        val lowStock = lowStockProducts.value
        val timeStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val text = buildString {
            append("*${shop.shopName.ifBlank { "BeBoss Store" }} - Daily Business Report*\n")
            append("$timeStr\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("*Today's Revenue:* ${summary.totalRevenue.toInt()} ${shop.currencySymbol}\n")
            append("*Net Estimated Profit:* ${summary.netProfit.toInt()} ${shop.currencySymbol}\n")
            append("*Transactions Completed:* ${summary.totalSalesCount}\n")
            append("*Items Sold:* ${summary.totalItemsSold.toInt()}\n")
            append("*Average Basket Value:* ${summary.averageOrderValue.toInt()} ${shop.currencySymbol}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (lowStock.isNotEmpty()) {
                append("*Low Stock Alert (${lowStock.size} items):*\n")
                lowStock.take(5).forEach { p ->
                    append("• ${p.name}: ${p.quantityInStock.toInt()} ${p.unit} left\n")
                }
            } else {
                append("All product stocks are healthy.\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("_Generated with BeBoss POS_")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Daily Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
