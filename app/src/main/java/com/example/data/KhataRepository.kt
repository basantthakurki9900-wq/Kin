package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class TxnResult {
    data class Success(val message: String, val account: CustomerAccount) : TxnResult()
    data class DepositLimitExceeded(val newAdvance: Double, val limit: Double, val details: TxnParams) : TxnResult()
    data class Error(val message: String) : TxnResult()
}

data class TxnParams(
    val accountId: String,
    val delta: Double,
    val note: String,
    val source: String,
    val items: String,
    val dueDate: Long
)

class KhataRepository(private val dao: KhataDao) {

    val activeAccounts: Flow<List<CustomerAccount>> = dao.getActiveAccountsFlow()
    val deletedAccounts: Flow<List<CustomerAccount>> = dao.getDeletedAccountsFlow()
    val allLogs: Flow<List<LedgerLog>> = dao.getAllLogsFlow()
    val stockItems: Flow<List<StockItem>> = dao.getStockItemsFlow()
    val settings: Flow<ShopSettings?> = dao.getSettingsFlow()

    fun getAccountFlow(id: String): Flow<CustomerAccount?> = dao.getAccountFlow(id)
    fun getLogsForAccountFlow(id: String): Flow<List<LedgerLog>> = dao.getLogsForAccountFlow(id)

    suspend fun getSettings(): ShopSettings {
        return dao.getSettings() ?: ShopSettings().also { dao.saveSettings(it) }
    }

    suspend fun saveSettings(settings: ShopSettings) {
        dao.saveSettings(settings)
    }

    suspend fun createAccount(
        name: String,
        mobile: String,
        source: String = "मैनुअल",
        limit: Double? = null
    ): Result<CustomerAccount> {
        val trimmedName = name.trim()
        val cleanPhone = mobile.filter { it.isDigit() }.takeLast(10)
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("नाम डालिए"))
        if (cleanPhone.length < 10) return Result.failure(IllegalArgumentException("मोबाइल नंबर 10 अंक का होना चाहिए"))

        val existing = dao.getAccountByMobile(cleanPhone)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("इस नंबर का खाता पहले से मौजूद है: ${existing.name}"))
        }

        val currentSettings = getSettings()
        val creditLimit = limit ?: currentSettings.defaultCreditLimit
        val account = CustomerAccount(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            mobile = cleanPhone,
            balance = 0.0,
            creditLimit = creditLimit,
            created = System.currentTimeMillis()
        )
        dao.insertAccount(account)

        val log = LedgerLog(
            id = UUID.randomUUID().toString(),
            type = "create",
            accId = account.id,
            name = account.name,
            mobile = account.mobile,
            amount = 0.0,
            beforeBalance = 0.0,
            afterBalance = 0.0,
            note = if (creditLimit > 0) "सीमा: ₹${creditLimit.toInt()}" else "",
            source = source
        )
        dao.insertLog(log)
        return Result.success(account)
    }

    suspend fun processTransaction(params: TxnParams, force: Boolean = false): TxnResult {
        val account = dao.getAccountById(params.accountId)
            ?: return TxnResult.Error("खाता नहीं मिला")

        val before = account.balance
        val newBal = Math.round((account.balance + params.delta) * 100.0) / 100.0
        val currentSettings = getSettings()

        if (params.delta < 0 && currentSettings.depositLimit > 0 && !force) {
            val newAdvance = if (newBal < 0) Math.abs(newBal) else 0.0
            if (newAdvance > currentSettings.depositLimit) {
                return TxnResult.DepositLimitExceeded(
                    newAdvance = newAdvance,
                    limit = currentSettings.depositLimit,
                    details = params
                )
            }
        }

        val updatedAccount = account.copy(balance = newBal)
        dao.updateAccount(updatedAccount)

        val isCredit = params.delta > 0
        val log = LedgerLog(
            id = UUID.randomUUID().toString(),
            type = if (isCredit) "add" else "sub",
            accId = updatedAccount.id,
            name = updatedAccount.name,
            mobile = updatedAccount.mobile,
            amount = Math.abs(params.delta),
            beforeBalance = before,
            afterBalance = newBal,
            note = params.note,
            source = params.source,
            items = params.items,
            dueDate = if (isCredit) params.dueDate else 0L
        )
        dao.insertLog(log)

        val actionMsg = if (isCredit) "उधार चढ़ाया गया" else "जमा किया गया"
        return TxnResult.Success("₹${Math.abs(params.delta).toInt()} $actionMsg", updatedAccount)
    }

    suspend fun deleteAccount(id: String) {
        val account = dao.getAccountById(id) ?: return
        val updated = account.copy(deleted = true, deletedAt = System.currentTimeMillis())
        dao.updateAccount(updated)

        dao.insertLog(
            LedgerLog(
                id = UUID.randomUUID().toString(),
                type = "delete",
                accId = account.id,
                name = account.name,
                mobile = account.mobile,
                amount = 0.0,
                beforeBalance = account.balance,
                afterBalance = account.balance,
                note = "खाता हटाया गया"
            )
        )
    }

    suspend fun restoreAccount(id: String) {
        val account = dao.getAccountById(id) ?: return
        val updated = account.copy(deleted = false, deletedAt = null)
        dao.updateAccount(updated)

        dao.insertLog(
            LedgerLog(
                id = UUID.randomUUID().toString(),
                type = "restore",
                accId = account.id,
                name = account.name,
                mobile = account.mobile,
                amount = 0.0,
                beforeBalance = account.balance,
                afterBalance = account.balance,
                note = "खाता वापस लाया गया"
            )
        )
    }

    suspend fun purgeAccount(id: String) {
        val account = dao.getAccountById(id) ?: return
        dao.purgeAccount(id)
        dao.insertLog(
            LedgerLog(
                id = UUID.randomUUID().toString(),
                type = "purge",
                accId = account.id,
                name = account.name,
                mobile = account.mobile,
                amount = 0.0,
                beforeBalance = account.balance,
                afterBalance = account.balance,
                note = "खाता हमेशा के लिए हटाया"
            )
        )
    }

    suspend fun updateAccountDetails(id: String, newName: String, newMobile: String, newLimit: Double): Result<CustomerAccount> {
        val account = dao.getAccountById(id) ?: return Result.failure(IllegalArgumentException("खाता नहीं मिला"))
        val trimmedName = newName.trim()
        val cleanMobile = newMobile.filter { it.isDigit() }.takeLast(10)
        if (trimmedName.isEmpty() || cleanMobile.length < 10) {
            return Result.failure(IllegalArgumentException("नाम और 10 अंक का मोबाइल नंबर डालें"))
        }

        val existing = dao.getAccountByMobile(cleanMobile)
        if (existing != null && existing.id != id) {
            return Result.failure(IllegalArgumentException("यह मोबाइल नंबर किसी और खाते में है"))
        }

        val oldInfo = "${account.name} / ${account.mobile}"
        val updated = account.copy(name = trimmedName, mobile = cleanMobile, creditLimit = newLimit)
        dao.updateAccount(updated)

        dao.insertLog(
            LedgerLog(
                id = UUID.randomUUID().toString(),
                type = "edit",
                accId = updated.id,
                name = updated.name,
                mobile = updated.mobile,
                amount = 0.0,
                beforeBalance = updated.balance,
                afterBalance = updated.balance,
                note = "पहले: $oldInfo"
            )
        )
        return Result.success(updated)
    }

    suspend fun deleteLogEntry(logId: String): Boolean {
        val log = dao.getLogById(logId) ?: return false
        val updated = log.copy(deleted = true, deletedAt = System.currentTimeMillis())
        dao.updateLog(updated)

        val account = dao.getAccountById(log.accId)
        if (account != null) {
            dao.insertLog(
                LedgerLog(
                    id = UUID.randomUUID().toString(),
                    type = "logdel",
                    accId = account.id,
                    name = account.name,
                    mobile = account.mobile,
                    amount = 0.0,
                    beforeBalance = account.balance,
                    afterBalance = account.balance,
                    note = "हटाई गई एंट्री: ${log.type} ₹${log.amount.toInt()}"
                )
            )
        }
        return true
    }

    suspend fun undoLastTxn(accId: String): Result<String> {
        val logs = dao.getLogsForAccount(accId).filter { it.type == "add" || it.type == "sub" }
        if (logs.isEmpty()) return Result.failure(IllegalStateException("कोई लेन-देन नहीं मिला"))

        val last = logs.first()
        val account = dao.getAccountById(accId) ?: return Result.failure(IllegalStateException("खाता नहीं मिला"))

        val reverseDelta = if (last.type == "add") -last.amount else last.amount
        val newBal = Math.round((account.balance + reverseDelta) * 100.0) / 100.0

        dao.updateAccount(account.copy(balance = newBal))
        dao.updateLog(last.copy(deleted = true, deletedAt = System.currentTimeMillis()))

        dao.insertLog(
            LedgerLog(
                id = UUID.randomUUID().toString(),
                type = "logdel",
                accId = account.id,
                name = account.name,
                mobile = account.mobile,
                amount = 0.0,
                beforeBalance = account.balance,
                afterBalance = newBal,
                note = "अनडू की गई एंट्री: ₹${last.amount.toInt()}"
            )
        )
        return Result.success("आखिरी एंट्री हटाई और बैलेंस वापस कर दिया")
    }

    suspend fun clearHistory(accId: String): Int {
        val logs = dao.getLogsForAccount(accId)
        val count = logs.size
        dao.softDeleteAllLogsForAccount(accId, System.currentTimeMillis())

        val account = dao.getAccountById(accId)
        if (account != null) {
            dao.insertLog(
                LedgerLog(
                    id = UUID.randomUUID().toString(),
                    type = "logdel",
                    accId = account.id,
                    name = account.name,
                    mobile = account.mobile,
                    amount = 0.0,
                    beforeBalance = account.balance,
                    afterBalance = account.balance,
                    note = "$count एंट्री साफ़ की गई"
                )
            )
        }
        return count
    }

    suspend fun addStockItem(name: String, note: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.insertStockItem(
            StockItem(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                note = note.trim()
            )
        )
    }

    suspend fun deleteStockItem(id: String) {
        dao.deleteStockItem(id)
    }

    suspend fun clearAllStock() {
        dao.clearAllStock()
    }

    suspend fun getCustomerByMobile(mobile: String): Pair<CustomerAccount, List<LedgerLog>>? {
        val clean = mobile.filter { it.isDigit() }.takeLast(10)
        val account = dao.getAccountByMobile(clean) ?: return null
        val logs = dao.getLogsForAccount(account.id)
        return Pair(account, logs)
    }

    suspend fun seedInitialDataIfEmpty() {
        val count = dao.getActiveAccounts().size
        if (count == 0) {
            val s = getSettings()
            if (s.shopName == "मेरी दुकान") {
                saveSettings(s.copy(shopName = "गुप्ता किराना स्टोर", upiId = "9876543210@upi"))
            }
            createAccount("राहुल शर्मा", "9876543210", "सिस्टम", 5000.0).onSuccess { acc ->
                processTransaction(
                    TxnParams(
                        accountId = acc.id,
                        delta = 1250.0,
                        note = "किराना सामान",
                        source = "मैनुअल",
                        items = "5 किलो आटा, 2 लीटर तेल, चीनी",
                        dueDate = System.currentTimeMillis() + 5 * 86400000L
                    )
                )
            }
            createAccount("अमित वर्मा", "9812345678", "सिस्टम", 3000.0).onSuccess { acc ->
                processTransaction(
                    TxnParams(
                        accountId = acc.id,
                        delta = 450.0,
                        note = "दैनिक राशन",
                        source = "मैनुअल",
                        items = "साबुन, चायपत्ती",
                        dueDate = System.currentTimeMillis() - 2 * 86400000L
                    )
                )
            }
            addStockItem("फॉर्च्यून तेल", "2 पेटी")
            addStockItem("चीनी 50kg", "1 बोरी")
        }
    }
}
