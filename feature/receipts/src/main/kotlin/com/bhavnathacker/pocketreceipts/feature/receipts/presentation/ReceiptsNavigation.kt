package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ReceiptScanToScanPrefillMapper
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
 * With [scan], it is the review form for a scanned receipt, prefilled from it.
 */
@Serializable
data class ReceiptFormKey(
    val receiptId: Long? = null,
    val scan: ScanPrefill? = null,
) : NavKey

/**
 * The values read from a scanned receipt, kept in the review form's key so they
 * survive configuration changes and process death with the back stack. Each is
 * null when nothing was read for it.
 *
 * @property date An ISO-8601 date (`2026-02-03`).
 * @property category A [ReceiptCategory][com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory] name.
 */
@Serializable
data class ScanPrefill(
    val merchant: String? = null,
    val date: String? = null,
    val total: ScanAmount? = null,
    val category: String? = null,
)

/**
 * The total in [ScanPrefill]: an amount in minor units and its ISO 4217 currency code.
 */
@Serializable
data class ScanAmount(
    val minorUnits: Long,
    val currencyCode: String,
)

/**
 * The review form for [scan]: the receipt form prefilled with its values.
 */
fun receiptReviewKey(scan: ReceiptScan): NavKey = ReceiptFormKey(scan = ReceiptScanToScanPrefillMapper()(scan))

/**
 * Registers the Receipts destinations with the app's [NavDisplay][androidx.navigation3.ui.NavDisplay].
 *
 * @param onNavigate Opens another receipts screen on top of the current one.
 * @param onBack Closes the current receipts screen.
 * @param onScanClick Starts scanning a receipt.
 */
fun EntryProviderScope<NavKey>.receiptsEntries(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onScanClick: () -> Unit,
) {
    entry<ReceiptsKey> {
        ReceiptListRoute(
            onReceiptClick = { id -> onNavigate(ReceiptDetailKey(id)) },
            onAddClick = { onNavigate(ReceiptFormKey()) },
            onScanClick = onScanClick,
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
            scan = key.scan,
            onClose = onBack,
        )
    }
}
