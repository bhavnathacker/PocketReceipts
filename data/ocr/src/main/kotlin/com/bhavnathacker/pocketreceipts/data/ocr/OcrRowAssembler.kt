package com.bhavnathacker.pocketreceipts.data.ocr

import javax.inject.Inject

/**
 * Rebuilds the visual rows of a receipt from ML Kit's lines.
 *
 * ML Kit often returns a row such as "TOTAL ........ 210.00" as separate lines.
 * Lines whose vertical centres lie within half the median line height of a row
 * join that row, left to right, separated by a space. Rows are returned top to
 * bottom.
 */
internal class OcrRowAssembler @Inject constructor() {

    operator fun invoke(lines: List<OcrLine>): List<String> {
        if (lines.isEmpty()) return emptyList()
        val tolerance = lines.map { it.height }.sorted()[lines.size / 2] / 2f
        val rows = mutableListOf<MutableList<OcrLine>>()
        lines.sortedBy { it.centerY }.forEach { line ->
            val row = rows.lastOrNull()
            if (row != null && kotlin.math.abs(line.centerY - row.map { it.centerY }.average()) <= tolerance) {
                row += line
            } else {
                rows += mutableListOf(line)
            }
        }
        return rows
            .sortedBy { row -> row.minOf { it.top } }
            .map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
    }
}
