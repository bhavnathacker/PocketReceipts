package com.bhavnathacker.pocketreceipts.feature.modelsetup.presentation.model

import androidx.compose.runtime.Immutable

/**
 * The model setup screen.
 *
 * @property status Where setup is; [ModelSetupStatus.Ready] opens the app.
 */
@Immutable
data class ModelSetupUiState(
    val status: ModelSetupStatus = ModelSetupStatus.Checking,
)

/** What the setup screen shows. */
@Immutable
sealed interface ModelSetupStatus {

    /** Setup hasn't found out yet whether the model is on the phone; nothing is shown. */
    data object Checking : ModelSetupStatus

    /** The model needs downloading: the screen offers Download for [sizeBytes]. */
    data class NotDownloaded(val sizeBytes: Long) : ModelSetupStatus

    /** The model is downloading; [percent] is 0 to 100. */
    data class Downloading(val percent: Int) : ModelSetupStatus

    /** The download is being checked. */
    data object Verifying : ModelSetupStatus

    /** The model is being loaded once to make sure the phone can run it. */
    data object Preparing : ModelSetupStatus

    /** The model is ready; the app opens. */
    data object Ready : ModelSetupStatus

    /** Setup failed with [error]; the screen offers Retry. */
    data class Failed(val error: ModelSetupError, val requiredBytes: Long = 0) : ModelSetupStatus
}

/** Why setup failed, in the words the screen uses. */
enum class ModelSetupError {
    NO_CONNECTION,
    NOT_ENOUGH_STORAGE,
    DOWNLOAD_FAILED,
    CANNOT_RUN,
}
