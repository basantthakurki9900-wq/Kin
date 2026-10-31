package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerAccount
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import java.util.Locale

@Composable
fun BulkRemindDialog(
    debtors: List<CustomerAccount>,
    onSendSingleReminder: (CustomerAccount) -> Unit,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${debtors.size} ग्राहकों को याद दिलाएँ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "प्रत्येक ग्राहक को WhatsApp पर बकाया राशि और पोर्टल लिंक का संदेश भेजें:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    itemsIndexed(debtors) { idx, acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "${idx + 1}. ${acc.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "📱 ${acc.mobile}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "₹" + String.format(Locale.ENGLISH, "%,.0f", acc.balance),
                                    fontWeight = FontWeight.Bold,
                                    color = KhataRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (debtors.isNotEmpty()) {
                        val acc = debtors[currentIndex % debtors.size]
                        onSendSingleReminder(acc)
                        currentIndex++
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary),
                modifier = Modifier.testTag("bulk_remind_send_next_button")
            ) {
                val nextName = if (debtors.isNotEmpty()) debtors[currentIndex % debtors.size].name else ""
                Text("WhatsApp भेजें ($nextName)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("बंद करें")
            }
        }
    )
}
