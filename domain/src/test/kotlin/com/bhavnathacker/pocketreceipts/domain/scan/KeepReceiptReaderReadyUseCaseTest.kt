package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRuntimeRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeepReceiptReaderReadyUseCaseTest {
    private lateinit var underTest: KeepReceiptReaderReadyUseCase

    private val modelRepository = mock<ModelRepository>()
    private val modelRuntimeRepository = mock<ModelRuntimeRepository>()

    private val modelPath = "/models/gemma.litertlm"

    @BeforeAll
    fun setUp() {
        underTest = KeepReceiptReaderReadyUseCase(
            modelRepository = modelRepository,
            modelRuntimeRepository = modelRuntimeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(modelRepository, modelRuntimeRepository)
    }

    private fun stubModelPath(path: String?) {
        modelRepository.stub { onBlocking { modelPath() } doSuspendableAnswer { path } }
    }

    private fun stubKeepLoaded(answer: suspend () -> Unit) {
        modelRuntimeRepository.stub { onBlocking { keepLoaded(any()) } doSuspendableAnswer { answer() } }
    }

    @Test
    fun `test that invoke keeps the model loaded until cancelled when the model is downloaded`() = runTest {
        stubModelPath(modelPath)
        stubKeepLoaded { awaitCancellation() }

        val job = launch { underTest() }
        runCurrent()

        verify(modelRuntimeRepository).keepLoaded(modelPath)
        assertThat(job.isActive).isTrue()
        job.cancel()
        runCurrent()
        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun `test that invoke returns without loading when the model isn't downloaded`() = runTest {
        stubModelPath(null)

        underTest()

        verify(modelRuntimeRepository, never()).keepLoaded(any())
    }

    @Test
    fun `test that invoke returns when loading the model fails`() = runTest {
        stubModelPath(modelPath)
        stubKeepLoaded { throw IOException("can't load") }

        underTest()

        verify(modelRuntimeRepository).keepLoaded(modelPath)
    }

    @Test
    fun `test that invoke returns when loading the model runs out of memory`() = runTest {
        stubModelPath(modelPath)
        stubKeepLoaded { throw OutOfMemoryError() }

        underTest()

        verify(modelRuntimeRepository).keepLoaded(modelPath)
    }
}
