package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerAccount
import com.example.data.LedgerLog
import com.example.data.ShopSettings
import com.example.data.StockItem
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StockScreen(
    stockItems: List<StockItem>,
    settings: ShopSettings,
    onAddItem: (name: String, note: String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("hi", "IN")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = KhataPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "📦 खत्म स्टॉक सूची",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "दुकान में जो सामान खत्म हो गया है उसकी डायरी",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add new stock item card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("खत्म हुआ सामान (जैसे: चीनी, तेल, आटा...)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_item_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("मात्रा / नोट (वैकल्पिक)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stock_item_note_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onAddItem(name.trim(), note.trim())
                                    name = ""
                                    note = ""
                                }
                            },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("stock_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("जोड़ें")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (stockItems.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "कोई सामान खत्म नहीं है 🎉",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(stockItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (item.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.note,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateFormat.format(Date(item.timestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = { onDeleteItem(item.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45)),
                            modifier = Modifier.testTag("stock_done_button_${item.id}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✔ आ गया", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val stockText = buildString {
                            append("📦 खत्म स्टॉक सूची – ${settings.shopName}\n\n")
                            stockItems.forEachIndexed { i, s ->
                                append("${i + 1}. ${s.name}${if (s.note.isNotBlank()) " (${s.note})" else ""}\n")
                            }
                        }
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, stockText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(intent, "स्टॉक सूची भेजें"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("stock_share_whatsapp_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2420))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💬 पूरी सूची WhatsApp पर भेजें")
                }
            }
        }
    }
}

@Composable
fun DeletedAccountsScreen(
    deletedAccounts: List<CustomerAccount>,
    onRestore: (String) -> Unit,
    onPurge: (String) -> Unit
) {
    var accountToPurge by remember { mutableStateOf<CustomerAccount?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "🗑️ हटाए गए खाते",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "जो खाते हटाए गए हैं वे यहाँ सुरक्षित हैं। आप उन्हें कभी भी वापस ला सकते हैं।",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (deletedAccounts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "कोई हटाया हुआ खाता नहीं है",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(deletedAccounts, key = { it.id }) { acc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = acc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = "📱 ${acc.mobile}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "बैलेंस: ₹${acc.balance.toInt()}",
                                fontWeight = FontWeight.Bold,
                                color = if (acc.balance > 0) KhataRed else KhataPrimary,
                                fontSize = 15.sp
                            )
                        }

                        if (acc.deletedAt != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "हटाने की तारीख: ${dateFormat.format(Date(acc.deletedAt))}",
                                fontSize = 11.sp,
                                color = KhataRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onRestore(acc.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45))
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("↩ वापस लाएँ", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { accountToPurge = acc },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDECEB), contentColor = KhataRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("❌ पूरा हटाएँ", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (accountToPurge != null) {
        val acc = accountToPurge!!
        AlertDialog(
            onDismissRequest = { accountToPurge = null },
            title = { Text("हमेशा के लिए हटाएँ?") },
            text = { Text("${acc.name} का खाता स्थायी रूप से हटा दिया जाएगा।") },
            confirmButton = {
                Button(
                    onClick = {
                        onPurge(acc.id)
                        accountToPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhataRed)
                ) {
                    Text("हाँ, पूरा हटाओ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToPurge = null }) {
                    Text("नहीं")
                }
            }
        )
    }
}

@Composable
fun HistoryScreen(
    allLogs: List<LedgerLog>
) {
    var selectedFilter by remember { mutableStateOf("all") } // "all", "add", "sub", "acc"
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN")) }

    val filteredLogs = remember(allLogs, selectedFilter) {
        allLogs.filter { log ->
            when (selectedFilter) {
                "add" -> log.type == "add"
                "sub" -> log.type == "sub"
                "acc" -> listOf("create", "edit", "delete", "restore", "purge", "logdel").contains(log.type)
                else -> true
            }
        }
    }

    val totalAdd = remember(allLogs) { allLogs.filter { it.type == "add" }.sumOf { it.amount } }
    val totalSub = remember(allLogs) { allLogs.filter { it.type == "sub" }.sumOf { it.amount } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "🕘 सारा हिसाब-किताब लेजर",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Totals card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "कुल उधार चढ़ा", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "+₹${totalAdd.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = KhataRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "कुल जमा हुआ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "-₹${totalSub.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = KhataPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("सब") }
                )
                FilterChip(
                    selected = selectedFilter == "add",
                    onClick = { selectedFilter = "add" },
                    label = { Text("उधार (+)") }
                )
                FilterChip(
                    selected = selectedFilter == "sub",
                    onClick = { selectedFilter = "sub" },
                    label = { Text("जमा (−)") }
                )
                FilterChip(
                    selected = selectedFilter == "acc",
                    onClick = { selectedFilter = "acc" },
                    label = { Text("खाता गतिविधियाँ") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (filteredLogs.isEmpty()) {
            item {
                Text(
                    text = "कोई लेन-देन नहीं मिला",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                val isAdd = log.type == "add"
                val isSub = log.type == "sub"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${log.name} · ${log.mobile}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            if (isAdd || isSub) {
                                Text(
                                    text = (if (isAdd) "+₹" else "-₹") + String.format(Locale.ENGLISH, "%,.0f", log.amount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdd) KhataRed else KhataPrimary
                                )
                            }
                        }

                        if (log.items.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "📦 सामान: ${log.items}", fontSize = 12.sp, color = Color(0xFF92400E))
                        }

                        if (log.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "📝 ${log.note}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: ShopSettings,
    onSaveSettings: (ShopSettings) -> Unit
) {
    var shopName by remember { mutableStateOf(settings.shopName) }
    var upiId by remember { mutableStateOf(settings.upiId) }
    var webPortalLink by remember { mutableStateOf(settings.webPortalLink) }
    var depositLimitText by remember { mutableStateOf(settings.depositLimit.toInt().toString()) }
    var defaultCreditLimitText by remember { mutableStateOf(settings.defaultCreditLimit.toInt().toString()) }
    var pin by remember { mutableStateOf(settings.pin) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "⚙️ ऐप सेटिंग्स",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("दुकान का नाम") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.trim() },
                        label = { Text("आपकी UPI ID (QR व पेमेंट लिंक के लिए)") },
                        placeholder = { Text("जैसे: 9876543210@upi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = webPortalLink,
                        onValueChange = { webPortalLink = it.trim() },
                        label = { Text("ग्राहक पोर्टल वेब लिंक") },
                        placeholder = { Text("जैसे: https://khata.page/portal") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = depositLimitText,
                        onValueChange = { depositLimitText = it },
                        label = { Text("जमा लिमिट (अधिकतम एडवांस) – 0 = कोई सीमा नहीं") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultCreditLimitText,
                        onValueChange = { defaultCreditLimitText = it },
                        label = { Text("डिफ़ॉल्ट उधार सीमा (नए खातों के लिए, 0 = कोई सीमा नहीं)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("ऐप PIN लॉक (खाली = लॉक बंद)") },
                        placeholder = { Text("4 से 6 अंकों का पिन") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val dep = depositLimitText.toDoubleOrNull() ?: 0.0
                            val dcl = defaultCreditLimitText.toDoubleOrNull() ?: 0.0
                            onSaveSettings(
                                settings.copy(
                                    shopName = shopName.trim().ifBlank { "मेरी दुकान" },
                                    upiId = upiId.trim(),
                                    webPortalLink = webPortalLink.trim(),
                                    depositLimit = dep,
                                    defaultCreditLimit = dcl,
                                    pin = pin.trim()
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
                    ) {
                        Text("सेव करें", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "💡 सारा डेटा आपके फ़ोन में सुरक्षित रूम डेटाबेस में रहता है।",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
