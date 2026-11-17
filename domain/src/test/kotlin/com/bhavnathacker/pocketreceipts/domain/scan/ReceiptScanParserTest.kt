package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.GetDefaultCurrencyUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/**
 * Synthetic OCR output of typical receipts, as `OcrRowAssembler` produces it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptScanParserTest {
    private lateinit var underTest: ReceiptScanParser

    private val indianReceipt = RecognizedText(
        listOf(
            "SHARMA GENERAL STORE",
            "MG Road, Bengaluru",
            "GSTIN 29ABCDE1234F1Z5",
            "Ph: 080 2345 6789",
            "Bill No 4521 Date: 03/02/2026",
            "Rice 5kg 1,250.00",
            "Dal 1kg 180.00",
            "Sub Total 1,430.00",
            "CGST 2.5% 35.75",
            "SGST 2.5% 35.75",
            "Grand Total Rs. 1,501.50",
        ),
    )

    private val usReceipt = RecognizedText(
        listOf(
            "BLUE BOTTLE CAFE",
            "123 Market St, San Francisco",
            "(415) 555-0123",
            "03/02/2026 09:41 AM",
            "Latte 5.25",
            "Croissant 4.50",
            "Subtotal 9.75",
            "Tax 0.85",
            "TOTAL $10.60",
        ),
    )

    private val ukReceipt = RecognizedText(
        listOf(
            "CORNER SHOP",
            "12 High Street",
            "Tel 020 7946 0000",
            "14/10/2026 18:02",
            "Milk 1.20",
            "Bread 1.10",
            "TOTAL £2.30",
            "CARD 2.30",
        ),
    )

    private val germanReceipt = RecognizedText(
        listOf(
            "Bäckerei Müller",
            "Hauptstr. 5, 10115 Berlin",
            "USt-IdNr. DE123456789",
            "Torte 1.230,00",
            "Kaffee 4,56",
            "SUMME EUR 1.234,56",
            "Datum 03.02.2026 14:05",
        ),
    )

    private val japaneseReceipt = RecognizedText(
        listOf(
            "SAKURA MART",
            "Tokyo",
            "2026/02/03 12:30",
            "Onigiri ¥150",
            "Tea ¥130",
            "TOTAL ¥280",
        ),
    )

    /** ML Kit's rows for a real, partly handwritten receipt (the handwriting isn't readable). */
    private val hitManReceipt = RecognizedText(
        lines = listOf(
            "RETAIL INVOICE",
            "HIT-MAN",
            "GF-G/4, Poojan Apartment,",
            "Opp. Seema Hall, Anandnagar Road,",
            "Satelite, Ahmedabad.",
            "MEN'S WEAR",
            "970008 24001191191 00194",
            "M/s. Bill No. :",
            "Dateea9|24",
            "Qty. Description Rate Amount",
            "39n",
            "TOTAL",
            "Exchange Time 1.00 to 4.00 pm. within 3 Days Only. For, HIT-MAN",
            "Silk & Embroidery Garments To Be Dry Cleaned Only. MEN'S WEAR",
            "Subject to Ahmedabad Jurisdiction",
        ),
        heights = listOf(66, 202, 70, 68, 55, 69, 63, 41, 133, 72, 145, 52, 104, 73, 83),
    )

    /** ML Kit's rows for a real printed pharmacy cash memo; the date reads `31-0ct-26`. */
    private val bharatMedicalReceipt = RecognizedText(
        lines = listOf(
            "BHARAT MEDICAL STORES",
            "OPP. SUN P PHOTO, SHYAMAL CHAR RASTA, SATELLITE, AHMEDABAD.",
            "B-107,B-105, POPULAR CENTRE, 132\"'ft. RING ROAD, NR. MEDILINK HOSPITAL",
            "INVOICE",
            "CASH MEMO GST No 24AANFB8284G176",
            "Customer: OPD",
            "ORIGINAL BSNL : (079) 26762820",
            "Doctor : Address: RAMESH RATHAV |WHATSHOP NO: 7778043555",
            "SELF Bill No C68642 31-0ct-26 21:44:2",
            "SR. |Cust Mo:",
            "DESCRIPTION HSN Mfg BatchNo Discounted Net",
            "CARTIGEN 1500 TAB",
            "ExpDtUnit MRP Disc% Amount Oty Amount",
            "2 30049099 PHA CTTS26020",
            "YASH INSECTS SPRAY",
            "11/28 15",
            "340.00 17% 282.20 30",
            "3004 COM 200 564.40",
            "1 200.00 O% 200.00 1",
            "200.00",
            "FREEEE HOME DELIVERY Gross Total 880.00",
            "sUBJECT TO AHMEDABAD JURISDICTION. Email-medicalbharat14 ®gmail.com",
            "Discount(Rs. 116.00",
            "*CUTTING STRIPS WONT BE RETURNED* *MEDICINES TO BE RETURNED WITHIN ONE MONTH*",
            "Round ff -0.40",
            "pLNo: 21-115808,21 B-115810. 20-115807, 20B-115809. AUTHORISED SIGN.",
            "NET 764.00",
            "RupeesSeven Hundred Sixty-Four Only",
        ),
        heights = listOf(251, 355, 192, 72, 67, 130, 59, 100, 79, 50, 52, 114, 64, 60, 110, 73, 48, 47, 45, 59, 64, 55, 71, 58, 59, 93, 69, 119),
    )

    @BeforeAll
    fun setUp() {
        val clock = Clock.fixed(LocalDate.of(2026, 11, 16).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        underTest = ReceiptScanParser(
            dateCandidateParser = DateCandidateParser(GetTodayUseCase(clock)),
            amountCandidateParser = AmountCandidateParser(GetDefaultCurrencyUseCase()),
            merchantCandidateParser = MerchantCandidateParser(),
            categoryCandidateParser = CategoryCandidateParser(),
        )
    }

    @Test
    fun `test that invoke finds the merchant of an Indian receipt`() {
        assertThat(underTest(indianReceipt, EN_IN).merchant).isEqualTo("SHARMA GENERAL STORE")
    }

    @Test
    fun `test that invoke finds the date of an Indian receipt day first`() {
        assertThat(underTest(indianReceipt, EN_IN).dates.first()).isEqualTo(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke preselects the grand total of an Indian receipt`() {
        assertThat(underTest(indianReceipt, EN_IN).totals.first()).isEqualTo(Money(150150, "INR"))
    }

    @Test
    fun `test that invoke finds the merchant of a US receipt`() {
        assertThat(underTest(usReceipt, EN_US).merchant).isEqualTo("BLUE BOTTLE CAFE")
    }

    @Test
    fun `test that invoke finds the date of a US receipt month first`() {
        assertThat(underTest(usReceipt, EN_US).dates.first()).isEqualTo(LocalDate.of(2026, 3, 2))
    }

    @Test
    fun `test that invoke preselects the total of a US receipt`() {
        assertThat(underTest(usReceipt, EN_US).totals.first()).isEqualTo(Money(1060, "USD"))
    }

    @Test
    fun `test that invoke finds the merchant of a UK receipt`() {
        assertThat(underTest(ukReceipt, EN_GB).merchant).isEqualTo("CORNER SHOP")
    }

    @Test
    fun `test that invoke finds the date of a UK receipt`() {
        assertThat(underTest(ukReceipt, EN_GB).dates.first()).isEqualTo(LocalDate.of(2026, 10, 14))
    }

    @Test
    fun `test that invoke preselects the total of a UK receipt`() {
        assertThat(underTest(ukReceipt, EN_GB).totals.first()).isEqualTo(Money(230, "GBP"))
    }

    @Test
    fun `test that invoke finds the merchant of a German receipt`() {
        assertThat(underTest(germanReceipt, DE_DE).merchant).isEqualTo("Bäckerei Müller")
    }

    @Test
    fun `test that invoke finds the date of a German receipt`() {
        assertThat(underTest(germanReceipt, DE_DE).dates.first()).isEqualTo(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke preselects the sum of a German receipt`() {
        assertThat(underTest(germanReceipt, DE_DE).totals.first()).isEqualTo(Money(123456, "EUR"))
    }

    @Test
    fun `test that invoke finds the merchant of a Japanese receipt`() {
        assertThat(underTest(japaneseReceipt, JA_JP).merchant).isEqualTo("SAKURA MART")
    }

    @Test
    fun `test that invoke finds the date of a Japanese receipt`() {
        assertThat(underTest(japaneseReceipt, JA_JP).dates.first()).isEqualTo(LocalDate.of(2026, 2, 3))
    }

    @Test
    fun `test that invoke preselects the yen total of a Japanese receipt`() {
        assertThat(underTest(japaneseReceipt, JA_JP).totals.first()).isEqualTo(Money(280, "JPY"))
    }

    @Test
    fun `test that invoke suggests Groceries for an Indian general store`() {
        assertThat(underTest(indianReceipt, EN_IN).category).isEqualTo(ReceiptCategory.GROCERIES)
    }

    @Test
    fun `test that invoke suggests Dining for a US cafe`() {
        assertThat(underTest(usReceipt, EN_US).category).isEqualTo(ReceiptCategory.DINING)
    }

    @Test
    fun `test that invoke suggests no category for a UK corner shop`() {
        assertThat(underTest(ukReceipt, EN_GB).category).isNull()
    }

    @Test
    fun `test that invoke suggests Shopping for a menswear receipt`() {
        val receipt = RecognizedText(listOf("MEN'S WEAR", "Linking Road", "Shirt 1,299.00", "Net Amount 1,169.10"))

        assertThat(underTest(receipt, EN_IN).category).isEqualTo(ReceiptCategory.SHOPPING)
    }

    @Test
    fun `test that invoke finds the shop name of a receipt titled retail invoice`() {
        assertThat(underTest(hitManReceipt, EN_IN).merchant).isEqualTo("HIT-MAN")
    }

    @Test
    fun `test that invoke suggests Shopping for a receipt from a menswear shop`() {
        assertThat(underTest(hitManReceipt, EN_IN).category).isEqualTo(ReceiptCategory.SHOPPING)
    }

    @Test
    fun `test that invoke offers no total when the amounts are handwritten`() {
        assertThat(underTest(hitManReceipt, EN_IN).totals).isEmpty()
    }

    @Test
    fun `test that invoke finds the printed date of a pharmacy cash memo`() {
        assertThat(underTest(bharatMedicalReceipt, EN_IN).dates).containsExactly(LocalDate.of(2026, 10, 31))
    }

    @Test
    fun `test that invoke finds the merchant of a pharmacy cash memo`() {
        assertThat(underTest(bharatMedicalReceipt, EN_IN).merchant).isEqualTo("BHARAT MEDICAL STORES")
    }

    @Test
    fun `test that invoke preselects the net amount of a pharmacy cash memo`() {
        assertThat(underTest(bharatMedicalReceipt, EN_IN).totals.first()).isEqualTo(Money(76400, "INR"))
    }

    @Test
    fun `test that invoke suggests Health for a pharmacy cash memo`() {
        assertThat(underTest(bharatMedicalReceipt, EN_IN).category).isEqualTo(ReceiptCategory.HEALTH)
    }

    @Test
    fun `test that invoke returns empty candidates when nothing was recognized`() {
        assertThat(underTest(RecognizedText(emptyList()), EN_IN)).isEqualTo(ReceiptScan(null, emptyList(), emptyList()))
    }

    private companion object {
        val EN_IN: Locale = Locale.forLanguageTag("en-IN")
        val EN_US: Locale = Locale.forLanguageTag("en-US")
        val EN_GB: Locale = Locale.forLanguageTag("en-GB")
        val DE_DE: Locale = Locale.forLanguageTag("de-DE")
        val JA_JP: Locale = Locale.forLanguageTag("ja-JP")
    }
}
