package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * A receipt's detail screen.
 *
 * @property closeEvent The screen should close: the receipt was deleted or no longer exists.
 * @property deleteFailedEvent Deleting failed unexpectedly; the receipt is still there.
 */
@Stable
data class ReceiptDetailUiState(
    val isLoading: Boolean = true,
    val receipt: ReceiptDetailUiModel? = null,
    val showDeleteConfirmation: Boolean = false,
    val closeEvent: StateEvent = consumed,
    val deleteFailedEvent: StateEvent = consumed,
)
