package com.bhavnathacker.pocketreceipts.feature.ask.presentation

import androidx.lifecycle.SavedStateHandle
import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.bhavnathacker.pocketreceipts.domain.ai.KeepModelReadyUseCase
import com.bhavnathacker.pocketreceipts.domain.spending.AnswerSpendingQuestionUseCase
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingAnswer
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.mapper.SpendingAnswerUiMapper
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskAnswerUiModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.io.IOException

@ExtendWith(CoroutineMainDispatcherExtension::class)
class AskViewModelTest {
    private lateinit var underTest: AskViewModel

    private val answerSpendingQuestionUseCase = mock<AnswerSpendingQuestionUseCase>()
    private val keepModelReadyUseCase = mock<KeepModelReadyUseCase>()
    private val spendingAnswerUiMapper = mock<SpendingAnswerUiMapper>()

    private val question = "How much did I spend on groceries in October?"
    private val answer = mock<SpendingAnswer.Totals>()
    private val answerUi = AskAnswerUiModel.NothingSpent(categoryLabel = 1, period = "1 – 31 Oct 2026")

    private var keepReadyCancelled = false

    @BeforeEach
    fun setUp() {
        keepReadyCancelled = false
        keepModelReadyUseCase.stub {
            onBlocking { invoke() } doSuspendableAnswer {
                try {
                    awaitCancellation()
                } finally {
                    keepReadyCancelled = true
                }
            }
        }
        answerSpendingQuestionUseCase.stub { onBlocking { invoke(any()) } doReturn answer }
        spendingAnswerUiMapper.stub { on { invoke(any(), any()) } doReturn answerUi }
    }

    @AfterEach
    fun tearDown() {
        reset(answerSpendingQuestionUseCase, keepModelReadyUseCase, spendingAnswerUiMapper)
    }

    private fun initViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) {
        underTest = AskViewModel(
            savedStateHandle = savedStateHandle,
            answerSpendingQuestionUseCase = answerSpendingQuestionUseCase,
            keepModelReadyUseCase = keepModelReadyUseCase,
            spendingAnswerUiMapper = spendingAnswerUiMapper,
        )
    }

    @Test
    fun `test that initial state has an empty question and no answer`() = runTest {
        initViewModel()

        val state = underTest.uiState.value
        assertThat(state.question).isEmpty()
        assertThat(state.answer).isNull()
        assertThat(state.canAsk).isFalse()
    }

    @Test
    fun `test that init restores the question from the saved state`() = runTest {
        val handle = SavedStateHandle()
        initViewModel(handle)
        underTest.onQuestionChange(question)

        initViewModel(handle)

        assertThat(underTest.uiState.value.question).isEqualTo(question)
    }

    @Test
    fun `test that onQuestionChange cuts the question to the maximum length`() = runTest {
        initViewModel()

        underTest.onQuestionChange("a".repeat(250))

        assertThat(underTest.uiState.value.question).hasLength(AskViewModel.MAX_QUESTION_LENGTH)
    }

    @Test
    fun `test that onExampleSelected puts the example in the question`() = runTest {
        initViewModel()

        underTest.onExampleSelected(question)

        assertThat(underTest.uiState.value.question).isEqualTo(question)
        assertThat(underTest.uiState.value.canAsk).isTrue()
    }

    @Test
    fun `test that onShown keeps the model ready`() = runTest {
        initViewModel()

        underTest.onShown()

        verify(keepModelReadyUseCase).invoke()
        assertThat(keepReadyCancelled).isFalse()
    }

    @Test
    fun `test that onShown keeps the model ready only once when called twice`() = runTest {
        initViewModel()

        underTest.onShown()
        underTest.onShown()

        verify(keepModelReadyUseCase, times(1)).invoke()
    }

    @Test
    fun `test that onHidden stops keeping the model ready`() = runTest {
        initViewModel()
        underTest.onShown()

        underTest.onHidden()

        assertThat(keepReadyCancelled).isTrue()
    }

    @Test
    fun `test that the model isn't kept ready before onShown`() = runTest {
        initViewModel()

        verify(keepModelReadyUseCase, never()).invoke()
    }

    @Test
    fun `test that onAsk shows the mapped answer`() = runTest {
        initViewModel()
        underTest.onQuestionChange(question)

        underTest.onAsk()

        verify(answerSpendingQuestionUseCase).invoke(question)
        val state = underTest.uiState.value
        assertThat(state.answer).isEqualTo(answerUi)
        assertThat(state.isAnswering).isFalse()
        assertThat(state.question).isEqualTo(question)
    }

    @Test
    fun `test that onAsk shows answering until the answer arrives`() = runTest {
        val result = CompletableDeferred<SpendingAnswer>()
        answerSpendingQuestionUseCase.stub { onBlocking { invoke(any()) } doSuspendableAnswer { result.await() } }
        initViewModel()
        underTest.onQuestionChange(question)

        underTest.onAsk()

        assertThat(underTest.uiState.value.isAnswering).isTrue()
        assertThat(underTest.uiState.value.canAsk).isFalse()
        result.complete(answer)
        assertThat(underTest.uiState.value.isAnswering).isFalse()
    }

    @Test
    fun `test that onAsk shows an error and keeps the question when the model fails`() = runTest {
        answerSpendingQuestionUseCase.stub { onBlocking { invoke(any()) } doSuspendableAnswer { throw IOException("engine") } }
        initViewModel()
        underTest.onQuestionChange(question)

        underTest.onAsk()

        val state = underTest.uiState.value
        assertThat(state.hasError).isTrue()
        assertThat(state.answer).isNull()
        assertThat(state.isAnswering).isFalse()
        assertThat(state.question).isEqualTo(question)
    }

    @Test
    fun `test that onAsk shows an error when there isn't enough memory for the model`() = runTest {
        answerSpendingQuestionUseCase.stub { onBlocking { invoke(any()) } doSuspendableAnswer { throw OutOfMemoryError("model") } }
        initViewModel()
        underTest.onQuestionChange(question)

        underTest.onAsk()

        assertThat(underTest.uiState.value.hasError).isTrue()
        assertThat(underTest.uiState.value.isAnswering).isFalse()
    }

    @Test
    fun `test that onQuestionChange clears the error`() = runTest {
        answerSpendingQuestionUseCase.stub { onBlocking { invoke(any()) } doSuspendableAnswer { throw IOException("engine") } }
        initViewModel()
        underTest.onQuestionChange(question)
        underTest.onAsk()

        underTest.onQuestionChange("$question ")

        assertThat(underTest.uiState.value.hasError).isFalse()
    }

    @Test
    fun `test that onAsk does nothing when the question is blank`() = runTest {
        initViewModel()
        underTest.onQuestionChange("   ")

        underTest.onAsk()

        verify(answerSpendingQuestionUseCase, never()).invoke(any())
    }

    @Test
    fun `test that onAsk does nothing while a question is being answered`() = runTest {
        answerSpendingQuestionUseCase.stub { onBlocking { invoke(any()) } doSuspendableAnswer { awaitCancellation() } }
        initViewModel()
        underTest.onQuestionChange(question)

        underTest.onAsk()
        underTest.onAsk()

        verify(answerSpendingQuestionUseCase, times(1)).invoke(any())
    }
}
