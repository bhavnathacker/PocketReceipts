package com.bhavnathacker.pocketreceipts.feature.ask.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavnathacker.pocketreceipts.domain.ai.KeepModelReadyUseCase
import com.bhavnathacker.pocketreceipts.domain.spending.AnswerSpendingQuestionUseCase
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.mapper.SpendingAnswerUiMapper
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Answers spending questions on the Ask screen.
 *
 * While the screen is shown ([onShown] to [onHidden]) it keeps the on-device model loaded, so
 * a question is read without waiting for the load. The question text survives process death;
 * the answer survives configuration changes.
 */
@HiltViewModel
internal class AskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val answerSpendingQuestionUseCase: AnswerSpendingQuestionUseCase,
    private val keepModelReadyUseCase: KeepModelReadyUseCase,
    private val spendingAnswerUiMapper: SpendingAnswerUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AskUiState(question = savedStateHandle[QUESTION_KEY] ?: ""))
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    private var keepReadyJob: Job? = null

    fun onQuestionChange(question: String) {
        val text = question.take(MAX_QUESTION_LENGTH)
        savedStateHandle[QUESTION_KEY] = text
        _uiState.update { it.copy(question = text, hasError = false) }
    }

    fun onExampleSelected(example: String) {
        onQuestionChange(example)
    }

    /** Ask is shown: load the model, and keep it loaded until [onHidden]. */
    fun onShown() {
        if (keepReadyJob?.isActive == true) return
        keepReadyJob = viewModelScope.launch { keepModelReadyUseCase() }
    }

    /** Ask is no longer shown: the model may be released by the usual rules. */
    fun onHidden() {
        keepReadyJob?.cancel()
        keepReadyJob = null
    }

    fun onAsk() {
        val state = _uiState.value
        if (!state.canAsk) return
        _uiState.update { it.copy(isAnswering = true, answer = null, hasError = false) }
        viewModelScope.launch {
            try {
                val answer = spendingAnswerUiMapper(answerSpendingQuestionUseCase(state.question))
                _uiState.update { it.copy(isAnswering = false, answer = answer) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to answer the question")
                _uiState.update { it.copy(isAnswering = false, hasError = true) }
            }
        }
    }

    companion object {
        /** The longest question that can be typed. */
        const val MAX_QUESTION_LENGTH = 200

        private const val QUESTION_KEY = "question"
    }
}
