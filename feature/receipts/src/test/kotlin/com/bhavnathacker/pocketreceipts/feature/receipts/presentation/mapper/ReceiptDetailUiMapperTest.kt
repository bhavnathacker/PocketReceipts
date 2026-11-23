package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.core.ui.R as CoreUiR
import com.bhavnathacker.pocketreceipts.core.ui.format.MoneyFormatter
import com.bhavnathacker.pocketreceipts.core.ui.format.ReceiptCategoryLabelMapper
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiModel
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Locale

class ReceiptDetailUiMapperTest {
    private lateinit var underTest: ReceiptDetailUiMapper

    private val moneyFormatter = mock<MoneyFormatter>()
    private val receiptDateFormatter = mock<ReceiptDateFormatter>()
    private val receiptCategoryLabelMapper = mock<ReceiptCategoryLabelMapper>()

    private val locale = Locale.UK
    private val receipt = Receipt(
        id = 7,
        merchant = "Corner Shop",
        purchaseDate = LocalDate.of(2026, 3, 3),
        total = Money(450, "GBP"),
        category = ReceiptCategory.GROCERIES,
        note = "milk",
    )

    @BeforeEach
    fun setUp() {
        underTest = ReceiptDetailUiMapper(
            moneyFormatter = moneyFormatter,
            receiptDateFormatter = receiptDateFormatter,
            receiptCategoryLabelMapper = receiptCategoryLabelMapper,
        )
        whenever(moneyFormatter(receipt.total, locale)).thenReturn("£4.50")
        whenever(receiptDateFormatter(receipt.purchaseDate, locale)).thenReturn("3 Mar 2026")
        whenever(receiptCategoryLabelMapper(ReceiptCategory.GROCERIES)).thenReturn(CoreUiR.string.category_groceries)
    }

    @AfterEach
    fun tearDown() {
        reset(moneyFormatter, receiptDateFormatter, receiptCategoryLabelMapper)
    }

    @Test
    fun `test that a receipt maps to its detail`() {
        assertThat(underTest(receipt, locale)).isEqualTo(
            ReceiptDetailUiModel(
                merchant = "Corner Shop",
                date = "3 Mar 2026",
                total = "£4.50",
                categoryLabel = CoreUiR.string.category_groceries,
                note = "milk",
            ),
        )
    }

    @Test
    fun `test that a receipt without a note maps to a null note`() {
        assertThat(underTest(receipt.copy(note = null), locale).note).isNull()
    }
}
