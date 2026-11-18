package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads a scanned receipt image and returns its checked values.
 *
 * The image's text is recognized, then the on-device model reads the receipt's
 * fields from that text, and [ReceiptFieldsChecker] drops invalid ones. When no
 * text was recognized, the model isn't run and an empty [ReceiptScan] is returned.
 *
 * The image is deleted once its text has been read, whether reading succeeds,
 * fails or is cancelled: the app never keeps scanned images.
 */
class ScanReceiptUseCase @Inject constructor(
    private val textRecognitionRepository: TextRecognitionRepository,
    private val scanImageRepository: ScanImageRepository,
    private val receiptExtractionRepository: ReceiptExtractionRepository,
    private val receiptFieldsChecker: ReceiptFieldsChecker,
    private val getToday: GetTodayUseCase,
    private val getDefaultCurrency: GetDefaultCurrencyUseCase,
) {
    suspend operator fun invoke(imageUri: String): ReceiptScan {
        val text = try {
            textRecognitionRepository.recognize(imageUri)
        } finally {
            withContext(NonCancellable) { scanImageRepository.delete(imageUri) }
        }
        val lines = text.lines.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ReceiptScan()
        val today = getToday()
        val defaultCurrency = getDefaultCurrency()
        val extracted = receiptExtractionRepository.extract(lines, today, defaultCurrency)
        return receiptFieldsChecker(extracted, today, defaultCurrency)
    }
}
