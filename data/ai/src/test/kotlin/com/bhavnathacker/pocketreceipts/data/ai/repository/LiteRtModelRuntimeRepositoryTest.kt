package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.AppVisibility
import com.bhavnathacker.pocketreceipts.data.ai.EngineFactory
import com.bhavnathacker.pocketreceipts.data.ai.GpuSupport
import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.google.ai.edge.litertlm.Engine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LiteRtModelRuntimeRepositoryTest {

    private val modelPath = "/models/gemma.litertlm"
    private val engines = mutableListOf<Engine>()
    private var loadFailure: Throwable? = null

    private fun TestScope.createRepository(): LiteRtModelRuntimeRepository {
        val runtime = LiteRtModelRuntime(
            engineFactory = EngineFactory { _, _ ->
                loadFailure?.let { throw it }
                mock<Engine>().also { engines += it }
            },
            gpuSupport = object : GpuSupport {
                override val isAvailable = false
            },
            appVisibility = object : AppVisibility {
                override val isInForeground = MutableStateFlow(true)
            },
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        runCurrent()
        return LiteRtModelRuntimeRepository(runtime = runtime)
    }

    @Test
    fun `test that canLoad returns true and releases the model when it loads`() = runTest {
        val underTest = createRepository()

        assertThat(underTest.canLoad(modelPath)).isTrue()
        verify(engines.single()).close()
    }

    @Test
    fun `test that canLoad returns false when the model can't be loaded`() = runTest {
        val underTest = createRepository()
        loadFailure = IOException("can't load")

        assertThat(underTest.canLoad(modelPath)).isFalse()
        assertThat(engines).isEmpty()
    }

    @Test
    fun `test that canLoad returns false when there isn't enough memory`() = runTest {
        val underTest = createRepository()
        loadFailure = OutOfMemoryError()

        assertThat(underTest.canLoad(modelPath)).isFalse()
    }

    @Test
    fun `test that keepLoaded keeps the model loaded until cancelled`() = runTest {
        val underTest = createRepository()

        val hold = launch { underTest.keepLoaded(modelPath) }
        runCurrent()

        assertThat(engines).hasSize(1)
        verify(engines.single(), never()).close()
        assertThat(hold.isActive).isTrue()
        hold.cancel()
    }
}
