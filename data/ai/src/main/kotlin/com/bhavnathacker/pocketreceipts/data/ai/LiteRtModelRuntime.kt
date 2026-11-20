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
 * All engine work, and all of this class's state, runs on the single [InferenceDispatcher]
 * thread.
 */
@Singleton
internal class LiteRtModelRuntime @Inject constructor(
    private val engineFactory: EngineFactory,
    appVisibility: AppVisibility,
    @param:InferenceDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private var engine: Engine? = null
    private var enginePath: String? = null
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

    /** Runs [block] with the engine for [modelPath], loading it first if needed. */
    suspend fun <T> withEngine(modelPath: String, block: (Engine) -> T): T = withContext(dispatcher) {
        val engine = acquire(modelPath)
        try {
            block(engine)
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

    private fun loadedEngine(modelPath: String): Engine {
        engine?.takeIf { enginePath == modelPath }?.let { return it }
        closeEngine()
        val start = TimeSource.Monotonic.markNow()
        val loaded = try {
            engineFactory.create(modelPath)
        } catch (e: Exception) {
            Timber.e(e, "The model couldn't be loaded")
            throw e
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "Not enough memory to load the model")
            throw e
        }
        Timber.d("Model loaded in %s", start.elapsedNow())
        engine = loaded
        enginePath = modelPath
        return loaded
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
        closing.close()
        Timber.d("Model released")
    }

    private companion object {
        /** How long the model stays loaded after its last use while the app is in the foreground. */
        val IDLE_TIMEOUT = 2.minutes
    }
}
