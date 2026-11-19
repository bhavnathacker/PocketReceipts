package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model

import com.bhavnathacker.pocketreceipts.domain.ai.ModelFailure
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import javax.inject.Inject

/** Maps the model's [ModelState] to what the setup screen shows. */
internal class ModelSetupStatusMapper @Inject constructor() {

    operator fun invoke(state: ModelState): ModelSetupStatus = when (state) {
        is ModelState.NotDownloaded -> ModelSetupStatus.NotDownloaded(state.sizeBytes)
        is ModelState.Downloading -> ModelSetupStatus.Downloading(percent(state.downloadedBytes, state.totalBytes))
        ModelState.Verifying -> ModelSetupStatus.Verifying
        ModelState.Loading -> ModelSetupStatus.Preparing
        ModelState.Ready -> ModelSetupStatus.Ready
        is ModelState.NotEnoughStorage ->
            ModelSetupStatus.Failed(ModelSetupError.NOT_ENOUGH_STORAGE, requiredBytes = state.requiredBytes)
        is ModelState.Failed -> ModelSetupStatus.Failed(
            when (state.reason) {
                ModelFailure.NO_CONNECTION -> ModelSetupError.NO_CONNECTION
                ModelFailure.DOWNLOAD_FAILED, ModelFailure.CORRUPT -> ModelSetupError.DOWNLOAD_FAILED
            },
        )
        ModelState.CannotRun -> ModelSetupStatus.Failed(ModelSetupError.CANNOT_RUN)
    }

    private fun percent(downloaded: Long, total: Long): Int =
        if (total <= 0) 0 else (downloaded * 100 / total).toInt().coerceIn(0, 100)
}
