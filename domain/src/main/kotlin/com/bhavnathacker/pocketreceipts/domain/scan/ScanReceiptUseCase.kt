package com.bhavnathacker.pocketreceipts.domain.scan

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads a scanned receipt image and returns its candidate values.
 *
 * The image is deleted once it has been read, whether reading succeeds, fails or
 * is cancelled: the app never keeps scanned images.
 */
class ScanReceiptUseCase @Inject constructor(
    private val textRecognitionRepository: TextRecognitionRepository,
    private val scanImageRepository: ScanImageRepository,
    private val receiptScanParser: ReceiptScanParser,
) {
    suspend operator fun invoke(imageUri: String): ReceiptScan {
        val text = try {
            textRecognitionRepository.recognize(imageUri)
        } finally {
            withContext(NonCancellable) { scanImageRepository.delete(imageUri) }
        }
        return receiptScanParser(text)
    }
}
