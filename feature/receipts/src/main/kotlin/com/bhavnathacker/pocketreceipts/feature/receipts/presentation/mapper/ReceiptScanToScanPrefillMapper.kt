package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanAmount
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanPrefill
import javax.inject.Inject

/**
 * Turns a [ReceiptScan] into the [ScanPrefill] stored in the review form's key.
 */
internal class ReceiptScanToScanPrefillMapper @Inject constructor() {

    operator fun invoke(scan: ReceiptScan): ScanPrefill =
        ScanPrefill(
            merchant = scan.merchant,
            date = scan.date?.toString(),
            total = scan.total?.let { ScanAmount(it.minorUnits, it.currencyCode) },
            category = scan.category?.name,
        )
}
