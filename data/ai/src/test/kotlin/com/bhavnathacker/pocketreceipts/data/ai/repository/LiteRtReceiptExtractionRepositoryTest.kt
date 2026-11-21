package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.AppVisibility
import com.bhavnathacker.pocketreceipts.data.ai.EngineFactory
import com.bhavnathacker.pocketreceipts.data.ai.GpuSupport
import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.bhavnathacker.pocketreceipts.data.ai.ReceiptPromptBuilder
import com.bhavnathacker.pocketreceipts.data.ai.ReceiptReplyParser
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.scan.ExtractedReceipt
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Message
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LiteRtReceiptExtractionRepositoryTest {

    private val modelPath = "/models/gemma.litertlm"
    private val today = LocalDate.of(2026, 11, 19)
    private val extracted = ExtractedReceipt(merchant = "Corner Shop")

    private val conversations = mutableListOf<Conversation>()
    private val engines = mutableListOf<Engine>()

    private fun newConversation(): Conversation = mock<Conversation> {
        on { sendMessage(any<String>(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn
            Message.model("{}")
    }.also { conversations += it }

    private val engineFactory = EngineFactory { _, _ ->
        mock<Engine> {
            on { createConversation(any()) } doAnswer { newConversation() }
        }.also { engines += it }
    }

    private val modelRepository = mock<ModelRepository> {
        onBlocking { modelPath() } doSuspendableAnswer { modelPath }
    }

    private val replyParser = mock<ReceiptReplyParser> {
        on { invoke(any()) } doReturn extracted
    }

    @Test
    fun `test that extract reads each receipt in a new conversation on the same engine`() = runTest {
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
        val underTest = LiteRtReceiptExtractionRepository(
            modelRepository = modelRepository,
            runtime = runtime,
            promptBuilder = ReceiptPromptBuilder(),
            replyParser = replyParser,
        )

        val first = underTest.extract(listOf("CORNER SHOP", "TOTAL 4.50"), today, "EUR")
        val second = underTest.extract(listOf("BHARAT MEDICAL STORES", "NET 764.00"), today, "INR")

        assertThat(first).isEqualTo(extracted)
        assertThat(second).isEqualTo(extracted)
        assertThat(engines).hasSize(1)
        verify(engines.single(), times(2)).createConversation(any())
        assertThat(conversations).hasSize(2)
        conversations.forEach { verify(it).close() }
    }
}
