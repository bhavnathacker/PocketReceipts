package com.bhavnathacker.pocketreceipts.data.ai

import com.google.ai.edge.litertlm.Engine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LiteRtModelRuntimeTest {

    private val modelPath = "/models/gemma.litertlm"
    private val foreground = MutableStateFlow(true)
    private val engines = mutableListOf<Engine>()
    private var failNextLoad: Throwable? = null

    private val engineFactory = EngineFactory {
        failNextLoad?.let { failure ->
            failNextLoad = null
            throw failure
        }
        mock<Engine>().also { engines += it }
    }

    private fun TestScope.createRuntime() = LiteRtModelRuntime(
        engineFactory = engineFactory,
        appVisibility = object : AppVisibility {
            override val isInForeground = foreground
        },
        dispatcher = StandardTestDispatcher(testScheduler),
    ).also { runCurrent() }

    @Test
    fun `test that withEngine uses the engine loaded by hold`() = runTest {
        val underTest = createRuntime()

        val hold = launch { underTest.hold(modelPath) }
        runCurrent()
        val used = underTest.withEngine(modelPath) { it }

        assertThat(engines).hasSize(1)
        assertThat(used).isSameInstanceAs(engines.single())
        hold.cancel()
    }

    @Test
    fun `test that withEngine waits for the load in progress when hold started it`() = runTest {
        val underTest = createRuntime()

        val hold = launch { underTest.hold(modelPath) }
        val read = launch { underTest.withEngine(modelPath) { } }
        runCurrent()

        assertThat(read.isCompleted).isTrue()
        assertThat(engines).hasSize(1)
        hold.cancel()
    }

    @Test
    fun `test that withEngine keeps the engine loaded between calls when they are less than 2 minutes apart`() = runTest {
        val underTest = createRuntime()

        underTest.withEngine(modelPath) { }
        advanceTimeBy(1.minutes)
        underTest.withEngine(modelPath) { }

        assertThat(engines).hasSize(1)
        verify(engines.single(), never()).close()
    }

    @Test
    fun `test that the engine is closed 2 minutes after its last holder ends`() = runTest {
        val underTest = createRuntime()

        val hold = launch { underTest.hold(modelPath) }
        runCurrent()
        hold.cancel()
        advanceTimeBy(2.minutes - 1.seconds)
        verify(engines.single(), never()).close()

        advanceTimeBy(2.seconds)
        verify(engines.single()).close()
    }

    @Test
    fun `test that a new holder cancels the pending release`() = runTest {
        val underTest = createRuntime()
        underTest.withEngine(modelPath) { }
        advanceTimeBy(1.minutes)

        val hold = launch { underTest.hold(modelPath) }
        advanceTimeBy(5.minutes)

        verify(engines.single(), never()).close()
        assertThat(engines).hasSize(1)
        hold.cancel()
    }

    @Test
    fun `test that the engine is closed at once when the app goes to the background with no holder`() = runTest {
        val underTest = createRuntime()
        underTest.withEngine(modelPath) { }

        foreground.value = false
        runCurrent()

        verify(engines.single()).close()
    }

    @Test
    fun `test that the engine stays loaded in the background while a holder is active`() = runTest {
        val underTest = createRuntime()
        val hold = launch { underTest.hold(modelPath) }
        runCurrent()

        foreground.value = false
        advanceTimeBy(10.minutes)
        verify(engines.single(), never()).close()

        hold.cancel()
        runCurrent()
        verify(engines.single()).close()
    }

    @Test
    fun `test that withEngine loads the model again after a failed load`() = runTest {
        val underTest = createRuntime()
        failNextLoad = IOException("can't load")

        assertThrows<IOException> { underTest.withEngine(modelPath) { } }
        underTest.withEngine(modelPath) { }

        assertThat(engines).hasSize(1)
    }

    @Test
    fun `test that hold throws when the model can't be loaded`() = runTest {
        val underTest = createRuntime()
        failNextLoad = OutOfMemoryError()

        assertThrows<OutOfMemoryError> { underTest.hold(modelPath) }
    }

    @Test
    fun `test that releaseNow closes the engine when there is no holder`() = runTest {
        val underTest = createRuntime()
        underTest.withEngine(modelPath) { }

        underTest.releaseNow()

        verify(engines.single()).close()
    }

    @Test
    fun `test that releaseNow keeps the engine while a holder is active`() = runTest {
        val underTest = createRuntime()
        val hold = launch { underTest.hold(modelPath) }
        runCurrent()

        underTest.releaseNow()

        verify(engines.single(), never()).close()
        hold.cancel()
    }
}
