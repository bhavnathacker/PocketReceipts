package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.MonitorReceiptUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptFieldError
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.receipt.SaveReceiptResult
import com.bhavnathacker.pocketreceipts.domain.receipt.SaveReceiptUseCase
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.AmountInputFormatter
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.AmountInputParser
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ScanPrefillToReceiptScanMapper
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptFormUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

/**
 * The add/edit receipt form: adds a receipt when [receiptId] is `null`,
 * otherwise loads that receipt once and saves changes to it. With [scan], it
 * reviews a scanned receipt: the form starts from the values read from it, or
 * the manual-entry defaults for values that weren't read, and saving adds the receipt.
 */
@HiltViewModel(assistedFactory = ReceiptFormViewModel.Factory::class)
internal class ReceiptFormViewModel @AssistedInject constructor(
    private val monitorReceiptUseCase: MonitorReceiptUseCase,
    private val saveReceiptUseCase: SaveReceiptUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    getTodayUseCase: GetTodayUseCase,
    private val amountInputParser: AmountInputParser,
    private val amountInputFormatter: AmountInputFormatter,
    scanPrefillToReceiptScanMapper: ScanPrefillToReceiptScanMapper,
    @Assisted private val receiptId: Long?,
    @Assisted scan: ScanPrefill?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(receiptId: Long?, scan: ScanPrefill?): ReceiptFormViewModel
    }

    private val _uiState = MutableStateFlow(
        getTodayUseCase().let { today ->
            val initial = ReceiptFormUiState(
                isEditing = receiptId != null,
                isLoading = receiptId != null,
                today = today,
                currencyCode = getDefaultCurrencyUseCase(),
                purchaseDate = today,
            )
            if (scan == null) initial else initial.prefilledFrom(scanPrefillToReceiptScanMapper(scan))
        },
    )
    val uiState: StateFlow<ReceiptFormUiState> = _uiState.asStateFlow()

    init {
        if (receiptId != null) loadReceipt(receiptId)
    }

    private fun loadReceipt(id: Long) {
        viewModelScope.launch {
            runCatching {
                monitorReceiptUseCase(id).first()
            }.onSuccess { receipt ->
                _uiState.update { state ->
                    if (receipt == null) {
                        state.copy(isLoading = false, closeEvent = triggered)
                    } else {
                        state.copy(
                            isLoading = false,
                            merchant = receipt.merchant,
                            amount = amountInputFormatter(receipt.total),
                            currencyCode = receipt.total.currencyCode,
                            purchaseDate = receipt.purchaseDate,
                            category = receipt.category,
                            note = receipt.note.orEmpty(),
                        )
                    }
                }
            }.onFailure {
                Timber.e(it, "Failed to load receipt for editing")
                _uiState.update { it.copy(isLoading = false, closeEvent = triggered) }
            }
        }
    }

    fun onMerchantChange(merchant: String) {
        _uiState.update { it.copy(merchant = merchant, merchantError = null) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount, amountError = null) }
    }

    fun onCurrencyChange(currencyCode: String) {
        _uiState.update { it.copy(currencyCode = currencyCode, amountError = null) }
    }

    /** Dates after today are ignored, so the form always keeps a valid date. */
    fun onDateChange(date: LocalDate) {
        _uiState.update { state ->
            if (date.isAfter(state.today)) state else state.copy(purchaseDate = date, dateError = null)
        }
    }

    fun onCategoryChange(category: ReceiptCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note, noteError = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        _uiState.update { it.copy(isSaving = true) }

        val parsedAmount = amountInputParser(state.amount, state.currencyCode)
        val input = ReceiptInput(
            merchant = state.merchant,
            purchaseDate = state.purchaseDate,
            // An unparseable amount is sent as 0 so the other fields are still validated.
            total = Money(minorUnits = parsedAmount ?: 0, currencyCode = state.currencyCode),
            category = state.category,
            note = state.note.takeIf { it.isNotBlank() },
        )
        viewModelScope.launch {
            runCatching {
                saveReceiptUseCase(receiptId, input)
            }.onSuccess { result ->
                _uiState.update {
                    when (result) {
                        is SaveReceiptResult.Saved -> it.copy(isSaving = false, closeEvent = triggered)
                        is SaveReceiptResult.Invalid -> it.withErrors(result.errors, amountUnparseable = parsedAmount == null)
                    }
                }
            }.onFailure {
                Timber.e(it, "Failed to save receipt")
                _uiState.update { it.copy(isSaving = false, saveFailedEvent = triggered) }
            }
        }
    }

    fun onNothingRecognizedEventConsumed() {
        _uiState.update { it.copy(nothingRecognizedEvent = consumed) }
    }

    fun onCloseEventConsumed() {
        _uiState.update { it.copy(closeEvent = consumed) }
    }

    fun onSaveFailedEventConsumed() {
        _uiState.update { it.copy(saveFailedEvent = consumed) }
    }

    private fun ReceiptFormUiState.prefilledFrom(scan: ReceiptScan): ReceiptFormUiState {
        val date = scan.date?.takeUnless { it.isAfter(today) }
        val total = scan.total
        val nothingRecognized = scan.merchant == null && date == null && total == null
        return copy(
            isReview = true,
            merchant = scan.merchant.orEmpty(),
            purchaseDate = date ?: today,
            amount = total?.let { amountInputFormatter(it) }.orEmpty(),
            currencyCode = total?.currencyCode ?: currencyCode,
            category = scan.category ?: ReceiptCategory.OTHER,
            nothingRecognizedEvent = if (nothingRecognized) triggered else consumed,
        )
    }

    private fun ReceiptFormUiState.withErrors(
        errors: Set<ReceiptFieldError>,
        amountUnparseable: Boolean,
    ) = copy(
        isSaving = false,
        merchantError = when {
            ReceiptFieldError.MerchantBlank in errors -> R.string.receipt_form_error_merchant_blank
            ReceiptFieldError.MerchantTooLong in errors -> R.string.receipt_form_error_merchant_too_long
            else -> null
        },
        amountError = when {
            amountUnparseable -> R.string.receipt_form_error_amount_invalid
            ReceiptFieldError.TotalNotPositive in errors -> R.string.receipt_form_error_amount_not_positive
            else -> null
        },
        dateError = R.string.receipt_form_error_date_future.takeIf { ReceiptFieldError.DateInFuture in errors },
        noteError = R.string.receipt_form_error_note_too_long.takeIf { ReceiptFieldError.NoteTooLong in errors },
    )
}
