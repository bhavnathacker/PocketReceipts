package com.bhavnathacker.pocketreceipts.data.receipts.repository

import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptDao
import com.bhavnathacker.pocketreceipts.data.receipts.mapper.ReceiptEntityMapper
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** [ReceiptRepository] backed by the on-device Room database. */
@Singleton
internal class DefaultReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val receiptEntityMapper: ReceiptEntityMapper,
    private val clock: Clock,
) : ReceiptRepository {

    override fun monitorReceipts(): Flow<List<Receipt>> =
        receiptDao.observeAll().map { receiptEntityMapper(it) }

    override fun monitorReceipt(id: Long): Flow<Receipt?> =
        receiptDao.observeById(id).map { entity -> entity?.let { receiptEntityMapper(it) } }

    override suspend fun addReceipt(input: ReceiptInput): Long {
        val now = clock.millis()
        return receiptDao.insert(receiptEntityMapper(input = input, id = 0, createdAt = now, updatedAt = now))
    }

    override suspend fun updateReceipt(id: Long, input: ReceiptInput) {
        val createdAt = receiptDao.getCreatedAt(id) ?: return
        receiptDao.update(receiptEntityMapper(input = input, id = id, createdAt = createdAt, updatedAt = clock.millis()))
    }

    override suspend fun deleteReceipt(id: Long) {
        receiptDao.deleteById(id)
    }
}
