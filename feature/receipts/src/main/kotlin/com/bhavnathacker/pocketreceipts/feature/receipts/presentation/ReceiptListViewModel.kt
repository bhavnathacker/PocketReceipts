package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavnathacker.pocketreceipts.domain.receipt.MonitorReceiptsUseCase
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ReceiptListItemUiMapper
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ReceiptListViewModel @Inject constructor(
    private val monitorReceiptsUseCase: MonitorReceiptsUseCase,
    private val receiptListItemUiMapper: ReceiptListItemUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptListUiState>(ReceiptListUiState.Loading)
    val uiState: StateFlow<ReceiptListUiState> = _uiState.asStateFlow()

    init {
        monitorReceipts()
    }

    private fun monitorReceipts() {
        viewModelScope.launch {
            monitorReceiptsUseCase()
                .catch {
                    Timber.e(it, "Failed to load receipts")
                    _uiState.update { ReceiptListUiState.Error }
                }
                .collect { receipts ->
                    _uiState.update {
                        if (receipts.isEmpty()) {
                            ReceiptListUiState.Empty
                        } else {
                            ReceiptListUiState.Receipts(receiptListItemUiMapper(receipts))
                        }
                    }
                }
        }
    }
}
