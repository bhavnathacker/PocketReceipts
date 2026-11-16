package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Locale

class ReceiptDateFormatterTest {
    private lateinit var underTest: ReceiptDateFormatter

    @BeforeEach
    fun setUp() {
        underTest = ReceiptDateFormatter()
    }

    @Test
    fun `test that a date is formatted in the medium style of en-US`() {
        assertThat(underTest(LocalDate.of(2026, 3, 3), Locale.US)).isEqualTo("Mar 3, 2026")
    }

    @Test
    fun `test that a date is formatted in the medium style of en-GB`() {
        assertThat(underTest(LocalDate.of(2026, 3, 3), Locale.UK)).isEqualTo("3 Mar 2026")
    }
}
