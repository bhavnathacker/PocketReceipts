package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavnathacker.pocketreceipts.domain.receipt.DeleteReceiptUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.MonitorReceiptUseCase
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ReceiptDetailUiMapper
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Shows one receipt and deletes it after the user confirms. Closes itself when
 * the receipt no longer exists, whether deleted here or elsewhere.
 */
@HiltViewModel(assistedFactory = ReceiptDetailViewModel.Factory::class)
internal class ReceiptDetailViewModel @AssistedInject constructor(
    private val monitorReceiptUseCase: MonitorReceiptUseCase,
    private val deleteReceiptUseCase: DeleteReceiptUseCase,
    private val receiptDetailUiMapper: ReceiptDetailUiMapper,
    @Assisted private val receiptId: Long,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(receiptId: Long): ReceiptDetailViewModel
    }

    private val _uiState = MutableStateFlow(ReceiptDetailUiState())
    val uiState: StateFlow<ReceiptDetailUiState> = _uiState.asStateFlow()

    init {
        monitorReceipt()
    }

    private fun monitorReceipt() {
        viewModelScope.launch {
            monitorReceiptUseCase(receiptId)
                .catch {
                    Timber.e(it, "Failed to load receipt")
                    _uiState.update { state -> state.copy(isLoading = false, closeEvent = triggered) }
                }
                .collect { receipt ->
                    _uiState.update { state ->
                        if (receipt == null) {
                            state.copy(isLoading = false, closeEvent = triggered)
                        } else {
                            state.copy(isLoading = false, receipt = receiptDetailUiMapper(receipt))
                        }
                    }
                }
        }
    }

    fun onDeleteClick() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun onDeleteDismiss() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun onDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
        viewModelScope.launch {
            runCatching {
                deleteReceiptUseCase(receiptId)
            }.onSuccess {
                _uiState.update { it.copy(closeEvent = triggered) }
            }.onFailure {
                Timber.e(it, "Failed to delete receipt")
                _uiState.update { it.copy(deleteFailedEvent = triggered) }
            }
        }
    }

    fun onCloseEventConsumed() {
        _uiState.update { it.copy(closeEvent = consumed) }
    }

    fun onDeleteFailedEventConsumed() {
        _uiState.update { it.copy(deleteFailedEvent = consumed) }
    }
}
