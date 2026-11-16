package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListItemUiModel
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a [Receipt] to a row of the receipt list.
 */
internal class ReceiptListItemUiMapper @Inject constructor(
    private val moneyFormatter: MoneyFormatter,
    private val receiptDateFormatter: ReceiptDateFormatter,
    private val receiptCategoryLabelMapper: ReceiptCategoryLabelMapper,
) {

    operator fun invoke(receipt: Receipt, locale: Locale = Locale.getDefault()): ReceiptListItemUiModel =
        ReceiptListItemUiModel(
            id = receipt.id,
            merchant = receipt.merchant,
            date = receiptDateFormatter(receipt.purchaseDate, locale),
            total = moneyFormatter(receipt.total, locale),
            categoryLabel = receiptCategoryLabelMapper(receipt.category),
        )

    operator fun invoke(receipts: List<Receipt>, locale: Locale = Locale.getDefault()): List<ReceiptListItemUiModel> =
        receipts.map { invoke(it, locale) }
}
