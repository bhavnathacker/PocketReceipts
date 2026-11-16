package com.bhavnathacker.pocketreceipts.domain.scan

/**
 * The text read from a scanned receipt.
 *
 * @property lines The visual rows of the receipt, top to bottom. Text that sits on
 * one row of the paper (a label and its amount) is one line, left to right.
 */
data class RecognizedText(
    val lines: List<String>,
)
