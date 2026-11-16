package com.bhavnathacker.pocketreceipts.data.receipts.mapper

import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptEntity
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import java.time.LocalDate
import javax.inject.Inject

/**
 * Maps between [ReceiptEntity] rows and the domain [Receipt] / [ReceiptInput].
 */
internal class ReceiptEntityMapper @Inject constructor(
    private val receiptCategoryMapper: ReceiptCategoryMapper,
) {

    operator fun invoke(entity: ReceiptEntity): Receipt =
        Receipt(
            id = entity.id,
            merchant = entity.merchant,
            purchaseDate = LocalDate.ofEpochDay(entity.purchaseDate),
            total = Money(minorUnits = entity.totalMinor, currencyCode = entity.currencyCode),
            category = receiptCategoryMapper(entity.category),
            note = entity.note,
        )

    operator fun invoke(entities: List<ReceiptEntity>): List<Receipt> =
        entities.map { invoke(it) }

    /**
     * A row for [input]. Use `id = 0` for a new row so Room assigns the id.
     */
    operator fun invoke(
        input: ReceiptInput,
        id: Long,
        createdAt: Long,
        updatedAt: Long,
    ): ReceiptEntity =
        ReceiptEntity(
            id = id,
            merchant = input.merchant,
            purchaseDate = input.purchaseDate.toEpochDay(),
            totalMinor = input.total.minorUnits,
            currencyCode = input.total.currencyCode,
            category = receiptCategoryMapper(input.category),
            note = input.note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
