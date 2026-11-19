package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.bhavnathacker.pocketreceipts.data.ai.ReceiptPromptBuilder
import com.bhavnathacker.pocketreceipts.data.ai.ReceiptReplyParser
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.scan.ExtractedReceipt
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptExtractionRepository
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.TimeSource

/**
 * Asks the on-device model for a receipt's fields in one turn, with greedy
 * sampling (the same text always gives the same answer) and a short reply limit.
 *
 * The model stays loaded between receipts ([LiteRtModelRuntime]), but every receipt
 * gets a new conversation, so nothing from one receipt reaches the next.
 */
internal class LiteRtReceiptExtractionRepository @Inject constructor(
    private val modelRepository: ModelRepository,
    private val runtime: LiteRtModelRuntime,
    private val promptBuilder: ReceiptPromptBuilder,
    private val replyParser: ReceiptReplyParser,
) : ReceiptExtractionRepository {

    override suspend fun extract(lines: List<String>, today: LocalDate, defaultCurrency: String): ExtractedReceipt {
        val modelPath = checkNotNull(modelRepository.modelPath()) { "The model isn't downloaded" }
        val prompt = promptBuilder(lines, today, defaultCurrency)
        val reply = runtime.withEngine(modelPath) { engine ->
            engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                    maxOutputToken = MAX_OUTPUT_TOKENS,
                ),
            ).use { conversation ->
                val start = TimeSource.Monotonic.markNow()
                conversation.sendMessage(prompt).toString()
                    .also { Timber.d("Receipt read in %s", start.elapsedNow()) }
            }
        }
        return replyParser(reply)
    }

    private companion object {
        /** A filled-in fields object is well under this. */
        const val MAX_OUTPUT_TOKENS = 256
    }
}
