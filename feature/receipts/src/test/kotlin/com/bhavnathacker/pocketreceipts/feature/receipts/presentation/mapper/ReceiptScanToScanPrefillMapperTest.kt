package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.scan.ReceiptScan
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.ScanAmount
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReceiptScanToScanPrefillMapperTest {
    private lateinit var underTest: ReceiptScanToScanPrefillMapper

    @BeforeEach
    fun setUp() {
        underTest = ReceiptScanToScanPrefillMapper()
    }

    @Test
    fun `test that invoke keeps the merchant`() {
        assertThat(underTest(ReceiptScan("Corner Shop", emptyList(), emptyList())).merchant).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke writes dates as ISO text in order`() {
        val scan = ReceiptScan(null, listOf(LocalDate.of(2026, 2, 3), LocalDate.of(2026, 1, 5)), emptyList())

        assertThat(underTest(scan).dates).containsExactly("2026-02-03", "2026-01-05").inOrder()
    }

    @Test
    fun `test that invoke maps totals to scan amounts in order`() {
        val scan = ReceiptScan(null, emptyList(), listOf(Money(21000, "INR"), Money(20000, "INR")))

        assertThat(underTest(scan).totals)
            .containsExactly(ScanAmount(21000, "INR"), ScanAmount(20000, "INR"))
            .inOrder()
    }

    @Test
    fun `test that invoke keeps at most five dates`() {
        val scan = ReceiptScan(null, (1..7).map { LocalDate.of(2026, 1, it) }, emptyList())

        assertThat(underTest(scan).dates).hasSize(5)
    }

    @Test
    fun `test that invoke keeps at most eight totals`() {
        val scan = ReceiptScan(null, emptyList(), (1..10).map { Money(it * 100L, "INR") })

        assertThat(underTest(scan).totals).hasSize(8)
    }
}
