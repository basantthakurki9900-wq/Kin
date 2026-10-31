package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerAccount
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import java.util.Calendar

@Composable
fun AddAccountDialog(
    defaultCreditLimit: Double,
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, limit: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf(if (defaultCreditLimit > 0) defaultCreditLimit.toInt().toString() else "0") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("नया ग्राहक खाता", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("ग्राहक का नाम") },
                    placeholder = { Text("जैसे: राहुल शर्मा") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_account_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                            mobile = it
                            errorMsg = null
                        }
                    },
                    label = { Text("मोबाइल नंबर (10 अंक)") },
                    placeholder = { Text("जैसे: 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_account_phone_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("उधार सीमा (0 = कोई सीमा नहीं)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_account_limit_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg!!, color = KhataRed, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMsg = "नाम डालना आवश्यक है"
                        return@Button
                    }
                    if (mobile.length != 10) {
                        errorMsg = "मोबाइल नंबर 10 अंक का होना चाहिए"
                        return@Button
                    }
                    val lim = limitText.toDoubleOrNull() ?: 0.0
                    onConfirm(name.trim(), mobile, lim)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary),
                modifier = Modifier.testTag("new_account_save_button")
            ) {
                Text("खाता बनाएँ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द")
            }
        }
    )
}

@Composable
fun TransactionDialog(
    account: CustomerAccount,
    isCredit: Boolean, // true = Udhar (+), false = Jama (-)
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, items: String, dueDate: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var items by remember { mutableStateOf("") }
    var dueDaysText by remember { mutableStateOf(if (isCredit) "7" else "0") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val title = if (isCredit) "➕ ${account.name} – उधार चढ़ाओ" else "➖ ${account.name} – पैसे घटाओ / जमा"
    val themeColor = if (isCredit) KhataRed else KhataPrimary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = themeColor)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("रकम ₹") },
                    placeholder = { Text("जैसे: 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_amount_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("विवरण / नोट (वैकल्पिक)") },
                    placeholder = { Text(if (isCredit) "जैसे: सामान ले गया" else "जैसे: नकद मिला / ऑनलाइन") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_note_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (isCredit) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = items,
                        onValueChange = { items = it },
                        label = { Text("📦 सामान क्या दिया? (वैकल्पिक)") },
                        placeholder = { Text("जैसे: 2 किलो चीनी, 1 लीटर तेल") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("txn_items_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = dueDaysText,
                        onValueChange = { dueDaysText = it },
                        label = { Text("📅 वापसी कितने दिनों में? (वैकल्पिक)") },
                        placeholder = { Text("दिनों की संख्या, जैसे: 7") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("txn_due_days_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg!!, color = KhataRed, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMsg = "कृपया सही रकम दर्ज करें"
                        return@Button
                    }
                    val days = dueDaysText.toIntOrNull() ?: 0
                    val dueTimestamp = if (days > 0 && isCredit) {
                        System.currentTimeMillis() + days * 86400000L
                    } else 0L

                    onConfirm(amt, note.trim(), items.trim(), dueTimestamp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                modifier = Modifier.testTag("txn_save_button")
            ) {
                Text(if (isCredit) "उधार चढ़ाएँ" else "जमा करें")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द")
            }
        }
    )
}

@Composable
fun EditAccountDialog(
    account: CustomerAccount,
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, limit: Double) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var mobile by remember { mutableStateOf(account.mobile) }
    var limitText by remember { mutableStateOf(account.creditLimit.toInt().toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("खाता विवरण बदलें", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("ग्राहक का नाम") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                            mobile = it
                            errorMsg = null
                        }
                    },
                    label = { Text("मोबाइल नंबर (10 अंक)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("उधार सीमा (0 = कोई सीमा नहीं)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg!!, color = KhataRed, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty() || mobile.length != 10) {
                        errorMsg = "नाम और 10 अंक का मोबाइल नंबर दर्ज करें"
                        return@Button
                    }
                    val lim = limitText.toDoubleOrNull() ?: 0.0
                    onConfirm(name.trim(), mobile, lim)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
            ) {
                Text("सुरक्षित करें")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द")
            }
        }
    )
}
