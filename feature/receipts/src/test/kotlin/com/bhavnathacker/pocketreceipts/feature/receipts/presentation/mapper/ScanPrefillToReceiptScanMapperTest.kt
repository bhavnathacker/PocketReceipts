package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanAmount
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanPrefill
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ScanPrefillToReceiptScanMapperTest {
    private lateinit var underTest: ScanPrefillToReceiptScanMapper

    @BeforeEach
    fun setUp() {
        underTest = ScanPrefillToReceiptScanMapper()
    }

    @Test
    fun `test that invoke reads back the scan it was written from`() {
        val prefill = ScanPrefill("Corner Shop", listOf("2026-02-03"), listOf(ScanAmount(450, "GBP")))

        assertThat(underTest(prefill))
            .isEqualTo(ReceiptScan("Corner Shop", listOf(LocalDate.of(2026, 2, 3)), listOf(Money(450, "GBP"))))
    }

    @Test
    fun `test that invoke skips a date that isn't ISO text`() {
        val prefill = ScanPrefill(null, listOf("not a date", "2026-02-03"), emptyList())

        assertThat(underTest(prefill).dates).containsExactly(LocalDate.of(2026, 2, 3))
    }
}
