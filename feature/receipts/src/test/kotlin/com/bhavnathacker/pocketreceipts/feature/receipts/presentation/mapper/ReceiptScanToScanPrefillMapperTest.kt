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

class ReceiptScanToScanPrefillMapperTest {
    private lateinit var underTest: ReceiptScanToScanPrefillMapper

    @BeforeEach
    fun setUp() {
        underTest = ReceiptScanToScanPrefillMapper()
    }

    @Test
    fun `test that invoke keeps the merchant`() {
        assertThat(underTest(ReceiptScan(merchant = "Corner Shop")).merchant).isEqualTo("Corner Shop")
    }

    @Test
    fun `test that invoke writes the date as ISO text`() {
        assertThat(underTest(ReceiptScan(date = LocalDate.of(2026, 10, 31))).date).isEqualTo("2026-10-31")
    }

    @Test
    fun `test that invoke keeps the total's minor units and currency`() {
        assertThat(underTest(ReceiptScan(total = Money(76400, "INR"))).total).isEqualTo(ScanAmount(76400, "INR"))
    }

    @Test
    fun `test that invoke writes the category by name`() {
        assertThat(underTest(ReceiptScan(category = ReceiptCategory.HEALTH)).category).isEqualTo("HEALTH")
    }

    @Test
    fun `test that invoke leaves missing values null`() {
        assertThat(underTest(ReceiptScan())).isEqualTo(ScanPrefill())
    }
}
