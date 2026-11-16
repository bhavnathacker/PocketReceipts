package com.bhavnathacker.pocketreceipts.domain.receipt

/**
 * An exact amount of money in one currency.
 *
 * @property minorUnits The amount in the currency's smallest unit (cents, pence, paise...):
 * 12.30 EUR is `1230`, 1500 JPY is `1500`. The number of decimal places comes from
 * [java.util.Currency.getDefaultFractionDigits] for [currencyCode].
 * @property currencyCode ISO 4217 currency code, e.g. `EUR`, `INR`, `JPY`.
 */
data class Money(
    val minorUnits: Long,
    val currencyCode: String,
)
