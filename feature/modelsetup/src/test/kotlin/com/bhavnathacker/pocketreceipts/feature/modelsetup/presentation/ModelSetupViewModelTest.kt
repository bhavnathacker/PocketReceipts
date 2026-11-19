package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation

import app.cash.turbine.test
import com.bhavnathacker.pocketreceipts.core.test.extension.CoroutineMainDispatcherExtension
import com.bhavnathacker.pocketreceipts.domain.ai.ModelFailure
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import com.bhavnathacker.pocketreceipts.domain.ai.MonitorModelStateUseCase
import com.bhavnathacker.pocketreceipts.domain.ai.StartModelDownloadUseCase
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupError
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupStatus
import com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model.ModelSetupStatusMapper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ModelSetupViewModelTest {
    private lateinit var underTest: ModelSetupViewModel

    private val monitorModelStateUseCase = mock<MonitorModelStateUseCase>()
    private val startModelDownloadUseCase = mock<StartModelDownloadUseCase>()
    private val modelStates = MutableStateFlow<ModelState>(ModelState.NotDownloaded(SIZE))

    @AfterEach
    fun tearDown() {
        reset(monitorModelStateUseCase, startModelDownloadUseCase)
    }

    private fun initViewModel() {
        whenever(monitorModelStateUseCase()).thenReturn(modelStates)
        underTest = ModelSetupViewModel(
            monitorModelStateUseCase = monitorModelStateUseCase,
            startModelDownloadUseCase = startModelDownloadUseCase,
            modelSetupStatusMapper = ModelSetupStatusMapper(),
        )
    }

    @Test
    fun `test that uiState is Checking until the model state is known`() = runTest {
        whenever(monitorModelStateUseCase()).thenReturn(emptyFlow())
        underTest = ModelSetupViewModel(monitorModelStateUseCase, startModelDownloadUseCase, ModelSetupStatusMapper())

        assertThat(underTest.uiState.value.status).isEqualTo(ModelSetupStatus.Checking)
    }

    @Test
    fun `test that uiState offers the download with its size when the model is not downloaded`() = runTest {
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().status).isEqualTo(ModelSetupStatus.NotDownloaded(SIZE))
        }
    }

    @Test
    fun `test that uiState follows the download progress`() = runTest {
        initViewModel()

        underTest.uiState.test {
            skipItems(1)
            modelStates.value = ModelState.Downloading(downloadedBytes = SIZE / 4, totalBytes = SIZE)
            assertThat(awaitItem().status).isEqualTo(ModelSetupStatus.Downloading(25))
            modelStates.value = ModelState.Verifying
            assertThat(awaitItem().status).isEqualTo(ModelSetupStatus.Verifying)
            modelStates.value = ModelState.Loading
            assertThat(awaitItem().status).isEqualTo(ModelSetupStatus.Preparing)
            modelStates.value = ModelState.Ready
            assertThat(awaitItem().status).isEqualTo(ModelSetupStatus.Ready)
        }
    }

    @Test
    fun `test that uiState shows the no connection error`() = runTest {
        modelStates.value = ModelState.Failed(ModelFailure.NO_CONNECTION)
        initViewModel()

        assertThat(underTest.uiState.value.status).isEqualTo(ModelSetupStatus.Failed(ModelSetupError.NO_CONNECTION))
    }

    @Test
    fun `test that uiState shows the storage error with the space needed`() = runTest {
        modelStates.value = ModelState.NotEnoughStorage(requiredBytes = 3_000_000_000)
        initViewModel()

        assertThat(underTest.uiState.value.status)
            .isEqualTo(ModelSetupStatus.Failed(ModelSetupError.NOT_ENOUGH_STORAGE, requiredBytes = 3_000_000_000))
    }

    @Test
    fun `test that uiState shows the download failed error for a corrupt download`() = runTest {
        modelStates.value = ModelState.Failed(ModelFailure.CORRUPT)
        initViewModel()

        assertThat(underTest.uiState.value.status).isEqualTo(ModelSetupStatus.Failed(ModelSetupError.DOWNLOAD_FAILED))
    }

    @Test
    fun `test that uiState shows the cannot run error`() = runTest {
        modelStates.value = ModelState.CannotRun
        initViewModel()

        assertThat(underTest.uiState.value.status).isEqualTo(ModelSetupStatus.Failed(ModelSetupError.CANNOT_RUN))
    }

    @Test
    fun `test that onDownloadClick starts the download`() = runTest {
        initViewModel()

        underTest.onDownloadClick()

        verify(startModelDownloadUseCase).invoke()
    }

    @Test
    fun `test that onDownloadClick shows the download failed error when the download can't start`() = runTest {
        startModelDownloadUseCase.stub { onBlocking { invoke() } doThrow IllegalStateException("no downloader") }
        initViewModel()

        underTest.onDownloadClick()

        assertThat(underTest.uiState.value.status).isEqualTo(ModelSetupStatus.Failed(ModelSetupError.DOWNLOAD_FAILED))
    }

    @Test
    fun `test that onRetryClick downloads again after a download failure`() = runTest {
        modelStates.value = ModelState.Failed(ModelFailure.DOWNLOAD_FAILED)
        initViewModel()

        underTest.onRetryClick()

        verify(startModelDownloadUseCase).invoke()
    }

    @Test
    fun `test that onRetryClick loads the model again when the phone couldn't run it`() = runTest {
        modelStates.value = ModelState.CannotRun
        initViewModel()

        underTest.onRetryClick()

        verify(monitorModelStateUseCase, times(2)).invoke()
        verify(startModelDownloadUseCase, never()).invoke()
    }

    @Test
    fun `test that uiState shows the cannot run error when following the setup fails`() = runTest {
        whenever(monitorModelStateUseCase()).thenReturn(flow { throw IllegalStateException("broken") })
        underTest = ModelSetupViewModel(monitorModelStateUseCase, startModelDownloadUseCase, ModelSetupStatusMapper())

        assertThat(underTest.uiState.value.status).isEqualTo(ModelSetupStatus.Failed(ModelSetupError.CANNOT_RUN))
    }

    private companion object {
        const val SIZE = 2_588_147_712L
    }
}
