package com.bhavnathacker.pocketreceipts.data.ai

import com.google.ai.edge.litertlm.Engine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

/**
 * Owns the one loaded LiteRT-LM [Engine], so the model is loaded once and shared.
 *
 * Callers that need the model are holders: a [withEngine] block while it runs, and a
 * [hold] until it's cancelled. The first holder loads the model; later ones use it, or
 * wait for the load in progress. While any holder is active the engine is never closed.
 * With no holders, the engine is closed after [IDLE_TIMEOUT] (2 minutes) while the app is in the
 * foreground, or at once when the app is in the background, to give its memory back.
 *
 * The engine runs on the GPU where [GpuSupport] finds OpenCL, and on the CPU otherwise. If the
 * GPU can't load the model, or a [withEngine] block fails on it, the runtime switches to the
 * CPU for the rest of the process (and runs that block again on the CPU).
 *
 * All engine work, and all of this class's state, runs on the single [InferenceDispatcher]
 * thread.
 */
@Singleton
internal class LiteRtModelRuntime @Inject constructor(
    private val engineFactory: EngineFactory,
    private val gpuSupport: GpuSupport,
    appVisibility: AppVisibility,
    @param:InferenceDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private var engine: Engine? = null
    private var enginePath: String? = null
    private var engineBackend: ReaderBackend? = null

    /** Null until the first load asks [gpuSupport]; false after a GPU failure. */
    private var gpuUsable: Boolean? = null
    private var holders = 0
    private var inForeground = true
    private var releaseJob: Job? = null

    init {
        scope.launch {
            appVisibility.isInForeground.collect { foreground ->
                mutex.withLock {
                    inForeground = foreground
                    scheduleRelease()
                }
            }
        }
    }

    /**
     * Runs [block] with the engine for [modelPath], loading it first if needed. If [block]
     * throws on a GPU engine, it runs once more on a CPU engine.
     */
    suspend fun <T> withEngine(modelPath: String, block: (Engine) -> T): T = withContext(dispatcher) {
        val engine = acquire(modelPath)
        try {
            try {
                block(engine)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cpuEngine = mutex.withLock { switchToCpuAfterGpuFailure(engine, modelPath, e) } ?: throw e
                block(cpuEngine)
            }
        } finally {
            withContext(NonCancellable) { releaseHolder() }
        }
    }

    /** Loads the engine for [modelPath] if needed and keeps it loaded until cancelled. */
    suspend fun hold(modelPath: String): Nothing = withContext(dispatcher) {
        acquire(modelPath)
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { releaseHolder() }
        }
    }

    /** Closes the engine now unless a holder is using it. */
    suspend fun releaseNow() = withContext(dispatcher) {
        mutex.withLock {
            if (holders == 0) closeEngine()
        }
    }

    private suspend fun acquire(modelPath: String): Engine = mutex.withLock {
        holders++
        releaseJob?.cancel()
        try {
            loadedEngine(modelPath)
        } catch (t: Throwable) {
            holders--
            scheduleRelease()
            throw t
        }
    }

    private suspend fun releaseHolder() = mutex.withLock {
        holders--
        scheduleRelease()
    }

    /**
     * Called with [mutex] held after a block failed on [failed]. Returns a CPU engine to run
     * the block on again, or null when [failed] wasn't a GPU engine (the failure stands).
     */
    private fun switchToCpuAfterGpuFailure(failed: Engine, modelPath: String, failure: Exception): Engine? {
        if (engineBackend != ReaderBackend.GPU) return null
        Timber.w(failure, "Reading failed on the GPU; using the CPU from now on")
        gpuUsable = false
        if (engine === failed) closeEngine()
        return loadedEngine(modelPath)
    }

    private fun loadedEngine(modelPath: String): Engine {
        engine?.takeIf { enginePath == modelPath }?.let { return it }
        closeEngine()
        val start = TimeSource.Monotonic.markNow()
        val (loaded, backend) = try {
            createEngine(modelPath)
        } catch (e: Exception) {
            Timber.e(e, "The model couldn't be loaded")
            throw e
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "Not enough memory to load the model")
            throw e
        }
        Timber.d("Model loaded on %s in %s", backend, start.elapsedNow())
        engine = loaded
        enginePath = modelPath
        engineBackend = backend
        return loaded
    }

    /** Creates the engine on the GPU when it's usable, falling back to the CPU if that fails. */
    private fun createEngine(modelPath: String): Pair<Engine, ReaderBackend> {
        val useGpu = gpuUsable ?: gpuSupport.isAvailable.also { gpuUsable = it }
        if (useGpu) {
            try {
                return engineFactory.create(modelPath, ReaderBackend.GPU) to ReaderBackend.GPU
            } catch (e: Exception) {
                Timber.w(e, "The model couldn't be loaded on the GPU; using the CPU")
            } catch (e: OutOfMemoryError) {
                Timber.w(e, "Not enough memory to load the model on the GPU; using the CPU")
            }
            gpuUsable = false
        }
        return engineFactory.create(modelPath, ReaderBackend.CPU) to ReaderBackend.CPU
    }

    /** Called with [mutex] held, whenever the holders or the app's visibility change. */
    private fun scheduleRelease() {
        releaseJob?.cancel()
        releaseJob = null
        if (holders > 0 || engine == null) return
        if (!inForeground) {
            closeEngine()
        } else {
            releaseJob = scope.launch {
                delay(IDLE_TIMEOUT)
                mutex.withLock {
                    if (holders == 0) closeEngine()
                }
            }
        }
    }

    private fun closeEngine() {
        val closing = engine ?: return
        engine = null
        enginePath = null
        engineBackend = null
        closing.close()
        Timber.d("Model released")
    }

    private companion object {
        /** How long the model stays loaded after its last use while the app is in the foreground. */
        val IDLE_TIMEOUT = 2.minutes
    }
}
