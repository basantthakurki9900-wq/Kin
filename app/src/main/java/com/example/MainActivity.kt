package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomerAccount
import com.example.ui.KhataViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.BulkRemindDialog
import com.example.ui.components.EditAccountDialog
import com.example.ui.components.PinLockScreen
import com.example.ui.components.TransactionDialog
import com.example.ui.components.VoiceAssistantSheet
import com.example.ui.screens.AccountsListScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerPortalScreen
import com.example.ui.screens.DeletedAccountsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataRed
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: KhataViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val activeAccounts by viewModel.filteredActiveAccounts.collectAsStateWithLifecycle()
                val rawActiveAccounts by viewModel.rawActiveAccounts.collectAsStateWithLifecycle()
                val deletedAccounts by viewModel.deletedAccounts.collectAsStateWithLifecycle()
                val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
                val stockItems by viewModel.stockItems.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
                val selectedAccountLogs by viewModel.selectedAccountLogs.collectAsStateWithLifecycle()
                val customerPortalState by viewModel.customerPortalState.collectAsStateWithLifecycle()
                val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
                val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

                // Bottom sheet for Voice Assistant
                var showVoiceSheet by remember { mutableStateOf(false) }
                val voiceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                // Dialog states
                var showAddAccountDialog by remember { mutableStateOf(false) }
                var showTxnDialog by remember { mutableStateOf(false) }
                var txnIsCredit by remember { mutableStateOf(true) }
                var showEditAccountDialog by remember { mutableStateOf(false) }
                var showBulkRemindDialog by remember { mutableStateOf(false) }
                var depositLimitWarning by remember { mutableStateOf<Triple<String, Double, Double>?>(null) } // (accId, newAdv, limit)

                // Permission launcher for Voice (RECORD_AUDIO)
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.startListeningVoice()
                        showVoiceSheet = true
                    } else {
                        Toast.makeText(context, "माइक की अनुमति दें या टाइप करके कमान दें", Toast.LENGTH_SHORT).show()
                        showVoiceSheet = true
                    }
                }

                // Handle ViewModel events (Toast, WhatsApp, UPI)
                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is UiEvent.ShareWhatsApp -> {
                                try {
                                    val url = "https://api.whatsapp.com/send?phone=91${event.phone}&text=${Uri.encode(event.message)}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, event.message)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "WhatsApp चुनें"))
                                }
                            }
                            is UiEvent.OpenUpi -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.uriString))
                                    context.startActivity(Intent.createChooser(intent, "UPI ऐप चुनें"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "कोई UPI ऐप नहीं मिला", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                // Check PIN Lock
                val isPinRequired = settings.pin.isNotBlank() && !isUnlocked
                if (isPinRequired) {
                    PinLockScreen(
                        shopName = settings.shopName,
                        onUnlock = { pin -> viewModel.unlockApp(pin) }
                    )
                    return@MyApplicationTheme
                }

                // Account Detail Screen when an account is selected
                if (selectedAccount != null) {
                    val acc = selectedAccount!!
                    CustomerDetailScreen(
                        account = acc,
                        logs = selectedAccountLogs,
                        settings = settings,
                        onBack = { viewModel.closeAccount() },
                        onAddCredit = {
                            txnIsCredit = true
                            showTxnDialog = true
                        },
                        onAddPayment = {
                            txnIsCredit = false
                            showTxnDialog = true
                        },
                        onEditAccount = { showEditAccountDialog = true },
                        onDeleteAccount = { viewModel.deleteAccount(acc.id) },
                        onDeleteLogEntry = { logId -> viewModel.deleteLogEntry(logId) },
                        onClearHistory = { viewModel.clearCustomerHistory(acc.id) },
                        onShareWhatsApp = { viewModel.shareCustomerOnWhatsApp(acc.id) }
                    )
                } else {
                    // Main scaffold with bottom navigation
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = KhataPrimary
                            ) {
                                val navItems = listOf(
                                    Triple(0, "खाते", Icons.Default.People),
                                    Triple(1, "स्टॉक", Icons.Default.Inventory2),
                                    Triple(2, "हटाए गए", Icons.Default.Delete),
                                    Triple(3, "इतिहास", Icons.Default.History),
                                    Triple(4, "सेटिंग", Icons.Default.Settings)
                                )

                                navItems.forEach { (tabIndex, label, icon) ->
                                    NavigationBarItem(
                                        selected = currentTab == tabIndex,
                                        onClick = { viewModel.selectTab(tabIndex) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = KhataPrimary,
                                            selectedTextColor = KhataPrimary,
                                            indicatorColor = Color(0xFFE6F5EE)
                                        ),
                                        modifier = Modifier.testTag("nav_item_$tabIndex")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                0 -> {
                                    AccountsListScreen(
                                        accounts = activeAccounts,
                                        allLogs = allLogs,
                                        settings = settings,
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.searchQuery.value = it },
                                        onAccountClick = { id -> viewModel.openAccount(id) },
                                        onAddNewAccount = { showAddAccountDialog = true },
                                        onOpenVoiceAssistant = {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPermission) {
                                                viewModel.startListeningVoice()
                                                showVoiceSheet = true
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        onOpenCustomerPortal = { viewModel.selectTab(5) },
                                        onBulkRemind = { showBulkRemindDialog = true }
                                    )
                                }
                                1 -> {
                                    StockScreen(
                                        stockItems = stockItems,
                                        settings = settings,
                                        onAddItem = { name, note -> viewModel.addStockItem(name, note) },
                                        onDeleteItem = { id -> viewModel.deleteStockItem(id) }
                                    )
                                }
                                2 -> {
                                    DeletedAccountsScreen(
                                        deletedAccounts = deletedAccounts,
                                        onRestore = { id -> viewModel.restoreAccount(id) },
                                        onPurge = { id -> viewModel.purgeAccount(id) }
                                    )
                                }
                                3 -> {
                                    HistoryScreen(allLogs = allLogs)
                                }
                                4 -> {
                                    SettingsScreen(
                                        settings = settings,
                                        onSaveSettings = { newSet -> viewModel.saveShopSettings(newSet) }
                                    )
                                }
                                5 -> {
                                    CustomerPortalScreen(
                                        settings = settings,
                                        portalState = customerPortalState,
                                        onSearchMobile = { mobile -> viewModel.searchCustomerPortal(mobile) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Assistant Bottom Sheet
                if (showVoiceSheet) {
                    VoiceAssistantSheet(
                        sheetState = voiceSheetState,
                        voiceState = voiceState,
                        onStartListening = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.startListeningVoice()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopListening = { viewModel.stopListeningVoice() },
                        onSubmitCommand = { cmd -> viewModel.submitVoiceCommand(cmd) },
                        onDismiss = {
                            viewModel.stopListeningVoice()
                            showVoiceSheet = false
                        }
                    )
                }

                // Add Account Dialog
                if (showAddAccountDialog) {
                    AddAccountDialog(
                        defaultCreditLimit = settings.defaultCreditLimit,
                        onDismiss = { showAddAccountDialog = false },
                        onConfirm = { name, mobile, limit ->
                            showAddAccountDialog = false
                            viewModel.createAccount(name, mobile, limit)
                        }
                    )
                }

                // Transaction Dialog
                if (showTxnDialog && selectedAccount != null) {
                    val acc = selectedAccount!!
                    TransactionDialog(
                        account = acc,
                        isCredit = txnIsCredit,
                        onDismiss = { showTxnDialog = false },
                        onConfirm = { amt, note, items, due ->
                            showTxnDialog = false
                            val delta = if (txnIsCredit) amt else -amt
                            viewModel.addTransaction(
                                accountId = acc.id,
                                delta = delta,
                                note = note,
                                items = items,
                                dueDate = due,
                                onLimitExceeded = { newAdv, limit ->
                                    depositLimitWarning = Triple(acc.id, newAdv, limit)
                                }
                            )
                        }
                    )
                }

                // Deposit Limit Warning Dialog
                if (depositLimitWarning != null) {
                    val (accId, newAdv, limit) = depositLimitWarning!!
                    AlertDialog(
                        onDismissRequest = { depositLimitWarning = null },
                        title = { Text("⚠ जमा लिमिट ओवर!", color = KhataRed) },
                        text = {
                            Text("इस खाते में ज़्यादा से ज़्यादा ₹${limit.toInt()} तक जमा हो सकता है। यह रकम जोड़ने पर एडवांस ₹${newAdv.toInt()} हो जाएगा। क्या फिर भी जमा करना चाहते हैं?")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    depositLimitWarning = null
                                    viewModel.forceAddTransaction(accId, -newAdv, "लिमिट-ओवर-स्वीकृत", "")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = KhataRed)
                            ) {
                                Text("हाँ, ज़रूरी है")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { depositLimitWarning = null }) {
                                Text("नहीं")
                            }
                        }
                    )
                }

                // Edit Account Dialog
                if (showEditAccountDialog && selectedAccount != null) {
                    val acc = selectedAccount!!
                    EditAccountDialog(
                        account = acc,
                        onDismiss = { showEditAccountDialog = false },
                        onConfirm = { name, mobile, limit ->
                            showEditAccountDialog = false
                            viewModel.updateAccountDetails(acc.id, name, mobile, limit)
                        }
                    )
                }

                // Bulk Reminders Dialog
                if (showBulkRemindDialog) {
                    val debtors = rawActiveAccounts.filter { it.balance > 0 }
                    BulkRemindDialog(
                        debtors = debtors,
                        onSendSingleReminder = { debtor ->
                            viewModel.shareCustomerOnWhatsApp(debtor.id)
                        },
                        onDismiss = { showBulkRemindDialog = false }
                    )
                }
            }
        }
    }
}
