package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.MonitorReceiptUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptFieldError
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.receipt.SaveReceiptResult
import com.bhavnathacker.pocketreceipts.domain.receipt.SaveReceiptUseCase
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.AmountInputFormatter
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.AmountInputParser
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.time.LocalDate

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ReceiptFormViewModelTest {
    private lateinit var underTest: ReceiptFormViewModel

    private val monitorReceiptUseCase = mock<MonitorReceiptUseCase>()
    private val saveReceiptUseCase = mock<SaveReceiptUseCase>()
    private val getDefaultCurrencyUseCase = mock<GetDefaultCurrencyUseCase>()
    private val getTodayUseCase = mock<GetTodayUseCase>()
    private val amountInputParser = mock<AmountInputParser>()
    private val amountInputFormatter = mock<AmountInputFormatter>()

    private val today = LocalDate.of(2026, 3, 3)
    private val receipt = Receipt(
        id = 7,
        merchant = "Corner Shop",
        purchaseDate = LocalDate.of(2026, 3, 1),
        total = Money(12345650, "INR"),
        category = ReceiptCategory.GROCERIES,
        note = "milk",
    )

    @BeforeEach
    fun setUp() {
        getTodayUseCase.stub { on { invoke() } doReturn today }
        getDefaultCurrencyUseCase.stub { on { invoke(any()) } doReturn "INR" }
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flowOf(receipt) }
        amountInputFormatter.stub { on { invoke(any(), any()) } doReturn "123456.50" }
        amountInputParser.stub { on { invoke(any(), any(), any()) } doReturn 450L }
        saveReceiptUseCase.stub { onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Saved(1) }
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorReceiptUseCase,
            saveReceiptUseCase,
            getDefaultCurrencyUseCase,
            getTodayUseCase,
            amountInputParser,
            amountInputFormatter,
        )
    }

    private fun initViewModel(receiptId: Long? = null) {
        underTest = ReceiptFormViewModel(
            monitorReceiptUseCase = monitorReceiptUseCase,
            saveReceiptUseCase = saveReceiptUseCase,
            getDefaultCurrencyUseCase = getDefaultCurrencyUseCase,
            getTodayUseCase = getTodayUseCase,
            amountInputParser = amountInputParser,
            amountInputFormatter = amountInputFormatter,
            receiptId = receiptId,
        )
    }

    private fun fillValidForm() {
        underTest.onMerchantChange("Corner Shop")
        underTest.onAmountChange("4.50")
    }

    @Test
    fun `test that a new receipt starts with today, the default currency and Other`() = runTest {
        initViewModel()

        val state = underTest.uiState.value
        assertThat(state.isEditing).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.merchant).isEmpty()
        assertThat(state.amount).isEmpty()
        assertThat(state.purchaseDate).isEqualTo(today)
        assertThat(state.today).isEqualTo(today)
        assertThat(state.currencyCode).isEqualTo("INR")
        assertThat(state.category).isEqualTo(ReceiptCategory.OTHER)
        assertThat(state.note).isEmpty()
    }

    @Test
    fun `test that editing pre-fills the form with the receipt`() = runTest {
        initViewModel(receiptId = 7)

        val state = underTest.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.merchant).isEqualTo("Corner Shop")
        assertThat(state.amount).isEqualTo("123456.50")
        assertThat(state.currencyCode).isEqualTo("INR")
        assertThat(state.purchaseDate).isEqualTo(LocalDate.of(2026, 3, 1))
        assertThat(state.category).isEqualTo(ReceiptCategory.GROCERIES)
        assertThat(state.note).isEqualTo("milk")
    }

    @Test
    fun `test that editing fires closeEvent when receipt not found`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flowOf(null) }
        initViewModel(receiptId = 7)

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that field changes update the state`() = runTest {
        initViewModel()

        underTest.onMerchantChange("Corner Shop")
        underTest.onAmountChange("4.50")
        underTest.onCurrencyChange("EUR")
        underTest.onCategoryChange(ReceiptCategory.DINING)
        underTest.onNoteChange("lunch")
        underTest.onDateChange(LocalDate.of(2026, 3, 2))

        val state = underTest.uiState.value
        assertThat(state.merchant).isEqualTo("Corner Shop")
        assertThat(state.amount).isEqualTo("4.50")
        assertThat(state.currencyCode).isEqualTo("EUR")
        assertThat(state.category).isEqualTo(ReceiptCategory.DINING)
        assertThat(state.note).isEqualTo("lunch")
        assertThat(state.purchaseDate).isEqualTo(LocalDate.of(2026, 3, 2))
    }

    @Test
    fun `test that onDateChange ignores a date after today`() = runTest {
        initViewModel()

        underTest.onDateChange(today.plusDays(1))

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(today)
    }

    @Test
    fun `test that onSave saves a new receipt with the parsed amount and fires closeEvent`() = runTest {
        initViewModel()
        fillValidForm()
        underTest.onNoteChange("lunch")

        underTest.onSave()

        verify(saveReceiptUseCase).invoke(
            null,
            ReceiptInput(
                merchant = "Corner Shop",
                purchaseDate = today,
                total = Money(450, "INR"),
                category = ReceiptCategory.OTHER,
                note = "lunch",
            ),
        )
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onSave updates the edited receipt`() = runTest {
        initViewModel(receiptId = 7)

        underTest.onSave()

        verify(saveReceiptUseCase).invoke(
            7,
            ReceiptInput(
                merchant = "Corner Shop",
                purchaseDate = LocalDate.of(2026, 3, 1),
                total = Money(450, "INR"),
                category = ReceiptCategory.GROCERIES,
                note = "milk",
            ),
        )
    }

    @Test
    fun `test that onSave saves a blank note as null`() = runTest {
        initViewModel()
        fillValidForm()

        underTest.onSave()

        verify(saveReceiptUseCase).invoke(
            null,
            ReceiptInput("Corner Shop", today, Money(450, "INR"), ReceiptCategory.OTHER, note = null),
        )
    }

    @Test
    fun `test that onSave shows the invalid amount error when the amount cannot be parsed`() = runTest {
        amountInputParser.stub { on { invoke(any(), any(), any()) } doReturn null }
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Invalid(setOf(ReceiptFieldError.TotalNotPositive))
        }
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.amountError).isEqualTo(R.string.receipt_form_error_amount_invalid)
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onSave shows the zero amount error when the amount is zero`() = runTest {
        amountInputParser.stub { on { invoke(any(), any(), any()) } doReturn 0L }
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Invalid(setOf(ReceiptFieldError.TotalNotPositive))
        }
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.amountError).isEqualTo(R.string.receipt_form_error_amount_not_positive)
    }

    @Test
    fun `test that onSave shows an error for every invalid field`() = runTest {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Invalid(
                setOf(ReceiptFieldError.MerchantBlank, ReceiptFieldError.NoteTooLong, ReceiptFieldError.DateInFuture),
            )
        }
        initViewModel()

        underTest.onSave()

        val state = underTest.uiState.value
        assertThat(state.merchantError).isEqualTo(R.string.receipt_form_error_merchant_blank)
        assertThat(state.noteError).isEqualTo(R.string.receipt_form_error_note_too_long)
        assertThat(state.dateError).isEqualTo(R.string.receipt_form_error_date_future)
        assertThat(state.amountError).isNull()
        assertThat(state.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onSave shows the merchant too long error`() = runTest {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Invalid(setOf(ReceiptFieldError.MerchantTooLong))
        }
        initViewModel()

        underTest.onSave()

        assertThat(underTest.uiState.value.merchantError).isEqualTo(R.string.receipt_form_error_merchant_too_long)
    }

    @Test
    fun `test that editing a field clears its error`() = runTest {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Invalid(
                setOf(ReceiptFieldError.MerchantBlank, ReceiptFieldError.TotalNotPositive, ReceiptFieldError.NoteTooLong),
            )
        }
        initViewModel()
        underTest.onSave()

        underTest.onMerchantChange("C")
        underTest.onAmountChange("1")
        underTest.onNoteChange("n")

        val state = underTest.uiState.value
        assertThat(state.merchantError).isNull()
        assertThat(state.amountError).isNull()
        assertThat(state.noteError).isNull()
    }

    @Test
    fun `test that onSave does nothing while a save is in progress`() = runTest {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doSuspendableAnswer { kotlinx.coroutines.awaitCancellation() }
        }
        initViewModel()
        fillValidForm()

        underTest.onSave()
        underTest.onSave()

        assertThat(underTest.uiState.value.isSaving).isTrue()
        verify(saveReceiptUseCase).invoke(anyOrNull(), any())
    }

    @Test
    fun `test that onSave fires saveFailedEvent when saving throws`() = runTest {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doSuspendableAnswer { throw IllegalStateException("db") }
        }
        initViewModel()
        fillValidForm()

        underTest.onSave()

        val state = underTest.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.saveFailedEvent).isEqualTo(StateEvent.Triggered)
        assertThat(state.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that consuming events resets them`() = runTest {
        initViewModel()
        fillValidForm()
        underTest.onSave()

        underTest.onCloseEventConsumed()
        underTest.onSaveFailedEventConsumed()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
        assertThat(underTest.uiState.value.saveFailedEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that the edit form does not reload after the user starts typing`() = runTest {
        initViewModel(receiptId = 7)

        underTest.onMerchantChange("Changed")

        assertThat(underTest.uiState.value.merchant).isEqualTo("Changed")
        verify(saveReceiptUseCase, never()).invoke(anyOrNull(), any())
    }
}
