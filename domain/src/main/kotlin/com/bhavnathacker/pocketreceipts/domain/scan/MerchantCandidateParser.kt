package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptValidator
import javax.inject.Inject

/**
 * Suggests the merchant: the first of the top [TOP_LINES] lines that reads like a
 * name. Such a line has at least three letters, more letters than digits, and
 * isn't a date, an amount, a phone number, a web or email address, or a tax
 * registration line. The result is trimmed, its inner whitespace collapsed, and
 * cut to [ReceiptValidator.MAX_MERCHANT_LENGTH] characters.
 */
class MerchantCandidateParser @Inject constructor() {

    operator fun invoke(lines: List<String>): String? =
        lines.take(TOP_LINES)
            .map { it.trim().replace(WHITESPACE, " ") }
            .firstOrNull(::isNameLike)
            ?.take(ReceiptValidator.MAX_MERCHANT_LENGTH)

    private fun isNameLike(line: String): Boolean {
        val letters = line.count { it.isLetter() }
        val digits = line.count { it.isDigit() }
        return letters >= MIN_LETTERS &&
            letters > digits &&
            NOT_A_NAME.none { it.containsMatchIn(line) }
    }

    companion object {
        /** How many lines from the top are searched. */
        const val TOP_LINES = 5

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
