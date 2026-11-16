package com.bhavnathacker.pocketreceipts.feature.capture.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import kotlinx.serialization.Serializable

/**
 * The capture flow: opens the document scanner and reads the scan.
 */
@Serializable
data object CaptureKey : NavKey

/**
 * Registers the capture destination with the app's [NavDisplay][androidx.navigation3.ui.NavDisplay].
 *
 * @param onScanned The scan was read; the app shows it for review.
 * @param onAddManually The user chose to enter the receipt by hand instead.
 * @param onClose The user cancelled; the capture flow should close.
 */
fun EntryProviderScope<NavKey>.captureEntries(
    onScanned: (ReceiptScan) -> Unit,
    onAddManually: () -> Unit,
    onClose: () -> Unit,
) {
    entry<CaptureKey> {
        CaptureRoute(
            onScanned = onScanned,
            onAddManually = onAddManually,
            onClose = onClose,
        )
    }
}
