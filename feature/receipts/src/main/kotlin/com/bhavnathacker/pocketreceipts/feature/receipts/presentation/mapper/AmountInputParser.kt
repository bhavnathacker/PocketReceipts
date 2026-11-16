package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Parses an amount typed by the user into minor units of a currency, or returns
 * `null` if the text isn't a valid amount for that currency.
 *
 * - The decimal separator is [locale]'s (`.` in en-US and en-IN, `,` in de-DE).
 * - The locale's grouping separator is accepted in Western (`1,234,567`) and
 *   Indian (`12,34,567`) grouping. A grouped number must look like one: the
 *   last group has exactly 3 digits and the others 2 or 3, so `4.50` typed in
 *   de-DE (where `.` groups) is rejected instead of read as 450.
 * - At most as many decimals as the currency has: none for JPY, 2 for EUR, 3 for KWD.
 * - No sign, letters or other characters.
 */
internal class AmountInputParser @Inject constructor() {

    operator fun invoke(
        text: String,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): Long? {
        val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val decimalSeparator = symbols.decimalSeparator
        val groupingSeparators = groupingSeparators(symbols.groupingSeparator)

        val trimmed = text.trim()
        val parts = trimmed.split(decimalSeparator)
        if (parts.size > 2) return null
        val integerPart = parts[0]
        val fractionPart = parts.getOrNull(1)

        if (fractionPart != null) {
            if (fractionDigits == 0 || fractionPart.isEmpty() || fractionPart.length > fractionDigits) return null
            if (!fractionPart.all { it.isDigit() }) return null
        }
        val integerDigits = parseGroupedInteger(integerPart, groupingSeparators) ?: return null

        return runCatching {
            BigDecimal("$integerDigits.${fractionPart.orEmpty()}0")
                .movePointRight(fractionDigits)
                .setScale(0)
                .longValueExact()
        }.getOrNull()
    }

    /**
     * The digits of [text] without grouping separators, or `null` if it isn't
     * a plain or correctly grouped whole number.
     */
    private fun parseGroupedInteger(text: String, groupingSeparators: Set<Char>): String? {
        if (text.isEmpty()) return null
        val groups = text.split(*groupingSeparators.toCharArray())
        if (!groups.all { group -> group.isNotEmpty() && group.all { it.isDigit() } }) return null
        if (groups.size > 1) {
            if (groups.first().length > 3) return null
            if (groups.last().length != 3) return null
            if (groups.drop(1).dropLast(1).any { it.length !in 2..3 }) return null
        }
        return groups.joinToString("")
    }

    /** Locales that group with a (narrow) no-break space also accept a plain space. */
    private fun groupingSeparators(grouping: Char): Set<Char> =
        if (grouping.isWhitespace() || grouping == ' ' || grouping == ' ') {
            setOf(grouping, ' ', ' ', ' ')
        } else {
            setOf(grouping)
        }
}
