package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * One row of the receipt list, with display-ready text.
 */
@Immutable
data class ReceiptListItemUiModel(
    val id: Long,
    val merchant: String,
    val date: String,
    val total: String,
    @param:StringRes val categoryLabel: Int,
)
