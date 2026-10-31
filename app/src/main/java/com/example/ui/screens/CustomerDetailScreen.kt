package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerAccount
import com.example.data.LedgerLog
import com.example.data.ShopSettings
import com.example.ui.components.QrCodeDialog
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import com.example.voice.HindiVoiceParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerDetailScreen(
    account: CustomerAccount,
    logs: List<LedgerLog>,
    settings: ShopSettings,
    onBack: () -> Unit,
    onAddCredit: () -> Unit,
    onAddPayment: () -> Unit,
    onEditAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDeleteLogEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    var showQrDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<LedgerLog?>(null) }

    val dateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN")) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Surface(
            color = KhataPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("customer_detail_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "वापस", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = account.name,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📱 ${account.mobile}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Big Hero Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                account.balance > 0 -> "उधार बाकी"
                                account.balance < 0 -> "एडवांस / आपको देना"
                                else -> "हिसाब बराबर"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "₹" + String.format(Locale.ENGLISH, "%,.0f", Math.abs(account.balance)),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                account.balance > 0 -> KhataRed
                                account.balance < 0 -> KhataPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        if (account.creditLimit > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isOver = account.balance > account.creditLimit
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isOver) Color(0xFFFDECEB) else Color(0xFFE6F5EE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isOver)
                                        "⚠ उधार सीमा पार! (सीमा: ₹${account.creditLimit.toInt()})"
                                    else
                                        "उधार सीमा: ₹${account.creditLimit.toInt()} · बचा: ₹${Math.max(0.0, account.creditLimit - account.balance).toInt()}",
                                    color = if (isOver) KhataRed else KhataPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Add/Subtract Buttons
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddCredit,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("action_add_credit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KhataRed)
                    ) {
                        Text("➕ उधार चढ़ाओ (+)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onAddPayment,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("action_add_payment_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
                    ) {
                        Text("➖ पैसे जमा (−)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Secondary Action Buttons Grid
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onShareWhatsApp,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45))
                        ) {
                            Text("💬 WhatsApp भेजें", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45))
                        ) {
                            Text("▦ QR कोड", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Share printable statement text
                                val statementText = buildString {
                                    append("📄 हिसाब स्टेटमेंट – ${settings.shopName}\n")
                                    append("ग्राहक: ${account.name} (📱 ${account.mobile})\n")
                                    append("मौजूदा बैलेंस: ₹${account.balance.toInt()} (${if (account.balance > 0) "उधार बाकी" else "एडवांस"})\n\n")
                                    append("लेन-देन का इतिहास:\n")
                                    logs.forEach { l ->
                                        val dt = dateTimeFormat.format(Date(l.timestamp))
                                        val sign = if (l.type == "add") "+" else if (l.type == "sub") "-" else ""
                                        append("• $dt: $sign₹${l.amount.toInt()} (${l.type}) ${l.items.ifBlank { "" }} ${l.note.ifBlank { "" }}\n")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, statementText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "स्टेटमेंट साझा करें"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45))
                        ) {
                            Text("🖨️ स्टेटमेंट साझा", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onEditAccount,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45))
                        ) {
                            Text("✏️ खाता बदलें", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showClearHistoryConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6EFEB), contentColor = Color(0xFF075C45))
                        ) {
                            Text("🧹 हिस्ट्री साफ़", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDECEB), contentColor = KhataRed)
                        ) {
                            Text("🗑️ खाता हटाएँ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Transaction History Section
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "लेन-देन इतिहास",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "अभी तक कोई इतिहास नहीं है",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    val isAdd = log.type == "add"
                    val isSub = log.type == "sub"
                    val isOverdue = log.dueDate > 0 && log.dueDate < System.currentTimeMillis() && isAdd

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (log.type) {
                                            "add" -> "उधार (+)"
                                            "sub" -> "जमा (−)"
                                            "create" -> "खाता बना"
                                            "edit" -> "खाता बदला"
                                            "delete" -> "खाता हटाया"
                                            "restore" -> "खाता वापस"
                                            else -> log.type
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    if (log.source.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFFF3CD)
                                        ) {
                                            Text(
                                                text = log.source,
                                                fontSize = 10.sp,
                                                color = Color(0xFF8A6D00),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isAdd || isSub) {
                                        Text(
                                            text = (if (isAdd) "+₹" else "-₹") + String.format(Locale.ENGLISH, "%,.0f", log.amount),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdd) KhataRed else KhataPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { logToDelete = log },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "एंट्री हटाएँ",
                                            tint = KhataRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (isAdd || isSub) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "पहले ₹${log.beforeBalance.toInt()} → अब ₹${log.afterBalance.toInt()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (log.items.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF8E1),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "📦 सामान: ${log.items}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (log.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "📝 ${log.note}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (log.dueDate > 0 && isAdd) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isOverdue) Color(0xFFFDECEB) else Color(0xFFE6F5EE)
                                ) {
                                    Text(
                                        text = "📅 वापसी: ${dateTimeFormat.format(Date(log.dueDate)).split(",")[0]}" +
                                                (if (isOverdue) " (तारीख बीत गई!)" else ""),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOverdue) KhataRed else KhataPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateTimeFormat.format(Date(log.timestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    // QR Dialog
    if (showQrDialog) {
        QrCodeDialog(
            account = account,
            settings = settings,
            onDismiss = { showQrDialog = false }
        )
    }

    // Delete Account Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("${account.name} का खाता हटाएँ?") },
            text = { Text("यह 'हटाए गए' खाते वाले टैब में चला जाएगा, जहाँ से आप इसे कभी भी वापस ला सकते हैं।") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhataRed)
                ) {
                    Text("हाँ, हटाओ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("नहीं")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("पूरी हिस्ट्री साफ़ करें?") },
            text = { Text("खाते का बैलेंस ₹${account.balance.toInt()} बिल्कुल वैसे ही रहेगा, सिर्फ़ इतिहास की एंट्री हटेंगी।") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryConfirm = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhataRed)
                ) {
                    Text("हाँ, साफ़ करो")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("नहीं")
                }
            }
        )
    }

    // Delete Single Log Entry Dialog
    if (logToDelete != null) {
        val entry = logToDelete!!
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("यह एंट्री हटाएँ?") },
            text = { Text("खाते का बैलेंस नहीं बदलेगा, सिर्फ इतिहास से यह रिकॉर्ड हटेगा।") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLogEntry(entry.id)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhataRed)
                ) {
                    Text("हाँ, हटाओ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { logToDelete = null }) {
                    Text("नहीं")
                }
            }
        )
    }
}
