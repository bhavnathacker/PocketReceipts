package com.bhavnathacker.pocketreceipts.domain.spending

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Turns the model's reading of a question into a [SpendingQuery], or `null` when it can't be
 * answered. Nothing is guessed: a value that fails a check makes the whole question
 * unanswerable rather than being widened (an unknown category never becomes every category).
 *
 * - The category must be a [ReceiptCategory] name (any case); `null` or blank means every category.
 * - A month must be 1–12, a quarter 1–4 and a year 1900–9999.
 * - A month or quarter without a year is its most recent occurrence that has started by
 *   [today]; a year without a number is this year.
 * - A range needs two ISO-8601 dates, the first not after the second.
 */
class SpendingQueryChecker @Inject constructor() {

    operator fun invoke(question: InterpretedQuestion, today: LocalDate): SpendingQuery? {
        if (question !is InterpretedQuestion.Spending) return null
        val category = category(question.category) ?: return null
        return when (val period = question.period) {
            is InterpretedPeriod.Month -> month(period, today)
            is InterpretedPeriod.Quarter -> quarter(period, today)
            is InterpretedPeriod.Year -> year(period.year ?: today.year)
            is InterpretedPeriod.Range -> range(period)
            InterpretedPeriod.AllTime -> Dates(null, null)
        }?.let { SpendingQuery(category.value, it.from, it.to) }
    }

    /** The category, wrapped so that "every category" (`null`) differs from "invalid". */
    private fun category(name: String?): Category? {
        val text = name?.trim()?.uppercase()
        if (text.isNullOrEmpty()) return Category(null)
        return ReceiptCategory.entries.firstOrNull { it.name == text }?.let { Category(it) }
    }

    private fun month(period: InterpretedPeriod.Month, today: LocalDate): Dates? {
        val month = period.month?.takeIf { it in 1..12 } ?: return null
        val year = period.year ?: if (month <= today.monthValue) today.year else today.year - 1
        return months(year, month, month)
    }

    private fun quarter(period: InterpretedPeriod.Quarter, today: LocalDate): Dates? {
        val quarter = period.quarter?.takeIf { it in 1..4 } ?: return null
        val firstMonth = quarter * 3 - 2
        val year = period.year ?: if (firstMonth <= today.monthValue) today.year else today.year - 1
        return months(year, firstMonth, firstMonth + 2)
    }

    private fun year(year: Int): Dates? = months(year, 1, 12)

    private fun months(year: Int, firstMonth: Int, lastMonth: Int): Dates? {
        if (year !in MIN_YEAR..MAX_YEAR) return null
        return Dates(YearMonth.of(year, firstMonth).atDay(1), YearMonth.of(year, lastMonth).atEndOfMonth())
    }

    private fun range(period: InterpretedPeriod.Range): Dates? {
        val from = date(period.from) ?: return null
        val to = date(period.to) ?: return null
        return if (from.isAfter(to)) null else Dates(from, to)
    }

    private fun date(text: String?): LocalDate? =
        text?.trim()?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }

    private data class Category(val value: ReceiptCategory?)

    private data class Dates(val from: LocalDate?, val to: LocalDate?)

    private companion object {
        const val MIN_YEAR = 1900
        const val MAX_YEAR = 9999
    }
}
