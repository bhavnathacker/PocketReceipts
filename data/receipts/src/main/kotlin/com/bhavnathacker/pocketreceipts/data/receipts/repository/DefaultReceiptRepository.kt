package com.bhavnathacker.pocketreceipts.data.receipts.repository

import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptDao
import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptsDatabaseProvider
import com.bhavnathacker.pocketreceipts.data.receipts.mapper.ReceiptEntityMapper
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ReceiptRepository] backed by the encrypted Room database. The database is
 * opened lazily on first use; if that fails, the error reaches the caller
 * through the returned flow or suspend call.
 */
@Singleton
internal class DefaultReceiptRepository @Inject constructor(
    private val databaseProvider: ReceiptsDatabaseProvider,
    private val receiptEntityMapper: ReceiptEntityMapper,
    private val clock: Clock,
) : ReceiptRepository {

    override fun monitorReceipts(): Flow<List<Receipt>> =
        flow { emitAll(dao().observeAll()) }
            .map { receiptEntityMapper(it) }

    override fun monitorReceipt(id: Long): Flow<Receipt?> =
        flow { emitAll(dao().observeById(id)) }
            .map { entity -> entity?.let { receiptEntityMapper(it) } }

    override suspend fun addReceipt(input: ReceiptInput): Long {
        val now = clock.millis()
        return dao().insert(receiptEntityMapper(input = input, id = 0, createdAt = now, updatedAt = now))
    }

    override suspend fun updateReceipt(id: Long, input: ReceiptInput) {
        val dao = dao()
        val createdAt = dao.getCreatedAt(id) ?: return
        dao.update(receiptEntityMapper(input = input, id = id, createdAt = createdAt, updatedAt = clock.millis()))
    }

    override suspend fun deleteReceipt(id: Long) {
        dao().deleteById(id)
    }

    private suspend fun dao(): ReceiptDao = databaseProvider.get().receiptDao()
}
