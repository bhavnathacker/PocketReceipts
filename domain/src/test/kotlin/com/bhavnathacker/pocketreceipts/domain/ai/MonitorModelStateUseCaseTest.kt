package com.bhavnathacker.pocketreceipts.domain.ai

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorModelStateUseCaseTest {
    private lateinit var underTest: MonitorModelStateUseCase

    private val modelRepository = mock<ModelRepository>()
    private val modelRuntimeRepository = mock<ModelRuntimeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorModelStateUseCase(
            modelRepository = modelRepository,
            modelRuntimeRepository = modelRuntimeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(modelRepository, modelRuntimeRepository)
    }

    @Test
    fun `test that invoke passes download states through`() = runTest {
        val downloading = ModelState.Downloading(downloadedBytes = 10, totalBytes = 100)
        whenever(modelRepository.monitorModelState()).thenReturn(flowOf(downloading))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(downloading)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke does not load the model when it is not verified yet`() = runTest {
        whenever(modelRepository.monitorModelState()).thenReturn(flowOf(ModelState.Verifying))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Verifying)
            awaitComplete()
        }
        verify(modelRuntimeRepository, never()).canLoad(any())
    }

    @Test
    fun `test that invoke marks the load verified when the model loads`() = runTest {
        whenever(modelRepository.monitorModelState()).thenReturn(flowOf(ModelState.Loading))
        whenever(modelRepository.modelPath()).thenReturn(MODEL_PATH)
        whenever(modelRuntimeRepository.canLoad(MODEL_PATH)).thenReturn(true)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Loading)
            awaitComplete()
        }
        verify(modelRepository).markLoadVerified()
    }

    @Test
    fun `test that invoke emits CannotRun when the model fails to load`() = runTest {
        whenever(modelRepository.monitorModelState()).thenReturn(flowOf(ModelState.Loading))
        whenever(modelRepository.modelPath()).thenReturn(MODEL_PATH)
        whenever(modelRuntimeRepository.canLoad(MODEL_PATH)).thenReturn(false)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Loading)
            assertThat(awaitItem()).isEqualTo(ModelState.CannotRun)
            awaitComplete()
        }
        verify(modelRepository, never()).markLoadVerified()
    }

    @Test
    fun `test that invoke emits CannotRun when there is no model path`() = runTest {
        whenever(modelRepository.monitorModelState()).thenReturn(flowOf(ModelState.Loading))
        whenever(modelRepository.modelPath()).thenReturn(null)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Loading)
            assertThat(awaitItem()).isEqualTo(ModelState.CannotRun)
            awaitComplete()
        }
    }

    private companion object {
        const val MODEL_PATH = "/storage/emulated/0/Android/data/app/files/models/gemma-4-E2B-it.litertlm"
    }
}
