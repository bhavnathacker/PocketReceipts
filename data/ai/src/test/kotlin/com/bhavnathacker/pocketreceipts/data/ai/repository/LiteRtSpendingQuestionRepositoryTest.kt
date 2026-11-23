package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.AppVisibility
import com.bhavnathacker.pocketreceipts.data.ai.EngineFactory
import com.bhavnathacker.pocketreceipts.data.ai.GpuSupport
import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.bhavnathacker.pocketreceipts.data.ai.SpendingQuestionPromptBuilder
import com.bhavnathacker.pocketreceipts.data.ai.SpendingQuestionReplyParser
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedPeriod
import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedQuestion
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Message
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LiteRtSpendingQuestionRepositoryTest {

    private val modelPath = "/models/gemma.litertlm"
    private val today = LocalDate.of(2026, 11, 21)

    private var reply = """{"category": "GROCERIES", "period": {"type": "month", "year": 2026, "month": 10}}"""
    private val prompts = mutableListOf<String>()
    private val conversations = mutableListOf<Conversation>()
    private val engines = mutableListOf<Engine>()

    private fun newConversation(): Conversation = mock<Conversation> {
        on { sendMessage(any<String>(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doAnswer {
            prompts += it.getArgument<String>(0)
            Message.model(reply)
        }
    }.also { conversations += it }

    private val engineFactory = EngineFactory { _, _ ->
        mock<Engine> {
            on { createConversation(any()) } doAnswer { newConversation() }
        }.also { engines += it }
    }

    private val modelRepository = mock<ModelRepository> {
        onBlocking { modelPath() } doSuspendableAnswer { modelPath }
    }

    private fun TestScope.repository(): LiteRtSpendingQuestionRepository {
        val runtime = LiteRtModelRuntime(
            engineFactory = engineFactory,
            gpuSupport = object : GpuSupport {
                override val isAvailable = false
            },
            appVisibility = object : AppVisibility {
                override val isInForeground = MutableStateFlow(true)
            },
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        runCurrent()
        return LiteRtSpendingQuestionRepository(
            modelRepository = modelRepository,
            runtime = runtime,
            promptBuilder = SpendingQuestionPromptBuilder(),
            replyParser = SpendingQuestionReplyParser(),
        )
    }

    @Test
    fun `test that interpret returns the parsed reading of the question`() = runTest {
        val actual = repository().interpret("How much did I spend on groceries in October?", today)

        assertThat(actual).isEqualTo(InterpretedQuestion.Spending("GROCERIES", InterpretedPeriod.Month(2026, 10)))
        assertThat(prompts.single()).endsWith("Question: How much did I spend on groceries in October?")
        assertThat(prompts.single()).contains("Today is 2026-11-21")
    }

    @Test
    fun `test that interpret reads with greedy sampling and a short reply limit`() = runTest {
        repository().interpret("How much in 2025?", today)

        val config = argumentCaptor<ConversationConfig>().apply { verify(engines.single()).createConversation(capture()) }.firstValue
        assertThat(config.maxOutputToken).isEqualTo(64)
        assertThat(config.samplerConfig?.topK).isEqualTo(1)
        assertThat(config.samplerConfig?.temperature).isEqualTo(0.0)
    }

    @Test
    fun `test that interpret reads each question in a new conversation on the same engine`() = runTest {
        val underTest = repository()

        underTest.interpret("How much in 2025?", today)
        underTest.interpret("How much this quarter?", today)

        assertThat(engines).hasSize(1)
        verify(engines.single(), times(2)).createConversation(any())
        assertThat(conversations).hasSize(2)
        conversations.forEach { verify(it).close() }
    }

    @Test
    fun `test that interpret returns Unsupported when the reply can't be read`() = runTest {
        reply = "Sorry, I can't help with that."

        assertThat(repository().interpret("How much in 2025?", today)).isEqualTo(InterpretedQuestion.Unsupported)
    }
}
