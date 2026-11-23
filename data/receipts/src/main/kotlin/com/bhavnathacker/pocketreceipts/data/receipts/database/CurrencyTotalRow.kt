package com.bhavnathacker.pocketreceipts.data.receipts.database

import androidx.room.ColumnInfo

/**
 * One currency's row of [ReceiptDao.currencyTotals]: the exact sum of its receipts' totals in
 * minor units, and how many receipts were added up.
 */
internal data class CurrencyTotalRow(
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    @ColumnInfo(name = "receipt_count") val receiptCount: Int,
)
