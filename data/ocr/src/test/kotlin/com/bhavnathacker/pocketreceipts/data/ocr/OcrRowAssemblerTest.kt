package com.bhavnathacker.pocketreceipts.data.ocr

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Boxes are taken from ML Kit's output for a drawn receipt (see the change notes). */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OcrRowAssemblerTest {
    private lateinit var underTest: OcrRowAssembler

    @BeforeAll
    fun setUp() {
        underTest = OcrRowAssembler()
    }

    @Test
    fun `test that invoke joins lines on the same row left to right`() {
        val lines = listOf(
            OcrLine("210.00", top = 891, bottom = 920, left = 562),
            OcrLine("TOTAL", top = 889, bottom = 921, left = 80),
        )

        assertThat(underTest(lines).lines).containsExactly("TOTAL 210.00")
    }

    @Test
    fun `test that invoke orders rows top to bottom`() {
        val lines = listOf(
            OcrLine("TOTAL", top = 889, bottom = 921, left = 80),
            OcrLine("CORNER SHOP", top = 121, bottom = 150, left = 82),
            OcrLine("Milk", top = 450, bottom = 480, left = 84),
        )

        assertThat(underTest(lines).lines).containsExactly("CORNER SHOP", "Milk", "TOTAL").inOrder()
    }

    @Test
    fun `test that invoke groups a slightly skewed row`() {
        val lines = listOf(
            OcrLine("TOTAL", top = 880, bottom = 912, left = 80),
            OcrLine("210.00", top = 893, bottom = 922, left = 562),
            OcrLine("Card", top = 990, bottom = 1020, left = 80),
        )

        assertThat(underTest(lines).lines).containsExactly("TOTAL 210.00", "Card").inOrder()
    }

    @Test
    fun `test that invoke keeps lines a full row apart on separate rows`() {
        val lines = listOf(
            OcrLine("SUBTOTAL", top = 671, bottom = 700, left = 82),
            OcrLine("TAX", top = 781, bottom = 810, left = 81),
        )

        assertThat(underTest(lines).lines).containsExactly("SUBTOTAL", "TAX").inOrder()
    }

    @Test
    fun `test that invoke returns no rows when there are no lines`() {
        assertThat(underTest(emptyList()).lines).isEmpty()
    }

    /** The header of a real receipt: a large shop name beside a block of small address lines. */
    private val hitManHeader = listOf(
        OcrLine("RETAIL INVOICE", top = 611, bottom = 677, left = 481),
        OcrLine("GF-G/4, Poojan Apartment,", top = 709, bottom = 779, left = 514),
        OcrLine("Opp. Seema Hall, Anandnagar Road,", top = 776, bottom = 844, left = 513),
        OcrLine("Satelite, Ahmedabad.", top = 842, bottom = 897, left = 511),
        OcrLine("970008 24001191191 00194", top = 924, bottom = 987, left = 551),
        OcrLine("HIT-MAN", top = 664, bottom = 866, left = 1472),
        OcrLine("MEN'S WEAR", top = 878, bottom = 947, left = 1666),
    )

    @Test
    fun `test that invoke keeps a much taller line apart from the small text beside it`() {
        assertThat(underTest(hitManHeader).lines).containsExactly(
            "RETAIL INVOICE",
            "HIT-MAN",
            "GF-G/4, Poojan Apartment,",
            "Opp. Seema Hall, Anandnagar Road,",
            "Satelite, Ahmedabad.",
            "MEN'S WEAR",
            "970008 24001191191 00194",
        ).inOrder()
    }
}
