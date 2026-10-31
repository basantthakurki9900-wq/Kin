package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CustomerAccount
import com.example.data.KhataDatabase
import com.example.data.KhataRepository
import com.example.data.LedgerLog
import com.example.data.ShopSettings
import com.example.data.StockItem
import com.example.data.TxnParams
import com.example.data.TxnResult
import com.example.voice.VoiceAction
import com.example.voice.VoiceAssistantManager
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShareWhatsApp(val phone: String, val message: String) : UiEvent()
    data class OpenUpi(val uriString: String) : UiEvent()
}

data class CustomerPortalState(
    val searched: Boolean = false,
    val loading: Boolean = false,
    val account: CustomerAccount? = null,
    val logs: List<LedgerLog> = emptyList(),
    val error: String? = null
)

class KhataViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KhataDatabase.getDatabase(application)
    val repository = KhataRepository(database.khataDao())
    val voiceAssistant = VoiceAssistantManager(application, repository, viewModelScope)

    val voiceState: StateFlow<VoiceState> = voiceAssistant.voiceState

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    val settings: StateFlow<ShopSettings> = repository.settings
        .combine(flowOf(ShopSettings())) { set, default -> set ?: default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopSettings())

    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: Stock, 2: Deleted, 3: History, 4: Settings, 5: Portal
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    val selectedAccount: StateFlow<CustomerAccount?> = _selectedAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getAccountFlow(id) else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedAccountLogs: StateFlow<List<LedgerLog>> = _selectedAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getLogsForAccountFlow(id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawActiveAccounts: StateFlow<List<CustomerAccount>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedAccounts: StateFlow<List<CustomerAccount>> = repository.deletedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<LedgerLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockItems: StateFlow<List<StockItem>> = repository.stockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")

    val filteredActiveAccounts: StateFlow<List<CustomerAccount>> = combine(
        rawActiveAccounts,
        searchQuery
    ) { list, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            list
        } else {
            list.filter {
                it.name.lowercase().contains(q) || it.mobile.contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer Portal State
    private val _customerPortalState = MutableStateFlow(CustomerPortalState())
    val customerPortalState: StateFlow<CustomerPortalState> = _customerPortalState.asStateFlow()

    // PIN lock state
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            // Check voice action triggers
            voiceState.collect { state ->
                if (state is VoiceState.Completed && state.action != null) {
                    handleVoiceAction(state.action)
                }
            }
        }
    }

    private fun handleVoiceAction(action: VoiceAction) {
        when (action) {
            is VoiceAction.OpenAccount -> {
                _selectedAccountId.value = action.accountId
                _currentTab.value = 0
            }
            is VoiceAction.NavigateTab -> {
                _selectedAccountId.value = null
                _currentTab.value = action.tabIndex
            }
            is VoiceAction.ShareWhatsApp -> {
                _selectedAccountId.value = action.accountId
                shareCustomerOnWhatsApp(action.accountId)
            }
            is VoiceAction.ShowQR -> {
                _selectedAccountId.value = action.accountId
            }
            is VoiceAction.ShowStatement -> {
                _selectedAccountId.value = action.accountId
            }
        }
    }

    fun selectTab(tab: Int) {
        _selectedAccountId.value = null
        _currentTab.value = tab
    }

    fun openAccount(id: String) {
        _selectedAccountId.value = id
    }

    fun closeAccount() {
        _selectedAccountId.value = null
    }

    fun unlockApp(pin: String): Boolean {
        val actualPin = settings.value.pin
        return if (actualPin.isEmpty() || pin == actualPin) {
            _isUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun createAccount(name: String, mobile: String, limit: Double) {
        viewModelScope.launch {
            val res = repository.createAccount(name, mobile, "मैनुअल", limit)
            if (res.isSuccess) {
                val acc = res.getOrThrow()
                _selectedAccountId.value = acc.id
                emitToast("${acc.name} का खाता बन गया")
            } else {
                emitToast(res.exceptionOrNull()?.message ?: "त्रुटि आई")
            }
        }
    }

    fun addTransaction(
        accountId: String,
        delta: Double,
        note: String,
        items: String,
        dueDate: Long,
        onLimitExceeded: (Double, Double) -> Unit
    ) {
        viewModelScope.launch {
            val params = TxnParams(accountId, delta, note, "मैनुअल", items, dueDate)
            when (val result = repository.processTransaction(params)) {
                is TxnResult.Success -> {
                    emitToast(result.message)
                }
                is TxnResult.DepositLimitExceeded -> {
                    onLimitExceeded(result.newAdvance, result.limit)
                }
                is TxnResult.Error -> {
                    emitToast(result.message)
                }
            }
        }
    }

    fun forceAddTransaction(accountId: String, delta: Double, note: String, items: String) {
        viewModelScope.launch {
            val params = TxnParams(accountId, delta, note, "लिमिट-स्वीकृत", items, 0L)
            when (val result = repository.processTransaction(params, force = true)) {
                is TxnResult.Success -> emitToast(result.message)
                is TxnResult.Error -> emitToast(result.message)
                else -> {}
            }
        }
    }

    fun updateAccountDetails(id: String, name: String, mobile: String, limit: Double) {
        viewModelScope.launch {
            val res = repository.updateAccountDetails(id, name, mobile, limit)
            if (res.isSuccess) {
                emitToast("खाता सही हो गया")
            } else {
                emitToast(res.exceptionOrNull()?.message ?: "त्रुटि आई")
            }
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            _selectedAccountId.value = null
            emitToast("खाता हटा दिया गया (हटाए गए में सुरक्षित)")
        }
    }

    fun restoreAccount(id: String) {
        viewModelScope.launch {
            repository.restoreAccount(id)
            emitToast("खाता वापस आ गया")
        }
    }

    fun purgeAccount(id: String) {
        viewModelScope.launch {
            repository.purgeAccount(id)
            emitToast("खाता हमेशा के लिए हटा दिया गया")
        }
    }

    fun deleteLogEntry(logId: String) {
        viewModelScope.launch {
            repository.deleteLogEntry(logId)
            emitToast("एंट्री हटा दी गई")
        }
    }

    fun clearCustomerHistory(accId: String) {
        viewModelScope.launch {
            val cnt = repository.clearHistory(accId)
            emitToast("$cnt एंट्री वाली पूरी हिस्ट्री साफ़ कर दी")
        }
    }

    fun addStockItem(name: String, note: String) {
        viewModelScope.launch {
            repository.addStockItem(name, note)
            emitToast("खत्म स्टॉक में जोड़ा गया")
        }
    }

    fun deleteStockItem(id: String) {
        viewModelScope.launch {
            repository.deleteStockItem(id)
            emitToast("स्टॉक सूची से हटाया")
        }
    }

    fun saveShopSettings(newSettings: ShopSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            emitToast("सेटिंग्स सुरक्षित हो गईं")
        }
    }

    // Customer Portal Search
    fun searchCustomerPortal(mobile: String) {
        viewModelScope.launch {
            _customerPortalState.value = CustomerPortalState(searched = true, loading = true)
            val result = repository.getCustomerByMobile(mobile)
            if (result != null) {
                _customerPortalState.value = CustomerPortalState(
                    searched = true,
                    loading = false,
                    account = result.first,
                    logs = result.second
                )
            } else {
                _customerPortalState.value = CustomerPortalState(
                    searched = true,
                    loading = false,
                    error = "इस मोबाइल नंबर का कोई खाता नहीं मिला। कृपया सही नंबर डालें।"
                )
            }
        }
    }

    fun resetCustomerPortal() {
        _customerPortalState.value = CustomerPortalState()
    }

    fun shareCustomerOnWhatsApp(accountId: String) {
        viewModelScope.launch {
            val acc = rawActiveAccounts.value.find { it.id == accountId } ?: return@launch
            val shop = settings.value

            val balanceText = when {
                acc.balance > 0 -> "बकाया ₹${acc.balance.toInt()}"
                acc.balance < 0 -> "एडवांस जमा ₹${Math.abs(acc.balance).toInt()}"
                else -> "हिसाब बराबर"
            }

            val webLink = if (shop.webPortalLink.isNotBlank()) shop.webPortalLink else "https://khata.page/portal"
            val customerPortalUrl = "$webLink?mobile=${acc.mobile}"

            val upiText = if (shop.upiId.isNotBlank() && acc.balance > 0) {
                "\n\n💳 तुरंत UPI पेमेंट करें:\nupi://pay?pa=${shop.upiId}&pn=${shop.shopName}&am=${acc.balance.toInt()}&cu=INR&tn=KhataPayment\n(UPI ID: ${shop.upiId})"
            } else ""

            val message = """
                नमस्ते ${acc.name} जी,
                ${shop.shopName} में आपका हिसाब:
                💰 $balanceText

                🔎 अपना पूरा हिसाब और हिस्ट्री ऑनलाइन देखें:
                $customerPortalUrl

                (ऊपर दिए गए लिंक को खोलकर अपना मोबाइल नंबर ${acc.mobile} डालें और तुरंत पूरा खाता विवरण देखें)$upiText

                धन्यवाद 🙏
                ${shop.shopName}
            """.trimIndent()

            _eventFlow.emit(UiEvent.ShareWhatsApp(acc.mobile, message))
        }
    }

    fun startListeningVoice() {
        voiceAssistant.startListening()
    }

    fun stopListeningVoice() {
        voiceAssistant.stopListening()
    }

    fun submitVoiceCommand(text: String) {
        voiceAssistant.processCommand(text, fromVoice = false)
    }

    private suspend fun emitToast(msg: String) {
        _eventFlow.emit(UiEvent.ShowToast(msg))
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.release()
    }
}
