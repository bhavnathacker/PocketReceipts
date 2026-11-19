package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
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
    fun `test that invoke reads every value back`() {
        val prefill = ScanPrefill(
            merchant = "BHARAT MEDICAL STORES",
            date = "2026-10-31",
            total = ScanAmount(76400, "INR"),
            category = "HEALTH",
        )

        assertThat(underTest(prefill)).isEqualTo(
            ReceiptScan(
                merchant = "BHARAT MEDICAL STORES",
                date = LocalDate.of(2026, 10, 31),
                total = Money(76400, "INR"),
                category = ReceiptCategory.HEALTH,
            ),
        )
    }

    @Test
    fun `test that invoke drops a date that is not ISO-8601 text`() {
        assertThat(underTest(ScanPrefill(date = "31/10/2026")).date).isNull()
    }

    @Test
    fun `test that invoke drops an unknown category name`() {
        assertThat(underTest(ScanPrefill(category = "MEDICAL")).category).isNull()
    }

    @Test
    fun `test that invoke leaves missing values null`() {
        assertThat(underTest(ScanPrefill())).isEqualTo(ReceiptScan())
    }
}
