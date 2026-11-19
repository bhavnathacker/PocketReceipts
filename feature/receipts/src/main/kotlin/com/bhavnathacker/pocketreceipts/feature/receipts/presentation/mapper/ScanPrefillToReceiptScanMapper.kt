package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanPrefill
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Reads the [ScanPrefill] in the review form's key back into a [ReceiptScan].
 * A date that isn't ISO-8601 text, and an unknown category name, become null.
 */
internal class ScanPrefillToReceiptScanMapper @Inject constructor() {

    operator fun invoke(prefill: ScanPrefill): ReceiptScan =
        ReceiptScan(
            merchant = prefill.merchant,
            date = prefill.date?.let { text ->
                try {
                    LocalDate.parse(text)
                } catch (_: DateTimeParseException) {
                    null
                }
            },
            total = prefill.total?.let { Money(it.minorUnits, it.currencyCode) },
            category = ReceiptCategory.entries.firstOrNull { it.name == prefill.category },
        )
}
