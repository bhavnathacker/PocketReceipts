package com.bhavnathacker.pocketreceipts.domain.receipt

/**
 * Why a [ReceiptInput] field is not valid.
 */
enum class ReceiptFieldError {
    /** The merchant is empty or whitespace only. */
    MerchantBlank,

    /** The trimmed merchant is longer than [ReceiptValidator.MAX_MERCHANT_LENGTH]. */
    MerchantTooLong,

    /** The total is zero or negative. */
    TotalNotPositive,

    /** The purchase date is after today. */
    DateInFuture,

    /** The trimmed note is longer than [ReceiptValidator.MAX_NOTE_LENGTH]. */
    NoteTooLong,
}
