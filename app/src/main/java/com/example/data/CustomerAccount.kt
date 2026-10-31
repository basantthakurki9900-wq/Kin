package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_accounts")
data class CustomerAccount(
    @PrimaryKey val id: String,
    val name: String,
    val mobile: String,
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val created: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val deletedAt: Long? = null
)
