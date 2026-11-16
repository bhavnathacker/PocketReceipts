package com.bhavnathacker.pocketreceipts.data.receipts.mapper

import com.bhavnathacker.pocketreceipts.data.receipts.database.ReceiptEntity
import com.bhavnathacker.pocketreceipts.domain.receipt.Money
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptCategory
import com.bhavnathacker.pocketreceipts.domain.receipt.ReceiptInput
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDate

class ReceiptEntityMapperTest {
    private lateinit var underTest: ReceiptEntityMapper

    private val receiptCategoryMapper = mock<ReceiptCategoryMapper>()

    private val date = LocalDate.of(2026, 3, 3)

    @BeforeEach
    fun setUp() {
        underTest = ReceiptEntityMapper(receiptCategoryMapper = receiptCategoryMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(receiptCategoryMapper)
    }

    @Test
    fun `test that an entity maps to a receipt`() {
        whenever(receiptCategoryMapper("groceries")).thenReturn(ReceiptCategory.GROCERIES)

        val result = underTest(createEntity())

        assertThat(result).isEqualTo(
            Receipt(
                id = 7,
                merchant = "Corner Shop",
                purchaseDate = date,
                total = Money(minorUnits = 12345650, currencyCode = "INR"),
                category = ReceiptCategory.GROCERIES,
                note = "milk",
            ),
        )
    }

    @Test
    fun `test that a null note maps to a null note`() {
        whenever(receiptCategoryMapper("groceries")).thenReturn(ReceiptCategory.GROCERIES)

        val result = underTest(createEntity(note = null))

        assertThat(result.note).isNull()
    }

    @Test
    fun `test that an entity list maps to a receipt list in order`() {
        whenever(receiptCategoryMapper("groceries")).thenReturn(ReceiptCategory.GROCERIES)

        val result = underTest(listOf(createEntity(id = 2), createEntity(id = 1)))

        assertThat(result.map { it.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun `test that an input maps to an entity with the given id and timestamps`() {
        whenever(receiptCategoryMapper(ReceiptCategory.GROCERIES)).thenReturn("groceries")
        val input = ReceiptInput(
            merchant = "Corner Shop",
            purchaseDate = date,
            total = Money(minorUnits = 12345650, currencyCode = "INR"),
            category = ReceiptCategory.GROCERIES,
            note = "milk",
        )

        val result = underTest(input = input, id = 7, createdAt = 1_000, updatedAt = 2_000)

        assertThat(result).isEqualTo(createEntity(createdAt = 1_000, updatedAt = 2_000))
    }

    @Test
    fun `test that the purchase date is stored as epoch day`() {
        whenever(receiptCategoryMapper(ReceiptCategory.OTHER)).thenReturn("other")
        val input = ReceiptInput(
            merchant = "m",
            purchaseDate = LocalDate.of(1970, 1, 2),
            total = Money(1, "EUR"),
            category = ReceiptCategory.OTHER,
            note = null,
        )

        val result = underTest(input = input, id = 0, createdAt = 0, updatedAt = 0)

        assertThat(result.purchaseDate).isEqualTo(1)
    }

    private fun createEntity(
        id: Long = 7,
        note: String? = "milk",
        createdAt: Long = 1_000,
        updatedAt: Long = 2_000,
    ) = ReceiptEntity(
        id = id,
        merchant = "Corner Shop",
        purchaseDate = date.toEpochDay(),
        totalMinor = 12345650,
        currencyCode = "INR",
        category = "groceries",
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
