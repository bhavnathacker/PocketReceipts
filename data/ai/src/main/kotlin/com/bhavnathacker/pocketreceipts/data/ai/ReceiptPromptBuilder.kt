package com.bhavnathacker.pocketreceipts.data.ai

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds the prompt that asks the model for a receipt's fields as one JSON object.
 * The receipt's OCR rows are numbered so the model can tell rows apart.
 */
internal class ReceiptPromptBuilder @Inject constructor() {

    operator fun invoke(lines: List<String>, today: LocalDate, defaultCurrency: String): String =
        buildString {
            appendLine("You read the text of a shop receipt and extract its fields.")
            appendLine(
                "The text was read from a photo by OCR, one numbered line per row of the receipt. " +
                    "It can contain OCR mistakes, such as 0 for the letter O.",
            )
            appendLine()
            appendLine("Today is $today. The default currency is $defaultCurrency.")
            appendLine()
            appendLine("Reply with only one JSON object and no other text, in this form:")
            appendLine("""{"merchant": "...", "date": "YYYY-MM-DD", "total": "1234.50", "currency": "XXX", "category": "..."}""")
            appendLine()
            appendLine("Rules:")
            appendLine(
                "- merchant: the shop or business name as printed. Not an address, a document title " +
                    "(such as TAX INVOICE, RETAIL INVOICE or CASH MEMO) or a customer's name.",
            )
            appendLine(
                "- date: the purchase or invoice date as YYYY-MM-DD. A two-digit year means 20YY. " +
                    "Not an expiry date.",
            )
            appendLine(
                "- total: the amount actually paid, as a plain number with a dot for decimals and no " +
                    "thousands separators. When the receipt shows a gross total and a lower net, payable " +
                    "or grand total, use the amount paid.",
            )
            appendLine(
                "- currency: the ISO 4217 code of the total. Use a currency symbol or code printed on the " +
                    "receipt (such as ₹, Rs, €, \$, £). If there is none, use the currency of the country the " +
                    "receipt is from, judging by its address, phone numbers or tax numbers (an Indian GST number " +
                    "or city means INR). Use $defaultCurrency only when you can't tell.",
            )
            appendLine(
                "- category: one of ${ReceiptCategory.entries.joinToString(", ") { it.name }}, " +
                    "chosen from the kind of shop and what was bought.",
            )
            appendLine("- Use null for a field you can't find. Don't guess.")
            appendLine()
            appendLine("Receipt:")
            lines.forEachIndexed { index, line -> appendLine("${index + 1}. $line") }
        }.trimEnd()
}
