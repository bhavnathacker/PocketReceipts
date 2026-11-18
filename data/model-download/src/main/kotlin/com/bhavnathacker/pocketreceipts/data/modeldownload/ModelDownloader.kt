package com.bhavnathacker.pocketreceipts.data.modeldownload

import java.io.File

/**
 * Runs file downloads outside the app's process, so they continue while the app
 * is in the background or closed.
 */
internal interface ModelDownloader {

    /** Starts downloading [url] into [destination] and returns the download's id. */
    fun enqueue(url: String, destination: File): Long

    /** The download's current status, or null if there is no download with [id]. */
    fun query(id: Long): DownloadStatus?

    /** Cancels the download with [id] and deletes what it wrote. */
    fun remove(id: Long)
}

/** The status of a download started with [ModelDownloader.enqueue]. */
internal sealed interface DownloadStatus {

    /** Queued or downloading: [downloadedBytes] of [totalBytes] (-1 when not known yet). */
    data class Running(val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus

    /** Paused until the phone has a network connection again. */
    data class WaitingForNetwork(val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus

    /** Finished; the file is complete. */
    data object Successful : DownloadStatus

    /** Stopped for good; [insufficientSpace] when the phone ran out of storage. */
    data class Failed(val insufficientSpace: Boolean) : DownloadStatus
}
