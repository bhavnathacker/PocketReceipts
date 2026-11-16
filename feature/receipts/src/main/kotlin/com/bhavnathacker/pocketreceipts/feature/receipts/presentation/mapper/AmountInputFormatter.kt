package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Turns [Money] into editable amount text for the receipt form: no currency
 * symbol and no grouping, with [locale]'s decimal separator (`123456.50`,
 * `4,50`, `1500`). [AmountInputParser] reads it back unchanged.
 */
internal class AmountInputFormatter @Inject constructor() {

    operator fun invoke(money: Money, locale: Locale = Locale.getDefault()): String {
        val fractionDigits = Currency.getInstance(money.currencyCode).defaultFractionDigits.coerceAtLeast(0)
        val plain = BigDecimal.valueOf(money.minorUnits, fractionDigits).toPlainString()
        return plain.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
    }
}
