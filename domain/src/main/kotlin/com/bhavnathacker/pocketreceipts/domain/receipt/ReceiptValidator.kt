package com.bhavnathacker.pocketreceipts.domain.receipt

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Checks a [ReceiptInput] against the receipt rules and returns every field
 * that breaks one. An empty set means the input can be saved.
 *
 * Length rules apply to the trimmed text; "today" comes from [clock].
 */
class ReceiptValidator @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(input: ReceiptInput): Set<ReceiptFieldError> = buildSet {
        val merchant = input.merchant.trim()
        when {
            merchant.isEmpty() -> add(ReceiptFieldError.MerchantBlank)
            merchant.length > MAX_MERCHANT_LENGTH -> add(ReceiptFieldError.MerchantTooLong)
        }
        if (input.total.minorUnits <= 0) add(ReceiptFieldError.TotalNotPositive)
        if (input.purchaseDate.isAfter(LocalDate.now(clock))) add(ReceiptFieldError.DateInFuture)
        if ((input.note?.trim()?.length ?: 0) > MAX_NOTE_LENGTH) add(ReceiptFieldError.NoteTooLong)
    }

    companion object {
        /** Maximum merchant length, after trimming. */
        const val MAX_MERCHANT_LENGTH = 100

        /** Maximum note length, after trimming. */
        const val MAX_NOTE_LENGTH = 500
    }
}
