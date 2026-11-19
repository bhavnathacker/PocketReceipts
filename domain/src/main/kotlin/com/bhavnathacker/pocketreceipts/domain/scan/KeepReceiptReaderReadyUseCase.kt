package com.bhavnathacker.pocketreceipts.domain.scan

import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRuntimeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the receipt reader (the on-device model) while a scan is in progress and keeps it
 * loaded until the caller's coroutine is cancelled, so the scan is read without waiting for
 * the model to load.
 *
 * Returns quietly when the model isn't downloaded or can't be loaded: reading the scan then
 * loads the model itself and reports any failure.
 */
class KeepReceiptReaderReadyUseCase @Inject constructor(
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
            // Reading the scan loads the model again and reports the failure.
        } catch (e: OutOfMemoryError) {
            // As above: reading reports it.
        }
    }
}
