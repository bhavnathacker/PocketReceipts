package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Finds the amounts printed on a receipt and orders them as total candidates.
 *
 * - A number is an amount when it has exactly its currency's decimal places
 *   (`120.00`, `4,50`), or when a currency marker sits next to it (`Rs 1,500`).
 *   This keeps out phone numbers, quantities, years and invoice numbers.
 * - The decimal separator is the last `.` or `,`; earlier `,` `.` `'` or spaces are
 *   digit grouping, so `1,23,456.50`, `1.234,56` and `1 234,56` all read without
 *   the locale. Values are exact, in minor units.
 * - A marker next to the amount (`₹`, `Rs`, `INR`, `€`, `£`, `¥`, `$` or an ISO
 *   code) sets its currency; otherwise the default currency for [locale] is used.
 *   `$` is the default currency when that currency's symbol is `$`, else USD.
 * - The amount on the last line labelled as a total comes first. The other
 *   distinct amounts follow, largest first, up to [MAX_TOTALS] in all.
 */
class AmountCandidateParser @Inject constructor(
    private val getDefaultCurrency: GetDefaultCurrencyUseCase,
) {
    operator fun invoke(lines: List<String>, locale: Locale = Locale.getDefault()): List<Money> {
        val defaultCurrency = getDefaultCurrency(locale)
        val amountsByLine = lines.map { line -> amountsIn(line, defaultCurrency, locale) }
        val labelledTotal = lines.indices
            .lastOrNull { isTotalLine(lines[it]) && amountsByLine[it].isNotEmpty() }
            ?.let { amountsByLine[it].last() }
        val others = amountsByLine.flatten().distinct().sortedByDescending { it.minorUnits }
        return (listOfNotNull(labelledTotal) + others).distinct().take(MAX_TOTALS)
    }

    private fun amountsIn(line: String, defaultCurrency: String, locale: Locale): List<Money> {
        val totalLine = isTotalLine(line)
        return NUMBER.findAll(line).mapNotNull { match ->
            val marker = markerBefore(line.substring(0, match.range.first))
                ?: markerAfter(line.substring(match.range.last + 1))
            val currency = when (marker) {
                null -> defaultCurrency
                DOLLAR -> dollarCurrency(defaultCurrency, locale)
                else -> marker
            }
            val fractionDigits = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
            val number = parseNumber(match.value, fractionDigits) ?: return@mapNotNull null
            val counts = number.hasDecimals || marker != null || (fractionDigits == 0 && totalLine)
            if (counts && number.minorUnits > 0) Money(number.minorUnits, currency) else null
        }.toList()
    }

    private fun parseNumber(token: String, fractionDigits: Int): ParsedNumber? {
        val lastSeparator = token.indexOfLast { it == '.' || it == ',' }
        val hasDecimals = fractionDigits > 0 && lastSeparator >= 0 &&
            token.length - lastSeparator - 1 == fractionDigits
        val integerPart = if (hasDecimals) token.substring(0, lastSeparator) else token
        val fraction = if (hasDecimals) token.substring(lastSeparator + 1) else "0".repeat(fractionDigits)
        // A space only groups digits in comma-decimal formats (`1 234,56`).
        if (' ' in integerPart && hasDecimals && token[lastSeparator] == '.') {
            return parseNumber(token.substringAfterLast(' '), fractionDigits)
        }
        val separators = integerPart.filter { !it.isDigit() }.toSet()
        if (separators.size > 1) return null
        if (hasDecimals && token[lastSeparator] in separators) return null
        val groups = integerPart.split(GROUP_SEPARATOR)
        if (groups.size > 1) {
            val validGroups = groups.first().length in 1..3 &&
                groups.drop(1).dropLast(1).all { it.length in 2..3 } &&
                groups.last().length == 3
            if (!validGroups) return null
        }
        val digits = groups.joinToString("") + fraction
        return digits.toLongOrNull()?.let { ParsedNumber(it, hasDecimals) }
    }

    private fun markerBefore(text: String): String? =
        MARKER_BEFORE.find(text.trimEnd())?.groupValues?.get(1)?.let(::currencyOfMarker)

    private fun markerAfter(text: String): String? =
        MARKER_AFTER.find(text.trimStart())?.groupValues?.get(1)?.let(::currencyOfMarker)

    private fun currencyOfMarker(marker: String): String? = when (marker) {
        "₹", "Rs", "Rs.", "RS", "RS." -> "INR"
        "€" -> "EUR"
        "£" -> "GBP"
        "¥" -> "JPY"
        "$" -> DOLLAR
        in NOT_CURRENCY_WORDS -> null
        else -> runCatching { Currency.getInstance(marker).currencyCode }.getOrNull()
    }

    private fun dollarCurrency(defaultCurrency: String, locale: Locale): String =
        if (Currency.getInstance(defaultCurrency).getSymbol(locale) == "$") defaultCurrency else "USD"

    private data class ParsedNumber(val minorUnits: Long, val hasDecimals: Boolean)

    companion object {
        /** The most total candidates returned. */
        const val MAX_TOTALS = 8

        private const val DOLLAR = "$"

        private val NUMBER = Regex(
            """(?<![\d.,'])\d+(?:(?:[.,']|[  ](?=\d{3}(?!\d)))\d+)*(?!\d)(?!\s?%)""",
        )
        private val GROUP_SEPARATOR = Regex("""[.,'  ]""")
        private val MARKER_BEFORE = Regex("""(₹|(?<!\p{L})R[Ss]\.?|€|£|¥|\$|(?<!\p{L})[A-Z]{3})$""")
        private val MARKER_AFTER = Regex("""^(₹|€|£|¥|\$|[A-Z]{3}(?!\p{L}))""")

        /** English words that are also ISO 4217 codes, so they aren't read as currencies. */
        private val NOT_CURRENCY_WORDS = setOf("ALL", "CUP", "TOP", "PEN", "MOP", "BOB", "SOS")

        private val EXCLUDED_LABELS = Regex(
            """\b(SUB[\s-]?TOTAL|TOTAL\s+(TAX|ITEMS?|QTY|SAVINGS?|DISCOUNTS?))\b""",
        )
        private val TOTAL_LABELS = Regex(
            """\b(GRAND\s+TOTAL|TOTAL|AMOUNT\s+DUE|BALANCE\s+DUE|NET\s+AMOUNT|NET\s+PAYABLE|""" +
                """AMOUNT\s+PAYABLE|TO\s+PAY|SUMME|GESAMT|TOTAL\s+TTC)\b""",
        )

        internal fun isTotalLine(line: String): Boolean =
            TOTAL_LABELS.containsMatchIn(EXCLUDED_LABELS.replace(line.uppercase(Locale.ROOT), " "))
    }
}
