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

        assertThat(underTest(lines)).containsExactly("TOTAL 210.00")
    }

    @Test
    fun `test that invoke orders rows top to bottom`() {
        val lines = listOf(
            OcrLine("TOTAL", top = 889, bottom = 921, left = 80),
            OcrLine("CORNER SHOP", top = 121, bottom = 150, left = 82),
            OcrLine("Milk", top = 450, bottom = 480, left = 84),
        )

        assertThat(underTest(lines)).containsExactly("CORNER SHOP", "Milk", "TOTAL").inOrder()
    }

    @Test
    fun `test that invoke groups a slightly skewed row`() {
        val lines = listOf(
            OcrLine("TOTAL", top = 880, bottom = 912, left = 80),
            OcrLine("210.00", top = 893, bottom = 922, left = 562),
            OcrLine("Card", top = 990, bottom = 1020, left = 80),
        )

        assertThat(underTest(lines)).containsExactly("TOTAL 210.00", "Card").inOrder()
    }

    @Test
    fun `test that invoke keeps lines a full row apart on separate rows`() {
        val lines = listOf(
            OcrLine("SUBTOTAL", top = 671, bottom = 700, left = 82),
            OcrLine("TAX", top = 781, bottom = 810, left = 81),
        )

        assertThat(underTest(lines)).containsExactly("SUBTOTAL", "TAX").inOrder()
    }

    @Test
    fun `test that invoke returns no rows when there are no lines`() {
        assertThat(underTest(emptyList())).isEmpty()
    }
}
