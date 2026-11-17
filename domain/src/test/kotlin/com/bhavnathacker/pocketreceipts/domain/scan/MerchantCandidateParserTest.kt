package com.bhavnathacker.pocketreceipts.domain.scan

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MerchantCandidateParserTest {
    private lateinit var underTest: MerchantCandidateParser

    @BeforeAll
    fun setUp() {
        underTest = MerchantCandidateParser()
    }

    @Test
    fun `test that invoke returns the first name like line`() {
        assertThat(underTest(listOf("CORNER SHOP", "12 High Street", "Tel 020 7946 0000")))
            .isEqualTo("CORNER SHOP")
    }

    @Test
    fun `test that invoke skips a date line`() {
        assertThat(underTest(listOf("03/02/2026 10:15", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips an amount line`() {
        assertThat(underTest(listOf("TOTAL 210.00", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a phone line`() {
        assertThat(underTest(listOf("Ph: +91 98765 43210", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a web address line`() {
        assertThat(underTest(listOf("www.cornershop.in", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips an email line`() {
        assertThat(underTest(listOf("hello@cornershop.in", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a tax registration line`() {
        assertThat(underTest(listOf("GSTIN 29ABCDE1234F1Z5", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a line with fewer than three letters`() {
        assertThat(underTest(listOf("#12 *", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a line that is mostly digits`() {
        assertThat(underTest(listOf("0042 1234 ABC", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke collapses whitespace`() {
        assertThat(underTest(listOf("  CORNER    SHOP  "))).isEqualTo("CORNER SHOP")
    }

    @Test
    fun `test that invoke cuts the name to one hundred characters`() {
        assertThat(underTest(listOf("A".repeat(150)))).hasLength(100)
    }

    @Test
    fun `test that invoke returns null when no name is in the top five lines`() {
        val lines = listOf("01/02/2026", "12.00", "#1", "www.shop.in", "Tel 020 7946 0000", "Corner Shop")

        assertThat(underTest(lines)).isNull()
    }

    @Test
    fun `test that invoke returns null when there are no lines`() {
        assertThat(underTest(emptyList())).isNull()
    }

    @Test
    fun `test that invoke skips a retail invoice title`() {
        assertThat(underTest(listOf("RETAIL INVOICE", "HIT-MAN"))).isEqualTo("HIT-MAN")
    }

    @Test
    fun `test that invoke skips a tax invoice title`() {
        assertThat(underTest(listOf("TAX INVOICE", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a cash memo title`() {
        assertThat(underTest(listOf("Cash Memo", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke skips a copy marker`() {
        assertThat(underTest(listOf("ORIGINAL FOR RECIPIENT", "Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke strips a welcome prefix`() {
        assertThat(underTest(listOf("Welcome to Corner Shop"))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke prefers a line printed much larger than the others`() {
        val lines = listOf("GF-G/4, Poojan Apartment,", "Opp. Seema Hall, Anandnagar Road,", "HIT-MAN", "MEN'S WEAR")

        assertThat(underTest(lines, heights = listOf(70, 68, 202, 69))).isEqualTo("HIT-MAN")
    }

    @Test
    fun `test that invoke keeps the first name like line when sizes are similar`() {
        val lines = listOf("Corner Shop", "High Street Branch")

        assertThat(underTest(lines, heights = listOf(70, 80))).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke picks the shop name from a real receipt header`() {
        val lines = listOf(
            "RETAIL INVOICE",
            "HIT-MAN",
            "GF-G/4, Poojan Apartment,",
            "Opp. Seema Hall, Anandnagar Road,",
            "Satelite, Ahmedabad.",
            "MEN'S WEAR",
            "970008 24001191191 00194",
        )

        assertThat(underTest(lines, heights = listOf(66, 202, 70, 68, 55, 69, 63))).isEqualTo("HIT-MAN")
    }
}
