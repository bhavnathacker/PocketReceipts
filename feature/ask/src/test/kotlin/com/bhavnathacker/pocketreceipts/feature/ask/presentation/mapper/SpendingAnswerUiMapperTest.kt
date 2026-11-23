package com.bhavnathacker.pocketreceipts.feature.ask.presentation.mapper

import com.bhavnathacker.pocketreceipts.core.ui.R as CoreUiR
import com.bhavnathacker.pocketreceipts.core.ui.format.MoneyFormatter
import com.bhavnathacker.pocketreceipts.core.ui.format.ReceiptCategoryLabelMapper
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingAnswer
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingQuery
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingTotal
import com.bhavnathacker.pocketreceipts.feature.ask.R
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskAnswerUiModel
import com.bhavnathacker.pocketreceipts.feature.ask.presentation.model.AskTotalUiModel
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Locale

class SpendingAnswerUiMapperTest {
    private lateinit var underTest: SpendingAnswerUiMapper

    private val moneyFormatter = mock<MoneyFormatter>()
    private val dateRangeFormatter = mock<DateRangeFormatter>()

    private val locale = Locale.UK
    private val october1 = LocalDate.of(2026, 10, 1)
    private val october31 = LocalDate.of(2026, 10, 31)

    @BeforeEach
    fun setUp() {
        underTest = SpendingAnswerUiMapper(
            moneyFormatter = moneyFormatter,
            receiptCategoryLabelMapper = ReceiptCategoryLabelMapper(),
            dateRangeFormatter = dateRangeFormatter,
        )
        whenever(dateRangeFormatter(october1, october31, locale)).thenReturn("1–31 Oct 2026")
    }

    @Test
    fun `test that invoke shows the category, period and total`() {
        whenever(moneyFormatter(Money(20000, "INR"), locale)).thenReturn("₹200.00")
        val answer = SpendingAnswer.Totals(
            SpendingQuery(ReceiptCategory.GROCERIES, october1, october31),
            listOf(SpendingTotal(Money(20000, "INR"), 2)),
        )

        assertThat(underTest(answer, locale)).isEqualTo(
            AskAnswerUiModel.Totals(
                categoryLabel = CoreUiR.string.category_groceries,
                period = "1–31 Oct 2026",
                totals = listOf(AskTotalUiModel("₹200.00", 2)),
            ),
        )
    }

    @Test
    fun `test that invoke keeps one line per currency in order`() {
        whenever(moneyFormatter(Money(150000, "INR"), locale)).thenReturn("₹1,500.00")
        whenever(moneyFormatter(Money(1230, "EUR"), locale)).thenReturn("€12.30")
        val answer = SpendingAnswer.Totals(
            SpendingQuery(null, october1, october31),
            listOf(SpendingTotal(Money(150000, "INR"), 3), SpendingTotal(Money(1230, "EUR"), 1)),
        )

        val actual = underTest(answer, locale) as AskAnswerUiModel.Totals

        assertThat(actual.totals).containsExactly(AskTotalUiModel("₹1,500.00", 3), AskTotalUiModel("€12.30", 1)).inOrder()
    }

    @Test
    fun `test that invoke labels a question without a category as all spending`() {
        val answer = SpendingAnswer.Totals(SpendingQuery(null, october1, october31), emptyList())

        assertThat(underTest(answer, locale)).isEqualTo(AskAnswerUiModel.NothingSpent(R.string.ask_all_spending, "1–31 Oct 2026"))
    }

    @Test
    fun `test that invoke formats a quarter's dates`() {
        val december31 = LocalDate.of(2026, 12, 31)
        whenever(dateRangeFormatter(october1, december31, locale)).thenReturn("1 Oct – 31 Dec 2026")
        val answer = SpendingAnswer.Totals(SpendingQuery(ReceiptCategory.SHOPPING, october1, december31), emptyList())

        assertThat((underTest(answer, locale) as AskAnswerUiModel.NothingSpent).period).isEqualTo("1 Oct – 31 Dec 2026")
    }

    @Test
    fun `test that invoke formats a range across years`() {
        val from = LocalDate.of(2025, 12, 15)
        val to = LocalDate.of(2026, 1, 14)
        whenever(dateRangeFormatter(from, to, locale)).thenReturn("15 Dec 2025 – 14 Jan 2026")
        val answer = SpendingAnswer.Totals(SpendingQuery(null, from, to), emptyList())

        assertThat((underTest(answer, locale) as AskAnswerUiModel.NothingSpent).period).isEqualTo("15 Dec 2025 – 14 Jan 2026")
    }

    @Test
    fun `test that invoke has no period for all time`() {
        val answer = SpendingAnswer.Totals(SpendingQuery(ReceiptCategory.DINING, null, null), emptyList())

        assertThat(underTest(answer, locale)).isEqualTo(AskAnswerUiModel.NothingSpent(CoreUiR.string.category_dining, null))
        verify(dateRangeFormatter, never()).invoke(any(), any(), any())
    }

    @Test
    fun `test that invoke shows nothing spent when no receipt matches`() {
        val answer = SpendingAnswer.Totals(SpendingQuery(ReceiptCategory.GROCERIES, october1, october31), emptyList())

        assertThat(underTest(answer, locale))
            .isEqualTo(AskAnswerUiModel.NothingSpent(CoreUiR.string.category_groceries, "1–31 Oct 2026"))
    }

    @Test
    fun `test that invoke maps an unanswerable question`() {
        assertThat(underTest(SpendingAnswer.Unanswerable, locale)).isEqualTo(AskAnswerUiModel.Unanswerable)
    }
}
