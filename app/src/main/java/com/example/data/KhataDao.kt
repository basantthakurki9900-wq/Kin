package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {

    // --- Accounts ---
    @Query("SELECT * FROM customer_accounts WHERE deleted = 0 ORDER BY balance DESC, name ASC")
    fun getActiveAccountsFlow(): Flow<List<CustomerAccount>>

    @Query("SELECT * FROM customer_accounts WHERE deleted = 0")
    suspend fun getActiveAccounts(): List<CustomerAccount>

    @Query("SELECT * FROM customer_accounts WHERE deleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedAccountsFlow(): Flow<List<CustomerAccount>>

    @Query("SELECT * FROM customer_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): CustomerAccount?

    @Query("SELECT * FROM customer_accounts WHERE id = :id LIMIT 1")
    fun getAccountFlow(id: String): Flow<CustomerAccount?>

    @Query("SELECT * FROM customer_accounts WHERE mobile = :mobile AND deleted = 0 LIMIT 1")
    suspend fun getAccountByMobile(mobile: String): CustomerAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: CustomerAccount)

    @Update
    suspend fun updateAccount(account: CustomerAccount)

    @Query("DELETE FROM customer_accounts WHERE id = :id")
    suspend fun purgeAccount(id: String)

    // --- Logs ---
    @Query("SELECT * FROM ledger_logs WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<LedgerLog>>

    @Query("SELECT * FROM ledger_logs WHERE accId = :accId AND deleted = 0 ORDER BY timestamp DESC")
    fun getLogsForAccountFlow(accId: String): Flow<List<LedgerLog>>

    @Query("SELECT * FROM ledger_logs WHERE accId = :accId AND deleted = 0 ORDER BY timestamp DESC")
    suspend fun getLogsForAccount(accId: String): List<LedgerLog>

    @Query("SELECT * FROM ledger_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: String): LedgerLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LedgerLog)

    @Update
    suspend fun updateLog(log: LedgerLog)

    @Query("UPDATE ledger_logs SET deleted = 1, deletedAt = :deletedAt WHERE accId = :accId AND deleted = 0")
    suspend fun softDeleteAllLogsForAccount(accId: String, deletedAt: Long)

    // --- Stock Items ---
    @Query("SELECT * FROM stock_items ORDER BY timestamp DESC")
    fun getStockItemsFlow(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items ORDER BY timestamp DESC")
    suspend fun getStockItems(): List<StockItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItem)

    @Query("DELETE FROM stock_items WHERE id = :id")
    suspend fun deleteStockItem(id: String)

    @Query("DELETE FROM stock_items")
    suspend fun clearAllStock()

    // --- Settings ---
    @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<ShopSettings?>

    @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): ShopSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ShopSettings)
}
