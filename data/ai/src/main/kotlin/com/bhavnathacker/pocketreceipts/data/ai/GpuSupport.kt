package com.bhavnathacker.pocketreceipts.data.ai

import javax.inject.Inject

/** Whether this phone can run the model on its GPU. */
internal interface GpuSupport {

    /** True when LiteRT-LM's GPU backend can use OpenCL here. Blocking on first use. */
    val isAvailable: Boolean
}

/**
 * Checks for OpenCL by loading the vendor's `libOpenCL.so`, which is what LiteRT-LM's GPU
 * backend loads (the manifest declares it as an optional native library). Without OpenCL,
 * the GPU backend falls back to WebGPU, which can run for minutes before failing, so
 * those phones must use the CPU from the start.
 */
internal class OpenClGpuSupport @Inject constructor() : GpuSupport {

    override val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("OpenCL")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
