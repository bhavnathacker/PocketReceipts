package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.Locale

class AmountInputParserTest {
    private lateinit var underTest: AmountInputParser

    private val enUs = Locale.forLanguageTag("en-US")
    private val deDe = Locale.forLanguageTag("de-DE")
    private val enIn = Locale.forLanguageTag("en-IN")

    @BeforeEach
    fun setUp() {
        underTest = AmountInputParser()
    }

    @Test
    fun `test that a dot decimal is parsed in en-US`() {
        assertThat(underTest("4.50", "USD", enUs)).isEqualTo(450)
    }

    @Test
    fun `test that a comma decimal is parsed in de-DE`() {
        assertThat(underTest("4,50", "EUR", deDe)).isEqualTo(450)
    }

    @Test
    fun `test that a dot is not taken as grouping for a two digit group in de-DE`() {
        assertThat(underTest("4.50", "EUR", deDe)).isNull()
    }

    @Test
    fun `test that de-DE grouping is accepted`() {
        assertThat(underTest("1.234,56", "EUR", deDe)).isEqualTo(123456)
    }

    @Test
    fun `test that a plain en-IN amount is parsed`() {
        assertThat(underTest("123456.50", "INR", enIn)).isEqualTo(12345650)
    }

    @Test
    fun `test that en-IN lakh grouping is parsed`() {
        assertThat(underTest("1,23,456.50", "INR", enIn)).isEqualTo(12345650)
    }

    @Test
    fun `test that en-US thousands grouping is parsed`() {
        assertThat(underTest("1,234,567.89", "USD", enUs)).isEqualTo(123456789)
    }

    @Test
    fun `test that a whole amount is parsed`() {
        assertThat(underTest("450", "INR", enIn)).isEqualTo(45000)
    }

    @Test
    fun `test that a single decimal digit is parsed`() {
        assertThat(underTest("12.3", "EUR", enUs)).isEqualTo(1230)
    }

    @Test
    fun `test that surrounding whitespace is ignored`() {
        assertThat(underTest("  4.50 ", "USD", enUs)).isEqualTo(450)
    }

    @Test
    fun `test that zero is parsed as zero`() {
        assertThat(underTest("0", "USD", enUs)).isEqualTo(0)
    }

    @Test
    fun `test that whole yen are parsed`() {
        assertThat(underTest("1500", "JPY", Locale.JAPAN)).isEqualTo(1500)
    }

    @Test
    fun `test that a decimal part is rejected for yen`() {
        assertThat(underTest("1500.5", "JPY", Locale.JAPAN)).isNull()
    }

    @Test
    fun `test that three decimals are accepted for a three decimal currency`() {
        assertThat(underTest("2.750", "KWD", enUs)).isEqualTo(2750)
    }

    @ParameterizedTest
    @CsvSource(
        "99.505, INR",
        "4.505, USD",
    )
    fun `test that too many decimals are rejected`(text: String, currency: String) {
        assertThat(underTest(text, currency, enIn)).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "abc", "4.5a", "1.2.3", "-4.50", "+4.50", "4..5", ".", "1,234.5,6", "12,34", "1,2345", ",123"])
    fun `test that malformed input is rejected in en-US`(text: String) {
        assertThat(underTest(text, "USD", enUs)).isNull()
    }

    @Test
    fun `test that an amount too large for minor units is rejected`() {
        assertThat(underTest("999999999999999999999", "USD", enUs)).isNull()
    }
}
