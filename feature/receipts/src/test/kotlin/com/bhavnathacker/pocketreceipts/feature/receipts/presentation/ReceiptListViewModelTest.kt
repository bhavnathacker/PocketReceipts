package com.bhavnathacker.pocketreceipts.feature.receipts.presentation

import app.cash.turbine.test
import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.bhavnathacker.pocketreceipts.domain.receipt.MonitorReceiptsUseCase
import com.bhavnathacker.pocketreceipts.domain.receipt.Receipt
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.mapper.ReceiptListItemUiMapper
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListItemUiModel
import com.bhavnathacker.pocketreceipts.feature.receipts.presentation.model.ReceiptListUiState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ReceiptListViewModelTest {
    private lateinit var underTest: ReceiptListViewModel

    private val monitorReceiptsUseCase = mock<MonitorReceiptsUseCase>()
    private val receiptListItemUiMapper = mock<ReceiptListItemUiMapper>()

    @BeforeEach
    fun setUp() {
        monitorReceiptsUseCase.stub {
            on { invoke() } doReturn emptyFlow()
        }
    }

    @AfterEach
    fun tearDown() {
        reset(monitorReceiptsUseCase, receiptListItemUiMapper)
    }

    private fun initViewModel() {
        underTest = ReceiptListViewModel(
            monitorReceiptsUseCase = monitorReceiptsUseCase,
            receiptListItemUiMapper = receiptListItemUiMapper,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        monitorReceiptsUseCase.stub {
            on { invoke() } doReturn flow { awaitCancellation() }
        }
        initViewModel()

        assertThat(underTest.uiState.value).isEqualTo(ReceiptListUiState.Loading)
    }

    @Test
    fun `test that state is Empty when there are no receipts`() = runTest {
        monitorReceiptsUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList<Receipt>())
                awaitCancellation()
            }
        }
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ReceiptListUiState.Empty)
        }
    }

    @Test
    fun `test that state contains mapped receipts when use case emits receipts`() = runTest {
        val receipts = listOf(mock<Receipt>())
        val items = listOf(
            ReceiptListItemUiModel(id = 1, merchant = "Corner Shop", date = "3 Mar 2026", total = "€4.50", categoryLabel = 0),
        )
        monitorReceiptsUseCase.stub {
            on { invoke() } doReturn flow {
                emit(receipts)
                awaitCancellation()
            }
        }
        receiptListItemUiMapper.stub {
            on { invoke(any<List<Receipt>>(), any()) } doReturn items
        }
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ReceiptListUiState.Receipts(items))
        }
    }

    @Test
    fun `test that state becomes Empty when the last receipt is deleted`() = runTest {
        val items = listOf(
            ReceiptListItemUiModel(id = 1, merchant = "Corner Shop", date = "", total = "", categoryLabel = 0),
        )
        monitorReceiptsUseCase.stub {
            on { invoke() } doReturn flow {
                emit(listOf(mock<Receipt>()))
                emit(emptyList<Receipt>())
                awaitCancellation()
            }
        }
        receiptListItemUiMapper.stub {
            on { invoke(any<List<Receipt>>(), any()) } doReturn items
        }
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ReceiptListUiState.Empty)
        }
    }

    @Test
    fun `test that state is Error when receipts cannot be loaded`() = runTest {
        monitorReceiptsUseCase.stub {
            on { invoke() } doReturn flow { throw IllegalStateException("db") }
        }
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ReceiptListUiState.Error)
        }
    }
}
