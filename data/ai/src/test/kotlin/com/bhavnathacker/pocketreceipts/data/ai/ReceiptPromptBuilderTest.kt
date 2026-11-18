package com.bhavnathacker.pocketreceipts.data.ai

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReceiptPromptBuilderTest {
    private lateinit var underTest: ReceiptPromptBuilder

    @BeforeEach
    fun setUp() {
        underTest = ReceiptPromptBuilder()
    }

    @Test
    fun `test that invoke builds the extraction prompt`() {
        val prompt = underTest(
            lines = listOf("BHARAT MEDICAL STORES", "SELF Bill No C68642 31-0ct-26 21:44:2", "NET 764.00"),
            today = LocalDate.of(2026, 11, 18),
            defaultCurrency = "INR",
        )

        assertThat(prompt).isEqualTo(
            """
            You read the text of a shop receipt and extract its fields.
            The text was read from a photo by OCR, one numbered line per row of the receipt. It can contain OCR mistakes, such as 0 for the letter O.

            Today is 2026-11-18. The default currency is INR.

            Reply with only one JSON object and no other text, in this form:
            {"merchant": "...", "date": "YYYY-MM-DD", "total": "1234.50", "currency": "XXX", "category": "..."}

            Rules:
            - merchant: the shop or business name as printed. Not an address, a document title (such as TAX INVOICE, RETAIL INVOICE or CASH MEMO) or a customer's name.
            - date: the purchase or invoice date as YYYY-MM-DD. A two-digit year means 20YY. Not an expiry date.
            - total: the amount actually paid, as a plain number with a dot for decimals and no thousands separators. When the receipt shows a gross total and a lower net, payable or grand total, use the amount paid.
            - currency: the ISO 4217 code of the total. Use INR when the receipt shows no currency.
            - category: one of GROCERIES, DINING, TRANSPORT, SHOPPING, BILLS, HEALTH, ENTERTAINMENT, TRAVEL, OTHER, chosen from the kind of shop and what was bought.
            - Use null for a field you can't find. Don't guess.

            Receipt:
            1. BHARAT MEDICAL STORES
            2. SELF Bill No C68642 31-0ct-26 21:44:2
            3. NET 764.00
            """.trimIndent(),
        )
    }
}
