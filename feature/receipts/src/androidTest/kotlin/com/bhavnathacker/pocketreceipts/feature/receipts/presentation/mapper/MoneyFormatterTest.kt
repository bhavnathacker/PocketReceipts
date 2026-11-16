package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Runs on a device: [MoneyFormatter] uses Android's ICU, which the JVM doesn't have.
 */
@RunWith(AndroidJUnit4::class)
class MoneyFormatterTest {
    private lateinit var underTest: MoneyFormatter

    @Before
    fun setUp() {
        underTest = MoneyFormatter()
    }

    @Test
    fun test_that_euros_keep_two_decimals_in_en_US() {
        assertThat(underTest(Money(1230, "EUR"), Locale.US)).isEqualTo("€12.30")
    }

    @Test
    fun test_that_euros_use_the_de_DE_decimal_comma() {
        assertThat(underTest(Money(1230, "EUR"), Locale.GERMANY)).isEqualTo("12,30 €")
    }

    @Test
    fun test_that_rupees_use_lakh_grouping_in_en_IN() {
        assertThat(underTest(Money(12345650, "INR"), Locale.forLanguageTag("en-IN"))).isEqualTo("₹1,23,456.50")
    }

    @Test
    fun test_that_yen_have_no_decimals() {
        assertThat(underTest(Money(1500, "JPY"), Locale.US)).isEqualTo("¥1,500")
    }

    @Test
    fun test_that_a_three_decimal_currency_shows_three_decimals() {
        assertThat(underTest(Money(2750, "KWD"), Locale.US)).endsWith("2.750")
    }
}
