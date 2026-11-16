package com.bhavnathacker.pocketreceipts.domain.receipt

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteReceiptUseCaseTest {
    private lateinit var underTest: DeleteReceiptUseCase

    private val receiptRepository = mock<ReceiptRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteReceiptUseCase(receiptRepository = receiptRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(receiptRepository)
    }

    @Test
    fun `test that invoke deletes the receipt with the given id`() = runTest {
        underTest(7)

        verify(receiptRepository).deleteReceipt(7)
    }

    @Test
    fun `test that invoke throws when repository fails`() = runTest {
        whenever(receiptRepository.deleteReceipt(7)).thenThrow(IllegalStateException("db"))

        assertThrows<IllegalStateException> { underTest(7) }
    }
}
