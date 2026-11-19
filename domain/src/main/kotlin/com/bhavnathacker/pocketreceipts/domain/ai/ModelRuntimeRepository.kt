package com.bhavnathacker.pocketreceipts.domain.ai

/**
 * Runs the on-device model.
 */
interface ModelRuntimeRepository {

    /** Loads the model at [modelPath] once and releases it; false if it can't be loaded here. */
    suspend fun canLoad(modelPath: String): Boolean

    /**
     * Loads the model at [modelPath] if it isn't loaded yet, then keeps it loaded until the
     * calling coroutine is cancelled: it doesn't return normally. Work that needs the model
     * meanwhile uses the loaded one.
     *
     * @throws Exception if the model can't be loaded.
     */
    suspend fun keepLoaded(modelPath: String)
}
