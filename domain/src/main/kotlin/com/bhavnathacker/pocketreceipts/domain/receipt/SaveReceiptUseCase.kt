package com.bhavnathacker.pocketreceipts.domain.receipt

import javax.inject.Inject

/**
 * Saves a receipt: adds it when [invoke]'s `id` is `null`, otherwise updates the
 * receipt with that id.
 *
 * Merchant and note are trimmed, and a blank note is saved as `null`. The input
 * is validated with [ReceiptValidator]; if it is invalid nothing is saved.
 */
class SaveReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val receiptValidator: ReceiptValidator,
) {
    suspend operator fun invoke(id: Long?, input: ReceiptInput): SaveReceiptResult {
        val normalized = input.copy(
            merchant = input.merchant.trim(),
            note = input.note?.trim()?.takeIf { it.isNotEmpty() },
        )
        val errors = receiptValidator(normalized)
        if (errors.isNotEmpty()) return SaveReceiptResult.Invalid(errors)

        return if (id == null) {
            SaveReceiptResult.Saved(receiptRepository.addReceipt(normalized))
        } else {
            receiptRepository.updateReceipt(id, normalized)
            SaveReceiptResult.Saved(id)
        }
    }
}
