package com.bhavnathacker.pocketreceipts.domain.receipt

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveReceiptUseCaseTest {
    private lateinit var underTest: SaveReceiptUseCase

    private val receiptRepository = mock<ReceiptRepository>()
    private val receiptValidator = mock<ReceiptValidator>()

    private val input = ReceiptInput(
        merchant = "Corner Shop",
        purchaseDate = LocalDate.of(2026, 3, 3),
        total = Money(450, "EUR"),
        category = ReceiptCategory.GROCERIES,
        note = "milk",
    )

    @BeforeAll
    fun setUp() {
        underTest = SaveReceiptUseCase(
            receiptRepository = receiptRepository,
            receiptValidator = receiptValidator,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(receiptRepository, receiptValidator)
        whenever(receiptValidator(any())).thenReturn(emptySet())
    }

    @Test
    fun `test that invoke adds the receipt and returns its id when id is null`() = runTest {
        whenever(receiptRepository.addReceipt(input)).thenReturn(42)

        val result = underTest(id = null, input = input)

        assertThat(result).isEqualTo(SaveReceiptResult.Saved(42))
    }

    @Test
    fun `test that invoke updates the receipt and returns its id when id is given`() = runTest {
        val result = underTest(id = 7, input = input)

        verify(receiptRepository).updateReceipt(7, input)
        assertThat(result).isEqualTo(SaveReceiptResult.Saved(7))
    }

    @Test
    fun `test that invoke trims merchant and note before validating and saving`() = runTest {
        val trimmed = input.copy(merchant = "Corner Shop", note = "milk")
        whenever(receiptRepository.addReceipt(trimmed)).thenReturn(1)

        underTest(id = null, input = input.copy(merchant = "  Corner Shop  ", note = "  milk "))

        verify(receiptValidator).invoke(trimmed)
        verify(receiptRepository).addReceipt(trimmed)
    }

    @Test
    fun `test that invoke saves a null note when note is only whitespace`() = runTest {
        val expected = input.copy(note = null)
        whenever(receiptRepository.addReceipt(expected)).thenReturn(1)

        underTest(id = null, input = input.copy(note = "   "))

        verify(receiptRepository).addReceipt(expected)
    }

    @Test
    fun `test that invoke returns Invalid without saving when validation fails`() = runTest {
        val errors = setOf(ReceiptFieldError.MerchantBlank, ReceiptFieldError.TotalNotPositive)
        whenever(receiptValidator(any())).thenReturn(errors)

        val result = underTest(id = 7, input = input)

        assertThat(result).isEqualTo(SaveReceiptResult.Invalid(errors))
        verifyNoInteractions(receiptRepository)
    }
}
