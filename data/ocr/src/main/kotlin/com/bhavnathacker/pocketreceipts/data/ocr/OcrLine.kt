package com.bhavnathacker.pocketreceipts.data.ocr

/**
 * One line of text as ML Kit found it, with its bounding box in image pixels.
 */
internal data class OcrLine(
    val text: String,
    val top: Int,
    val bottom: Int,
    val left: Int,
) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Int get() = bottom - top
}
