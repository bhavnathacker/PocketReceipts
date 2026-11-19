package com.bhavnathacker.pocketreceipts.domain.ai

/**
 * Runs the on-device model.
 */
interface ModelRuntimeRepository {

    /** Loads the model at [modelPath] once and releases it; false if it can't be loaded here. */
    suspend fun canLoad(modelPath: String): Boolean
}
