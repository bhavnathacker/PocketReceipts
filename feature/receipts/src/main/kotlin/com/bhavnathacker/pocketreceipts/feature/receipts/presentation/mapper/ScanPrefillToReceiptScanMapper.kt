package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanPrefill
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Reads the [ScanPrefill] in the review form's key back into a [ReceiptScan].
 * Dates that aren't ISO-8601 text are skipped.
 */
internal class ScanPrefillToReceiptScanMapper @Inject constructor() {

    operator fun invoke(prefill: ScanPrefill): ReceiptScan =
        ReceiptScan(
            merchant = prefill.merchant,
            dates = prefill.dates.mapNotNull { text ->
                try {
                    LocalDate.parse(text)
                } catch (_: DateTimeParseException) {
                    null
                }
            },
            totals = prefill.totals.map { Money(it.minorUnits, it.currencyCode) },
        )
}
