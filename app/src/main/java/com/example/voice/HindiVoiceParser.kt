package com.example.voice

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedTxn(
    val amount: Double?,
    val items: String,
    val dueDate: Long,
    val note: String
)

object HindiVoiceParser {

    private val NUM_WORDS = mapOf(
        "शून्य" to 0, "जीरो" to 0, "ज़ीरो" to 0, "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4,
        "पांच" to 5, "पाँच" to 5, "छह" to 6, "छः" to 6, "छे" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10,
        "ग्यारह" to 11, "बारह" to 12, "तेरह" to 13, "चौदह" to 14, "पंद्रह" to 15, "सोलह" to 16, "सत्रह" to 17,
        "अठारह" to 18, "उन्नीस" to 19, "बीस" to 20, "इक्कीस" to 21, "बाईस" to 22, "तेईस" to 23, "चौबीस" to 24,
        "पच्चीस" to 25, "छब्बीस" to 26, "सत्ताईस" to 27, "सताईस" to 27, "अट्ठाईस" to 28, "अठाईस" to 28,
        "उनतीस" to 29, "तीस" to 30, "इकतीस" to 31, "बत्तीस" to 32, "तैंतीस" to 33, "चौंतीस" to 34,
        "पैंतीस" to 35, "पैतीस" to 35, "छत्तीस" to 36, "सैंतीस" to 37, "सैतीस" to 37, "अड़तीस" to 38,
        "उनतालीस" to 39, "चालीस" to 40, "इकतालीस" to 41, "बयालीस" to 42, "तैंतालीस" to 43, "चवालीस" to 44,
        "पैंतालीस" to 45, "छियालीस" to 46, "सैंतालीस" to 47, "अड़तालीस" to 48, "उनचास" to 49, "पचास" to 50,
        "इक्यावन" to 51, "बावन" to 52, "तिरपन" to 53, "चौवन" to 54, "पचपन" to 55, "छप्पन" to 56,
        "सत्तावन" to 57, "अट्ठावन" to 58, "उनसठ" to 59, "साठ" to 60, "इकसठ" to 61, "बासठ" to 62,
        "तिरसठ" to 63, "चौंसठ" to 64, "पैंसठ" to 65, "छियासठ" to 66, "सड़सठ" to 67, "अड़सठ" to 68,
        "उनहत्तर" to 69, "सत्तर" to 70, "इकहत्तर" to 71, "बहत्तर" to 72, "तिहत्तर" to 73, "चौहत्तर" to 74,
        "पचहत्तर" to 75, "छिहत्तर" to 76, "सतहत्तर" to 77, "अठहत्तर" to 78, "उनासी" to 79, "अस्सी" to 80,
        "इक्यासी" to 81, "बयासी" to 82, "तिरासी" to 83, "चौरासी" to 84, "पचासी" to 85, "छियासी" to 86,
        "सत्तासी" to 87, "अट्ठासी" to 88, "नवासी" to 89, "नब्बे" to 90, "इक्यानवे" to 91, "बानवे" to 92,
        "तिरानवे" to 93, "चौरानवे" to 94, "पंचानवे" to 95, "छियानवे" to 96, "सत्तानवे" to 97, "अट्ठानवे" to 98,
        "निन्यानवे" to 99
    )

    private val MUL_WORDS = mapOf("सौ" to 100, "हजार" to 1000, "लाख" to 100000)
    private val CURRENCY_WORDS = setOf("रुपये", "रुपए", "रुपया", "रूपये", "रुपैया", "रु", "rs")
    private val QUANTITY_UNITS = setOf("किलो", "kg", "किलोग्राम", "लीटर", "लिटर", "ग्राम", "पैकेट", "पीस", "दर्जन", "बोरी", "डिब्बा", "बोतल", "पाव", "पैक", "कट्टा", "पेटी")
    private val TIME_UNITS = setOf("दिन", "दिनों", "हफ्ते", "हफ्ता", "सप्ताह", "महीने", "महीना", "तारीख")

    private val MONTH_MAP = mapOf(
        "जनवरी" to 1, "फरवरी" to 2, "मार्च" to 3, "अप्रैल" to 4, "मई" to 5, "जून" to 6,
        "जुलाई" to 7, "अगस्त" to 8, "सितंबर" to 9, "सितम्बर" to 9, "अक्टूबर" to 10, "अक्तूबर" to 10,
        "नवंबर" to 11, "नवम्बर" to 11, "दिसंबर" to 12, "दिसम्बर" to 12
    )

    private val MINUS_WORDS = setOf("घटा", "घटाओ", "घटाइए", "घटाना", "माइनस", "कम", "जमा", "मिले", "मिला", "मिली", "चुका", "चुकाए", "चुकाया", "चुकता", "लौटा", "लौटाए", "लौटाया", "कट", "कटा", "कटवा", "भरे", "भरा", "आए", "आया", "आई", "minus", "sub")
    private val PLUS_WORDS = setOf("जोड़", "जोड़ो", "जोड़ना", "प्लस", "चढ़ा", "चढ़ाओ", "चढ़ाना", "उधार", "दिया", "दिए", "दी", "बढ़ा", "बढ़ाओ", "लिख", "लिखो", "plus", "add", "लिया")

    fun normalize(text: String): String {
        var s = text.lowercase().replace("₹", " रुपये ")
        val devNums = "०१२३४५६७८९"
        val engNums = "0123456789"
        for (i in devNums.indices) {
            s = s.replace(devNums[i], engNums[i])
        }
        s = s.replace(Regex("[,?!;:]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return s
    }

    fun extractPhone(tokens: List<String>): String? {
        val digits = StringBuilder()
        for (token in tokens) {
            if (token.all { it.isDigit() }) {
                digits.append(token)
            } else if (NUM_WORDS.containsKey(token) && NUM_WORDS[token]!! <= 9) {
                digits.append(NUM_WORDS[token])
            } else {
                if (digits.length >= 10) return digits.takeLast(10).toString()
                digits.clear()
            }
        }
        return if (digits.length >= 10) digits.takeLast(10).toString() else null
    }

    fun parseAmount(tokens: List<String>): Double? {
        var i = 0
        var foundAmount: Double? = null

        while (i < tokens.size) {
            val token = tokens[i]
            if (isNumericToken(token) || MUL_WORDS.containsKey(token)) {
                var total = 0.0
                var current = 0.0
                var j = i

                while (j < tokens.size && (isNumericToken(tokens[j]) || MUL_WORDS.containsKey(tokens[j]))) {
                    val tok = tokens[j]
                    if (MUL_WORDS.containsKey(tok)) {
                        val mul = MUL_WORDS[tok]!!.toDouble()
                        if (tok == "सौ") {
                            current = (if (current == 0.0) 1.0 else current) * mul
                        } else {
                            total += (if (current == 0.0) 1.0 else current) * mul
                            current = 0.0
                        }
                    } else {
                        current += tokenValue(tok)
                    }
                    j++
                }

                val finalRun = total + current
                val nextWord = if (j < tokens.size) tokens[j] else ""
                val prevWord = if (i > 0) tokens[i - 1] else ""

                // Ignore if followed by quantity unit or time unit
                if (finalRun > 0 && !QUANTITY_UNITS.contains(nextWord) && !TIME_UNITS.contains(nextWord) && !MONTH_MAP.containsKey(nextWord) && prevWord != "नंबर") {
                    if (CURRENCY_WORDS.contains(nextWord) || CURRENCY_WORDS.contains(prevWord)) {
                        return finalRun
                    }
                    if (foundAmount == null) {
                        foundAmount = finalRun
                    }
                }
                i = j
            } else {
                i++
            }
        }
        return foundAmount
    }

    private fun isNumericToken(t: String): Boolean {
        return t.matches(Regex("^\\d+(\\.\\d+)?k?$")) || NUM_WORDS.containsKey(t)
    }

    private fun tokenValue(t: String): Double {
        if (t.matches(Regex("^\\d+(\\.\\d+)?k?$"))) {
            return if (t.endsWith("k")) t.dropLast(1).toDouble() * 1000.0 else t.toDouble()
        }
        return NUM_WORDS[t]?.toDouble() ?: 0.0
    }

    fun detectDirection(text: String): Int {
        val replaced = text.replace(Regex("दे (दिए|दिये|दिया|दी)( |$)"), "चुका ")
        val tokens = replaced.split(" ")
        var minusIdx = -1
        var plusIdx = -1

        for (i in tokens.indices) {
            val tok = tokens[i]
            if (minusIdx < 0 && (MINUS_WORDS.contains(tok) || (tok == "वापस" && i + 1 < tokens.size && listOf("दिए", "किए", "कर", "किया", "दिया").contains(tokens[i + 1])))) {
                minusIdx = i
            }
            if (plusIdx < 0 && PLUS_WORDS.contains(tok)) {
                plusIdx = i
            }
        }

        return when {
            minusIdx < 0 && plusIdx < 0 -> 0
            plusIdx < 0 -> -1
            minusIdx < 0 -> 1
            minusIdx < plusIdx -> -1
            else -> 1
        }
    }

    fun parseDueDate(text: String): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (text.contains("परसों")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            return cal.timeInMillis
        }
        if (text.contains("कल")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        if (text.contains("अगले हफ्ते") || text.contains("अगले सप्ताह")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            return cal.timeInMillis
        }
        if (text.contains("अगले महीने")) {
            cal.add(Calendar.MONTH, 1)
            return cal.timeInMillis
        }

        val tokens = text.split(" ")
        for (i in tokens.indices) {
            if (isNumericToken(tokens[i]) && i + 1 < tokens.size) {
                val unit = tokens[i + 1]
                val amount = tokenValue(tokens[i]).toInt()
                if (unit.startsWith("दिन")) {
                    cal.add(Calendar.DAY_OF_YEAR, amount)
                    return cal.timeInMillis
                } else if (unit.startsWith("हफ्त") || unit.startsWith("सप्ता")) {
                    cal.add(Calendar.DAY_OF_YEAR, amount * 7)
                    return cal.timeInMillis
                } else if (unit.startsWith("महीन")) {
                    cal.add(Calendar.MONTH, amount)
                    return cal.timeInMillis
                }
            }
        }
        return 0L
    }

    fun parseItems(tokens: List<String>): String {
        val itemsIdx = tokens.indexOf("सामान")
        var start = -1
        if (itemsIdx >= 0 && itemsIdx + 1 < tokens.size) {
            start = itemsIdx + 1
        } else {
            for (i in 1 until tokens.size) {
                if (QUANTITY_UNITS.contains(tokens[i]) && isNumericToken(tokens[i - 1])) {
                    start = i - 1
                    break
                }
            }
        }

        if (start < 0) return ""

        val stopWords = setOf("वापसी", "तारीख", "नोट", "रुपये", "रुपए", "रुपया", "रु", "rs", "दिया", "दिए", "उधार", "प्लस", "जोड़ो", "लिख", "जमा", "माइनस")
        val itemTokens = mutableListOf<String>()
        var i = start
        while (i < tokens.size) {
            val tok = tokens[i]
            if (stopWords.contains(tok)) break
            itemTokens.add(tok)
            i++
        }
        return itemTokens.joinToString(" ").trim()
    }

    fun extractTxn(normalizedText: String): ParsedTxn {
        val tokens = normalizedText.split(" ")
        val amt = parseAmount(tokens)
        val items = parseItems(tokens)
        val dueDate = parseDueDate(normalizedText)
        val noteIdx = tokens.indexOf("नोट")
        val note = if (noteIdx >= 0 && noteIdx + 1 < tokens.size) {
            tokens.subList(noteIdx + 1, tokens.size).joinToString(" ")
        } else ""

        return ParsedTxn(amount = amt, items = items, dueDate = dueDate, note = note)
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
        return sdf.format(timestamp)
    }
}
