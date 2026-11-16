package com.bhavnathacker.pocketreceipts.domain.receipt

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorReceiptUseCaseTest {
    private lateinit var underTest: MonitorReceiptUseCase

    private val receiptRepository = mock<ReceiptRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorReceiptUseCase(receiptRepository = receiptRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(receiptRepository)
    }

    @Test
    fun `test that invoke emits the receipt with the given id`() = runTest {
        val receipt = Receipt(
            id = 7,
            merchant = "Corner Shop",
            purchaseDate = LocalDate.of(2026, 3, 3),
            total = Money(450, "EUR"),
            category = ReceiptCategory.GROCERIES,
            note = "milk",
        )
        whenever(receiptRepository.monitorReceipt(7)).thenReturn(flowOf(receipt))

        underTest(7).test {
            assertThat(awaitItem()).isEqualTo(receipt)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits null when receipt not found`() = runTest {
        whenever(receiptRepository.monitorReceipt(7)).thenReturn(flowOf(null))

        underTest(7).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }
}
