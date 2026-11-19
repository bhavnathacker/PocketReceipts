package com.bhavnathacker.pocketreceipts.domain.scan

/**
 * The fields the on-device model read from a receipt's text, exactly as it
 * returned them. Nothing here is checked yet: see [ReceiptFieldsChecker].
 *
 * @property merchant The shop's name.
 * @property date The purchase date, expected as ISO-8601 (`2026-10-31`).
 * @property total The amount paid, expected as a plain decimal (`764.00`).
 * @property currency The total's currency, expected as an ISO 4217 code (`INR`).
 * @property category One of the app's category names (`HEALTH`).
 */
data class ExtractedReceipt(
    val merchant: String? = null,
    val date: String? = null,
    val total: String? = null,
    val currency: String? = null,
    val category: String? = null,
)
