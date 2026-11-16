package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import app.cash.turbine.test
import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.bhavnathacker.pocketreceipts.domain.receipt.DeleteReceiptUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.MonitorReceiptUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ReceiptDetailUiMapper
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptDetailUiModel
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ReceiptDetailViewModelTest {
    private lateinit var underTest: ReceiptDetailViewModel

    private val monitorReceiptUseCase = mock<MonitorReceiptUseCase>()
    private val deleteReceiptUseCase = mock<DeleteReceiptUseCase>()
    private val receiptDetailUiMapper = mock<ReceiptDetailUiMapper>()

    private val receipt = mock<Receipt>()
    private val detail = ReceiptDetailUiModel(
        merchant = "Corner Shop",
        date = "3 Mar 2026",
        total = "£4.50",
        categoryLabel = 0,
        note = null,
    )
    private val receiptFlow = MutableStateFlow<Receipt?>(receipt)

    @BeforeEach
    fun setUp() {
        receiptFlow.value = receipt
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn receiptFlow }
        receiptDetailUiMapper.stub { on { invoke(any(), any()) } doReturn detail }
    }

    @AfterEach
    fun tearDown() {
        reset(monitorReceiptUseCase, deleteReceiptUseCase, receiptDetailUiMapper)
    }

    private fun initViewModel() {
        underTest = ReceiptDetailViewModel(
            monitorReceiptUseCase = monitorReceiptUseCase,
            deleteReceiptUseCase = deleteReceiptUseCase,
            receiptDetailUiMapper = receiptDetailUiMapper,
            receiptId = 7,
        )
    }

    private fun stubDeleteThrows() {
        deleteReceiptUseCase.stub {
            onBlocking { invoke(any()) } doSuspendableAnswer { throw IllegalStateException("db") }
        }
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flow { awaitCancellation() } }
        initViewModel()

        assertThat(underTest.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `test that initial state has no receipt`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flow { awaitCancellation() } }
        initViewModel()

        assertThat(underTest.uiState.value.receipt).isNull()
    }

    @Test
    fun `test that state shows the mapped receipt`() = runTest {
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().receipt).isEqualTo(detail)
        }
    }

    @Test
    fun `test that state stops loading when the receipt is loaded`() = runTest {
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().isLoading).isFalse()
        }
    }

    @Test
    fun `test that closeEvent fires when the receipt no longer exists`() = runTest {
        initViewModel()

        receiptFlow.value = null

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that closeEvent fires when the receipt cannot be loaded`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flow { throw IllegalStateException("db") } }
        initViewModel()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onDeleteClick shows the delete confirmation`() = runTest {
        initViewModel()

        underTest.onDeleteClick()

        assertThat(underTest.uiState.value.showDeleteConfirmation).isTrue()
    }

    @Test
    fun `test that onDeleteClick does not delete the receipt`() = runTest {
        initViewModel()

        underTest.onDeleteClick()

        verify(deleteReceiptUseCase, never()).invoke(any())
    }

    @Test
    fun `test that onDeleteDismiss hides the delete confirmation`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteDismiss()

        assertThat(underTest.uiState.value.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `test that onDeleteDismiss does not delete the receipt`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteDismiss()

        verify(deleteReceiptUseCase, never()).invoke(any())
    }

    @Test
    fun `test that onDeleteDismiss does not fire closeEvent`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteDismiss()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onDeleteConfirm deletes the receipt`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        verify(deleteReceiptUseCase).invoke(7)
    }

    @Test
    fun `test that onDeleteConfirm hides the delete confirmation`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        assertThat(underTest.uiState.value.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `test that onDeleteConfirm fires closeEvent when the receipt is deleted`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onDeleteConfirm fires deleteFailedEvent when deleting throws`() = runTest {
        stubDeleteThrows()
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        assertThat(underTest.uiState.value.deleteFailedEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onDeleteConfirm does not fire closeEvent when deleting throws`() = runTest {
        stubDeleteThrows()
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onDeleteConfirm hides the delete confirmation when deleting throws`() = runTest {
        stubDeleteThrows()
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        assertThat(underTest.uiState.value.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `test that onCloseEventConsumed resets closeEvent`() = runTest {
        initViewModel()
        receiptFlow.value = null

        underTest.onCloseEventConsumed()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
    }

    @Test
    fun `test that onDeleteFailedEventConsumed resets deleteFailedEvent`() = runTest {
        stubDeleteThrows()
        initViewModel()
        underTest.onDeleteClick()
        underTest.onDeleteConfirm()

        underTest.onDeleteFailedEventConsumed()

        assertThat(underTest.uiState.value.deleteFailedEvent).isEqualTo(StateEvent.Consumed)
    }
}
