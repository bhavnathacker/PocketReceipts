package com.bhavnathacker.pocketreceipts.data.ai

import com.bhavnathacker.pocketreceipts.domain.scan.ExtractedReceipt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Reads the model's reply: the first `{` to the last `}` is decoded as the fields
 * object, so text or code fences around it are ignored. Unquoted values (a total
 * written as a number) are read as text and unknown keys are ignored.
 *
 * @throws UnreadableReplyException when the reply holds no fields object.
 */
internal class ReceiptReplyParser @Inject constructor() {

    operator fun invoke(reply: String): ExtractedReceipt {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end < start) throw UnreadableReplyException("No JSON object in the reply")
        val fields = try {
            json.decodeFromString<ReceiptFieldsDto>(reply.substring(start, end + 1))
        } catch (e: SerializationException) {
            throw UnreadableReplyException("The reply's JSON can't be read", e)
        } catch (e: IllegalArgumentException) {
            throw UnreadableReplyException("The reply's JSON can't be read", e)
        }
        return ExtractedReceipt(
            merchant = fields.merchant.orNullText(),
            date = fields.date.orNullText(),
            total = fields.total.orNullText(),
            currency = fields.currency.orNullText(),
            category = fields.category.orNullText(),
        )
    }

    /** The model sometimes writes `"null"` or an empty string for a missing value. */
    private fun String?.orNullText(): String? = this?.trim()?.takeUnless { it.isEmpty() || it.equals("null", true) }

    @Serializable
    private data class ReceiptFieldsDto(
        @SerialName("merchant") val merchant: String? = null,
        @SerialName("date") val date: String? = null,
        @SerialName("total") val total: String? = null,
        @SerialName("currency") val currency: String? = null,
        @SerialName("category") val category: String? = null,
    )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

/** The model's reply couldn't be read as a receipt's fields. */
internal class UnreadableReplyException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
