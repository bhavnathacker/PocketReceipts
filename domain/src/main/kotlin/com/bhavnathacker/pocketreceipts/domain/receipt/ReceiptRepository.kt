package com.bhavnathacker.pocketreceipts.domain.receipt

import kotlinx.coroutines.flow.Flow

/**
 * On-device store of the user's receipts.
 */
interface ReceiptRepository {

    /**
     * All receipts, newest purchase date first and, for the same date, most
     * recently added first. Emits again whenever receipts change.
     */
    fun monitorReceipts(): Flow<List<Receipt>>

    /**
     * The receipt with [id], or `null` if it does not exist (or was deleted).
     * Emits again whenever it changes.
     */
    fun monitorReceipt(id: Long): Flow<Receipt?>

    /**
     * Stores a new receipt and returns its id. [input] must already be valid.
     */
    suspend fun addReceipt(input: ReceiptInput): Long

    /**
     * Replaces the fields of the receipt with [id]. [input] must already be valid.
     */
    suspend fun updateReceipt(id: Long, input: ReceiptInput)

    /**
     * Permanently deletes the receipt with [id]. Does nothing if it does not exist.
     */
    suspend fun deleteReceipt(id: Long)
}
