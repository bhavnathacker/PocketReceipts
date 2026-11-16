package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiModel
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a [Receipt] to what its detail screen shows.
 */
internal class ReceiptDetailUiMapper @Inject constructor(
    private val moneyFormatter: MoneyFormatter,
    private val receiptDateFormatter: ReceiptDateFormatter,
    private val receiptCategoryLabelMapper: ReceiptCategoryLabelMapper,
) {

    operator fun invoke(receipt: Receipt, locale: Locale = Locale.getDefault()): ReceiptDetailUiModel =
        ReceiptDetailUiModel(
            merchant = receipt.merchant,
            date = receiptDateFormatter(receipt.purchaseDate, locale),
            total = moneyFormatter(receipt.total, locale),
            categoryLabel = receiptCategoryLabelMapper(receipt.category),
            note = receipt.note,
        )
}
