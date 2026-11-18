package com.bhavnathacker.pocketreceipts.domain.scan

import java.time.LocalDate

/**
 * Reads a receipt's fields from its recognized text with the on-device model.
 */
interface ReceiptExtractionRepository {

    /**
     * Returns the fields the model found in [lines] (the receipt's rows, top to
     * bottom). [today] and [defaultCurrency] help the model resolve two-digit years
     * and amounts without a currency.
     *
     * @throws Exception when the model can't be run or its reply can't be read.
     */
    suspend fun extract(lines: List<String>, today: LocalDate, defaultCurrency: String): ExtractedReceipt
}
