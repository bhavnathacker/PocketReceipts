package com.bhavnathacker.pocketreceipts.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The processor the model runs on. */
internal enum class ReaderBackend { GPU, CPU }

/** Creates a LiteRT-LM [Engine] with the model loaded. A blocking call: run it on the inference thread. */
internal fun interface EngineFactory {

    /**
     * Creates an engine for the model at [modelPath] on [backend] and initializes it;
     * throws if it can't be loaded.
     */
    fun create(modelPath: String, backend: ReaderBackend): Engine
}

/**
 * Creates engines on the GPU or the CPU backend. Which one to use is decided by
 * [LiteRtModelRuntime]: the GPU only where [GpuSupport] finds OpenCL. The `cacheDir` keeps
 * the GPU's prepared programs, so only the first GPU load on a phone is slow.
 */
internal class LiteRtEngineFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : EngineFactory {

    override fun create(modelPath: String, backend: ReaderBackend): Engine {
        val liteRtBackend = when (backend) {
            ReaderBackend.GPU -> Backend.GPU()
            ReaderBackend.CPU -> Backend.CPU()
        }
        val engine = Engine(EngineConfig(modelPath = modelPath, backend = liteRtBackend, cacheDir = context.cacheDir.path))
        try {
            engine.initialize()
        } catch (t: Throwable) {
            engine.close()
            throw t
        }
        return engine
    }
}
