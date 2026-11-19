package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptValidator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Currency
import javax.inject.Inject

/**
 * Turns the model's raw answer into a [ReceiptScan], dropping every value the
 * receipt form wouldn't accept:
 * - a date that isn't ISO-8601 or is after today;
 * - a currency that isn't a known ISO 4217 code ([defaultCurrency] is used instead);
 * - a total that isn't a positive amount, or has more decimals than its currency allows;
 * - a category that isn't a [ReceiptCategory] name.
 *
 * A blank merchant is dropped; a long one is cut to [ReceiptValidator.MAX_MERCHANT_LENGTH].
 */
class ReceiptFieldsChecker @Inject constructor() {

    operator fun invoke(extracted: ExtractedReceipt, today: LocalDate, defaultCurrency: String): ReceiptScan =
        ReceiptScan(
            merchant = merchant(extracted.merchant),
            date = date(extracted.date, today),
            total = total(extracted.total, currency(extracted.currency) ?: defaultCurrency),
            category = category(extracted.category),
        )

    private fun merchant(text: String?): String? =
        text?.trim()?.replace(WHITESPACE, " ")?.takeIf { it.isNotEmpty() }
            ?.take(ReceiptValidator.MAX_MERCHANT_LENGTH)

    private fun date(text: String?, today: LocalDate): LocalDate? =
        text?.trim()?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }?.takeUnless { it.isAfter(today) }

    private fun currency(code: String?): String? =
        code?.trim()?.uppercase()?.takeIf { it in ISO_CURRENCY_CODES }

    private fun total(text: String?, currency: String): Money? {
        val amount = text?.trim()?.toBigDecimalOrNull() ?: return null
        val fractionDigits = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > fractionDigits) return null
        val minorUnits = amount.movePointRight(fractionDigits)
        return if (minorUnits > MAX_MINOR_UNITS) null else Money(minorUnits.longValueExact(), currency)
    }

    private fun category(name: String?): ReceiptCategory? =
        name?.trim()?.uppercase()?.let { text -> ReceiptCategory.entries.firstOrNull { it.name == text } }

    private companion object {
        val WHITESPACE = Regex("""\s+""")
        val MAX_MINOR_UNITS: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE)
        val ISO_CURRENCY_CODES: Set<String> by lazy {
            Currency.getAvailableCurrencies().mapTo(HashSet()) { it.currencyCode }
        }
    }
}
