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
class MonitorReceiptsUseCaseTest {
    private lateinit var underTest: MonitorReceiptsUseCase

    private val receiptRepository = mock<ReceiptRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorReceiptsUseCase(receiptRepository = receiptRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(receiptRepository)
    }

    @Test
    fun `test that invoke emits receipts from repository`() = runTest {
        val receipts = listOf(
            Receipt(
                id = 1,
                merchant = "Corner Shop",
                purchaseDate = LocalDate.of(2026, 3, 3),
                total = Money(450, "EUR"),
                category = ReceiptCategory.GROCERIES,
                note = null,
            ),
        )
        whenever(receiptRepository.monitorReceipts()).thenReturn(flowOf(receipts))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(receipts)
            awaitComplete()
        }
    }
}
