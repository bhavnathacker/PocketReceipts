package com.bhavnathacker.pocketreceipts.domain.scan

/**
 * Reads the text of a scanned receipt image on the device.
 */
interface TextRecognitionRepository {

    /**
     * Recognizes the text in the image at [imageUri].
     *
     * @throws Exception when the image can't be read or recognition fails.
     */
    suspend fun recognize(imageUri: String): RecognizedText
}
