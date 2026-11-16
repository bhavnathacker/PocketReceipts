package com.bhavnathacker.pocketreceipts.domain.receipt

/**
 * Outcome of [SaveReceiptUseCase].
 */
sealed interface SaveReceiptResult {

    /** The receipt was saved under [id]. */
    data class Saved(val id: Long) : SaveReceiptResult

    /** Nothing was saved because of [errors]. */
    data class Invalid(val errors: Set<ReceiptFieldError>) : SaveReceiptResult
}
