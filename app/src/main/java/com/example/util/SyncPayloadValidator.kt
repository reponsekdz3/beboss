package com.example.util

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Strict schema, version, size, nesting-depth, foreign-key, and numeric bounds validator
 * for all incoming DataShare packages, P2P LAN packets, and cloud snapshots.
 */
object SyncPayloadValidator {

    const val CURRENT_SCHEMA_VERSION = 3
    const val MAX_RAW_PAYLOAD_BYTES = 5 * 1024 * 1024 // 5 MB max JSON package
    const val MAX_TOKEN_LENGTH = 4 * 1024 * 1024
    const val MAX_JSON_DEPTH = 12
    const val MAX_RECORDS_PER_ARRAY = 25_000

    private val allowedArrayKeys = listOf(
        "branches",
        "users",
        "products",
        "customers",
        "sales",
        "saleItems",
        "customerPayments",
        "purchases",
        "stockTransfers",
        "tombstones"
    )

    fun validateAndParseRoot(rawJson: String): JSONObject {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Sync payload is empty." }
        val byteLen = trimmed.toByteArray(StandardCharsets.UTF_8).size
        require(byteLen <= MAX_RAW_PAYLOAD_BYTES) {
            "Sync payload exceeds maximum safe size (${byteLen / 1024} KB > 5120 KB)."
        }
        requireDepthWithinBounds(trimmed)

        val root = try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid sync package JSON: ${e.message}", e)
        }

        // Verify at least one recognized BeBoss envelope section exists
        val hasKnownSection = root.has("metadata") ||
            root.has("shopProfile") ||
            allowedArrayKeys.any { root.has(it) }
        require(hasKnownSection) { "Unrecognized BeBoss sync payload schema." }

        // Enforce schema version compatibility when declared
        val declaredSchemaVersion = root.optInt("schemaVersion",
            root.optJSONObject("metadata")?.optInt("schemaVersion", 1) ?: 1
        )
        require(declaredSchemaVersion in 1..CURRENT_SCHEMA_VERSION) {
            "Unsupported sync schema version ($declaredSchemaVersion > $CURRENT_SCHEMA_VERSION). Update BeBoss before importing."
        }

        for (key in allowedArrayKeys) {
            if (!root.has(key)) continue
            val arr = root.optJSONArray(key)
                ?: throw IllegalArgumentException("Malformed '$key' section: expected JSON array.")
            require(arr.length() <= MAX_RECORDS_PER_ARRAY) {
                "Section '$key' exceeds record cap ($MAX_RECORDS_PER_ARRAY)."
            }
            validateArrayEntries(key, arr)
        }

        // Cross-section referential integrity: prevent orphan saleItems when sales array is included
        val salesArr = root.optJSONArray("sales")
        val saleItemsArr = root.optJSONArray("saleItems")
        if (salesArr != null && saleItemsArr != null) {
            val saleIds = HashSet<String>(salesArr.length())
            for (i in 0 until salesArr.length()) {
                val id = salesArr.optJSONObject(i)?.optString("id", "").orEmpty().trim()
                if (id.isNotEmpty()) saleIds.add(id)
            }
            for (i in 0 until saleItemsArr.length()) {
                val item = saleItemsArr.optJSONObject(i) ?: continue
                val fkSaleId = item.optString("saleId", "").trim()
                require(fkSaleId.isNotEmpty() && fkSaleId in saleIds) {
                    "Orphan foreign key at saleItems[$i]: saleId '$fkSaleId' missing from sales."
                }
            }
        }

        root.optJSONObject("shopProfile")?.let { sp ->
            val taxRate = sp.optDouble("taxRate", 0.0)
            require(taxRate.isFinite() && taxRate in 0.0..100.0) { "Invalid shopProfile taxRate." }
            val monthlyFee = sp.optInt("monthlyFeeRwf", 5000)
            require(monthlyFee >= 0) { "Invalid shopProfile monthlyFeeRwf." }
        }

        return root
    }

    private fun validateArrayEntries(section: String, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
                ?: throw IllegalArgumentException("Invalid object at $section[$i].")
            val id = obj.optString("id", "").trim()
            require(id.isNotEmpty() && id.length <= 128) {
                "Missing or invalid record id at $section[$i]."
            }
            when (section) {
                "branches" -> {
                    require(obj.optString("name", "").isNotBlank()) { "Blank branch name at branches[$i]." }
                }
                "users" -> {
                    require(obj.optString("username", "").isNotBlank()) { "Blank username at users[$i]." }
                }
                "products" -> {
                    require(obj.optString("name", "").isNotBlank()) { "Blank product name at products[$i]." }
                    assertFiniteNonNegative(obj.optDouble("costPrice", 0.0), "products[$i].costPrice")
                    assertFiniteNonNegative(obj.optDouble("sellingPrice", 0.0), "products[$i].sellingPrice")
                    assertFiniteNonNegative(obj.optDouble("quantityInStock", 0.0), "products[$i].quantityInStock")
                }
                "customers" -> {
                    require(obj.optString("name", "").isNotBlank()) { "Blank customer name at customers[$i]." }
                    assertFiniteNonNegative(obj.optDouble("creditLimit", 0.0), "customers[$i].creditLimit")
                    assertFiniteNonNegative(obj.optDouble("debtBalance", 0.0), "customers[$i].debtBalance")
                }
                "sales" -> {
                    assertFiniteNonNegative(obj.optDouble("totalAmount", 0.0), "sales[$i].totalAmount")
                    assertFiniteNonNegative(obj.optDouble("amountPaid", 0.0), "sales[$i].amountPaid")
                    assertFiniteNonNegative(obj.optDouble("discountAmount", 0.0), "sales[$i].discountAmount")
                }
                "saleItems" -> {
                    require(obj.optString("saleId", "").isNotBlank()) { "Blank saleId at saleItems[$i]." }
                    assertFiniteNonNegative(obj.optDouble("quantitySold", 0.0), "saleItems[$i].quantitySold")
                    assertFiniteNonNegative(obj.optDouble("unitPriceAtSale", 0.0), "saleItems[$i].unitPriceAtSale")
                    assertFiniteNonNegative(obj.optDouble("costPriceAtSale", 0.0), "saleItems[$i].costPriceAtSale")
                    assertFiniteNonNegative(obj.optDouble("subtotal", 0.0), "saleItems[$i].subtotal")
                }
                "customerPayments" -> {
                    require(obj.optString("customerId", "").isNotBlank()) { "Blank customerId at customerPayments[$i]." }
                    assertFiniteNonNegative(obj.optDouble("amount", 0.0), "customerPayments[$i].amount")
                }
                "purchases" -> {
                    assertFiniteNonNegative(obj.optDouble("quantityPurchased", 0.0), "purchases[$i].quantityPurchased")
                    assertFiniteNonNegative(obj.optDouble("unitCostPrice", 0.0), "purchases[$i].unitCostPrice")
                    assertFiniteNonNegative(obj.optDouble("totalPurchaseCost", 0.0), "purchases[$i].totalPurchaseCost")
                }
                "stockTransfers" -> {
                    assertFiniteNonNegative(obj.optDouble("quantity", 0.0), "stockTransfers[$i].quantity")
                }
                "tombstones" -> {
                    require(obj.optString("tableName", "").isNotBlank()) { "Blank tableName at tombstones[$i]." }
                    require(obj.optString("recordId", "").isNotBlank()) { "Blank recordId at tombstones[$i]." }
                    assertFiniteNonNegative(obj.optDouble("deletedAt", 0.0), "tombstones[$i].deletedAt")
                }
            }
        }
    }

    private fun assertFiniteNonNegative(value: Double, field: String) {
        require(value.isFinite() && value >= 0.0) {
            "Numeric bounds violation on $field ($value)."
        }
    }

    private fun requireDepthWithinBounds(raw: String) {
        var depth = 0
        var inQuotes = false
        var escaped = false
        for (ch in raw) {
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inQuotes) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (ch == '{' || ch == '[') {
                    depth++
                    require(depth <= MAX_JSON_DEPTH) {
                        "JSON nesting depth exceeds safe limit ($MAX_JSON_DEPTH)."
                    }
                } else if (ch == '}' || ch == ']') {
                    depth = (depth - 1).coerceAtLeast(0)
                }
            }
        }
    }
}
