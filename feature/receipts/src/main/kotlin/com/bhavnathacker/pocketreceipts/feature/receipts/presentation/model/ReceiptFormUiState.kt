package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import java.time.LocalDate

/**
 * The add/edit receipt form. Text fields hold exactly what the user typed;
 * they are parsed and validated only on save.
 *
 * @property today The latest date the date picker allows.
 * @property isReview The form reviews a scanned receipt before it is added.
 * @property closeEvent The form should close: the receipt was saved, or the edited receipt no longer exists.
 * @property saveFailedEvent Saving failed unexpectedly; the input is kept so the user can retry.
 * @property nothingRecognizedEvent Nothing could be read from the scan; the form shows the defaults.
 */
@Stable
data class ReceiptFormUiState(
    val isEditing: Boolean,
    val isReview: Boolean = false,
    val isLoading: Boolean,
    val today: LocalDate,
    val merchant: String = "",
    val amount: String = "",
    val currencyCode: String,
    val purchaseDate: LocalDate,
    val category: ReceiptCategory = ReceiptCategory.OTHER,
    val note: String = "",
    @param:StringRes val merchantError: Int? = null,
    @param:StringRes val amountError: Int? = null,
    @param:StringRes val dateError: Int? = null,
    @param:StringRes val noteError: Int? = null,
    val isSaving: Boolean = false,
    val closeEvent: StateEvent = consumed,
    val saveFailedEvent: StateEvent = consumed,
    val nothingRecognizedEvent: StateEvent = consumed,
)
