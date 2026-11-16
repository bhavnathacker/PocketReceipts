package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Formats a purchase date in [locale]'s medium style, e.g. `Mar 3, 2026` or `3 Mar 2026`.
 */
internal class ReceiptDateFormatter @Inject constructor() {

    operator fun invoke(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
}
