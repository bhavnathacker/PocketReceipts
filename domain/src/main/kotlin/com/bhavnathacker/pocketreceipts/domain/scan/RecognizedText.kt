package com.bhavnathacker.pocketreceipts.domain.scan

/**
 * The text read from a scanned receipt.
 *
 * @property lines The visual rows of the receipt, top to bottom. Text that sits on
 * one row of the paper (a label and its amount) is one line, left to right.
 * @property heights The text height of each line in [lines], in image pixels, in the
 * same order; larger text (such as a shop's name) is taller. Empty when unknown.
 */
data class RecognizedText(
    val lines: List<String>,
    val heights: List<Int> = emptyList(),
)
