package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryCandidateParserTest {
    private lateinit var underTest: CategoryCandidateParser

    @BeforeAll
    fun setUp() {
        underTest = CategoryCandidateParser()
    }

    @Test
    fun `test that invoke suggests Shopping for a menswear shop`() {
        assertThat(underTest("Men's Wear", emptyList())).isEqualTo(ReceiptCategory.SHOPPING)
    }

    @Test
    fun `test that invoke suggests Shopping for a fashion store`() {
        assertThat(underTest("TRENDS FASHION", emptyList())).isEqualTo(ReceiptCategory.SHOPPING)
    }

    @Test
    fun `test that invoke suggests Health for a medical store`() {
        assertThat(underTest("SAI MEDICALS", emptyList())).isEqualTo(ReceiptCategory.HEALTH)
    }

    @Test
    fun `test that invoke suggests Health for a pharmacy`() {
        assertThat(underTest("City Pharmacy", emptyList())).isEqualTo(ReceiptCategory.HEALTH)
    }

    @Test
    fun `test that invoke suggests Groceries for a supermarket`() {
        assertThat(underTest("FRESH SUPERMARKET", emptyList())).isEqualTo(ReceiptCategory.GROCERIES)
    }

    @Test
    fun `test that invoke suggests Dining for a cafe`() {
        assertThat(underTest("BLUE BOTTLE CAFE", emptyList())).isEqualTo(ReceiptCategory.DINING)
    }

    @Test
    fun `test that invoke suggests Transport for a fuel station`() {
        assertThat(underTest("HP Petrol Pump", emptyList())).isEqualTo(ReceiptCategory.TRANSPORT)
    }

    @Test
    fun `test that invoke suggests Entertainment for a cinema`() {
        assertThat(underTest("PVR Cinemas", emptyList())).isEqualTo(ReceiptCategory.ENTERTAINMENT)
    }

    @Test
    fun `test that invoke uses a header line when the merchant name has no hint`() {
        assertThat(underTest("SHARMA & SONS", listOf("SHARMA & SONS", "Chemist and Druggist")))
            .isEqualTo(ReceiptCategory.HEALTH)
    }

    @Test
    fun `test that invoke prefers the merchant name over the header lines`() {
        assertThat(underTest("Men's Wear", listOf("Men's Wear", "Opp. City Hospital")))
            .isEqualTo(ReceiptCategory.SHOPPING)
    }

    @Test
    fun `test that invoke ignores lines below the header`() {
        val lines = listOf("ACME", "1", "2", "3", "4", "5", "6", "7", "Opp. City Hospital")

        assertThat(underTest("ACME", lines)).isNull()
    }

    @Test
    fun `test that invoke does not match a keyword inside another word`() {
        assertThat(underTest("BARBARA STUDIO", emptyList())).isNull()
    }

    @Test
    fun `test that invoke returns null when nothing hints at a category`() {
        assertThat(underTest("CORNER SHOP", emptyList())).isNull()
    }

    @Test
    fun `test that invoke returns null without a merchant or lines`() {
        assertThat(underTest(null, emptyList())).isNull()
    }

    @Test
    fun `test that invoke finds the trade printed below the address`() {
        val lines = listOf(
            "RETAIL INVOICE",
            "HIT-MAN",
            "GF-G/4, Poojan Apartment,",
            "Opp. Seema Hall, Anandnagar Road,",
            "Satelite, Ahmedabad.",
            "MEN'S WEAR",
        )

        assertThat(underTest("HIT-MAN", lines)).isEqualTo(ReceiptCategory.SHOPPING)
    }
}
