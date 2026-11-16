package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class AmountInputFormatterTest {
    private lateinit var underTest: AmountInputFormatter

    @BeforeEach
    fun setUp() {
        underTest = AmountInputFormatter()
    }

    @Test
    fun `test that minor units become plain text with a dot in en-IN`() {
        assertThat(underTest(Money(12345650, "INR"), Locale.forLanguageTag("en-IN"))).isEqualTo("123456.50")
    }

    @Test
    fun `test that minor units use the decimal comma in de-DE`() {
        assertThat(underTest(Money(450, "EUR"), Locale.GERMANY)).isEqualTo("4,50")
    }

    @Test
    fun `test that yen have no decimal part`() {
        assertThat(underTest(Money(1500, "JPY"), Locale.JAPAN)).isEqualTo("1500")
    }

    @Test
    fun `test that the formatted text parses back to the same amount`() {
        val locale = Locale.forLanguageTag("en-IN")

        val text = underTest(Money(9950, "INR"), locale)

        assertThat(AmountInputParser()(text, "INR", locale)).isEqualTo(9950)
    }
}
