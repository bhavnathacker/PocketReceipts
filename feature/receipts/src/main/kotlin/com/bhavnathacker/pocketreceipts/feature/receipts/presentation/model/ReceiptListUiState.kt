package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model

import androidx.compose.runtime.Stable

/**
 * What the receipt list shows.
 */
@Stable
sealed interface ReceiptListUiState {

    /** Receipts are being loaded. */
    data object Loading : ReceiptListUiState

    /** There are no receipts yet. */
    data object Empty : ReceiptListUiState

    /** The receipts, newest first. */
    data class Receipts(val items: List<ReceiptListItemUiModel>) : ReceiptListUiState

    /** The receipts couldn't be opened. */
    data object Error : ReceiptListUiState
}
