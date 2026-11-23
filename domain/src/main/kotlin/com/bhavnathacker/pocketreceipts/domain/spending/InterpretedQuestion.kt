package com.bhavnathacker.pocketreceipts.domain.spending

/**
 * The model's unchecked reading of a spending question. [SpendingQueryChecker] turns it into
 * a [SpendingQuery].
 */
sealed interface InterpretedQuestion {

    /**
     * A question about spending.
     *
     * @property category The category name the model gave, or `null` for every category.
     */
    data class Spending(
        val category: String?,
        val period: InterpretedPeriod,
    ) : InterpretedQuestion

    /** The model found no spending question, or its reply couldn't be read. */
    data object Unsupported : InterpretedQuestion
}

/**
 * The period the model read from a question. Values are as the model gave them and may be
 * missing or out of range.
 */
sealed interface InterpretedPeriod {

    /** A calendar month; [year] is `null` when the question names none. */
    data class Month(val year: Int?, val month: Int?) : InterpretedPeriod

    /** A calendar quarter, 1 to 4 (January–March is 1). */
    data class Quarter(val year: Int?, val quarter: Int?) : InterpretedPeriod

    /** A calendar year. */
    data class Year(val year: Int?) : InterpretedPeriod

    /** Dates from [from] to [to], both included, as ISO-8601 text. */
    data class Range(val from: String?, val to: String?) : InterpretedPeriod

    /** Every receipt, whatever its date. */
    data object AllTime : InterpretedPeriod
}
