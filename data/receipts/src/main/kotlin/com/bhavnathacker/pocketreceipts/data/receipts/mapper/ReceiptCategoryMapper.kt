package com.bhavnathacker.pocketreceipts.data.receipts.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import timber.log.Timber
import javax.inject.Inject

/**
 * Maps a [ReceiptCategory] to the stable key stored in the database and back.
 * Keys never change, even if an enum constant is renamed.
 */
internal class ReceiptCategoryMapper @Inject constructor() {

    operator fun invoke(category: ReceiptCategory): String =
        when (category) {
            ReceiptCategory.GROCERIES -> "groceries"
            ReceiptCategory.DINING -> "dining"
            ReceiptCategory.TRANSPORT -> "transport"
            ReceiptCategory.SHOPPING -> "shopping"
            ReceiptCategory.BILLS -> "bills"
            ReceiptCategory.HEALTH -> "health"
            ReceiptCategory.ENTERTAINMENT -> "entertainment"
            ReceiptCategory.TRAVEL -> "travel"
            ReceiptCategory.OTHER -> "other"
        }

    /**
     * The category stored under [key], or [ReceiptCategory.OTHER] for an unknown key.
     */
    operator fun invoke(key: String): ReceiptCategory =
        ReceiptCategory.entries.firstOrNull { invoke(it) == key }
            ?: ReceiptCategory.OTHER.also { Timber.w("Unknown receipt category key, using OTHER") }
}
