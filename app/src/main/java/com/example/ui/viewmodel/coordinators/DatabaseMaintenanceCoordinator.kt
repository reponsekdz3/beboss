package com.example.ui.viewmodel.coordinators

import android.app.Application
import android.content.Context
import com.example.data.model.DatabaseEngineStats
import com.example.data.model.DatabaseMaintenanceResult
import com.example.data.repository.BeBossRepository
import com.example.util.AppLanguage
import com.example.util.BeBossLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DatabaseMaintenanceCoordinator(
    private val application: Application,
    private val repository: BeBossRepository,
    private val scope: CoroutineScope,
    private val language: StateFlow<AppLanguage>,
    private val onClearUiSessionState: () -> Unit,
    private val emitSnackbar: suspend (String) -> Unit
) {
    private val _databaseStats = MutableStateFlow<DatabaseEngineStats?>(null)
    val databaseStats: StateFlow<DatabaseEngineStats?> = _databaseStats.asStateFlow()

    private val _databaseMaintenanceResult = MutableStateFlow<DatabaseMaintenanceResult?>(null)
    val databaseMaintenanceResult: StateFlow<DatabaseMaintenanceResult?> = _databaseMaintenanceResult.asStateFlow()

    private val _isOptimizingDb = MutableStateFlow(false)
    val isOptimizingDb: StateFlow<Boolean> = _isOptimizingDb.asStateFlow()

    private val _startupIntegrityOk = MutableStateFlow<Boolean?>(null)
    val startupIntegrityOk: StateFlow<Boolean?> = _startupIntegrityOk.asStateFlow()

    fun verifyStartupIntegritySilently() {
        scope.launch(Dispatchers.IO) {
            try {
                val result = repository.verifyDatabaseIntegrity()
                _startupIntegrityOk.value = result.success
                _databaseMaintenanceResult.value = result
                if (!result.success) {
                    BeBossLogger.e("DatabaseMaintenance", "Startup integrity issue: ${result.message}")
                }
            } catch (e: Exception) {
                _startupIntegrityOk.value = false
                BeBossLogger.e("DatabaseMaintenance", "Startup integrity check exception", e)
            }
        }
    }

    fun refreshDatabaseStats(context: Context? = null) {
        val targetCtx = context ?: application
        scope.launch {
            try {
                _databaseStats.value = repository.getDatabaseStats(targetCtx)
            } catch (e: Exception) {
                BeBossLogger.e("DatabaseMaintenance", "Failed to get database stats", e)
            }
        }
    }

    fun optimizeDatabase(context: Context? = null) {
        val targetCtx = context ?: application
        scope.launch {
            _isOptimizingDb.value = true
            try {
                val result = repository.optimizeDatabase(targetCtx)
                _databaseMaintenanceResult.value = result
                result.stats?.let { _databaseStats.value = it }
                repository.recordLocalAudit(
                    actorName = "System",
                    actorRole = "DATABASE",
                    action = "DB_OPTIMIZE",
                    details = result.message
                )
                emitSnackbar(result.message)
            } catch (e: Exception) {
                emitSnackbar("Optimization failed: ${e.message}")
            } finally {
                _isOptimizingDb.value = false
            }
        }
    }

    fun verifyDatabaseIntegrity() {
        scope.launch {
            _isOptimizingDb.value = true
            try {
                val result = repository.verifyDatabaseIntegrity()
                _startupIntegrityOk.value = result.success
                _databaseMaintenanceResult.value = result
                repository.recordLocalAudit(
                    actorName = "System",
                    actorRole = "DATABASE",
                    action = "DB_INTEGRITY_CHECK",
                    details = result.message
                )
                emitSnackbar(result.message)
            } catch (e: Exception) {
                _startupIntegrityOk.value = false
                emitSnackbar("Integrity check failed: ${e.message}")
            } finally {
                _isOptimizingDb.value = false
            }
        }
    }

    fun clearTransactionalDataOnly(context: Context? = null) {
        val targetCtx = context ?: application
        scope.launch {
            try {
                repository.clearTransactionalDataOnly()
                onClearUiSessionState()
                refreshDatabaseStats(targetCtx)
                repository.recordLocalAudit(
                    actorName = "Operator",
                    actorRole = "OWNER",
                    action = "DB_CLEAR_TRANSACTIONS",
                    details = "Cleared sales, purchases, and customer payment ledgers"
                )
                val msg = if (language.value == AppLanguage.KINYARWANDA)
                    "Amateka yose yo kugurisha yasibwe! Ibicuruzwa n'abakiriya byagumyeho."
                else "All sales & transaction history reset! Products & customers preserved."
                emitSnackbar(msg)
            } catch (e: Exception) {
                emitSnackbar("Failed to reset transactions: ${e.message}")
            }
        }
    }

    fun clearAllProducts(context: Context? = null, onClearedQuery: () -> Unit) {
        val targetCtx = context ?: application
        scope.launch {
            try {
                repository.clearAllProducts()
                onClearedQuery()
                refreshDatabaseStats(targetCtx)
                repository.recordLocalAudit(
                    actorName = "Operator",
                    actorRole = "OWNER",
                    action = "DB_CLEAR_PRODUCTS",
                    details = "Cleared product catalog"
                )
                val msg = if (language.value == AppLanguage.KINYARWANDA)
                    "Ibicuruzwa byose byasibwe mu bubiko!"
                else "All products and inventory stock wiped from database!"
                emitSnackbar(msg)
            } catch (e: Exception) {
                emitSnackbar("Failed to clear products: ${e.message}")
            }
        }
    }

    fun clearAllCustomers(context: Context? = null, onClearedQuery: () -> Unit) {
        val targetCtx = context ?: application
        scope.launch {
            try {
                repository.clearAllCustomers()
                onClearedQuery()
                refreshDatabaseStats(targetCtx)
                repository.recordLocalAudit(
                    actorName = "Operator",
                    actorRole = "OWNER",
                    action = "DB_CLEAR_CUSTOMERS",
                    details = "Cleared customer ledger"
                )
                val msg = if (language.value == AppLanguage.KINYARWANDA)
                    "Abakiriya bose n'imyenda byasibwe!"
                else "All customers and credit debt records cleared!"
                emitSnackbar(msg)
            } catch (e: Exception) {
                emitSnackbar("Failed to clear customers: ${e.message}")
            }
        }
    }

    fun resetWholeApp(context: Context? = null, onClearedAllQueries: () -> Unit) {
        val targetCtx = context ?: application
        scope.launch {
            try {
                repository.resetWholeApp(targetCtx)
                onClearUiSessionState()
                onClearedAllQueries()
                refreshDatabaseStats(targetCtx)
                val msg = if (language.value == AppLanguage.KINYARWANDA)
                    "Porogaramu yose yasubijwe ku ntangiriro (Factory Reset)! Amakuru yose yasibwe."
                else "Whole app factory reset complete! All products, customers & transactions wiped clean."
                emitSnackbar(msg)
            } catch (e: Exception) {
                emitSnackbar("Failed to reset app: ${e.message}")
            }
        }
    }

    fun seedSampleData() {
        scope.launch {
            try {
                repository.seedSampleShopData()
                emitSnackbar("Sample demo data loaded successfully!")
            } catch (e: Exception) {
                emitSnackbar("Failed to load sample data: ${e.message}")
            }
        }
    }
}
