package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.bhavnathacker.pocketreceipts.data.ai.SpendingQuestionPromptBuilder
import com.bhavnathacker.pocketreceipts.data.ai.SpendingQuestionReplyParser
import com.bhavnathacker.pocketreceipts.data.ai.UnreadableReplyException
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedQuestion
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingQuestionRepository
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.TimeSource

/**
 * Asks the on-device model to read a spending question in one turn, with greedy sampling
 * (the same question always gives the same reading) and a short reply limit.
 *
 * Every question gets a new conversation on the shared engine ([LiteRtModelRuntime]), so
 * nothing from one question, or from a scanned receipt, reaches the next. The question is
 * never logged.
 */
internal class LiteRtSpendingQuestionRepository @Inject constructor(
    private val modelRepository: ModelRepository,
    private val runtime: LiteRtModelRuntime,
    private val promptBuilder: SpendingQuestionPromptBuilder,
    private val replyParser: SpendingQuestionReplyParser,
) : SpendingQuestionRepository {

    override suspend fun interpret(question: String, today: LocalDate): InterpretedQuestion {
        val modelPath = checkNotNull(modelRepository.modelPath()) { "The model isn't downloaded" }
        val prompt = promptBuilder(question, today)
        val reply = runtime.withEngine(modelPath) { engine ->
            engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                    maxOutputToken = MAX_OUTPUT_TOKENS,
                ),
            ).use { conversation ->
                val start = TimeSource.Monotonic.markNow()
                conversation.sendMessage(prompt).toString()
                    .also { Timber.d("Question read in %s", start.elapsedNow()) }
            }
        }
        return try {
            replyParser(reply)
        } catch (e: UnreadableReplyException) {
            Timber.w(e, "The question's reading can't be used")
            InterpretedQuestion.Unsupported
        }
    }

    private companion object {
        /** The longest reply form, a range, is well under this. */
        const val MAX_OUTPUT_TOKENS = 64
    }
}
