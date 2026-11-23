package com.bhavnathacker.pocketreceipts.core.ui.format

import androidx.annotation.StringRes
import com.bhavnathacker.pocketreceipts.core.ui.R
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import javax.inject.Inject

/**
 * The display label of a [ReceiptCategory].
 */
class ReceiptCategoryLabelMapper @Inject constructor() {

    @StringRes
    operator fun invoke(category: ReceiptCategory): Int =
        when (category) {
            ReceiptCategory.GROCERIES -> R.string.category_groceries
            ReceiptCategory.DINING -> R.string.category_dining
            ReceiptCategory.TRANSPORT -> R.string.category_transport
            ReceiptCategory.SHOPPING -> R.string.category_shopping
            ReceiptCategory.BILLS -> R.string.category_bills
            ReceiptCategory.HEALTH -> R.string.category_health
            ReceiptCategory.ENTERTAINMENT -> R.string.category_entertainment
            ReceiptCategory.TRAVEL -> R.string.category_travel
            ReceiptCategory.OTHER -> R.string.category_other
        }
}
