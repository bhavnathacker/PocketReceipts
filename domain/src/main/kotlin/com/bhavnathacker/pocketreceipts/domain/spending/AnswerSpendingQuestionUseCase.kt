package com.bhavnathacker.pocketreceipts.domain.spending

import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import javax.inject.Inject

/**
 * Answers a spending question typed in plain language.
 *
 * The on-device model only reads the question into a category and period;
 * [SpendingQueryChecker] checks that reading, and the totals are added up exactly from the
 * saved receipts. The model never sees or computes an amount.
 *
 * @throws Exception when the model can't be run.
 */
class AnswerSpendingQuestionUseCase @Inject constructor(
    private val spendingQuestionRepository: SpendingQuestionRepository,
    private val spendingQueryChecker: SpendingQueryChecker,
    private val receiptRepository: ReceiptRepository,
    private val getToday: GetTodayUseCase,
) {
    suspend operator fun invoke(question: String): SpendingAnswer {
        val text = question.trim()
        if (text.isEmpty()) return SpendingAnswer.Unanswerable
        val today = getToday()
        val interpreted = spendingQuestionRepository.interpret(text, today)
        val query = spendingQueryChecker(interpreted, today) ?: return SpendingAnswer.Unanswerable
        val totals = receiptRepository.spendingTotals(query.from, query.to, query.category)
        return SpendingAnswer.Totals(query, totals)
    }
}
