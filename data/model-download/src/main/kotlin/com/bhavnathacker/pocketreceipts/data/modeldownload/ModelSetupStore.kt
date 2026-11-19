package com.bhavnathacker.pocketreceipts.data.modeldownload

/**
 * What model setup remembers between launches.
 */
internal interface ModelSetupStore {

    /** The id of the download in progress, or null when none was started. */
    var downloadId: Long?

    /** True once the verified model has been loaded on this phone. */
    var loadVerified: Boolean

    /** Why the last download attempt failed, or null if it didn't. */
    var failure: SetupFailure?
}

/** A failed setup attempt that setup keeps showing until the next attempt. */
internal enum class SetupFailure {
    DOWNLOAD_FAILED,
    CORRUPT,
    NOT_ENOUGH_STORAGE,
}
