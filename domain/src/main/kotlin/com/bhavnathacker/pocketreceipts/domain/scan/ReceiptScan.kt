package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import java.time.LocalDate

/**
 * The checked values read from a scanned receipt, for the user to confirm.
 * Each is null when the model didn't find it or its answer wasn't valid.
 *
 * @property merchant The shop's name.
 * @property date The purchase date, never after today.
 * @property total The amount paid.
 * @property category The receipt's category.
 */
data class ReceiptScan(
    val merchant: String? = null,
    val date: LocalDate? = null,
    val total: Money? = null,
    val category: ReceiptCategory? = null,
)
