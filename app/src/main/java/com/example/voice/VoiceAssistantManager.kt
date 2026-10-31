package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.CustomerAccount
import com.example.data.KhataRepository
import com.example.data.TxnParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    data class Listening(val interimText: String = "") : VoiceState()
    data class Processing(val commandText: String) : VoiceState()
    data class Completed(
        val spokenText: String,
        val responseMessage: String,
        val action: VoiceAction? = null
    ) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

sealed class VoiceAction {
    data class OpenAccount(val accountId: String) : VoiceAction()
    data class NavigateTab(val tabIndex: Int) : VoiceAction()
    data class ShareWhatsApp(val accountId: String) : VoiceAction()
    data class ShowQR(val accountId: String, val mode: String) : VoiceAction()
    data class ShowStatement(val accountId: String) : VoiceAction()
}

class VoiceAssistantManager(
    private val context: Context,
    private val repository: KhataRepository,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.w("VoiceAssistant", "TTS initialization failed: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            ttsReady = true
        }
    }

    fun startListening() {
        stopTts()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error("इस डिवाइस में स्पीच पहचान उपलब्ध नहीं है। नीचे टाइप करके कमान दें।")
            return
        }

        try {
            destroyRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening("🔴 सुन रहा हूँ... बोलिए")
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.Listening("आवाज़ आ रही है...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.Listening("समझ रहे हैं...")
                    }

                    override fun onError(error: Int) {
                        val errMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ऑडियो रिकॉर्डिंग में समस्या"
                            SpeechRecognizer.ERROR_NO_MATCH -> "कोई आवाज़ समझ नहीं आई, फिर बोलिए या टाइप करें"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "समय समाप्त। फिर से माइक दबाएँ या टाइप करें"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक की अनुमति दें"
                            SpeechRecognizer.ERROR_NETWORK -> "इंटरनेट कनेक्शन की आवश्यकता है"
                            else -> "पहचान में समस्या आई ($error)"
                        }
                        // Stop cleanly, DO NOT loop or restart!
                        _voiceState.value = VoiceState.Error(errMsg)
                        destroyRecognizer()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        destroyRecognizer()
                        if (text.isNotBlank()) {
                            processCommand(text, fromVoice = true)
                        } else {
                            _voiceState.value = VoiceState.Idle
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _voiceState.value = VoiceState.Listening("आपने कहा: $text")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("माइक शुरू करने में समस्या: ${e.message}")
            destroyRecognizer()
        }
    }

    /**
     * Critical fix: Calling this cleanly stops listening and resets state,
     * preventing any typing errors or on/off mic oscillation!
     */
    fun stopListening() {
        destroyRecognizer()
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Idle
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Error stopping recognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    fun stopTts() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Error stopping TTS", e)
        }
    }

    fun processCommand(rawText: String, fromVoice: Boolean = false) {
        stopListening()
        val text = rawText.trim()
        if (text.isBlank()) return

        _voiceState.value = VoiceState.Processing(text)

        scope.launch(Dispatchers.IO) {
            try {
                val normalized = HindiVoiceParser.normalize(text)
                val response = executeNormalizedCommand(rawText, normalized)

                _voiceState.value = VoiceState.Completed(
                    spokenText = rawText,
                    responseMessage = response.first,
                    action = response.second
                )

                if (fromVoice && ttsReady) {
                    tts?.speak(response.first.replace("₹", " रुपये "), TextToSpeech.QUEUE_FLUSH, null, "KhataTTS")
                }
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error("कमांड प्रोसेस करने में त्रुटि: ${e.message}")
            }
        }
    }

    private suspend fun executeNormalizedCommand(raw: String, norm: String): Pair<String, VoiceAction?> {
        val tokens = norm.split(" ")
        val accounts = repository.activeAccountsSync()

        // 1. Navigation Commands
        if (norm.contains("होम") || norm.contains("मुख्य पेज") || norm.contains("खाते खोलो") || norm.contains("सारे खाते")) {
            return Pair("होम पेज खुल गया।", VoiceAction.NavigateTab(0))
        }
        if (norm.contains("स्टॉक खोलो") || norm.contains("खत्म स्टॉक") || norm.contains("स्टॉक टैब")) {
            return Pair("खत्म स्टॉक खुल गया।", VoiceAction.NavigateTab(1))
        }
        if (norm.contains("हटाए गए") || norm.contains("डिलीटेड खाते")) {
            return Pair("हटाए गए खाते खुल गए।", VoiceAction.NavigateTab(2))
        }
        if (norm.contains("इतिहास खोलो") || norm.contains("हिस्ट्री खोलो") || norm.contains("लेजर")) {
            return Pair("इतिहास खुल गया।", VoiceAction.NavigateTab(3))
        }
        if (norm.contains("सेटिंग खोलो") || norm.contains("सेटिंग्स")) {
            return Pair("सेटिंग्स खुल गई।", VoiceAction.NavigateTab(4))
        }

        // 2. Stock Commands
        if (norm.contains("खत्म") || (norm.contains("स्टॉक") && (norm.contains("जोड़") || norm.contains("लिख")))) {
            val stopWords = setOf("खत्म", "हो", "गया", "गई", "है", "स्टॉक", "में", "जोड़ो", "लिखो", "सामान")
            val filtered = tokens.filterNot { stopWords.contains(it) }.joinToString(" ").trim()
            if (filtered.isNotBlank()) {
                repository.addStockItem(filtered, "")
                return Pair("$filtered को खत्म स्टॉक में जोड़ दिया।", VoiceAction.NavigateTab(1))
            }
        }
        if (norm.contains("आ गया") || norm.contains("आ गई") || norm.contains("मिल गया")) {
            val stock = repository.getStockItemsSync()
            val match = stock.find { norm.contains(it.name.lowercase()) }
            if (match != null) {
                repository.deleteStockItem(match.id)
                return Pair("${match.name} को स्टॉक सूची से हटा दिया।", VoiceAction.NavigateTab(1))
            }
        }

        // 3. New Account: "नया खाता राहुल 9876543210"
        if (norm.contains("नया खाता") || norm.contains("खाता बना") || norm.contains("नया ग्राहक")) {
            val phone = HindiVoiceParser.extractPhone(tokens)
            val nameWords = tokens.filterNot {
                listOf("नया", "खाता", "ग्राहक", "बनाओ", "बना", "जोड़ो", "का", "की", "मोबाइल", "नंबर", "है").contains(it) ||
                it.all { ch -> ch.isDigit() }
            }.joinToString(" ").trim()

            if (nameWords.isNotBlank() && phone != null) {
                val res = repository.createAccount(nameWords, phone, "आवाज़")
                return if (res.isSuccess) {
                    val acc = res.getOrThrow()
                    Pair("${acc.name} का नया खाता बन गया।", VoiceAction.OpenAccount(acc.id))
                } else {
                    Pair("खाता नहीं बन सका: ${res.exceptionOrNull()?.message}", null)
                }
            } else if (nameWords.isNotBlank()) {
                return Pair("कृपया ${nameWords} का 10 अंकों का मोबाइल नंबर भी बताएँ।", null)
            }
        }

        // 4. Match Account by Name or Phone
        val matchedAccount = findAccount(norm, accounts)

        // 5. Account specific actions
        if (matchedAccount != null) {
            // Undo last entry: "राहुल की आखिरी एंट्री हटाओ"
            if (norm.contains("आखिरी एंट्री") || norm.contains("पिछली एंट्री") || norm.contains("लास्ट एंट्री") || norm.contains("अनडू")) {
                val undoRes = repository.undoLastTxn(matchedAccount.id)
                return if (undoRes.isSuccess) {
                    Pair("${matchedAccount.name} की आखिरी एंट्री हटा दी गई।", VoiceAction.OpenAccount(matchedAccount.id))
                } else {
                    Pair(undoRes.exceptionOrNull()?.message ?: "एंट्री नहीं हट सकी", null)
                }
            }

            // WhatsApp: "राहुल को व्हाट्सएप भेजो"
            if (norm.contains("व्हाट्सएप") || norm.contains("whatsapp") || norm.contains("मैसेज")) {
                return Pair("${matchedAccount.name} के लिए व्हाट्सएप संदेश तैयार है।", VoiceAction.ShareWhatsApp(matchedAccount.id))
            }

            // QR: "राहुल का क्यूआर"
            if (norm.contains("क्यूआर") || norm.contains("qr")) {
                val mode = if (norm.contains("upi") || norm.contains("पेमेंट")) "upi" else "chk"
                return Pair("${matchedAccount.name} का QR कोड खुल रहा है।", VoiceAction.ShowQR(matchedAccount.id, mode))
            }

            // Balance check: "राहुल का बैलेंस बताओ"
            if (norm.contains("बैलेंस") || norm.contains("कितना बकाया") || norm.contains("कितने पैसे") || norm.contains("हिसाब")) {
                val bal = matchedAccount.balance
                val balMsg = when {
                    bal > 0 -> "₹${bal.toInt()} उधार बाकी है"
                    bal < 0 -> "₹${Math.abs(bal).toInt()} एडवांस जमा है"
                    else -> "हिसाब बिल्कुल बराबर है"
                }
                return Pair("${matchedAccount.name} के खाते में $balMsg।", VoiceAction.OpenAccount(matchedAccount.id))
            }

            // Transaction: "राहुल को 500 उधार", "राहुल के 200 जमा"
            val parsedTxn = HindiVoiceParser.extractTxn(norm)
            if (parsedTxn.amount != null && parsedTxn.amount > 0) {
                val dir = HindiVoiceParser.detectDirection(norm)
                val delta = if (dir < 0) -parsedTxn.amount else parsedTxn.amount
                val txnResult = repository.processTransaction(
                    TxnParams(
                        accountId = matchedAccount.id,
                        delta = delta,
                        note = parsedTxn.note,
                        source = "आवाज़",
                        items = parsedTxn.items,
                        dueDate = parsedTxn.dueDate
                    )
                )

                return when (txnResult) {
                    is com.example.data.TxnResult.Success -> {
                        val actionWord = if (delta > 0) "उधार चढ़ाया गया" else "जमा किया गया"
                        var msg = "${matchedAccount.name} के खाते में ₹${parsedTxn.amount.toInt()} $actionWord।"
                        if (parsedTxn.items.isNotBlank()) msg += " सामान: ${parsedTxn.items}।"
                        if (parsedTxn.dueDate > 0) msg += " वापसी: ${HindiVoiceParser.formatDate(parsedTxn.dueDate)}।"
                        Pair(msg, VoiceAction.OpenAccount(matchedAccount.id))
                    }
                    is com.example.data.TxnResult.DepositLimitExceeded -> {
                        Pair("जमा लिमिट ओवर! अधिकतम एडवांस सीमा ₹${txnResult.limit.toInt()} है।", VoiceAction.OpenAccount(matchedAccount.id))
                    }
                    is com.example.data.TxnResult.Error -> {
                        Pair(txnResult.message, null)
                    }
                }
            }

            // Open account
            if (norm.contains("खोलो") || norm.contains("दिखाओ") || norm.contains("खाता")) {
                return Pair("${matchedAccount.name} का खाता खुल गया।", VoiceAction.OpenAccount(matchedAccount.id))
            }
        }

        // 6. Global queries
        if (norm.contains("कुल उधार") || norm.contains("टोटल उधार") || norm.contains("कितना बकाया")) {
            val totalDue = accounts.filter { it.balance > 0 }.sumOf { it.balance }
            val count = accounts.filter { it.balance > 0 }.size
            return Pair("कुल $count खातों में ₹${totalDue.toInt()} का उधार बाकी है।", VoiceAction.NavigateTab(0))
        }

        if (norm.contains("मदद") || norm.contains("help") || norm.contains("क्या बोल सकते")) {
            return Pair("आप बोल सकते हैं: 'राहुल को 500 उधार', 'अमित के 200 जमा', 'नया खाता राहुल 9876543210', 'राहुल का बैलेंस बताओ', 'चीनी खत्म', 'इतिहास खोलो' आदि।", null)
        }

        return Pair("कमांड समझ नहीं आई। कृपया नाम और रकम स्पष्ट बोलें (जैसे: 'राहुल को 500 उधार') या टाइप करें।", null)
    }

    private fun findAccount(norm: String, accounts: List<CustomerAccount>): CustomerAccount? {
        val phone = norm.filter { it.isDigit() }.takeLast(10)
        if (phone.length == 10) {
            val acc = accounts.find { it.mobile == phone }
            if (acc != null) return acc
        }
        for (acc in accounts) {
            val accNameNorm = HindiVoiceParser.normalize(acc.name)
            if (accNameNorm.isNotBlank() && norm.contains(accNameNorm)) {
                return acc
            }
        }
        // Try word match
        val words = norm.split(" ")
        for (acc in accounts) {
            val firstWord = HindiVoiceParser.normalize(acc.name).split(" ").firstOrNull() ?: ""
            if (firstWord.length > 2 && words.contains(firstWord)) {
                return acc
            }
        }
        return null
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }

    fun release() {
        destroyRecognizer()
        tts?.shutdown()
    }
}

// Extension helpers for synchronized reading in background thread
private suspend fun KhataRepository.activeAccountsSync(): List<CustomerAccount> {
    return activeAccounts.first()
}

private suspend fun KhataRepository.getStockItemsSync(): List<com.example.data.StockItem> {
    return stockItems.first()
}
