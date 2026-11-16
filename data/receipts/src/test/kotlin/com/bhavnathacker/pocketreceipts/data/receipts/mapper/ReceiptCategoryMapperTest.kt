package com.bhavnathacker.pocketreceipts.data.receipts.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ReceiptCategoryMapperTest {
    private lateinit var underTest: ReceiptCategoryMapper

    @BeforeEach
    fun setUp() {
        underTest = ReceiptCategoryMapper()
    }

    @Test
    fun `test that every category maps to its stable key`() {
        val keys = ReceiptCategory.entries.associateWith { underTest(it) }

        assertThat(keys).containsExactly(
            ReceiptCategory.GROCERIES, "groceries",
            ReceiptCategory.DINING, "dining",
            ReceiptCategory.TRANSPORT, "transport",
            ReceiptCategory.SHOPPING, "shopping",
            ReceiptCategory.BILLS, "bills",
            ReceiptCategory.HEALTH, "health",
            ReceiptCategory.ENTERTAINMENT, "entertainment",
            ReceiptCategory.TRAVEL, "travel",
            ReceiptCategory.OTHER, "other",
        )
    }

    @ParameterizedTest
    @EnumSource(ReceiptCategory::class)
    fun `test that a key maps back to its category`(category: ReceiptCategory) {
        assertThat(underTest(underTest(category))).isEqualTo(category)
    }

    @Test
    fun `test that an unknown key maps to OTHER`() {
        assertThat(underTest("pets")).isEqualTo(ReceiptCategory.OTHER)
    }
}
