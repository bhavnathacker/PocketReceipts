package com.bhavnathacker.pocketreceipts.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Creates a LiteRT-LM [Engine] with the model loaded. A blocking call: run it on the inference thread. */
internal fun interface EngineFactory {

    /** Creates an engine for the model at [modelPath] and initializes it; throws if it can't be loaded. */
    fun create(modelPath: String): Engine
}

/**
 * Creates engines on the CPU backend: it behaves the same on every phone. (The GPU
 * backend falls back to WebGPU on phones without OpenCL, where it can run for
 * minutes before failing.)
 */
internal class LiteRtEngineFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : EngineFactory {

    override fun create(modelPath: String): Engine {
        val engine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU(), cacheDir = context.cacheDir.path))
        try {
            engine.initialize()
        } catch (t: Throwable) {
            engine.close()
            throw t
        }
        return engine
    }
}
