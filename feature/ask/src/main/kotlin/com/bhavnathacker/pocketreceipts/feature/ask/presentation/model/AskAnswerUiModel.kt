package com.bhavnathacker.pocketreceipts.feature.ask.presentation.model

import androidx.annotation.StringRes

/**
 * The answer shown on Ask.
 */
internal sealed interface AskAnswerUiModel {

    /**
     * Totals for [categoryLabel] (a category, or all spending) during [period].
     *
     * @property period The period's dates, formatted; `null` for all time.
     * @property totals One line per currency, the currency with the most receipts first.
     */
    data class Totals(
        @param:StringRes @get:StringRes val categoryLabel: Int,
        val period: String?,
        val totals: List<AskTotalUiModel>,
    ) : AskAnswerUiModel

    /** No receipt matched [categoryLabel] during [period] (`null` for all time). */
    data class NothingSpent(
        @param:StringRes @get:StringRes val categoryLabel: Int,
        val period: String?,
    ) : AskAnswerUiModel

    /** The question can't be answered. */
    data object Unanswerable : AskAnswerUiModel
}

/**
 * One currency's total.
 *
 * @property amount The total, formatted with its currency.
 * @property receiptCount How many receipts it adds up.
 */
internal data class AskTotalUiModel(
    val amount: String,
    val receiptCount: Int,
)
