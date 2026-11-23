package com.bhavnathacker.pocketreceipts.domain.spending

import com.bhavnathacker.pocketreceipts.domain.receipt.GetTodayUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnswerSpendingQuestionUseCaseTest {
    private lateinit var underTest: AnswerSpendingQuestionUseCase

    private val spendingQuestionRepository = mock<SpendingQuestionRepository>()
    private val receiptRepository = mock<ReceiptRepository>()

    private val today = LocalDate.of(2026, 11, 21)
    private val groceriesInOctober = InterpretedQuestion.Spending("GROCERIES", InterpretedPeriod.Month(2026, 10))

    @BeforeAll
    fun setUp() {
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        underTest = AnswerSpendingQuestionUseCase(
            spendingQuestionRepository = spendingQuestionRepository,
            spendingQueryChecker = SpendingQueryChecker(),
            receiptRepository = receiptRepository,
            getToday = GetTodayUseCase(clock),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(spendingQuestionRepository, receiptRepository)
    }

    @Test
    fun `test that invoke passes the trimmed question and today to the repository`() = runTest {
        whenever(spendingQuestionRepository.interpret(any(), any())).thenReturn(InterpretedQuestion.Unsupported)

        underTest("  How much on groceries in October?  ")

        verify(spendingQuestionRepository).interpret("How much on groceries in October?", today)
    }

    @Test
    fun `test that invoke returns the totals for the checked query`() = runTest {
        val totals = listOf(SpendingTotal(Money(20000, "INR"), 2))
        whenever(spendingQuestionRepository.interpret(any(), any())).thenReturn(groceriesInOctober)
        whenever(
            receiptRepository.spendingTotals(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), ReceiptCategory.GROCERIES),
        ).thenReturn(totals)

        val actual = underTest("How much on groceries in October?")

        assertThat(actual).isEqualTo(
            SpendingAnswer.Totals(
                SpendingQuery(ReceiptCategory.GROCERIES, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
                totals,
            ),
        )
    }

    @Test
    fun `test that invoke returns empty totals when no receipt matches`() = runTest {
        whenever(spendingQuestionRepository.interpret(any(), any())).thenReturn(groceriesInOctober)
        whenever(receiptRepository.spendingTotals(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(emptyList())

        val actual = underTest("How much on groceries in October?")

        assertThat((actual as SpendingAnswer.Totals).totals).isEmpty()
    }

    @Test
    fun `test that invoke returns Unanswerable without reading receipts when the reading fails the checks`() = runTest {
        whenever(spendingQuestionRepository.interpret(any(), any()))
            .thenReturn(InterpretedQuestion.Spending("PETS", InterpretedPeriod.Year(2025)))

        val actual = underTest("How much on my cat in 2025?")

        assertThat(actual).isEqualTo(SpendingAnswer.Unanswerable)
        verify(receiptRepository, never()).spendingTotals(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `test that invoke returns Unanswerable when the question is unsupported`() = runTest {
        whenever(spendingQuestionRepository.interpret(any(), any())).thenReturn(InterpretedQuestion.Unsupported)

        assertThat(underTest("Which shop did I visit most?")).isEqualTo(SpendingAnswer.Unanswerable)
    }

    @Test
    fun `test that invoke returns Unanswerable without running the model when the question is blank`() = runTest {
        assertThat(underTest("   ")).isEqualTo(SpendingAnswer.Unanswerable)
        verify(spendingQuestionRepository, never()).interpret(any(), any())
    }

    @Test
    fun `test that invoke throws when the model fails`() = runTest {
        whenever(spendingQuestionRepository.interpret(any(), any())).thenAnswer { throw IOException("engine") }

        assertThrows<IOException> { underTest("How much on groceries in October?") }
    }

    @Test
    fun `test that invoke rethrows cancellation`() = runTest {
        whenever(spendingQuestionRepository.interpret(any(), any())).thenAnswer { throw CancellationException("left") }

        assertThrows<CancellationException> { underTest("How much on groceries in October?") }
    }
}
