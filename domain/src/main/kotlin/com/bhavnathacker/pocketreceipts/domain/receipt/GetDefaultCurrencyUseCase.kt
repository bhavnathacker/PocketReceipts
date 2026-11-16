package com.bhavnathacker.pocketreceipts.domain.receipt

import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * The ISO 4217 currency code new receipts start with: the currency of [locale]'s
 * region, or [FALLBACK_CURRENCY] when the locale has no region or the region
 * has no currency.
 */
class GetDefaultCurrencyUseCase @Inject constructor() {

    operator fun invoke(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(locale)?.currencyCode }
            .getOrNull()
            ?: FALLBACK_CURRENCY

    companion object {
        const val FALLBACK_CURRENCY = "INR"
    }
}
