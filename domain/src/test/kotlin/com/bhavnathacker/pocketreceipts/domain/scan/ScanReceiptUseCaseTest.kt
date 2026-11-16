package com.bhavnathacker.pocketreceipts.domain.scan

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanReceiptUseCaseTest {
    private lateinit var underTest: ScanReceiptUseCase

    private val textRecognitionRepository = mock<TextRecognitionRepository>()
    private val scanImageRepository = mock<ScanImageRepository>()
    private val receiptScanParser = mock<ReceiptScanParser>()

    private val recognizedText = RecognizedText(listOf("CORNER SHOP", "TOTAL 4.50"))
    private val scan = ReceiptScan(merchant = "CORNER SHOP", dates = emptyList(), totals = emptyList())

    @BeforeAll
    fun setUp() {
        underTest = ScanReceiptUseCase(
            textRecognitionRepository = textRecognitionRepository,
            scanImageRepository = scanImageRepository,
            receiptScanParser = receiptScanParser,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(textRecognitionRepository, scanImageRepository, receiptScanParser)
    }

    @Test
    fun `test that invoke returns the parsed scan`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(recognizedText)
        whenever(receiptScanParser.invoke(any(), any())).thenReturn(scan)

        assertThat(underTest(IMAGE_URI)).isEqualTo(scan)
    }

    @Test
    fun `test that invoke parses the recognized text`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(recognizedText)
        whenever(receiptScanParser.invoke(any(), any())).thenReturn(scan)

        underTest(IMAGE_URI)

        verify(receiptScanParser).invoke(org.mockito.kotlin.eq(recognizedText), any())
    }

    @Test
    fun `test that invoke deletes the image when recognition succeeds`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(recognizedText)
        whenever(receiptScanParser.invoke(any(), any())).thenReturn(scan)

        underTest(IMAGE_URI)

        verify(scanImageRepository).delete(IMAGE_URI)
    }

    @Test
    fun `test that invoke throws when recognition fails`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenAnswer { throw IOException("unreadable") }

        assertThrows<IOException> { underTest(IMAGE_URI) }
    }

    @Test
    fun `test that invoke deletes the image when recognition fails`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenAnswer { throw IOException("unreadable") }

        runCatching { underTest(IMAGE_URI) }

        verify(scanImageRepository).delete(IMAGE_URI)
    }

    @Test
    fun `test that invoke deletes the image when cancelled`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).doSuspendableAnswer { awaitCancellation() }
        val job = launch { underTest(IMAGE_URI) }
        runCurrent()

        job.cancel()
        runCurrent()

        verify(scanImageRepository).delete(IMAGE_URI)
    }

    private companion object {
        const val IMAGE_URI = "file:///data/user/0/app/cache/mlkit_docscan_ui_client/1.jpg"
    }
}
