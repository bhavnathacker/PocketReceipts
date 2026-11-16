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

    @Test
    fun `test that initial state is loading`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flow { awaitCancellation() } }
        initViewModel()

        assertThat(underTest.uiState.value.isLoading).isTrue()
        assertThat(underTest.uiState.value.receipt).isNull()
    }

    @Test
    fun `test that state shows the mapped receipt`() = runTest {
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.receipt).isEqualTo(detail)
        }
    }

    @Test
    fun `test that closeEvent fires when the receipt no longer exists`() = runTest {
        initViewModel()

        receiptFlow.value = null

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onDeleteClick asks for confirmation without deleting`() = runTest {
        initViewModel()

        underTest.onDeleteClick()

        assertThat(underTest.uiState.value.showDeleteConfirmation).isTrue()
        verify(deleteReceiptUseCase, never()).invoke(any())
    }

    @Test
    fun `test that onDeleteDismiss hides the confirmation and keeps the receipt`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteDismiss()

        val state = underTest.uiState.value
        assertThat(state.showDeleteConfirmation).isFalse()
        assertThat(state.closeEvent).isEqualTo(StateEvent.Consumed)
        verify(deleteReceiptUseCase, never()).invoke(any())
    }

    @Test
    fun `test that onDeleteConfirm deletes the receipt and fires closeEvent`() = runTest {
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        verify(deleteReceiptUseCase).invoke(7)
        val state = underTest.uiState.value
        assertThat(state.showDeleteConfirmation).isFalse()
        assertThat(state.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that onDeleteConfirm fires deleteFailedEvent when deleting throws`() = runTest {
        deleteReceiptUseCase.stub {
            onBlocking { invoke(any()) } doSuspendableAnswer { throw IllegalStateException("db") }
        }
        initViewModel()
        underTest.onDeleteClick()

        underTest.onDeleteConfirm()

        val state = underTest.uiState.value
        assertThat(state.deleteFailedEvent).isEqualTo(StateEvent.Triggered)
        assertThat(state.closeEvent).isEqualTo(StateEvent.Consumed)
        assertThat(state.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `test that closeEvent fires when the receipt cannot be loaded`() = runTest {
        monitorReceiptUseCase.stub { on { invoke(7) } doReturn flow { throw IllegalStateException("bad key") } }
        initViewModel()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that consuming events resets them`() = runTest {
        initViewModel()
        receiptFlow.value = null

        underTest.onCloseEventConsumed()
        underTest.onDeleteFailedEventConsumed()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(StateEvent.Consumed)
        assertThat(underTest.uiState.value.deleteFailedEvent).isEqualTo(StateEvent.Consumed)
    }
}
