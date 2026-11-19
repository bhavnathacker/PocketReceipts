package com.bhavnathacker.pocketreceipts.domain.ai

/**
 * Where the on-device model that reads receipts is in its setup. The app is
 * usable only in [Ready].
 */
sealed interface ModelState {

    /** The model isn't on the phone; downloading it takes [sizeBytes]. */
    data class NotDownloaded(val sizeBytes: Long) : ModelState

    /** The model is downloading: [downloadedBytes] of [totalBytes] so far. */
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ModelState

    /** The download finished and its checksum is being checked. */
    data object Verifying : ModelState

    /** The verified model is being loaded once, to prove the phone can run it. */
    data object Loading : ModelState

    /** The model is verified and the phone can run it. */
    data object Ready : ModelState

    /** The phone has less free storage than the download needs ([requiredBytes]). */
    data class NotEnoughStorage(val requiredBytes: Long) : ModelState

    /** The download failed for [reason]; it can be started again. */
    data class Failed(val reason: ModelFailure) : ModelState

    /** The verified model can't be loaded on this phone (for example, too little memory). */
    data object CannotRun : ModelState
}

/** Why a model download failed. */
enum class ModelFailure {
    /** There was no network connection. */
    NO_CONNECTION,

    /** The download stopped for any other reason. */
    DOWNLOAD_FAILED,

    /** The downloaded file's size or checksum didn't match; it was deleted. */
    CORRUPT,
}
