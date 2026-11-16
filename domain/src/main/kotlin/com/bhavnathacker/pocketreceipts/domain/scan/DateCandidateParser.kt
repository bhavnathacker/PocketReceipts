package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import java.text.DateFormatSymbols
import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Finds the dates printed on a receipt, in order of appearance.
 *
 * Reads numeric dates (`03/02/2026`, `03.02.26`, `03-02-2026`), ISO dates
 * (`2026-02-03`) and dates with English or [locale] month names (`3 Feb 2026`,
 * `Feb 3, 2026`, `03-FEB-26`). When day and month could be swapped, [locale]'s
 * short date pattern decides the order. Two-digit years are 20yy. Dates after
 * today are dropped, duplicates are removed, and at most [MAX_DATES] are returned.
 */
class DateCandidateParser @Inject constructor(
    private val getToday: GetTodayUseCase,
) {
    operator fun invoke(lines: List<String>, locale: Locale = Locale.getDefault()): List<LocalDate> {
        val today = getToday()
        val monthFirst = isMonthFirst(locale)
        val months = monthNames(locale)
        return lines
            .flatMap { line -> datesIn(line, monthFirst, months) }
            .filterNot { it.isAfter(today) }
            .distinct()
            .take(MAX_DATES)
    }

    private fun datesIn(line: String, monthFirst: Boolean, months: Map<String, Int>): List<LocalDate> {
        val numeric = NUMERIC_DATE.findAll(line).mapNotNull { match ->
            val (first, _, second, third) = match.destructured
            numericDate(first, second, third, monthFirst)?.let { match.range.first to it }
        }
        val dayMonthYear = DAY_MONTH_YEAR.findAll(line).mapNotNull { match ->
            val (day, month, year) = match.destructured
            months[month.lowercase()]?.let { dateOrNull(year(year), it, day.toInt()) }
                ?.let { match.range.first to it }
        }
        val monthDayYear = MONTH_DAY_YEAR.findAll(line).mapNotNull { match ->
            val (month, day, year) = match.destructured
            months[month.lowercase()]?.let { dateOrNull(year(year), it, day.toInt()) }
                ?.let { match.range.first to it }
        }
        return (numeric + dayMonthYear + monthDayYear).sortedBy { it.first }.map { it.second }.toList()
    }

    private fun numericDate(first: String, second: String, third: String, monthFirst: Boolean): LocalDate? {
        if (first.length == 4) return dateOrNull(first.toInt(), second.toInt(), third.toInt())
        if (first.length > 2) return null
        val year = year(third)
        val a = first.toInt()
        val b = second.toInt()
        val dayFirst = dateOrNull(year, month = b, day = a)
        val monthFirstDate = dateOrNull(year, month = a, day = b)
        return if (monthFirst) monthFirstDate ?: dayFirst else dayFirst ?: monthFirstDate
    }

    private fun year(text: String): Int = when (text.length) {
        2 -> 2000 + text.toInt()
        4 -> text.toInt()
        else -> -1
    }

    private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
        if (year < 0) {
            null
        } else {
            try {
                LocalDate.of(year, month, day)
            } catch (_: DateTimeException) {
                null
            }
        }

    private fun isMonthFirst(locale: Locale): Boolean {
        val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.SHORT,
            null,
            IsoChronology.INSTANCE,
            locale,
        )
        return pattern.indexOf('M') < pattern.indexOf('d')
    }

    /** Lower-case month names and abbreviations, English and [locale]'s, to month numbers. */
    private fun monthNames(locale: Locale): Map<String, Int> = buildMap {
        listOf(Locale.ENGLISH, locale).forEach { language ->
            val symbols = DateFormatSymbols.getInstance(language)
            listOf(symbols.months, symbols.shortMonths).forEach { names ->
                names.take(12).forEachIndexed { index, name ->
                    val key = name.lowercase(language).trimEnd('.')
                    if (key.isNotEmpty()) put(key, index + 1)
                }
            }
        }
        put("sept", 9)
    }

    companion object {
        /** The most date candidates returned. */
        const val MAX_DATES = 5

        private val NUMERIC_DATE = Regex("""(?<![\d.,/-])(\d{1,4})([./-])(\d{1,2})\2(\d{2}|\d{4})(?![\d.,/-]?\d)""")
        private val DAY_MONTH_YEAR = Regex(
            """(?<!\d)(\d{1,2})(?:st|nd|rd|th)?[\s./-]*(\p{L}{3,})\.?[\s./,-]*(\d{4}|\d{2})(?!\d)""",
        )
        private val MONTH_DAY_YEAR = Regex("""(?<!\p{L})(\p{L}{3,})\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4}|\d{2})(?!\d)""")
    }
}
