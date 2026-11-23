package com.bhavnathacker.pocketreceipts.domain.receipt

import com.bhavnathacker.pocketreceipts.domain.spending.SpendingTotal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

    /**
     * The totals of the receipts whose purchase date is from [from] to [to] (both included;
     * `null` means no limit) and, unless [category] is `null`, whose category is [category].
     * Totals are added up exactly, one per currency, the currency with the most receipts
     * first (then by currency code). Empty when no receipt matches.
     */
    suspend fun spendingTotals(from: LocalDate?, to: LocalDate?, category: ReceiptCategory?): List<SpendingTotal>
}
