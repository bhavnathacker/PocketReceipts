package com.bhavnathacker.pocketreceipts.feature.capture.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhavnathacker.pocketreceipts.domain.scan.ScanReceiptUseCase
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureStatus
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Reads the image the document scanner returns and reports the scan, or why
 * there is none. Scanned images are deleted by [ScanReceiptUseCase].
 */
@HiltViewModel
internal class CaptureViewModel @Inject constructor(
    private val scanReceiptUseCase: ScanReceiptUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var readingJob: Job? = null

    fun onImageScanned(imageUri: String) {
        _uiState.update { it.copy(status = CaptureStatus.Reading) }
        readingJob = viewModelScope.launch {
            try {
                val scan = scanReceiptUseCase(imageUri)
                _uiState.update { it.copy(scannedEvent = triggered(scan)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to read the scanned receipt")
                _uiState.update { it.copy(status = CaptureStatus.ReadFailed) }
            }
        }
    }

    fun onCancel() {
        readingJob?.cancel()
        readingJob = null
        _uiState.update { it.copy(closeEvent = triggered) }
    }

    fun onScannerUnavailable() {
        _uiState.update { it.copy(status = CaptureStatus.Unavailable) }
    }

    fun onRetry() {
        _uiState.update { it.copy(status = CaptureStatus.Scanning, scanAttempt = it.scanAttempt + 1) }
    }

    fun onScannedEventConsumed() {
        _uiState.update { it.copy(scannedEvent = consumed()) }
    }

    fun onCloseEventConsumed() {
        _uiState.update { it.copy(closeEvent = consumed) }
    }
}
