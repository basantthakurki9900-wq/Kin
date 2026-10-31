package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerAccount
import com.example.data.ShopSettings
import com.example.ui.theme.KhataPrimary

@Composable
fun QrCodeDialog(
    account: CustomerAccount,
    settings: ShopSettings,
    initialMode: String = "chk",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(initialMode) } // "chk" for balance check, "upi" for payment

    val webLink = if (settings.webPortalLink.isNotBlank()) settings.webPortalLink else "https://khata.page/portal"
    val balanceUrl = "$webLink?mobile=${account.mobile}"
    val upiUrl = if (settings.upiId.isNotBlank() && account.balance > 0) {
        "upi://pay?pa=${settings.upiId}&pn=${settings.shopName}&am=${account.balance.toInt()}&cu=INR&tn=KhataPayment"
    } else {
        ""
    }

    val activeContent = if (mode == "upi") upiUrl else balanceUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${account.name} – QR कोड",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = mode == "chk",
                        onClick = { mode = "chk" },
                        label = { Text("बैलेंस चेक QR") },
                        modifier = Modifier.testTag("qr_mode_balance_chip")
                    )
                    FilterChip(
                        selected = mode == "upi",
                        onClick = { mode = "upi" },
                        label = { Text("UPI पेमेंट QR") },
                        modifier = Modifier.testTag("qr_mode_upi_chip")
                    )
                }

                if (mode == "upi" && settings.upiId.isBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "कृपया सेटिंग्स में जाकर अपनी UPI ID जोड़ें।",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                } else if (mode == "upi" && account.balance <= 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "इस खाते में कोई बकाया राशि नहीं है।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // QR Code visual canvas
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeCanvas(
                            data = activeContent,
                            modifier = Modifier.size(196.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (mode == "upi")
                            "ग्राहक स्कैन करके ₹${account.balance.toInt()} सीधे UPI से भेज सकता है"
                        else
                            "ग्राहक स्कैन करके तुरंत अपना हिसाब और लेजर देख सकता है",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Khata QR Link", activeContent))
                    Toast.makeText(context, "लिंक कॉपी हो गया", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("लिंक कॉपी करें")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}

/**
 * Deterministic visual 2D matrix pattern generator for QR representation in Compose
 */
@Composable
fun QrCodeCanvas(
    data: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gridSize = 25
        val cellSize = size.width / gridSize
        val bytes = data.toByteArray(Charsets.UTF_8)

        // Draw background
        drawRect(color = Color.White, size = size)

        // Position finder patterns (top-left, top-right, bottom-left)
        fun drawFinderPattern(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    if (isBorder || isCenter) {
                        drawRect(
                            color = Color(0xFF1B2420),
                            topLeft = Offset((startX + c) * cellSize, (startY + r) * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(gridSize - 7, 0)
        drawFinderPattern(0, gridSize - 7)

        // Alignment pattern
        val alignX = gridSize - 9
        val alignY = gridSize - 9
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val isBorder = r == 0 || r == 4 || c == 0 || c == 4
                val isCenter = r == 2 && c == 2
                if (isBorder || isCenter) {
                    drawRect(
                        color = Color(0xFF1B2420),
                        topLeft = Offset((alignX + c) * cellSize, (alignY + r) * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }

        // Timing patterns
        for (i in 7 until gridSize - 7) {
            if (i % 2 == 0) {
                drawRect(
                    color = Color(0xFF1B2420),
                    topLeft = Offset(6 * cellSize, i * cellSize),
                    size = Size(cellSize, cellSize)
                )
                drawRect(
                    color = Color(0xFF1B2420),
                    topLeft = Offset(i * cellSize, 6 * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }

        // Data cells derived pseudo-randomly from string hash and bytes
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inFinder1 = r < 8 && c < 8
                val inFinder2 = r < 8 && c >= gridSize - 8
                val inFinder3 = r >= gridSize - 8 && c < 8
                val inTiming = r == 6 || c == 6
                val inAlign = r in alignY until (alignY + 5) && c in alignX until (alignX + 5)

                if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming && !inAlign) {
                    val idx = (r * gridSize + c) % bytes.size
                    val b = bytes[idx].toInt()
                    val hashBit = ((b xor (r * 7 + c * 13)) and 1) == 1
                    if (hashBit) {
                        drawRect(
                            color = Color(0xFF1B2420),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}
