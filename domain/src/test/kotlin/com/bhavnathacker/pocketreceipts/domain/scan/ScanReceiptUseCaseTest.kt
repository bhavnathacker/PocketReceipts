package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
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
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanReceiptUseCaseTest {
    private lateinit var underTest: ScanReceiptUseCase

    private val textRecognitionRepository = mock<TextRecognitionRepository>()
    private val scanImageRepository = mock<ScanImageRepository>()
    private val receiptExtractionRepository = mock<ReceiptExtractionRepository>()

    private val today = LocalDate.of(2026, 11, 18)
    private val lines = listOf("CORNER SHOP", "TOTAL 4.50")
    private val defaultCurrency = GetDefaultCurrencyUseCase()(Locale.getDefault())

    @BeforeAll
    fun setUp() {
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        underTest = ScanReceiptUseCase(
            textRecognitionRepository = textRecognitionRepository,
            scanImageRepository = scanImageRepository,
            receiptExtractionRepository = receiptExtractionRepository,
            receiptFieldsChecker = ReceiptFieldsChecker(),
            getToday = GetTodayUseCase(clock),
            getDefaultCurrency = GetDefaultCurrencyUseCase(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(textRecognitionRepository, scanImageRepository, receiptExtractionRepository)
    }

    @Test
    fun `test that invoke returns the checked fields the model read`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(RecognizedText(lines))
        whenever(receiptExtractionRepository.extract(lines, today, defaultCurrency))
            .thenReturn(ExtractedReceipt(merchant = "CORNER SHOP", date = "2026-12-01", total = "4.50", currency = "EUR"))

        assertThat(underTest(IMAGE_URI))
            .isEqualTo(ReceiptScan(merchant = "CORNER SHOP", date = null, total = Money(450, "EUR")))
    }

    @Test
    fun `test that invoke does not run the model when no text was recognized`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(RecognizedText(listOf(" ", "")))

        assertThat(underTest(IMAGE_URI)).isEqualTo(ReceiptScan())
        verify(receiptExtractionRepository, never()).extract(any(), any(), any())
    }

    @Test
    fun `test that invoke deletes the image when recognition succeeds`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(RecognizedText(lines))
        whenever(receiptExtractionRepository.extract(any(), any(), any())).thenReturn(ExtractedReceipt())

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
    fun `test that invoke throws when extraction fails`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(RecognizedText(lines))
        whenever(receiptExtractionRepository.extract(any(), any(), any()))
            .thenAnswer { throw IllegalStateException("unreadable reply") }

        assertThrows<IllegalStateException> { underTest(IMAGE_URI) }
    }

    @Test
    fun `test that invoke deletes the image when extraction fails`() = runTest {
        whenever(textRecognitionRepository.recognize(IMAGE_URI)).thenReturn(RecognizedText(lines))
        whenever(receiptExtractionRepository.extract(any(), any(), any()))
            .thenAnswer { throw IllegalStateException("unreadable reply") }

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
