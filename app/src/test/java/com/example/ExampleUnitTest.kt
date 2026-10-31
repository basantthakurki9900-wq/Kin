package com.example

import com.example.voice.HindiVoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAmountParsing() {
        val norm = HindiVoiceParser.normalize("राहुल को 500 रुपये उधार")
        val parsed = HindiVoiceParser.extractTxn(norm)
        assertEquals(500.0, parsed.amount)
        val dir = HindiVoiceParser.detectDirection(norm)
        assertEquals(1, dir)
    }

    @Test
    fun testHindiNumberWords() {
        val norm = HindiVoiceParser.normalize("अमित के दो सौ पचास जमा")
        val parsed = HindiVoiceParser.extractTxn(norm)
        assertEquals(250.0, parsed.amount)
        val dir = HindiVoiceParser.detectDirection(norm)
        assertEquals(-1, dir)
    }

    @Test
    fun testItemsAndDueDate() {
        val norm = HindiVoiceParser.normalize("राहुल को 2 किलो चीनी 1 लीटर तेल 400 रुपये उधार कल तक")
        val parsed = HindiVoiceParser.extractTxn(norm)
        assertEquals(400.0, parsed.amount)
        assertTrue(parsed.items.contains("चीनी"))
        assertTrue(parsed.dueDate > System.currentTimeMillis() - 10000)
    }

    @Test
    fun testPhoneExtraction() {
        val tokens = listOf("नया", "खाता", "राहुल", "9876543210")
        val phone = HindiVoiceParser.extractPhone(tokens)
        assertEquals("9876543210", phone)
    }
}
