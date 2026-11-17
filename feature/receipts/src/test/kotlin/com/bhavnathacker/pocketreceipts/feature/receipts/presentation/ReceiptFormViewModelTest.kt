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
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ScanPrefillToReceiptScanMapper
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

    private val scan = ScanPrefill(
        merchant = "Corner Shop",
        dates = listOf("2026-02-03", "2026-01-30"),
        totals = listOf(ScanAmount(21000, "GBP"), ScanAmount(20000, "GBP")),
    )
    private val emptyScan = ScanPrefill(merchant = null, dates = emptyList(), totals = emptyList())

    private fun initViewModel(receiptId: Long? = null, scan: ScanPrefill? = null) {
        underTest = ReceiptFormViewModel(
            monitorReceiptUseCase = monitorReceiptUseCase,
            saveReceiptUseCase = saveReceiptUseCase,
            getDefaultCurrencyUseCase = getDefaultCurrencyUseCase,
            getTodayUseCase = getTodayUseCase,
            amountInputParser = amountInputParser,
            amountInputFormatter = amountInputFormatter,
            scanPrefillToReceiptScanMapper = ScanPrefillToReceiptScanMapper(),
            receiptId = receiptId,
            scan = scan,
        )
    }

    private fun stubFormattedTotals() {
        amountInputFormatter.stub {
            on { invoke(org.mockito.kotlin.eq(Money(21000, "GBP")), any()) } doReturn "210.00"
            on { invoke(org.mockito.kotlin.eq(Money(20000, "GBP")), any()) } doReturn "200.00"
        }
    }

    private fun fillValidForm() {
        underTest.onMerchantChange("Corner Shop")
        underTest.onAmountChange("4.50")
    }

    private fun stubInvalid(vararg errors: ReceiptFieldError) {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doReturn SaveReceiptResult.Invalid(errors.toSet())
        }
    }

    private fun stubSaveThrows() {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doSuspendableAnswer { throw IllegalStateException("db") }
        }
    }

    @Test
    fun `test that init sets the purchase date to today when adding a receipt`() = runTest {
        initViewModel()

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(today)
    }

    @Test
    fun `test that init sets the default currency when adding a receipt`() = runTest {
        initViewModel()

        assertThat(underTest.uiState.value.currencyCode).isEqualTo("INR")
    }

    @Test
    fun `test that init sets the category to Other when adding a receipt`() = runTest {
        initViewModel()

        assertThat(underTest.uiState.value.category).isEqualTo(ReceiptCategory.OTHER)
    }

    @Test
    fun `test that init leaves the text fields empty when adding a receipt`() = runTest {
        initViewModel()

        val state = underTest.uiState.value
        assertThat(listOf(state.merchant, state.amount, state.note)).containsExactly("", "", "")
    }

    @Test
    fun `test that init is not in edit mode when adding a receipt`() = runTest {
        initViewModel()

        assertThat(underTest.uiState.value.isEditing).isFalse()
    }

    @Test
    fun `test that init is in edit mode when editing a receipt`() = runTest {
        initViewModel(receiptId = 7)

        assertThat(underTest.uiState.value.isEditing).isTrue()
    }

    @Test
    fun `test that init pre-fills the form with the receipt when editing a receipt`() = runTest {
        initViewModel(receiptId = 7)

        val state = underTest.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.merchant).isEqualTo("Corner Shop")
        assertThat(state.amount).isEqualTo("123456.50")
        assertThat(state.currencyCode).isEqualTo("INR")
        assertThat(state.purchaseDate).isEqualTo(LocalDate.of(2026, 3, 1))
        assertThat(state.category).isEqualTo(ReceiptCategory.GROCERIES)
        assertThat(state.note).isEqualTo("milk")
    }

    @Test
    fun `test that init fires closeEvent when the edited receipt is not found`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flowOf(null) }
        initViewModel(receiptId = 7)

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onMerchantChange updates the merchant`() = runTest {
        initViewModel()

        underTest.onMerchantChange("Corner Shop")

        assertThat(underTest.uiState.value.merchant).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that onAmountChange updates the amount`() = runTest {
        initViewModel()

        underTest.onAmountChange("4.50")

        assertThat(underTest.uiState.value.amount).isEqualTo("4.50")
    }

    @Test
    fun `test that onCurrencyChange updates the currency`() = runTest {
        initViewModel()

        underTest.onCurrencyChange("EUR")

        assertThat(underTest.uiState.value.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun `test that onCategoryChange updates the category`() = runTest {
        initViewModel()

        underTest.onCategoryChange(ReceiptCategory.DINING)

        assertThat(underTest.uiState.value.category).isEqualTo(ReceiptCategory.DINING)
    }

    @Test
    fun `test that onNoteChange updates the note`() = runTest {
        initViewModel()

        underTest.onNoteChange("lunch")

        assertThat(underTest.uiState.value.note).isEqualTo("lunch")
    }

    @Test
    fun `test that onDateChange updates the purchase date`() = runTest {
        initViewModel()

        underTest.onDateChange(LocalDate.of(2026, 3, 2))

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(LocalDate.of(2026, 3, 2))
    }

    @Test
    fun `test that onDateChange ignores a date after today`() = runTest {
        initViewModel()

        underTest.onDateChange(today.plusDays(1))

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(today)
    }

    @Test
    fun `test that onMerchantChange does not reload the edited receipt`() = runTest {
        initViewModel(receiptId = 7)

        underTest.onMerchantChange("Changed")

        assertThat(underTest.uiState.value.merchant).isEqualTo("Changed")
    }

    @Test
    fun `test that onSave saves a new receipt with the parsed amount`() = runTest {
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
    }

    @Test
    fun `test that onSave fires closeEvent when the receipt is saved`() = runTest {
        initViewModel()
        fillValidForm()

        underTest.onSave()

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
        stubInvalid(ReceiptFieldError.TotalNotPositive)
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.amountError).isEqualTo(R.string.receipt_form_error_amount_invalid)
    }

    @Test
    fun `test that onSave shows the zero amount error when the amount is zero`() = runTest {
        amountInputParser.stub { on { invoke(any(), any(), any()) } doReturn 0L }
        stubInvalid(ReceiptFieldError.TotalNotPositive)
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.amountError).isEqualTo(R.string.receipt_form_error_amount_not_positive)
    }

    @Test
    fun `test that onSave shows an error for every invalid field`() = runTest {
        stubInvalid(ReceiptFieldError.MerchantBlank, ReceiptFieldError.NoteTooLong, ReceiptFieldError.DateInFuture)
        initViewModel()

        underTest.onSave()

        val state = underTest.uiState.value
        assertThat(state.merchantError).isEqualTo(R.string.receipt_form_error_merchant_blank)
        assertThat(state.noteError).isEqualTo(R.string.receipt_form_error_note_too_long)
        assertThat(state.dateError).isEqualTo(R.string.receipt_form_error_date_future)
    }

    @Test
    fun `test that onSave shows no error for a valid field when other fields are invalid`() = runTest {
        stubInvalid(ReceiptFieldError.MerchantBlank)
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.amountError).isNull()
    }

    @Test
    fun `test that onSave shows the merchant too long error`() = runTest {
        stubInvalid(ReceiptFieldError.MerchantTooLong)
        initViewModel()

        underTest.onSave()

        assertThat(underTest.uiState.value.merchantError).isEqualTo(R.string.receipt_form_error_merchant_too_long)
    }

    @Test
    fun `test that onSave does not fire closeEvent when validation fails`() = runTest {
        stubInvalid(ReceiptFieldError.MerchantBlank)
        initViewModel()

        underTest.onSave()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onMerchantChange clears the merchant error`() = runTest {
        stubInvalid(ReceiptFieldError.MerchantBlank)
        initViewModel()
        underTest.onSave()

        underTest.onMerchantChange("C")

        assertThat(underTest.uiState.value.merchantError).isNull()
    }

    @Test
    fun `test that onAmountChange clears the amount error`() = runTest {
        stubInvalid(ReceiptFieldError.TotalNotPositive)
        initViewModel()
        underTest.onSave()

        underTest.onAmountChange("1")

        assertThat(underTest.uiState.value.amountError).isNull()
    }

    @Test
    fun `test that onNoteChange clears the note error`() = runTest {
        stubInvalid(ReceiptFieldError.NoteTooLong)
        initViewModel()
        underTest.onSave()

        underTest.onNoteChange("n")

        assertThat(underTest.uiState.value.noteError).isNull()
    }

    @Test
    fun `test that onSave sets isSaving while the save is in progress`() = runTest {
        saveReceiptUseCase.stub {
            onBlocking { invoke(anyOrNull(), any()) } doSuspendableAnswer { kotlinx.coroutines.awaitCancellation() }
        }
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.isSaving).isTrue()
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

        verify(saveReceiptUseCase).invoke(anyOrNull(), any())
    }

    @Test
    fun `test that onSave fires saveFailedEvent when saving throws`() = runTest {
        stubSaveThrows()
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.saveFailedEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onSave clears isSaving when saving throws`() = runTest {
        stubSaveThrows()
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `test that onSave does not fire closeEvent when saving throws`() = runTest {
        stubSaveThrows()
        initViewModel()
        fillValidForm()

        underTest.onSave()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onCloseEventConsumed resets closeEvent`() = runTest {
        initViewModel()
        fillValidForm()
        underTest.onSave()

        underTest.onCloseEventConsumed()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onSaveFailedEventConsumed resets saveFailedEvent`() = runTest {
        stubSaveThrows()
        initViewModel()
        fillValidForm()
        underTest.onSave()

        underTest.onSaveFailedEventConsumed()

        assertThat(underTest.uiState.value.saveFailedEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that init marks the form as a review when a scan is given`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.isReview).isTrue()
    }

    @Test
    fun `test that init does not mark the form as a review without a scan`() {
        initViewModel()

        assertThat(underTest.uiState.value.isReview).isFalse()
    }

    @Test
    fun `test that init prefills the merchant from the scan`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.merchant).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that init prefills the first scanned date`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that init prefills the first scanned total as amount text`() {
        stubFormattedTotals()
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.amount).isEqualTo("210.00")
    }

    @Test
    fun `test that init prefills the currency of the first scanned total`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.currencyCode).isEqualTo("GBP")
    }

    @Test
    fun `test that init sets category Other for a scan`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.category).isEqualTo(ReceiptCategory.OTHER)
    }

    @Test
    fun `test that init exposes the scanned dates as candidates`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.dateCandidates)
            .containsExactly(LocalDate.of(2026, 2, 3), LocalDate.of(2026, 1, 30))
            .inOrder()
    }

    @Test
    fun `test that init exposes the scanned totals as candidates`() {
        initViewModel(scan = scan)

        assertThat(underTest.uiState.value.totalCandidates)
            .containsExactly(Money(21000, "GBP"), Money(20000, "GBP"))
            .inOrder()
    }

    @Test
    fun `test that init uses today when the scan has no dates`() {
        initViewModel(scan = emptyScan)

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(today)
    }

    @Test
    fun `test that init leaves the amount empty when the scan has no totals`() {
        initViewModel(scan = emptyScan)

        assertThat(underTest.uiState.value.amount).isEmpty()
    }

    @Test
    fun `test that init uses the default currency when the scan has no totals`() {
        initViewModel(scan = emptyScan)

        assertThat(underTest.uiState.value.currencyCode).isEqualTo("INR")
    }

    @Test
    fun `test that init leaves the merchant empty when the scan has none`() {
        initViewModel(scan = emptyScan)

        assertThat(underTest.uiState.value.merchant).isEmpty()
    }

    @Test
    fun `test that init fires nothingRecognizedEvent when the scan is empty`() {
        initViewModel(scan = emptyScan)

        assertThat(underTest.uiState.value.nothingRecognizedEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that init does not fire nothingRecognizedEvent when the scan found a merchant`() {
        initViewModel(scan = emptyScan.copy(merchant = "Corner Shop"))

        assertThat(underTest.uiState.value.nothingRecognizedEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onNothingRecognizedEventConsumed consumes the event`() {
        initViewModel(scan = emptyScan)

        underTest.onNothingRecognizedEventConsumed()

        assertThat(underTest.uiState.value.nothingRecognizedEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onDateCandidateClick sets the purchase date`() {
        initViewModel(scan = scan)

        underTest.onDateCandidateClick(LocalDate.of(2026, 1, 30))

        assertThat(underTest.uiState.value.purchaseDate).isEqualTo(LocalDate.of(2026, 1, 30))
    }

    @Test
    fun `test that onDateCandidateClick clears the date error`() = runTest {
        stubInvalid(ReceiptFieldError.DateInFuture)
        initViewModel(scan = scan)
        underTest.onSave()

        underTest.onDateCandidateClick(LocalDate.of(2026, 1, 30))

        assertThat(underTest.uiState.value.dateError).isNull()
    }

    @Test
    fun `test that onTotalCandidateClick sets the amount text`() {
        stubFormattedTotals()
        initViewModel(scan = scan)

        underTest.onTotalCandidateClick(Money(20000, "GBP"))

        assertThat(underTest.uiState.value.amount).isEqualTo("200.00")
    }

    @Test
    fun `test that onTotalCandidateClick sets the currency`() {
        initViewModel(scan = scan)

        underTest.onTotalCandidateClick(Money(450, "EUR"))

        assertThat(underTest.uiState.value.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun `test that onTotalCandidateClick clears the amount error`() = runTest {
        stubInvalid(ReceiptFieldError.TotalNotPositive)
        initViewModel(scan = scan)
        underTest.onSave()

        underTest.onTotalCandidateClick(Money(20000, "GBP"))

        assertThat(underTest.uiState.value.amountError).isNull()
    }

    @Test
    fun `test that onSave adds a new receipt when reviewing a scan`() = runTest {
        initViewModel(scan = scan)

        underTest.onSave()

        verify(saveReceiptUseCase).invoke(org.mockito.kotlin.isNull(), any())
    }
}
