package com.bhavnathacker.pocketreceipts.domain.spending

import com.bhavnathacker.pocketreceipts.domain.receipt.Money

/**
 * The receipts of one currency that match a [SpendingQuery].
 *
 * @property money Their totals added up exactly, in that currency.
 * @property receiptCount How many receipts were added up, at least one.
 */
data class SpendingTotal(
    val money: Money,
    val receiptCount: Int,
)
