package com.bhavnathacker.pocketreceipts.domain.scan

import java.util.Locale
import javax.inject.Inject

/**
 * Turns the recognized text of a receipt into candidate merchant, dates and
 * totals. [locale] decides ambiguous day/month order and the default currency.
 */
class ReceiptScanParser @Inject constructor(
    private val dateCandidateParser: DateCandidateParser,
    private val amountCandidateParser: AmountCandidateParser,
    private val merchantCandidateParser: MerchantCandidateParser,
) {
    operator fun invoke(text: RecognizedText, locale: Locale = Locale.getDefault()): ReceiptScan =
        ReceiptScan(
            merchant = merchantCandidateParser(text.lines),
            dates = dateCandidateParser(text.lines, locale),
            totals = amountCandidateParser(text.lines, locale),
        )
}
