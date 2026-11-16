package com.bhavnathacker.pocketreceipts.domain.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptValidatorTest {
    private lateinit var underTest: ReceiptValidator

    private val today = LocalDate.of(2026, 3, 3)
    private val clock = Clock.fixed(Instant.parse("2026-03-03T10:00:00Z"), ZoneOffset.UTC)

    private val valid = ReceiptInput(
        merchant = "Corner Shop",
        purchaseDate = today,
        total = Money(minorUnits = 450, currencyCode = "EUR"),
        category = ReceiptCategory.OTHER,
        note = null,
    )

    @BeforeAll
    fun setUp() {
        underTest = ReceiptValidator(clock = clock)
    }

    @Test
    fun `test that invoke returns no errors when input is valid`() {
        assertThat(underTest(valid)).isEmpty()
    }

    @Test
    fun `test that invoke returns MerchantBlank when merchant is empty`() {
        assertThat(underTest(valid.copy(merchant = ""))).containsExactly(ReceiptFieldError.MerchantBlank)
    }

    @Test
    fun `test that invoke returns MerchantBlank when merchant is only whitespace`() {
        assertThat(underTest(valid.copy(merchant = "   "))).containsExactly(ReceiptFieldError.MerchantBlank)
    }

    @Test
    fun `test that invoke accepts merchant of max length after trimming`() {
        val merchant = "  " + "a".repeat(ReceiptValidator.MAX_MERCHANT_LENGTH) + "  "

        assertThat(underTest(valid.copy(merchant = merchant))).isEmpty()
    }

    @Test
    fun `test that invoke returns MerchantTooLong when merchant exceeds max length after trimming`() {
        val merchant = "a".repeat(ReceiptValidator.MAX_MERCHANT_LENGTH + 1)

        assertThat(underTest(valid.copy(merchant = merchant))).containsExactly(ReceiptFieldError.MerchantTooLong)
    }

    @Test
    fun `test that invoke returns TotalNotPositive when total is zero`() {
        val input = valid.copy(total = Money(minorUnits = 0, currencyCode = "EUR"))

        assertThat(underTest(input)).containsExactly(ReceiptFieldError.TotalNotPositive)
    }

    @Test
    fun `test that invoke returns TotalNotPositive when total is negative`() {
        val input = valid.copy(total = Money(minorUnits = -1, currencyCode = "EUR"))

        assertThat(underTest(input)).containsExactly(ReceiptFieldError.TotalNotPositive)
    }

    @Test
    fun `test that invoke accepts a purchase date of today`() {
        assertThat(underTest(valid.copy(purchaseDate = today))).isEmpty()
    }

    @Test
    fun `test that invoke returns DateInFuture when purchase date is after today`() {
        val input = valid.copy(purchaseDate = today.plusDays(1))

        assertThat(underTest(input)).containsExactly(ReceiptFieldError.DateInFuture)
    }

    @Test
    fun `test that invoke accepts note of max length`() {
        val input = valid.copy(note = "n".repeat(ReceiptValidator.MAX_NOTE_LENGTH))

        assertThat(underTest(input)).isEmpty()
    }

    @Test
    fun `test that invoke returns NoteTooLong when note exceeds max length after trimming`() {
        val input = valid.copy(note = "n".repeat(ReceiptValidator.MAX_NOTE_LENGTH + 1))

        assertThat(underTest(input)).containsExactly(ReceiptFieldError.NoteTooLong)
    }

    @Test
    fun `test that invoke returns every error when several fields are invalid`() {
        val input = valid.copy(
            merchant = " ",
            purchaseDate = today.plusDays(3),
            total = Money(minorUnits = 0, currencyCode = "EUR"),
            note = "n".repeat(ReceiptValidator.MAX_NOTE_LENGTH + 1),
        )

        assertThat(underTest(input)).containsExactly(
            ReceiptFieldError.MerchantBlank,
            ReceiptFieldError.TotalNotPositive,
            ReceiptFieldError.DateInFuture,
            ReceiptFieldError.NoteTooLong,
        )
    }
}
