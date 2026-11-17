package com.bhavnathacker.pocketreceipts.data.ocr

import com.bhavnathacker.pocketreceipts.domain.scan.RecognizedText
import javax.inject.Inject
import kotlin.math.abs

/**
 * Rebuilds the visual rows of a receipt from ML Kit's lines.
 *
 * ML Kit often returns a row such as "TOTAL ........ 210.00" as separate lines.
 * Lines whose vertical centres lie within half the median line height of a row,
 * and whose text is of similar size, join that row, left to right, separated by
 * a space. Text much larger than its neighbours (a shop's name printed beside its
 * address) stays on a row of its own. Rows are returned top to bottom, each with
 * the height of its tallest text.
 */
internal class OcrRowAssembler @Inject constructor() {

    operator fun invoke(lines: List<OcrLine>): RecognizedText {
        if (lines.isEmpty()) return RecognizedText(emptyList(), emptyList())
        val tolerance = lines.map { it.height }.sorted()[lines.size / 2] / 2f
        val rows = mutableListOf<MutableList<OcrLine>>()
        lines.sortedBy { it.centerY }.forEach { line ->
            val row = rows.lastOrNull()
            val sameRow = row != null &&
                abs(line.centerY - row.map { it.centerY }.average()) <= tolerance &&
                row.all { similarSize(it, line) }
            if (sameRow) row!! += line else rows += mutableListOf(line)
        }
        val ordered = rows.sortedBy { row -> row.minOf { it.top } }
        return RecognizedText(
            lines = ordered.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } },
            heights = ordered.map { row -> row.maxOf { it.height } },
        )
    }

    private fun similarSize(a: OcrLine, b: OcrLine): Boolean {
        val larger = maxOf(a.height, b.height).toFloat()
        val smaller = minOf(a.height, b.height).coerceAtLeast(1)
        return larger / smaller <= MAX_HEIGHT_RATIO
    }

    private companion object {
        /** Lines more than this many times taller than another don't share its row. */
        const val MAX_HEIGHT_RATIO = 2f
    }
}
