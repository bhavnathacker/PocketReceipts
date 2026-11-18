package com.bhavnathacker.pocketreceipts.data.ai

import com.bhavnathacker.pocketreceipts.domain.scan.ExtractedReceipt
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReceiptReplyParserTest {
    private lateinit var underTest: ReceiptReplyParser

    @BeforeEach
    fun setUp() {
        underTest = ReceiptReplyParser()
    }

    private val bharat = ExtractedReceipt(
        merchant = "BHARAT MEDICAL STORES",
        date = "2026-10-31",
        total = "764.00",
        currency = "INR",
        category = "HEALTH",
    )

    @Test
    fun `test that invoke reads a clean reply`() {
        val reply = """{"merchant": "BHARAT MEDICAL STORES", "date": "2026-10-31", "total": "764.00", """ +
            """"currency": "INR", "category": "HEALTH"}"""

        assertThat(underTest(reply)).isEqualTo(bharat)
    }

    @Test
    fun `test that invoke reads a reply wrapped in a code fence and prose`() {
        val reply = """
            Here are the fields:
            ```json
            {"merchant": "BHARAT MEDICAL STORES", "date": "2026-10-31", "total": "764.00", "currency": "INR", "category": "HEALTH"}
            ```
        """.trimIndent()

        assertThat(underTest(reply)).isEqualTo(bharat)
    }

    @Test
    fun `test that invoke reads a total written as a number`() {
        assertThat(underTest("""{"total": 764.00}""").total).isEqualTo("764.00")
    }

    @Test
    fun `test that invoke reads null values as missing`() {
        assertThat(underTest("""{"merchant": null, "date": "null", "total": "", "currency": null, "category": null}"""))
            .isEqualTo(ExtractedReceipt())
    }

    @Test
    fun `test that invoke treats missing keys as missing values`() {
        assertThat(underTest("""{"merchant": "HIT-MAN"}""")).isEqualTo(ExtractedReceipt(merchant = "HIT-MAN"))
    }

    @Test
    fun `test that invoke ignores extra keys`() {
        assertThat(underTest("""{"merchant": "HIT-MAN", "items": [{"name": "shirt"}], "gst": "24AANFB"}""").merchant)
            .isEqualTo("HIT-MAN")
    }

    @Test
    fun `test that invoke throws when the reply has no JSON object`() {
        assertThrows<UnreadableReplyException> { underTest("I couldn't read this receipt.") }
    }

    @Test
    fun `test that invoke throws when the JSON is broken`() {
        assertThrows<UnreadableReplyException> { underTest("""{"merchant": "HIT-MAN", "total": }""") }
    }
}
