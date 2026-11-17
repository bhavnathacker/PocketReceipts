package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptValidator
import javax.inject.Inject

/**
 * Suggests the merchant from the top [TOP_LINES] lines that read like a name.
 * Such a line has at least three letters, more letters than digits, and isn't a
 * document title (`RETAIL INVOICE`, `CASH MEMO`), a date, an amount, a phone
 * number, a web or email address, or a tax registration line.
 *
 * When [heights] are known, a name-like line printed at least [PROMINENT_RATIO]
 * times the receipt's median text height wins (the largest one): shops print
 * their name biggest. Otherwise the first name-like line wins. A leading
 * "Welcome to" is dropped, whitespace is collapsed, and the result is cut to
 * [ReceiptValidator.MAX_MERCHANT_LENGTH] characters.
 */
class MerchantCandidateParser @Inject constructor() {

    operator fun invoke(lines: List<String>, heights: List<Int> = emptyList()): String? {
        val candidates = lines.take(TOP_LINES)
            .mapIndexed { index, line -> index to line.trim().replace(WHITESPACE, " ").replace(WELCOME, "") }
            .filter { (_, line) -> isNameLike(line) }
        val prominent = if (heights.size == lines.size && heights.isNotEmpty()) {
            val median = heights.sorted()[heights.size / 2]
            candidates
                .filter { (index, _) -> heights[index] >= median * PROMINENT_RATIO }
                .maxByOrNull { (index, _) -> heights[index] }
        } else {
            null
        }
        return (prominent ?: candidates.firstOrNull())?.second?.take(ReceiptValidator.MAX_MERCHANT_LENGTH)
    }

    private fun isNameLike(line: String): Boolean {
        val letters = line.count { it.isLetter() }
        val digits = line.count { it.isDigit() }
        return letters >= MIN_LETTERS &&
            letters > digits &&
            !isDocumentTitle(line) &&
            NOT_A_NAME.none { it.containsMatchIn(line) }
    }

    private fun isDocumentTitle(line: String): Boolean =
        line.lowercase().split(NON_LETTERS).filter { it.isNotEmpty() }.all { it in TITLE_WORDS }

    companion object {
        /** How many lines from the top are searched. */
        const val TOP_LINES = 5

        /** How much taller than the median text a line must be to count as prominent. */
        const val PROMINENT_RATIO = 1.5

        private val NON_LETTERS = Regex("""[^\p{L}]+""")
        private val WELCOME = Regex("""(?i)^welcome\s+to\s+""")

        /** Words that make up a document title or copy marker rather than a name. */
        private val TITLE_WORDS = setOf(
            "retail", "tax", "gst", "cash", "credit", "sales", "invoice", "bill", "memo", "receipt",
            "estimate", "quotation", "original", "duplicate", "triplicate", "copy", "cum", "for",
            "recipient", "customer", "supplier", "rechnung", "quittung", "kassenbon",
        )

        private const val MIN_LETTERS = 3
        private val WHITESPACE = Regex("""\s+""")
        private val NOT_A_NAME = listOf(
            // Dates
            Regex("""\d{1,4}[./-]\d{1,2}[./-]\d{2,4}"""),
            // Amounts
            Regex("""\d[.,]\d{2}(?!\d)"""),
            // Phone numbers
            Regex("""(?i)\b(tel|ph|phone|mob|mobile|fax)\b"""),
            Regex("""\+?\d[\d\s-]{6,}\d"""),
            // Web and email addresses
            Regex("""(?i)(https?://|www\.|@|\.(com|in|net|org|co|de|uk)\b)"""),
            // Tax registration lines
            Regex("""(?i)\b(gstin|gst|vat|tax\s*id|ust-?id(nr)?|tin)\b"""),
        )
    }
}
