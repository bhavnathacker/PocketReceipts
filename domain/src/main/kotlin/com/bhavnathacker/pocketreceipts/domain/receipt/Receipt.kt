package com.bhavnathacker.pocketreceipts.domain.receipt

import java.time.LocalDate

/**
 * A saved receipt.
 *
 * @property id Identifier assigned when the receipt was first saved.
 * @property merchant Where the purchase was made, trimmed and non-blank.
 * @property purchaseDate The day of the purchase, never in the future.
 * @property total The amount paid, greater than zero.
 * @property category The spending category.
 * @property note Optional free text, `null` when there is none.
 */
data class Receipt(
    val id: Long,
    val merchant: String,
    val purchaseDate: LocalDate,
    val total: Money,
    val category: ReceiptCategory,
    val note: String?,
)
