package com.bhavnathacker.pocketreceipts.domain.spending

/**
 * The answer to a spending question.
 */
sealed interface SpendingAnswer {

    /**
     * The question was read as [query].
     *
     * @property totals One total per currency, the currency with the most receipts first.
     * Empty when no receipt matches.
     */
    data class Totals(
        val query: SpendingQuery,
        val totals: List<SpendingTotal>,
    ) : SpendingAnswer

    /**
     * The question isn't about how much was spent in a category and period, or the model's
     * reading of it didn't pass the checks.
     */
    data object Unanswerable : SpendingAnswer
}
