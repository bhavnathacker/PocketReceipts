package com.bhavnathacker.pocketreceipts.data.ai

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Builds the prompt that asks the model to read a spending question into a category and a
 * period, as one JSON object. The model is never asked for, or given, an amount.
 *
 * The question goes last, on one line after a `Question:` marker, so its text reads as the
 * question and not as more instructions.
 */
internal class SpendingQuestionPromptBuilder @Inject constructor() {

    operator fun invoke(question: String, today: LocalDate): String =
        buildString {
            appendLine("You read a question about the user's spending and turn it into a query.")
            appendLine("Never answer the question or work out an amount.")
            appendLine()
            val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            appendLine("Today is $today, a $weekday.")
            appendLine()
            appendLine("Reply with only one JSON object and no other text, in one of these forms:")
            appendLine("""{"category": "GROCERIES", "period": {"type": "month", "year": 2026, "month": 10}}""")
            appendLine("""{"category": null, "period": {"type": "quarter", "year": 2026, "quarter": 4}}""")
            appendLine("""{"category": null, "period": {"type": "year", "year": 2025}}""")
            appendLine("""{"category": null, "period": {"type": "range", "from": "YYYY-MM-DD", "to": "YYYY-MM-DD"}}""")
            appendLine("""{"category": null, "period": {"type": "all"}}""")
            appendLine("""{"unsupported": true}""")
            appendLine()
            appendLine("Rules:")
            appendLine(
                "- category: one of ${ReceiptCategory.entries.joinToString(", ") { it.name }}, " +
                    "when the question names it or a clear synonym (supermarket food is GROCERIES, " +
                    "restaurants and cafes are DINING). Use null when the question is about all spending.",
            )
            appendLine(
                "- month: a calendar month, 1 to 12. Use \"year\": null when the question names no year, " +
                    "such as \"in October\". For \"this month\" or \"last month\", give the year from today.",
            )
            appendLine(
                "- quarter: a calendar quarter, 1 to 4 (1 is January to March). For \"this quarter\" or " +
                    "\"last quarter\", give the quarter and year from today.",
            )
            appendLine("- year: a calendar year, such as \"in 2025\", \"this year\" or \"last year\".")
            appendLine(
                "- range: any other dates, both included, such as \"the last 30 days\", \"this week\" " +
                    "(Monday to today) or \"since 1 March\" (to today).",
            )
            appendLine("- all: when the question names no period.")
            appendLine(
                "- Reply {\"unsupported\": true} when the question isn't asking how much was spent, " +
                    "or names spending that fits none of the categories. For example: which shop, " +
                    "the biggest purchase, averages, comparisons or lists of receipts.",
            )
            appendLine()
            append("Question: ")
            append(question.replace(LINE_BREAKS, " ").trim())
        }

    private companion object {
        val LINE_BREAKS = Regex("""\s*[\r\n]+\s*""")
    }
}
