package com.bhavnathacker.pocketreceipts.domain.spending

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import java.time.LocalDate

/**
 * A checked spending question: the receipts to add up.
 *
 * @property category Only receipts in this category, or every category when `null`.
 * @property from The first purchase date counted, or no lower limit when `null`.
 * @property to The last purchase date counted (included), or no upper limit when `null`.
 * Both are `null` for all time.
 */
data class SpendingQuery(
    val category: ReceiptCategory?,
    val from: LocalDate?,
    val to: LocalDate?,
)
