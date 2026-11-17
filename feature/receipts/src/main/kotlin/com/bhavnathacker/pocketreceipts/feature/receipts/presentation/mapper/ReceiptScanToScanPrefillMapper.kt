package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanAmount
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanPrefill
import javax.inject.Inject

/**
 * Turns a [ReceiptScan] into the [ScanPrefill] stored in the review form's key,
 * keeping at most [MAX_DATES] dates and [MAX_TOTALS] totals so the key stays small.
 */
internal class ReceiptScanToScanPrefillMapper @Inject constructor() {

    operator fun invoke(scan: ReceiptScan): ScanPrefill =
        ScanPrefill(
            merchant = scan.merchant,
            dates = scan.dates.take(MAX_DATES).map { it.toString() },
            totals = scan.totals.take(MAX_TOTALS).map { ScanAmount(it.minorUnits, it.currencyCode) },
            category = scan.category?.name,
        )

    companion object {
        const val MAX_DATES = 5
        const val MAX_TOTALS = 8
    }
}
