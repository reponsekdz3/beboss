package com.example.domain

import com.example.data.dao.SyncQueueDao
import com.example.data.dao.UserDao
import com.example.data.model.SyncQueueItem
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

/**
 * Domain Manager handling User Authentication, Role-based Access Control, and PBKDF2 credential upgrades.
 */
class StaffAuthManager(
    private val userDao: UserDao,
    private val syncQueueDao: SyncQueueDao
) {
    val allActiveUsers: Flow<List<User>> = userDao.getAllActiveUsers()

    suspend fun getUserByPin(pin: String): User? = withContext(Dispatchers.IO) {
        val trimmed = pin.trim()
        val allUsers = userDao.getAllActiveUsersList()
        for (user in allUsers) {
            if (SecurityUtils.verifyPin(trimmed, user.pinHash)) {
                // If stored PIN was using legacy hash or plain format, transparently upgrade to PBKDF2
                if (SecurityUtils.needsUpgrade(user.pinHash)) {
                    val upgradedUser = user.copy(
                        pinHash = SecurityUtils.hashPin(trimmed),
                        updatedAt = System.currentTimeMillis()
                    )
                    userDao.updateUser(upgradedUser)
                    return@withContext upgradedUser
                }
                return@withContext user
            }
        }
        null
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim().lowercase(Locale.getDefault()))
    }

    suspend fun verifyUserPassword(username: String, passwordAttempt: String): User? = withContext(Dispatchers.IO) {
        val user = userDao.getUserByUsername(username.trim().lowercase(Locale.getDefault())) ?: return@withContext null
        if (SecurityUtils.verifyPassword(passwordAttempt.trim(), user.password)) {
            if (SecurityUtils.needsUpgrade(user.password)) {
                val upgradedUser = user.copy(
                    password = SecurityUtils.hashPassword(passwordAttempt.trim()),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(upgradedUser)
                return@withContext upgradedUser
            }
            return@withContext user
        }
        null
    }

    suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
        val securedPin = if (user.pinHash.isNotBlank() && SecurityUtils.needsUpgrade(user.pinHash)) {
            SecurityUtils.hashPin(user.pinHash)
        } else {
            user.pinHash
        }

        val securedPassword = if (user.password.isNotBlank() && SecurityUtils.needsUpgrade(user.password)) {
            SecurityUtils.hashPassword(user.password)
        } else {
            user.password
        }

        val userToSave = user.copy(
            pinHash = securedPin,
            password = securedPassword,
            updatedAt = System.currentTimeMillis()
        )

        userDao.insertUser(userToSave)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "users",
                recordId = userToSave.id,
                operation = "UPDATE",
                payloadJson = """{"id":"${userToSave.id}","name":"${userToSave.name}","username":"${userToSave.username}","role":"${userToSave.role.name}","branchId":"${userToSave.assignedBranchId}","phone":"${userToSave.phone}","isActive":${userToSave.isActive}}"""
            )
        )
    }

    suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        userDao.deleteUser(userId)
        syncQueueDao.enqueue(
            SyncQueueItem(
                tableName = "users",
                recordId = userId,
                operation = "DELETE",
                payloadJson = """{"id":"$userId"}"""
            )
        )
    }

    suspend fun updateLastLogin(userId: String) = withContext(Dispatchers.IO) {
        userDao.updateLastLogin(userId)
    }

    suspend fun ensureDefaultAdminUser(): User = withContext(Dispatchers.IO) {
        val count = userDao.getUserCount()
        if (count == 0) {
            val defaultOwner = User(
                id = UUID.randomUUID().toString(),
                name = "Store Owner",
                username = "owner",
                email = "owner@beboss.rw",
                phone = "+250 788 123 456",
                pinHash = SecurityUtils.hashPin("2026"),
                password = SecurityUtils.hashPassword("OwnerSecure2026!"),
                role = UserRole.OWNER,
                profileColorHex = "#FF6B1A"
            )
            val defaultManager = User(
                id = UUID.randomUUID().toString(),
                name = "Store Manager",
                username = "manager",
                email = "manager@beboss.rw",
                phone = "+250 788 654 321",
                pinHash = SecurityUtils.hashPin("4821"),
                password = SecurityUtils.hashPassword("ManagerSecure2026!"),
                role = UserRole.MANAGER,
                profileColorHex = "#2563EB"
            )
            val defaultCashier = User(
                id = UUID.randomUUID().toString(),
                name = "Store Cashier",
                username = "cashier",
                email = "cashier@beboss.rw",
                phone = "+250 789 111 222",
                pinHash = SecurityUtils.hashPin("7392"),
                password = SecurityUtils.hashPassword("CashierSecure2026!"),
                role = UserRole.CASHIER,
                profileColorHex = "#10B981"
            )
            userDao.insertAllUsers(listOf(defaultOwner, defaultManager, defaultCashier))
            defaultOwner
        } else {
            userDao.getAllActiveUsers().first().firstOrNull() ?: User(
                name = "Store Owner",
                username = "owner",
                pinHash = SecurityUtils.hashPin("2026")
            )
        }
    }
}
