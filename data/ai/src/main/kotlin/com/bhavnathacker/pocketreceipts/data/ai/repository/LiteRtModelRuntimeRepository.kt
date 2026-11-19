package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRuntimeRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Loads the model with the shared [LiteRtModelRuntime]. */
internal class LiteRtModelRuntimeRepository @Inject constructor(
    private val runtime: LiteRtModelRuntime,
) : ModelRuntimeRepository {

    /** Loads the model once to prove it runs on this phone, then releases it: setup doesn't need it loaded. */
    override suspend fun canLoad(modelPath: String): Boolean =
        try {
            runtime.withEngine(modelPath) { true }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "The model can't be loaded on this phone")
            false
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "Not enough memory to load the model")
            false
        } finally {
            withContext(NonCancellable) { runtime.releaseNow() }
        }

    override suspend fun keepLoaded(modelPath: String) {
        runtime.hold(modelPath)
    }
}
