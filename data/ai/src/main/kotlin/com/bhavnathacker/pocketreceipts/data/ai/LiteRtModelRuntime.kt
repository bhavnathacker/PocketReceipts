package com.bhavnathacker.pocketreceipts.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the model with LiteRT-LM for one piece of work and releases it afterwards,
 * so the model's memory is only held while a receipt is being read.
 *
 * The GPU backend is tried first (faster, and lighter on memory); when it can't
 * initialize or fails while running, the work is repeated on the CPU backend.
 * All engine work runs on the [InferenceDispatcher] thread.
 */
@Singleton
internal class LiteRtModelRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:InferenceDispatcher private val dispatcher: CoroutineDispatcher,
) {

    /** Runs [block] with an initialized engine for [modelPath], then closes the engine. */
    suspend fun <T> withEngine(modelPath: String, block: (Engine) -> T): T = withContext(dispatcher) {
        try {
            run(modelPath, Backend.GPU(), block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "GPU backend failed; using the CPU backend")
            run(modelPath, Backend.CPU(), block)
        }
    }

    private fun <T> run(modelPath: String, backend: Backend, block: (Engine) -> T): T =
        Engine(EngineConfig(modelPath = modelPath, backend = backend, cacheDir = context.cacheDir.path)).use { engine ->
            engine.initialize()
            block(engine)
        }
}
