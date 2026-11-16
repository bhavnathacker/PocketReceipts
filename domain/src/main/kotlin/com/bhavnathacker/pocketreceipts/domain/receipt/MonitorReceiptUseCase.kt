package com.bhavnathacker.pocketreceipts.domain.receipt

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * One receipt, updated whenever it changes; `null` once it no longer exists.
 */
class MonitorReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    operator fun invoke(id: Long): Flow<Receipt?> = receiptRepository.monitorReceipt(id)
}
