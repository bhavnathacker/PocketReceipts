package com.bhavnathacker.pocketreceipts.data.receipts.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptDaoTest {
    private lateinit var database: ReceiptsDatabase
    private lateinit var underTest: ReceiptDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReceiptsDatabase::class.java,
        ).build()
        underTest = database.receiptDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test_that_observeAll_orders_by_date_then_most_recently_added() = runTest {
        val march3First = underTest.insert(entity(merchant = "A", day = 3, createdAt = 100))
        val march1 = underTest.insert(entity(merchant = "B", day = 1, createdAt = 200))
        val march3Second = underTest.insert(entity(merchant = "C", day = 3, createdAt = 300))

        val ids = underTest.observeAll().first().map { it.id }

        assertThat(ids).containsExactly(march3Second, march3First, march1).inOrder()
    }

    @Test
    fun test_that_observeAll_breaks_full_ties_by_newest_id() = runTest {
        val first = underTest.insert(entity(day = 3, createdAt = 100))
        val second = underTest.insert(entity(day = 3, createdAt = 100))

        val ids = underTest.observeAll().first().map { it.id }

        assertThat(ids).containsExactly(second, first).inOrder()
    }

    @Test
    fun test_that_insert_stores_all_fields_under_the_returned_id() = runTest {
        val id = underTest.insert(entity(merchant = "Corner Shop", note = "milk"))

        val stored = underTest.observeById(id).first()

        assertThat(stored).isEqualTo(entity(merchant = "Corner Shop", note = "milk").copy(id = id))
    }

    @Test
    fun test_that_update_replaces_fields() = runTest {
        val id = underTest.insert(entity(totalMinor = 450))

        underTest.update(entity(totalMinor = 500, updatedAt = 999).copy(id = id))

        val stored = underTest.observeById(id).first()
        assertThat(stored?.totalMinor).isEqualTo(500)
        assertThat(stored?.updatedAt).isEqualTo(999)
    }

    @Test
    fun test_that_getCreatedAt_returns_null_when_receipt_not_found() = runTest {
        assertThat(underTest.getCreatedAt(42)).isNull()
    }

    @Test
    fun test_that_deleteById_removes_the_receipt() = runTest {
        val kept = underTest.insert(entity(merchant = "kept"))
        val deleted = underTest.insert(entity(merchant = "deleted"))

        underTest.deleteById(deleted)

        assertThat(underTest.observeAll().first().map { it.id }).containsExactly(kept)
    }

    @Test
    fun test_that_observeById_emits_null_after_delete() = runTest {
        val id = underTest.insert(entity())

        underTest.observeById(id).test {
            assertThat(awaitItem()).isNotNull()
            underTest.deleteById(id)
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_that_currencyTotals_includes_both_end_dates() = runTest {
        underTest.insert(entity(day = 1, totalMinor = 100))
        underTest.insert(entity(day = 2, totalMinor = 200))
        underTest.insert(entity(day = 3, totalMinor = 400))
        underTest.insert(entity(day = 4, totalMinor = 800))

        val rows = underTest.currencyTotals(march(2), march(3), category = null)

        assertThat(rows).containsExactly(CurrencyTotalRow("EUR", 600, 2))
    }

    @Test
    fun test_that_currencyTotals_counts_only_the_category() = runTest {
        underTest.insert(entity(totalMinor = 12050, category = "groceries"))
        underTest.insert(entity(totalMinor = 7950, category = "groceries"))
        underTest.insert(entity(totalMinor = 30000, category = "dining"))

        val rows = underTest.currencyTotals(march(1), march(31), category = "groceries")

        assertThat(rows).containsExactly(CurrencyTotalRow("EUR", 20000, 2))
    }

    @Test
    fun test_that_currencyTotals_counts_every_category_when_category_is_null() = runTest {
        underTest.insert(entity(totalMinor = 100, category = "groceries"))
        underTest.insert(entity(totalMinor = 200, category = "dining"))

        val rows = underTest.currencyTotals(march(1), march(31), category = null)

        assertThat(rows).containsExactly(CurrencyTotalRow("EUR", 300, 2))
    }

    @Test
    fun test_that_currencyTotals_lists_the_currency_with_most_receipts_first() = runTest {
        underTest.insert(entity(totalMinor = 1230, currencyCode = "EUR"))
        underTest.insert(entity(totalMinor = 50000, currencyCode = "INR"))
        underTest.insert(entity(totalMinor = 50000, currencyCode = "INR"))
        underTest.insert(entity(totalMinor = 50000, currencyCode = "INR"))

        val rows = underTest.currencyTotals(march(1), march(31), category = null)

        assertThat(rows).containsExactly(
            CurrencyTotalRow("INR", 150000, 3),
            CurrencyTotalRow("EUR", 1230, 1),
        ).inOrder()
    }

    @Test
    fun test_that_currencyTotals_orders_currencies_with_equal_counts_by_code() = runTest {
        underTest.insert(entity(totalMinor = 100, currencyCode = "USD"))
        underTest.insert(entity(totalMinor = 100, currencyCode = "EUR"))

        val codes = underTest.currencyTotals(march(1), march(31), category = null).map { it.currencyCode }

        assertThat(codes).containsExactly("EUR", "USD").inOrder()
    }

    @Test
    fun test_that_currencyTotals_returns_no_rows_when_nothing_matches() = runTest {
        underTest.insert(entity(day = 3))

        assertThat(underTest.currencyTotals(march(4), march(31), category = null)).isEmpty()
    }

    @Test
    fun test_that_currencyTotals_accepts_unbounded_dates() = runTest {
        underTest.insert(entity(day = 3, totalMinor = 450))

        val rows = underTest.currencyTotals(Long.MIN_VALUE, Long.MAX_VALUE, category = null)

        assertThat(rows).containsExactly(CurrencyTotalRow("EUR", 450, 1))
    }

    private fun march(day: Int) = java.time.LocalDate.of(2026, 3, day).toEpochDay()

    private fun entity(
        merchant: String = "Corner Shop",
        day: Int = 3,
        totalMinor: Long = 450,
        currencyCode: String = "EUR",
        category: String = "other",
        note: String? = null,
        createdAt: Long = 100,
        updatedAt: Long = createdAt,
    ) = ReceiptEntity(
        merchant = merchant,
        purchaseDate = java.time.LocalDate.of(2026, 3, day).toEpochDay(),
        totalMinor = totalMinor,
        currencyCode = currencyCode,
        category = category,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
