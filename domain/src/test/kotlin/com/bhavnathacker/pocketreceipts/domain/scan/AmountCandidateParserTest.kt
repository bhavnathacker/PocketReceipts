package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AmountCandidateParserTest {
    private lateinit var underTest: AmountCandidateParser

    private val enIn = Locale.forLanguageTag("en-IN")
    private val enUs = Locale.forLanguageTag("en-US")
    private val enGb = Locale.forLanguageTag("en-GB")
    private val enCa = Locale.forLanguageTag("en-CA")
    private val deDe = Locale.forLanguageTag("de-DE")
    private val jaJp = Locale.forLanguageTag("ja-JP")

    private val labelledReceipt = listOf(
        "Milk 120.00",
        "Bread 80.00",
        "SUBTOTAL 200.00",
        "TAX 10.00",
        "TOTAL 210.00",
    )

    @BeforeAll
    fun setUp() {
        underTest = AmountCandidateParser(getDefaultCurrency = GetDefaultCurrencyUseCase())
    }

    @Test
    fun `test that invoke reads Indian digit grouping`() {
        assertThat(underTest(listOf("Grand Total ₹1,23,456.50"), enIn).first())
            .isEqualTo(Money(12345650, "INR"))
    }

    @Test
    fun `test that invoke reads dot grouping with a decimal comma`() {
        assertThat(underTest(listOf("SUMME 1.234,56"), deDe)).containsExactly(Money(123456, "EUR"))
    }

    @Test
    fun `test that invoke reads space grouping with a decimal comma`() {
        assertThat(underTest(listOf("Gesamt 1 234,56"), deDe)).containsExactly(Money(123456, "EUR"))
    }

    @Test
    fun `test that invoke ignores phone numbers`() {
        assertThat(underTest(listOf("Tel 020 7946 0000", "+91 98765 43210"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke ignores dates and years`() {
        assertThat(underTest(listOf("03.02.26", "03.02.2026", "Since 2026"), deDe)).isEmpty()
    }

    @Test
    fun `test that invoke ignores quantities`() {
        assertThat(underTest(listOf("2 x Milk", "Items: 3"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke ignores percentages`() {
        assertThat(underTest(listOf("GST 18.00%"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke ignores amounts without the currency's decimal places`() {
        assertThat(underTest(listOf("Weight 4.5"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke puts the labelled total first`() {
        assertThat(underTest(labelledReceipt, enGb).first()).isEqualTo(Money(21000, "GBP"))
    }

    @Test
    fun `test that invoke orders the other amounts largest first`() {
        assertThat(underTest(labelledReceipt, enGb).map { it.minorUnits })
            .containsExactly(21000L, 20000L, 12000L, 8000L, 1000L)
            .inOrder()
    }

    @Test
    fun `test that invoke uses the last labelled line`() {
        assertThat(underTest(listOf("TOTAL 50.00", "Card", "AMOUNT DUE 45.00"), enGb).first())
            .isEqualTo(Money(4500, "GBP"))
    }

    @Test
    fun `test that invoke does not treat a subtotal as the total`() {
        assertThat(underTest(listOf("TOTAL 90.00", "SUBTOTAL 100.00"), enGb).first())
            .isEqualTo(Money(9000, "GBP"))
    }

    @Test
    fun `test that invoke does not treat total tax as the total`() {
        assertThat(underTest(listOf("TOTAL 9.00", "TOTAL TAX 10.00"), enGb).first())
            .isEqualTo(Money(900, "GBP"))
    }

    @Test
    fun `test that invoke orders amounts largest first when no line is labelled`() {
        assertThat(underTest(listOf("4.50", "12.00", "16.50"), enUs).map { it.minorUnits })
            .containsExactly(1650L, 1200L, 450L)
            .inOrder()
    }

    @Test
    fun `test that invoke removes duplicate amounts`() {
        assertThat(underTest(listOf("Milk 200.00", "TOTAL 200.00"), enGb)).containsExactly(Money(20000, "GBP"))
    }

    @Test
    fun `test that invoke returns at most eight amounts`() {
        val lines = (1..10).map { "Item $it.00" }

        assertThat(underTest(lines, enGb)).hasSize(8)
    }

    @Test
    fun `test that invoke uses the default currency when no marker is printed`() {
        assertThat(underTest(listOf("TOTAL 210.00"), enGb)).containsExactly(Money(21000, "GBP"))
    }

    @Test
    fun `test that invoke uses the euro sign over the default currency`() {
        assertThat(underTest(listOf("Total € 4,50"), enIn)).containsExactly(Money(450, "EUR"))
    }

    @Test
    fun `test that invoke reads Rs as rupees`() {
        assertThat(underTest(listOf("NET AMOUNT Rs. 250.00"), enUs)).containsExactly(Money(25000, "INR"))
    }

    @Test
    fun `test that invoke reads the pound sign as GBP`() {
        assertThat(underTest(listOf("TOTAL £12.30"), enIn)).containsExactly(Money(1230, "GBP"))
    }

    @Test
    fun `test that invoke reads the yen sign as JPY without decimals`() {
        assertThat(underTest(listOf("合計 ¥1,500"), enUs)).containsExactly(Money(1500, "JPY"))
    }

    @Test
    fun `test that invoke reads an ISO currency code next to the amount`() {
        assertThat(underTest(listOf("TOTAL 4.50 EUR"), enIn)).containsExactly(Money(450, "EUR"))
    }

    @Test
    fun `test that invoke reads the dollar sign as the default currency when it uses that symbol`() {
        assertThat(underTest(listOf("TOTAL $12.00"), enCa)).containsExactly(Money(1200, "CAD"))
    }

    @Test
    fun `test that invoke reads the dollar sign as USD when the default currency uses another symbol`() {
        assertThat(underTest(listOf("TOTAL $12.00"), enIn)).containsExactly(Money(1200, "USD"))
    }

    @Test
    fun `test that invoke reads a whole amount next to a currency marker`() {
        assertThat(underTest(listOf("Paid Rs 1,500"), enIn)).containsExactly(Money(150000, "INR"))
    }

    @Test
    fun `test that invoke ignores a whole amount without a marker or label when the currency has no decimals`() {
        assertThat(underTest(listOf("Item 1500"), jaJp)).isEmpty()
    }

    @Test
    fun `test that invoke reads a whole amount on a total line when the currency has no decimals`() {
        assertThat(underTest(listOf("TOTAL 1,500"), jaJp)).containsExactly(Money(1500, "JPY"))
    }

    @Test
    fun `test that invoke uses the default currency next to a three letter word that is not a currency`() {
        assertThat(underTest(listOf("TAX 10.00"), enUs)).containsExactly(Money(1000, "USD"))
    }

    @Test
    fun `test that invoke uses the default currency next to an English word that is a currency code`() {
        assertThat(underTest(listOf("CUP 3.00"), enUs)).containsExactly(Money(300, "USD"))
    }

    @Test
    fun `test that invoke puts the net amount first when the gross total comes after it`() {
        val lines = listOf("NET AMOUNT 950.00", "GROSS TOTAL 1,000.00")

        assertThat(underTest(lines, enIn).first()).isEqualTo(Money(95000, "INR"))
    }

    @Test
    fun `test that invoke puts the net amount first when a plain total comes after it`() {
        val lines = listOf("Net Payable 950.00", "Total 1,000.00")

        assertThat(underTest(lines, enIn).first()).isEqualTo(Money(95000, "INR"))
    }

    @Test
    fun `test that invoke puts the grand total before a plain total`() {
        val lines = listOf("Grand Total 1,180.00", "Total Amount 1,000.00")

        assertThat(underTest(lines, enIn).first()).isEqualTo(Money(118000, "INR"))
    }

    @Test
    fun `test that invoke puts the amount due before a plain total`() {
        val lines = listOf("Amount Due 45.00", "Total 50.00")

        assertThat(underTest(lines, enGb).first()).isEqualTo(Money(4500, "GBP"))
    }

    @Test
    fun `test that invoke never preselects a gross amount`() {
        val lines = listOf("Item 300.00", "Gross Amount 1,000.00")

        assertThat(underTest(lines, enIn).first()).isNotEqualTo(Money(100000, "INR"))
    }

    @Test
    fun `test that invoke keeps a gross amount as a candidate`() {
        val lines = listOf("Gross Amount 1,000.00", "Net Amount 950.00")

        assertThat(underTest(lines, enIn)).contains(Money(100000, "INR"))
    }

    @Test
    fun `test that invoke ignores a time range`() {
        assertThat(underTest(listOf("Exchange Time 1.00 to 4.00 pm. within 3 Days Only."), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke ignores a single time`() {
        assertThat(underTest(listOf("Open till 10.30 PM"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke reads a whole amount on a total line`() {
        assertThat(underTest(listOf("TOTAL 3050"), enIn)).containsExactly(Money(305000, "INR"))
    }

    @Test
    fun `test that invoke ignores a whole number on an item total line`() {
        assertThat(underTest(listOf("Total Items 4"), enIn)).isEmpty()
    }

    @Test
    fun `test that invoke preselects the amount after a bare NET label over a gross total`() {
        val lines = listOf("Gross Total 880.00", "Discount(Rs. 116.00", "NET 764.00")

        assertThat(underTest(lines, enIn).first()).isEqualTo(Money(76400, "INR"))
    }

    @Test
    fun `test that invoke does not treat a net weight as the amount paid`() {
        assertThat(underTest(listOf("Net Wt 2.50 kg", "TOTAL 120.00"), enIn).first()).isEqualTo(Money(12000, "INR"))
    }

    @Test
    fun `test that invoke ignores the parts of a hyphenated licence number`() {
        assertThat(underTest(listOf("D.L No: 21-115808,21B-115810. 20-115807"), enIn)).isEmpty()
    }
}
