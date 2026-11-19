package com.bhavnathacker.pocketreceipts.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Downloads and verifies the on-device model file.
 */
interface ModelRepository {

    /**
     * The state of the model file. Emits [ModelState.Loading] once the file is
     * verified but hasn't been shown to load yet, and [ModelState.Ready] after
     * [markLoadVerified]. Never emits [ModelState.CannotRun]: loading is the
     * runtime's job (see [ModelRuntimeRepository]).
     */
    fun monitorModelState(): Flow<ModelState>

    /** Starts (or restarts, after a failure) the model download. */
    suspend fun startDownload()

    /** Records that the verified model loaded on this phone, so it isn't tested again. */
    suspend fun markLoadVerified()

    /** The absolute path of the verified model file, or null when there is none. */
    suspend fun modelPath(): String?
}
