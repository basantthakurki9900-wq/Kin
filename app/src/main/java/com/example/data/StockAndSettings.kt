package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey val id: String,
    val name: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shop_settings")
data class ShopSettings(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "मेरी दुकान",
    val upiId: String = "",
    val webPortalLink: String = "",
    val pin: String = "",
    val depositLimit: Double = 1000.0,
    val defaultCreditLimit: Double = 0.0
)
