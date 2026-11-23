package com.bhavnathacker.pocketreceipts.domain.spending

import java.time.LocalDate

/**
 * Reads spending questions with the on-device model.
 */
interface SpendingQuestionRepository {

    /**
     * Returns the model's reading of [question]. [today] lets it resolve words such as
     * "this quarter" or "last month". A reply that can't be read is [InterpretedQuestion.Unsupported].
     *
     * @throws Exception when the model can't be run.
     */
    suspend fun interpret(question: String, today: LocalDate): InterpretedQuestion
}
