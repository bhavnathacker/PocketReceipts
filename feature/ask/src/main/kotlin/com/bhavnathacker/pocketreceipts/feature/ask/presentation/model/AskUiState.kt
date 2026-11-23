package com.bhavnathacker.pocketreceipts.feature.ask.presentation.model

/**
 * State of the Ask screen.
 *
 * @property question The question being typed.
 * @property isAnswering True while a question is being answered.
 * @property answer The answer to the last question asked, or `null` before one is answered.
 * @property hasError True when the last question couldn't be answered because the model failed.
 */
internal data class AskUiState(
    val question: String = "",
    val isAnswering: Boolean = false,
    val answer: AskAnswerUiModel? = null,
    val hasError: Boolean = false,
) {
    /** Whether Ask can be tapped. */
    val canAsk: Boolean get() = question.isNotBlank() && !isAnswering
}
