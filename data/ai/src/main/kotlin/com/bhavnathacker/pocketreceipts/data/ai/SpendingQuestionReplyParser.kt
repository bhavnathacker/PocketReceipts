package com.bhavnathacker.pocketreceipts.data.ai

import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedPeriod
import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedQuestion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Reads the model's reply to [SpendingQuestionPromptBuilder]'s prompt: the first `{` to the
 * last `}` is decoded, so text or code fences around it are ignored. Numbers may be written as
 * strings; one that isn't a whole number is read as missing, for the checker to reject.
 *
 * A reply marked unsupported, or with no period or an unknown period type, is
 * [InterpretedQuestion.Unsupported].
 *
 * @throws UnreadableReplyException when the reply holds no object that can be read.
 */
internal class SpendingQuestionReplyParser @Inject constructor() {

    operator fun invoke(reply: String): InterpretedQuestion {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end < start) throw UnreadableReplyException("No JSON object in the reply")
        val dto = try {
            json.decodeFromString<QuestionDto>(reply.substring(start, end + 1))
        } catch (e: SerializationException) {
            throw UnreadableReplyException("The reply's JSON can't be read", e)
        } catch (e: IllegalArgumentException) {
            throw UnreadableReplyException("The reply's JSON can't be read", e)
        }
        if (dto.unsupported == true) return InterpretedQuestion.Unsupported
        val period = dto.period?.let(::period) ?: return InterpretedQuestion.Unsupported
        return InterpretedQuestion.Spending(category = dto.category.orNullText(), period = period)
    }

    private fun period(dto: PeriodDto): InterpretedPeriod? =
        when (dto.type?.trim()?.lowercase()) {
            "month" -> InterpretedPeriod.Month(year = dto.year.wholeNumber(), month = dto.month.wholeNumber())
            "quarter" -> InterpretedPeriod.Quarter(year = dto.year.wholeNumber(), quarter = dto.quarter.wholeNumber())
            "year" -> InterpretedPeriod.Year(year = dto.year.wholeNumber())
            "range" -> InterpretedPeriod.Range(from = dto.from.orNullText(), to = dto.to.orNullText())
            "all" -> InterpretedPeriod.AllTime
            else -> null
        }

    /** The model sometimes writes `"null"` or an empty string for a missing value. */
    private fun String?.orNullText(): String? = this?.trim()?.takeUnless { it.isEmpty() || it.equals("null", true) }

    private fun String?.wholeNumber(): Int? = orNullText()?.toIntOrNull()

    /** Values are read as text, so a number written with or without quotes reads the same. */
    @Serializable
    private data class QuestionDto(
        @SerialName("category") val category: String? = null,
        @SerialName("period") val period: PeriodDto? = null,
        @SerialName("unsupported") val unsupported: Boolean? = null,
    )

    @Serializable
    private data class PeriodDto(
        @SerialName("type") val type: String? = null,
        @SerialName("year") val year: String? = null,
        @SerialName("month") val month: String? = null,
        @SerialName("quarter") val quarter: String? = null,
        @SerialName("from") val from: String? = null,
        @SerialName("to") val to: String? = null,
    )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
