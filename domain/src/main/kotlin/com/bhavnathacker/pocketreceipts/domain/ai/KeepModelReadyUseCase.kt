package com.bhavnathacker.pocketreceipts.domain.ai

import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the on-device model ahead of use (while a scan is in progress, or while Ask is shown)
 * and keeps it loaded until the caller's coroutine is cancelled, so the work that follows runs
 * without waiting for the model to load.
 *
 * Returns quietly when the model isn't downloaded or can't be loaded: the work that needs the
 * model then loads it itself and reports any failure.
 */
class KeepModelReadyUseCase @Inject constructor(
    private val modelRepository: ModelRepository,
    private val modelRuntimeRepository: ModelRuntimeRepository,
) {
    suspend operator fun invoke() {
        val modelPath = modelRepository.modelPath() ?: return
        try {
            modelRuntimeRepository.keepLoaded(modelPath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The work that needs the model loads it again and reports the failure.
        } catch (e: OutOfMemoryError) {
            // As above: that work reports it.
        }
    }
}
