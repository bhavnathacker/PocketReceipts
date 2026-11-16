package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The Receipts top-level destination: the receipt list.
 */
@Serializable
data object ReceiptsKey : NavKey

/**
 * A receipt's detail screen, shown on top of the Receipts destination.
 */
@Serializable
data class ReceiptDetailKey(val receiptId: Long) : NavKey

/**
 * The receipt form: adds a receipt when [receiptId] is `null`, otherwise edits it.
 */
@Serializable
data class ReceiptFormKey(val receiptId: Long? = null) : NavKey

/**
 * Registers the Receipts destinations with the app's [NavDisplay][androidx.navigation3.ui.NavDisplay].
 *
 * @param onNavigate Opens another receipts screen on top of the current one.
 * @param onBack Closes the current receipts screen.
 */
fun EntryProviderScope<NavKey>.receiptsEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    entry<ReceiptsKey> {
        ReceiptListRoute(
            onReceiptClick = { id -> onNavigate(ReceiptDetailKey(id)) },
            onAddClick = { onNavigate(ReceiptFormKey()) },
        )
    }
    entry<ReceiptDetailKey> { key ->
        ReceiptDetailRoute(
            receiptId = key.receiptId,
            onEdit = { onNavigate(ReceiptFormKey(key.receiptId)) },
            onClose = onBack,
        )
    }
    entry<ReceiptFormKey> { key ->
        ReceiptFormRoute(
            receiptId = key.receiptId,
            onClose = onBack,
        )
    }
}
