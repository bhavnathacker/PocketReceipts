package com.bhavnathacker.pocketreceipts.domain.receipt

import javax.inject.Inject

/**
 * Permanently deletes a receipt.
 */
class DeleteReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    suspend operator fun invoke(id: Long) = receiptRepository.deleteReceipt(id)
}
