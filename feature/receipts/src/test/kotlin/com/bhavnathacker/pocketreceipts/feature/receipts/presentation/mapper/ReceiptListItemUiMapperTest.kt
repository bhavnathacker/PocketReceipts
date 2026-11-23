package com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper

import com.bhavnathacker.pocketreceipts.core.ui.R as CoreUiR
import com.bhavnathacker.pocketreceipts.core.ui.format.MoneyFormatter
import com.bhavnathacker.pocketreceipts.core.ui.format.ReceiptCategoryLabelMapper
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.feature.receipts.R
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListItemUiModel
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Locale

class ReceiptListItemUiMapperTest {
    private lateinit var underTest: ReceiptListItemUiMapper

    private val moneyFormatter = mock<MoneyFormatter>()
    private val receiptDateFormatter = mock<ReceiptDateFormatter>()
    private val receiptCategoryLabelMapper = mock<ReceiptCategoryLabelMapper>()

    private val locale = Locale.forLanguageTag("en-IN")
    private val receipt = Receipt(
        id = 7,
        merchant = "Corner Shop",
        purchaseDate = LocalDate.of(2026, 3, 3),
        total = Money(12345650, "INR"),
        category = ReceiptCategory.GROCERIES,
        note = "milk",
    )

    @BeforeEach
    fun setUp() {
        underTest = ReceiptListItemUiMapper(
            moneyFormatter = moneyFormatter,
            receiptDateFormatter = receiptDateFormatter,
            receiptCategoryLabelMapper = receiptCategoryLabelMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(moneyFormatter, receiptDateFormatter, receiptCategoryLabelMapper)
    }

    @Test
    fun `test that a receipt maps to a list item`() {
        whenever(moneyFormatter(receipt.total, locale)).thenReturn("₹1,23,456.50")
        whenever(receiptDateFormatter(receipt.purchaseDate, locale)).thenReturn("3 Mar 2026")
        whenever(receiptCategoryLabelMapper(ReceiptCategory.GROCERIES)).thenReturn(CoreUiR.string.category_groceries)

        val result = underTest(receipt, locale)

        assertThat(result).isEqualTo(
            ReceiptListItemUiModel(
                id = 7,
                merchant = "Corner Shop",
                date = "3 Mar 2026",
                total = "₹1,23,456.50",
                categoryLabel = CoreUiR.string.category_groceries,
            ),
        )
    }

    @Test
    fun `test that a receipt list maps to list items in order`() {
        whenever(moneyFormatter(any(), any())).thenReturn("")
        whenever(receiptDateFormatter(any(), any())).thenReturn("")
        whenever(receiptCategoryLabelMapper(any())).thenReturn(CoreUiR.string.category_other)

        val result = underTest(listOf(receipt.copy(id = 2), receipt.copy(id = 1)), locale)

        assertThat(result.map { it.id }).containsExactly(2L, 1L).inOrder()
    }
}
