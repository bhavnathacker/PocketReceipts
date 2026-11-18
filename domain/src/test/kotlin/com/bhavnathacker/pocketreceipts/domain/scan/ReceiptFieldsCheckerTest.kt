package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptValidator
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptFieldsCheckerTest {
    private lateinit var underTest: ReceiptFieldsChecker

    private val today = LocalDate.of(2026, 11, 18)

    @BeforeAll
    fun setUp() {
        underTest = ReceiptFieldsChecker()
    }

    private fun check(extracted: ExtractedReceipt) = underTest(extracted, today, defaultCurrency = "INR")

    @Test
    fun `test that invoke keeps every valid field`() {
        val extracted = ExtractedReceipt(
            merchant = "BHARAT MEDICAL STORES",
            date = "2026-10-31",
            total = "764.00",
            currency = "INR",
            category = "HEALTH",
        )

        assertThat(check(extracted)).isEqualTo(
            ReceiptScan(
                merchant = "BHARAT MEDICAL STORES",
                date = LocalDate.of(2026, 10, 31),
                total = Money(76400, "INR"),
                category = ReceiptCategory.HEALTH,
            ),
        )
    }

    @Test
    fun `test that invoke drops a date after today`() {
        assertThat(check(ExtractedReceipt(date = "2026-11-19")).date).isNull()
    }

    @Test
    fun `test that invoke keeps today's date`() {
        assertThat(check(ExtractedReceipt(date = "2026-11-18")).date).isEqualTo(today)
    }

    @Test
    fun `test that invoke drops a date that is not ISO-8601`() {
        assertThat(check(ExtractedReceipt(date = "31-Oct-26")).date).isNull()
    }

    @Test
    fun `test that invoke uses the receipt's currency for the total`() {
        assertThat(check(ExtractedReceipt(total = "4.50", currency = "eur")).total).isEqualTo(Money(450, "EUR"))
    }

    @Test
    fun `test that invoke uses the default currency when the currency is unknown`() {
        assertThat(check(ExtractedReceipt(total = "4.50", currency = "TAX")).total).isEqualTo(Money(450, "INR"))
    }

    @Test
    fun `test that invoke uses the default currency when there is no currency`() {
        assertThat(check(ExtractedReceipt(total = "3050")).total).isEqualTo(Money(305000, "INR"))
    }

    @Test
    fun `test that invoke converts a total to the currency's minor units`() {
        assertThat(check(ExtractedReceipt(total = "1500", currency = "JPY")).total).isEqualTo(Money(1500, "JPY"))
    }

    @Test
    fun `test that invoke drops a total that is not an amount`() {
        assertThat(check(ExtractedReceipt(total = "seven hundred")).total).isNull()
    }

    @Test
    fun `test that invoke drops a total that is not positive`() {
        assertThat(check(ExtractedReceipt(total = "-0.40")).total).isNull()
        assertThat(check(ExtractedReceipt(total = "0")).total).isNull()
    }

    @Test
    fun `test that invoke drops a total with more decimals than the currency has`() {
        assertThat(check(ExtractedReceipt(total = "4.505", currency = "EUR")).total).isNull()
    }

    @Test
    fun `test that invoke reads the category name in any case`() {
        assertThat(check(ExtractedReceipt(category = "shopping")).category).isEqualTo(ReceiptCategory.SHOPPING)
    }

    @Test
    fun `test that invoke drops an unknown category`() {
        assertThat(check(ExtractedReceipt(category = "MEDICAL")).category).isNull()
    }

    @Test
    fun `test that invoke drops a blank merchant`() {
        assertThat(check(ExtractedReceipt(merchant = "   ")).merchant).isNull()
    }

    @Test
    fun `test that invoke collapses whitespace in the merchant`() {
        assertThat(check(ExtractedReceipt(merchant = "  HIT-MAN\n Men's  Wear ")).merchant)
            .isEqualTo("HIT-MAN Men's Wear")
    }

    @Test
    fun `test that invoke cuts a long merchant to the form's limit`() {
        val merchant = check(ExtractedReceipt(merchant = "A".repeat(150))).merchant

        assertThat(merchant).hasLength(ReceiptValidator.MAX_MERCHANT_LENGTH)
    }

    @Test
    fun `test that invoke returns an empty scan when nothing was found`() {
        assertThat(check(ExtractedReceipt())).isEqualTo(ReceiptScan())
    }
}
