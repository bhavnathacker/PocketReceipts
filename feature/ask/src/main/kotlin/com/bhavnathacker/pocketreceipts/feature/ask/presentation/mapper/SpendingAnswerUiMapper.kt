package com.bhavnathacker.pocketreceipts.feature.ask.presentation.mapper

import com.bhavnathacker.pocketreceipts.core.ui.format.MoneyFormatter
import com.bhavnathacker.pocketreceipts.core.ui.format.ReceiptCategoryLabelMapper
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingAnswer
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingQuery
import com.bhavnathacker.pocketreceipts.feature.ask.R
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskAnswerUiModel
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskTotalUiModel
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a [SpendingAnswer] to what Ask shows: how the question was read (category and dates)
 * and the totals, formatted in [Locale].
 */
internal class SpendingAnswerUiMapper @Inject constructor(
    private val moneyFormatter: MoneyFormatter,
    private val receiptCategoryLabelMapper: ReceiptCategoryLabelMapper,
    private val dateRangeFormatter: DateRangeFormatter,
) {

    operator fun invoke(answer: SpendingAnswer, locale: Locale = Locale.getDefault()): AskAnswerUiModel =
        when (answer) {
            SpendingAnswer.Unanswerable -> AskAnswerUiModel.Unanswerable
            is SpendingAnswer.Totals -> {
                val categoryLabel = categoryLabel(answer.query)
                val period = period(answer.query, locale)
                if (answer.totals.isEmpty()) {
                    AskAnswerUiModel.NothingSpent(categoryLabel, period)
                } else {
                    AskAnswerUiModel.Totals(
                        categoryLabel = categoryLabel,
                        period = period,
                        totals = answer.totals.map { AskTotalUiModel(moneyFormatter(it.money, locale), it.receiptCount) },
                    )
                }
            }
        }

    private fun categoryLabel(query: SpendingQuery): Int =
        query.category?.let { receiptCategoryLabelMapper(it) } ?: R.string.ask_all_spending

    /** The period's dates, or `null` for all time (the checker gives both dates or neither). */
    private fun period(query: SpendingQuery, locale: Locale): String? {
        val from = query.from ?: return null
        val to = query.to ?: return null
        return dateRangeFormatter(from, to, locale)
    }
}
