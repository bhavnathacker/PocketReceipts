package com.bhavnathacker.pocketreceipts.feature.ask.presentation.mapper

import android.icu.text.DateFormat
import android.icu.text.DateIntervalFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.text.FieldPosition
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/**
 * Formats the dates from [LocalDate] to [LocalDate] (both included) compactly in a locale,
 * sharing the parts both dates have: `1–31 Oct 2026` or `Oct 1 – 31, 2026`,
 * `1 Oct – 31 Dec 2026`, and a single date when both are the same day.
 *
 * Uses Android's ICU interval formatter, which knows each locale's order and separators.
 */
internal class DateRangeFormatter @Inject constructor() {

    operator fun invoke(from: LocalDate, to: LocalDate, locale: Locale = Locale.getDefault()): String {
        val format = DateIntervalFormat.getInstance(DateFormat.YEAR_ABBR_MONTH_DAY, ULocale.forLocale(locale))
        return format.format(calendar(from, locale), calendar(to, locale), StringBuffer(), FieldPosition(0)).toString()
    }

    private fun calendar(date: LocalDate, locale: Locale): Calendar =
        Calendar.getInstance(TimeZone.GMT_ZONE, ULocale.forLocale(locale)).apply {
            clear()
            set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
}
