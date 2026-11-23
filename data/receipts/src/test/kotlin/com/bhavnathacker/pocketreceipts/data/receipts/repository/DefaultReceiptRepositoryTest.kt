package com.bhavnathacker.pocketreceipts.data.receipts.repository

import app.cash.turbine.test
import com.bhavnathacker.pocketreceipts.data.receipts.database.CurrencyTotalRow
import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptDao
import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptEntity
import com.bhavnathacker.pocketreceipts.data.receipts.mapper.ReceiptCategoryMapper
import com.bhavnathacker.pocketreceipts.data.receipts.mapper.ReceiptEntityMapper
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.bhavnathacker.pocketreceipts.domain.spending.SpendingTotal
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DefaultReceiptRepositoryTest {
    private lateinit var underTest: DefaultReceiptRepository

    private val receiptDao = mock<ReceiptDao>()
    private val receiptEntityMapper = mock<ReceiptEntityMapper>()

    private val now = Instant.parse("2026-03-03T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val input = ReceiptInput(
        merchant = "Corner Shop",
        purchaseDate = LocalDate.of(2026, 3, 3),
        total = Money(450, "EUR"),
        category = ReceiptCategory.GROCERIES,
        note = null,
    )
    private val entity = mock<ReceiptEntity>()
    private val receipt = Receipt(
        id = 7,
        merchant = "Corner Shop",
        purchaseDate = LocalDate.of(2026, 3, 3),
        total = Money(450, "EUR"),
        category = ReceiptCategory.GROCERIES,
        note = null,
    )

    @BeforeEach
    fun setUp() {
        underTest = DefaultReceiptRepository(
            receiptDao = receiptDao,
            receiptEntityMapper = receiptEntityMapper,
            receiptCategoryMapper = ReceiptCategoryMapper(),
            clock = clock,
        )
    }

    @Test
    fun `test that monitorReceipts emits mapped receipts`() = runTest {
        val entities = listOf(entity)
        whenever(receiptDao.observeAll()).thenReturn(flowOf(entities))
        whenever(receiptEntityMapper(entities)).thenReturn(listOf(receipt))

        underTest.monitorReceipts().test {
            assertThat(awaitItem()).containsExactly(receipt)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitorReceipt emits the mapped receipt`() = runTest {
        whenever(receiptDao.observeById(7)).thenReturn(flowOf(entity))
        whenever(receiptEntityMapper(entity)).thenReturn(receipt)

        underTest.monitorReceipt(7).test {
            assertThat(awaitItem()).isEqualTo(receipt)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitorReceipt emits null when receipt not found`() = runTest {
        whenever(receiptDao.observeById(7)).thenReturn(flowOf(null))

        underTest.monitorReceipt(7).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `test that addReceipt inserts a row with created and updated set to now`() = runTest {
        whenever(receiptEntityMapper(input, 0, now.toEpochMilli(), now.toEpochMilli())).thenReturn(entity)

        underTest.addReceipt(input)

        verify(receiptDao).insert(entity)
    }

    @Test
    fun `test that addReceipt returns the id of the inserted row`() = runTest {
        whenever(receiptEntityMapper(input, 0, now.toEpochMilli(), now.toEpochMilli())).thenReturn(entity)
        whenever(receiptDao.insert(entity)).thenReturn(42)

        val id = underTest.addReceipt(input)

        assertThat(id).isEqualTo(42)
    }

    @Test
    fun `test that updateReceipt keeps created_at and sets updated_at to now`() = runTest {
        whenever(receiptDao.getCreatedAt(7)).thenReturn(1_000)
        whenever(receiptEntityMapper(input, 7, 1_000, now.toEpochMilli())).thenReturn(entity)

        underTest.updateReceipt(7, input)

        verify(receiptDao).update(entity)
    }

    @Test
    fun `test that updateReceipt does nothing when receipt not found`() = runTest {
        whenever(receiptDao.getCreatedAt(7)).thenReturn(null)

        underTest.updateReceipt(7, input)

        verify(receiptDao, never()).update(any())
    }

    @Test
    fun `test that deleteReceipt deletes the row`() = runTest {
        underTest.deleteReceipt(7)

        verify(receiptDao).deleteById(7)
    }

    @Test
    fun `test that spendingTotals passes epoch days and the category key to the dao`() = runTest {
        whenever(receiptDao.currencyTotals(any(), any(), anyOrNull())).thenReturn(emptyList())

        underTest.spendingTotals(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), ReceiptCategory.GROCERIES)

        verify(receiptDao).currencyTotals(
            LocalDate.of(2026, 10, 1).toEpochDay(),
            LocalDate.of(2026, 10, 31).toEpochDay(),
            "groceries",
        )
    }

    @Test
    fun `test that spendingTotals passes no date limits and no category for all spending`() = runTest {
        whenever(receiptDao.currencyTotals(any(), any(), anyOrNull())).thenReturn(emptyList())

        underTest.spendingTotals(from = null, to = null, category = null)

        verify(receiptDao).currencyTotals(Long.MIN_VALUE, Long.MAX_VALUE, null)
    }

    @Test
    fun `test that spendingTotals maps rows to totals in their order`() = runTest {
        whenever(receiptDao.currencyTotals(any(), any(), anyOrNull())).thenReturn(
            listOf(CurrencyTotalRow("INR", 150000, 3), CurrencyTotalRow("EUR", 1230, 1)),
        )

        val actual = underTest.spendingTotals(from = null, to = null, category = null)

        assertThat(actual).containsExactly(
            SpendingTotal(Money(150000, "INR"), 3),
            SpendingTotal(Money(1230, "EUR"), 1),
        ).inOrder()
    }
}
