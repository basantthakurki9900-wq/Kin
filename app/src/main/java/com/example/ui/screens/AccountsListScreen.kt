package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerAccount
import com.example.data.LedgerLog
import com.example.data.ShopSettings
import com.example.ui.theme.KhataGold
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import java.util.Locale

@Composable
fun AccountsListScreen(
    accounts: List<CustomerAccount>,
    allLogs: List<LedgerLog>,
    settings: ShopSettings,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAccountClick: (String) -> Unit,
    onAddNewAccount: () -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenCustomerPortal: () -> Unit,
    onBulkRemind: () -> Unit
) {
    val totalOwed = remember(accounts) {
        accounts.filter { it.balance > 0 }.sumOf { it.balance }
    }
    val totalAdvance = remember(accounts) {
        accounts.filter { it.balance < 0 }.sumOf { Math.abs(it.balance) }
    }

    // Check overdue accounts
    val currentTime = System.currentTimeMillis()
    val overdueAccountIds = remember(allLogs, accounts) {
        allLogs.filter { it.dueDate > 0 && it.dueDate < currentTime && it.type == "add" }
            .map { it.accId }
            .toSet()
    }
    val overdueCount = accounts.count { it.balance > 0 && overdueAccountIds.contains(it.id) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header stats
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KhataPrimary)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📒 " + settings.shopName.ifBlank { "मेरी दुकान" },
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${accounts.size} सक्रिय खाते",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }

                        // Shortcut to Customer Portal
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .clickable { onOpenCustomerPortal() }
                                .testTag("open_customer_portal_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ग्राहक पोर्टल",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Outstanding & Advance Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "कुल उधार बाकी",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹" + String.format(Locale.ENGLISH, "%,.0f", totalOwed),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KhataRed
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "एडवांस जमा",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹" + String.format(Locale.ENGLISH, "%,.0f", totalAdvance),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KhataPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Overdue banner if any
            if (overdueCount > 0) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "⏰ $overdueCount खातों में वापसी तारीख बीत गई है।",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onBulkRemind,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("bulk_remind_alert_button")
                            ) {
                                Text("याद दिलाएँ", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accounts_search_bar"),
                        placeholder = { Text("🔍 नाम या मोबाइल से खोजें...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "साफ़ करें")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Customer Accounts list
            if (accounts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "कोई खाता नहीं मिला" else "अभी कोई खाता नहीं है। नीचे '+ नया खाता' दबाएँ।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(accounts, key = { it.id }) { acc ->
                    val isOverdue = overdueAccountIds.contains(acc.id) && acc.balance > 0
                    val isLimitBreached = acc.creditLimit > 0 && acc.balance > acc.creditLimit

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clickable { onAccountClick(acc.id) }
                            .testTag("customer_card_${acc.mobile}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverdue) Color(0xFFFDECEB) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (acc.balance > 0) Color(0xFFFDECEB) else Color(0xFFE6F5EE),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (acc.balance > 0) KhataRed else KhataPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isOverdue) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "⏰", fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📱 ${acc.mobile}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isLimitBreached) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "⚠ सीमा पार (सीमा: ₹${acc.creditLimit.toInt()})",
                                            fontSize = 11.sp,
                                            color = KhataRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Balance Column
                            Column(horizontalAlignment = Alignment.End) {
                                when {
                                    acc.balance > 0 -> {
                                        Text(
                                            text = "₹" + String.format(Locale.ENGLISH, "%,.0f", acc.balance),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KhataRed
                                        )
                                        Text(
                                            text = "उधार बाकी",
                                            fontSize = 11.sp,
                                            color = KhataRed,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    acc.balance < 0 -> {
                                        Text(
                                            text = "₹" + String.format(Locale.ENGLISH, "%,.0f", Math.abs(acc.balance)),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KhataPrimary
                                        )
                                        Text(
                                            text = "एडवांस जमा",
                                            fontSize = 11.sp,
                                            color = KhataPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "₹0",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "बराबर",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Buttons on Home Screen
        // Bottom Right: Prominent Voice Mic button (per user: "इसका माइक होम पेज पर भी आए मोबाइल के ताकि उधर से कोई भी कमान दे सकते हैं")
        FloatingActionButton(
            onClick = onOpenVoiceAssistant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(62.dp)
                .testTag("home_voice_mic_fab"),
            shape = CircleShape,
            containerColor = KhataRed,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "वॉयस असिस्टेंट खोलें",
                modifier = Modifier.size(32.dp)
            )
        }

        // Bottom Left: + नया खाता FAB
        ExtendedFloatingActionButton(
            onClick = onAddNewAccount,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .testTag("add_new_account_fab"),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("+ नया खाता", fontWeight = FontWeight.Bold) },
            containerColor = KhataPrimary,
            contentColor = Color.White
        )
    }
}
