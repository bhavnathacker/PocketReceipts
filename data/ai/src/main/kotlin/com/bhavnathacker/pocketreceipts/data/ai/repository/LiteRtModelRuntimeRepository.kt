package com.bhavnathacker.pocketreceipts.data.ai.repository

import com.bhavnathacker.pocketreceipts.data.ai.LiteRtModelRuntime
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRuntimeRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Proves the model can run on this phone by loading it once with [LiteRtModelRuntime]. */
internal class LiteRtModelRuntimeRepository @Inject constructor(
    private val runtime: LiteRtModelRuntime,
) : ModelRuntimeRepository {

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
        }
}
