package com.bhavnathacker.pocketreceipts.feature.capture.presentation

import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.domain.scan.ScanReceiptUseCase
import com.bhavnathacker.pocketreceipts.feature.capture.presentation.model.CaptureStatus
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import java.io.IOException

@ExtendWith(CoroutineMainDispatcherExtension::class)
class CaptureViewModelTest {
    private lateinit var underTest: CaptureViewModel

    private val scanReceiptUseCase = mock<ScanReceiptUseCase>()

    private val scan = ReceiptScan(merchant = "Corner Shop")

    @AfterEach
    fun tearDown() {
        reset(scanReceiptUseCase)
    }

    private fun initViewModel() {
        underTest = CaptureViewModel(scanReceiptUseCase = scanReceiptUseCase)
    }

    private fun stubScan(answer: suspend () -> ReceiptScan) {
        scanReceiptUseCase.stub { onBlocking { invoke(any()) } doSuspendableAnswer { answer() } }
    }

    @Test
    fun `test that initial status is Scanning`() {
        initViewModel()

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.Scanning)
    }

    @Test
    fun `test that onImageScanned moves to Reading`() = runTest {
        stubScan { awaitCancellation() }
        initViewModel()

        underTest.onImageScanned(IMAGE_URI)

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.Reading)
    }

    @Test
    fun `test that onImageScanned fires scannedEvent with the scan when reading succeeds`() = runTest {
        stubScan { scan }
        initViewModel()

        underTest.onImageScanned(IMAGE_URI)

        assertThat(underTest.uiState.value.scannedEvent).isEqualTo(StateEventWithContentTriggered(scan))
    }

    @Test
    fun `test that onImageScanned moves to ReadFailed when reading fails`() = runTest {
        stubScan { throw IOException("unreadable") }
        initViewModel()

        underTest.onImageScanned(IMAGE_URI)

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.ReadFailed)
    }

    @Test
    fun `test that onImageScanned moves to ReadFailed when the receipt reader fails`() = runTest {
        stubScan { throw IllegalStateException("unreadable reply") }
        initViewModel()

        underTest.onImageScanned(IMAGE_URI)

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.ReadFailed)
    }

    @Test
    fun `test that onImageScanned moves to ReadFailed when the receipt reader runs out of memory`() = runTest {
        stubScan { throw OutOfMemoryError("model") }
        initViewModel()

        underTest.onImageScanned(IMAGE_URI)

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.ReadFailed)
    }

    @Test
    fun `test that onCancel fires closeEvent`() {
        initViewModel()

        underTest.onCancel()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onCancel cancels reading`() = runTest {
        var cancelled = false
        stubScan {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        initViewModel()
        underTest.onImageScanned(IMAGE_URI)

        underTest.onCancel()

        assertThat(cancelled).isTrue()
    }

    @Test
    fun `test that onScannerUnavailable moves to Unavailable`() {
        initViewModel()

        underTest.onScannerUnavailable()

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.Unavailable)
    }

    @Test
    fun `test that onRetry moves back to Scanning`() = runTest {
        stubScan { throw IOException("unreadable") }
        initViewModel()
        underTest.onImageScanned(IMAGE_URI)

        underTest.onRetry()

        assertThat(underTest.uiState.value.status).isEqualTo(CaptureStatus.Scanning)
    }

    @Test
    fun `test that onRetry starts a new scan attempt`() {
        initViewModel()

        underTest.onRetry()

        assertThat(underTest.uiState.value.scanAttempt).isEqualTo(1)
    }

    @Test
    fun `test that onScannedEventConsumed consumes scannedEvent`() = runTest {
        stubScan { scan }
        initViewModel()
        underTest.onImageScanned(IMAGE_URI)

        underTest.onScannedEventConsumed()

        assertThat(underTest.uiState.value.scannedEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
    }

    @Test
    fun `test that onCloseEventConsumed consumes closeEvent`() {
        initViewModel()
        underTest.onCancel()

        underTest.onCloseEventConsumed()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    private companion object {
        const val IMAGE_URI = "file:///cache/mlkit_docscan_ui_client/1.jpg"
    }
}
