package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_logs")
data class LedgerLog(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "add", "sub", "create", "edit", "delete", "restore", "purge", "logdel"
    val accId: String,
    val name: String,
    val mobile: String,
    val amount: Double = 0.0,
    val beforeBalance: Double = 0.0,
    val afterBalance: Double = 0.0,
    val note: String = "",
    val source: String = "", // "आवाज़", "मैनुअल", etc.
    val items: String = "",
    val dueDate: Long = 0L,
    val deleted: Boolean = false,
    val deletedAt: Long? = null
)
