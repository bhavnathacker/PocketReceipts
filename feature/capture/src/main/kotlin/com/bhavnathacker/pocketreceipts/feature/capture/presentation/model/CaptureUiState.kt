package com.bhavnathacker.pocketreceipts.feature.capture.presentation.model

import androidx.compose.runtime.Stable
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Where the capture flow is.
 */
enum class CaptureStatus {
    /** The document scanner is open (or about to open); the screen stays blank. */
    Scanning,

    /** The scanned image is being read. */
    Reading,

    /** The scanner can't run on this device. */
    Unavailable,

    /** Reading the scanned image failed. */
    ReadFailed,
}

/**
 * The capture screen.
 *
 * @property scanAttempt Increases on every retry, so the scanner is launched once per attempt.
 * @property scannedEvent The scan was read; carries its candidate values.
 * @property closeEvent The user cancelled; the capture flow should close.
 */
@Stable
data class CaptureUiState(
    val status: CaptureStatus = CaptureStatus.Scanning,
    val scanAttempt: Int = 0,
    val scannedEvent: StateEventWithContent<ReceiptScan> = consumed(),
    val closeEvent: StateEvent = consumed,
)
