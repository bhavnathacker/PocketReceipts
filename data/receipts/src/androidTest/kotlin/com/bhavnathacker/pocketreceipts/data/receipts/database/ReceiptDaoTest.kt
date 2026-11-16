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

    private fun entity(
        merchant: String = "Corner Shop",
        day: Int = 3,
        totalMinor: Long = 450,
        note: String? = null,
        createdAt: Long = 100,
        updatedAt: Long = createdAt,
    ) = ReceiptEntity(
        merchant = merchant,
        purchaseDate = java.time.LocalDate.of(2026, 3, day).toEpochDay(),
        totalMinor = totalMinor,
        currencyCode = "EUR",
        category = "other",
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
