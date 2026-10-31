package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.ShopSettings
import com.example.ui.CustomerPortalState
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerPortalScreen(
    settings: ShopSettings,
    portalState: CustomerPortalState,
    onSearchMobile: (String) -> Unit
) {
    val context = LocalContext.current
    var inputMobile by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Portal Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFE6F5EE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = KhataPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = settings.shopName.ifBlank { "मेरी दुकान" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "🔍 ऑनलाइन ग्राहक हिसाब पोर्टल",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = KhataPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "नमस्ते! अपना 10 अंकों का मोबाइल नंबर डालें और तुरंत अपना बकाया / एडवांस राशि और पूरा लेन-देन देखें।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mobile search box
                    OutlinedTextField(
                        value = inputMobile,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                inputMobile = it
                            }
                        },
                        label = { Text("मोबाइल नंबर (10 अंक)") },
                        placeholder = { Text("जैसे: 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("portal_mobile_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (inputMobile.length == 10) {
                                onSearchMobile(inputMobile)
                            }
                        },
                        enabled = inputMobile.length == 10 && !portalState.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("portal_search_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
                    ) {
                        if (portalState.loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("हिसाब देखें", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // Error message
        if (portalState.error != null) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFDECEB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = portalState.error,
                        color = KhataRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Account Details when found
        if (portalState.account != null) {
            val acc = portalState.account

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = acc.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "सत्यापित",
                                tint = KhataPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "📱 ${acc.mobile}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Balance display
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                acc.balance > 0 -> Color(0xFFFDECEB)
                                acc.balance < 0 -> Color(0xFFE6F5EE)
                                else -> Color(0xFFF4F7F5)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when {
                                        acc.balance > 0 -> "आपका कुल बकाया"
                                        acc.balance < 0 -> "आपका कुल एडवांस जमा"
                                        else -> "आपका हिसाब"
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "₹" + String.format(Locale.ENGLISH, "%,.0f", Math.abs(acc.balance)),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        acc.balance > 0 -> KhataRed
                                        acc.balance < 0 -> KhataPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                Text(
                                    text = when {
                                        acc.balance > 0 -> "कृपया समय पर भुगतान करें"
                                        acc.balance < 0 -> "दुकानदार की तरफ जमा"
                                        else -> "हिसाब बिल्कुल बराबर है"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // UPI Payment Button if shop has UPI ID and user owes money
                        if (settings.upiId.isNotBlank() && acc.balance > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val upiUri = Uri.parse("upi://pay?pa=${settings.upiId}&pn=${Uri.encode(settings.shopName)}&am=${acc.balance.toInt()}&cu=INR&tn=KhataPayment")
                                    val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                    context.startActivity(Intent.createChooser(intent, "UPI ऐप चुनें"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("portal_upi_pay_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B7A5C))
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("💳 UPI से ₹${acc.balance.toInt()} पेमेंट करें", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Customer logs section
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "आपकी लेन-देन हिस्ट्री",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (portalState.logs.isEmpty()) {
                item {
                    Text(
                        text = "कोई लेन-देन उपलब्ध नहीं है",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(portalState.logs) { log ->
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
                                    text = if (isAdd) "उधार लिया (+)" else if (isSub) "पैसे चुकाए (−)" else log.type,
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📦 सामान: ${log.items}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                            }

                            if (log.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "📝 ${log.note}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
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
}
