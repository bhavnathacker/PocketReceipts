package com.bhavnathacker.pocketreceipts.core.ui.format

import android.icu.text.NumberFormat
import android.icu.util.Currency
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

/**
 * Formats [Money] for display in [locale], with the currency's symbol and
 * exactly its number of decimals: `€12.30`, `12,30 €`, `₹1,23,456.50`, `¥1,500`.
 *
 * Uses Android's ICU formatter rather than `java.text.NumberFormat`, which
 * can't do Indian (lakh/crore) digit grouping.
 */
class MoneyFormatter @Inject constructor() {

    operator fun invoke(money: Money, locale: Locale = Locale.getDefault()): String {
        val fractionDigits = java.util.Currency.getInstance(money.currencyCode)
            .defaultFractionDigits
            .coerceAtLeast(0)
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(money.currencyCode)
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
        return format.format(BigDecimal.valueOf(money.minorUnits, fractionDigits))
    }
}
