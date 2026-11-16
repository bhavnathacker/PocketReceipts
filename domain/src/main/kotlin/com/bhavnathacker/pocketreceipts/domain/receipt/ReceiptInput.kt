package com.bhavnathacker.pocketreceipts.domain.receipt

import java.time.LocalDate

/**
 * The editable fields of a receipt, as entered by the user (or later, proposed
 * by capture and confirmed by the user). It is validated before it is saved.
 */
data class ReceiptInput(
    val merchant: String,
    val purchaseDate: LocalDate,
    val total: Money,
    val category: ReceiptCategory,
    val note: String?,
)
