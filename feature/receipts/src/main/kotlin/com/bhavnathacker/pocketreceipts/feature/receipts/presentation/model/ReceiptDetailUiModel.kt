package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * A receipt's fields, ready to display on its detail screen.
 */
@Immutable
data class ReceiptDetailUiModel(
    val merchant: String,
    val date: String,
    val total: String,
    @param:StringRes val categoryLabel: Int,
    val note: String?,
)
