package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReceiptCategoryLabelMapperTest {
    private lateinit var underTest: ReceiptCategoryLabelMapper

    @BeforeEach
    fun setUp() {
        underTest = ReceiptCategoryLabelMapper()
    }

    @Test
    fun `test that every category maps to its label`() {
        val labels = ReceiptCategory.entries.associateWith { underTest(it) }

        assertThat(labels).containsExactly(
            ReceiptCategory.GROCERIES, R.string.category_groceries,
            ReceiptCategory.DINING, R.string.category_dining,
            ReceiptCategory.TRANSPORT, R.string.category_transport,
            ReceiptCategory.SHOPPING, R.string.category_shopping,
            ReceiptCategory.BILLS, R.string.category_bills,
            ReceiptCategory.HEALTH, R.string.category_health,
            ReceiptCategory.ENTERTAINMENT, R.string.category_entertainment,
            ReceiptCategory.TRAVEL, R.string.category_travel,
            ReceiptCategory.OTHER, R.string.category_other,
        )
    }
}
