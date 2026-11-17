package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import java.util.Locale
import javax.inject.Inject

/**
 * Suggests a receipt's category from words that name the kind of shop: the
 * merchant name first (`Men's Wear` → Shopping, `Sai Medicals` → Health), then the
 * top [HEADER_LINES] lines, where a shop's trade is often printed under its name.
 * Keywords match whole words, with an optional plural `s`; when a text hints at
 * more than one category, the earliest hint wins. Returns null when nothing hints
 * at a category.
 */
class CategoryCandidateParser @Inject constructor() {

    operator fun invoke(merchant: String?, lines: List<String>): ReceiptCategory? =
        merchant?.let(::categoryIn) ?: categoryIn(lines.take(HEADER_LINES).joinToString("\n"))

    private fun categoryIn(text: String): ReceiptCategory? {
        // "Men's" -> "mens", so possessives read as plain words.
        val normalized = text.lowercase(Locale.ROOT).replace(APOSTROPHE, "")
        return KEYWORD_PATTERNS
            .mapNotNull { (category, pattern) -> pattern.find(normalized)?.let { it.range.first to category } }
            .minByOrNull { it.first }
            ?.second
    }

    companion object {
        /** How many lines from the top are searched when the merchant name has no hint. */
        const val HEADER_LINES = 5

        private val APOSTROPHE = Regex("""['’]""")

        private val KEYWORDS: Map<ReceiptCategory, List<String>> = mapOf(
            ReceiptCategory.HEALTH to listOf(
                "medical", "pharmacy", "pharma", "chemist", "druggist", "drug", "medicine", "hospital",
                "clinic", "diagnostic", "pathology", "dental", "dentist", "healthcare", "optician",
                "nursing home", "apotheke",
            ),
            ReceiptCategory.SHOPPING to listOf(
                "wear", "menswear", "womenswear", "fashion", "apparel", "garment", "boutique", "clothing",
                "clothes", "textile", "footwear", "shoe", "saree", "jewellery", "jeweller", "jewelry",
                "jeweler", "electronics", "mall", "emporium", "tailor",
            ),
            ReceiptCategory.GROCERIES to listOf(
                "supermarket", "super market", "hypermarket", "grocery", "groceries", "kirana", "provision",
                "mart", "general store", "vegetable", "fruit", "dairy", "supermarkt",
            ),
            ReceiptCategory.DINING to listOf(
                "restaurant", "cafe", "café", "coffee", "bistro", "pub", "bar", "dhaba", "eatery", "diner",
                "grill", "pizza", "pizzeria", "burger", "biryani", "canteen", "bakery", "sweet", "kitchen",
                "food court",
            ),
            ReceiptCategory.TRANSPORT to listOf(
                "petrol", "petroleum", "fuel", "filling station", "gas station", "parking", "taxi", "cab",
                "metro", "toll",
            ),
            ReceiptCategory.TRAVEL to listOf(
                "airline", "airways", "airport", "travels", "tours", "resort", "lodge", "motel", "hostel",
            ),
            ReceiptCategory.ENTERTAINMENT to listOf(
                "cinema", "theatre", "theater", "multiplex", "movie", "gaming", "bowling", "amusement",
            ),
            ReceiptCategory.BILLS to listOf(
                "electricity", "broadband", "telecom", "recharge", "insurance", "utilities", "water supply",
            ),
        )

        private val KEYWORD_PATTERNS: List<Pair<ReceiptCategory, Regex>> =
            KEYWORDS.map { (category, words) ->
                val alternatives = words.joinToString("|") { Regex.escape(it).replace(" ", """\E\s+\Q""") }
                category to Regex("""(?<!\p{L})(?:$alternatives)s?(?!\p{L})""")
            }
    }
}
