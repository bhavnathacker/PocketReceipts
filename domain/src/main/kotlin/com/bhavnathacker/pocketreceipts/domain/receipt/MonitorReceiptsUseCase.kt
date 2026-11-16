package com.bhavnathacker.pocketreceipts.domain.receipt

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * All receipts, newest purchase date first, updated whenever they change.
 */
class MonitorReceiptsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    operator fun invoke(): Flow<List<Receipt>> = receiptRepository.monitorReceipts()
}
