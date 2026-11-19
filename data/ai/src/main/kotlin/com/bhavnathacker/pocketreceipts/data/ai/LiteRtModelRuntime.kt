package com.bhavnathacker.pocketreceipts.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the model with LiteRT-LM for one piece of work and releases it afterwards,
 * so the model's memory is only held while a receipt is being read.
 *
 * The model runs on the CPU backend: it behaves the same on every phone. (The GPU
 * backend falls back to WebGPU on phones without OpenCL, where it can run for
 * minutes before failing.) All engine work runs on the [InferenceDispatcher] thread.
 */
@Singleton
internal class LiteRtModelRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:InferenceDispatcher private val dispatcher: CoroutineDispatcher,
) {

    /** Runs [block] with an initialized engine for [modelPath], then closes the engine. */
    suspend fun <T> withEngine(modelPath: String, block: (Engine) -> T): T = withContext(dispatcher) {
        val config = EngineConfig(modelPath = modelPath, backend = Backend.CPU(), cacheDir = context.cacheDir.path)
        Engine(config).use { engine ->
            engine.initialize()
            block(engine)
        }
    }
}
