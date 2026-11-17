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
 *   (`120.00`, `4,50`), when a currency marker sits next to it (`Rs 1,500`), or
 *   when it is on a line labelled as a total (`TOTAL 3050`). This keeps out phone
 *   numbers, quantities, years and invoice numbers. Times (`10.30 pm`,
 *   `1.00 to 4.00 pm`) are never amounts.
 * - The decimal separator is the last `.` or `,`; earlier `,` `.` `'` or spaces are
 *   digit grouping, so `1,23,456.50`, `1.234,56` and `1 234,56` all read without
 *   the locale. Values are exact, in minor units.
 * - A marker next to the amount (`₹`, `Rs`, `INR`, `€`, `£`, `¥`, `$` or an ISO
 *   code) sets its currency; otherwise the default currency for [locale] is used.
 *   `$` is the default currency when that currency's symbol is `$`, else USD.
 * - The labelled total comes first. Labels for the amount actually paid (`NET
 *   AMOUNT`, `NET PAYABLE`, `AMOUNT DUE`, `GRAND TOTAL`, ...) outrank a plain
 *   `TOTAL`, wherever they appear; within a rank the last such line wins. Gross,
 *   sub-total and tax-total lines are never preselected. The other distinct
 *   amounts follow, largest first, up to [MAX_TOTALS] in all.
 */
class AmountCandidateParser @Inject constructor(
    private val getDefaultCurrency: GetDefaultCurrencyUseCase,
) {
    operator fun invoke(lines: List<String>, locale: Locale = Locale.getDefault()): List<Money> {
        val defaultCurrency = getDefaultCurrency(locale)
        val amountsByLine = lines.map { line -> amountsIn(line, defaultCurrency, locale) }
        val labelledTotal = lines.indices
            .filter { amountsByLine[it].isNotEmpty() }
            .mapNotNull { index -> totalRank(lines[index])?.let { rank -> index to rank } }
            // Highest rank first; within a rank, the last line.
            .maxWithOrNull(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first })
            ?.let { (index, _) -> amountsByLine[index].last() }
        // Without a total label, the largest amount not on an excluded line (gross, sub-total) leads.
        val preselected = labelledTotal ?: lines.indices
            .filterNot { isExcludedLine(lines[it]) }
            .flatMap { amountsByLine[it] }
            .maxByOrNull { it.minorUnits }
        val others = amountsByLine.flatten().sortedByDescending { it.minorUnits }
        return (listOfNotNull(preselected) + others).distinct().take(MAX_TOTALS)
    }

    private fun amountsIn(rawLine: String, defaultCurrency: String, locale: Locale): List<Money> {
        val line = TIME.replace(rawLine) { " ".repeat(it.value.length) }
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
            val counts = number.hasDecimals || marker != null || totalLine
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
        else -> marker.takeIf { it in ISO_CURRENCY_CODES }
    }

    private fun dollarCurrency(defaultCurrency: String, locale: Locale): String =
        if (Currency.getInstance(defaultCurrency).getSymbol(locale) == "$") defaultCurrency else "USD"

    private data class ParsedNumber(val minorUnits: Long, val hasDecimals: Boolean)

    companion object {
        /** The most total candidates returned. */
        const val MAX_TOTALS = 8

        private const val DOLLAR = "$"

        /** A time of day or a range of them, such as `10.30 pm` or `1.00 to 4.00 pm`. */
        private val TIME = Regex(
            """(?i)(?<![\d.,])\d{1,2}[.:]\d{2}(?:\s*(?:to|-|–)\s*\d{1,2}[.:]\d{2})?\s*(?:a\.?\s?m|p\.?\s?m|hrs?)\b\.?""",
        )

        private val NUMBER = Regex(
            """(?<![\d.,'])(?<!\d[-/])\d+(?:(?:[.,']|[  ](?=\d{3}(?!\d)))\d+)*(?!\d)(?!\s?%)""",
        )
        private val GROUP_SEPARATOR = Regex("""[.,'  ]""")
        private val MARKER_BEFORE = Regex("""(₹|(?<!\p{L})R[Ss]\.?|€|£|¥|\$|(?<!\p{L})[A-Z]{3})$""")
        private val MARKER_AFTER = Regex("""^(₹|€|£|¥|\$|[A-Z]{3}(?!\p{L}))""")

        /**
         * Known ISO 4217 codes. Android's `Currency.getInstance` accepts any
         * well-formed three-letter code (`TAX`), so a code is checked against this set.
         */
        private val ISO_CURRENCY_CODES: Set<String> by lazy {
            Currency.getAvailableCurrencies().mapTo(HashSet()) { it.currencyCode }
        }

        /** English words that are also ISO 4217 codes, so they aren't read as currencies. */
        private val NOT_CURRENCY_WORDS = setOf("ALL", "CUP", "TOP", "PEN", "MOP", "BOB", "SOS")

        /** Lines that are never the total: sub-totals, tax and item totals, gross amounts. */
        private val EXCLUDED_LABELS = Regex(
            """\b(SUB[\s-]?TOTAL|TOTAL\s+(TAX|ITEMS?|QTY|SAVINGS?|DISCOUNTS?)|""" +
                """GROSS(\s+(TOTAL|AMOUNT|AMT|VALUE))?|TOTAL\s+GROSS)\b""",
        )

        /**
         * Labels for the amount actually paid; they outrank [TOTAL_LABELS]. A bare
         * `NET` counts only right before an amount, so `Net Wt` doesn't.
         */
        private val PAID_LABELS = Regex(
            """\b(NET\s+(AMOUNT|AMT|PAYABLE|TOTAL|PAY)|NET(?=\s*[:-]?\s*(RS\.?|₹|INR)?\s*\d)|AMOUNT\s+(DUE|PAYABLE)|BALANCE\s+DUE|""" +
                """TOTAL\s+(DUE|PAYABLE)|TO\s+PAY|GRAND\s+TOTAL|TOTAL\s+TTC)\b""",
        )
        private val TOTAL_LABELS = Regex("""\b(TOTAL|SUMME|GESAMT)\b""")

        private const val RANK_PAID = 2
        private const val RANK_TOTAL = 1

        /** How strongly [line]'s label marks it as the total, or null if it doesn't. */
        internal fun totalRank(line: String): Int? {
            val label = EXCLUDED_LABELS.replace(line.uppercase(Locale.ROOT), " ")
            return when {
                PAID_LABELS.containsMatchIn(label) -> RANK_PAID
                TOTAL_LABELS.containsMatchIn(label) -> RANK_TOTAL
                else -> null
            }
        }

        internal fun isTotalLine(line: String): Boolean = totalRank(line) != null

        private fun isExcludedLine(line: String): Boolean =
            EXCLUDED_LABELS.containsMatchIn(line.uppercase(Locale.ROOT))
    }
}
