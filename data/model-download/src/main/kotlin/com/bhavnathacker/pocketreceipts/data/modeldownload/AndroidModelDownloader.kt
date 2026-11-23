package com.bhavnathacker.pocketreceipts.data.modeldownload

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * [ModelDownloader] backed by the system [DownloadManager]: the system resumes the
 * download after network drops, keeps it going when the app is killed, and shows
 * its progress in a notification.
 */
internal class AndroidModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelDownloader {

    private val downloadManager = checkNotNull(context.getSystemService<DownloadManager>())

    override fun enqueue(url: String, destination: File): Long =
        downloadManager.enqueue(
            DownloadManager.Request(url.toUri())
                .setDestinationUri(Uri.fromFile(destination))
                .setTitle(context.getString(R.string.model_download_title))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true),
        )

    override fun query(id: Long): DownloadStatus? =
        downloadManager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Successful
                DownloadManager.STATUS_FAILED ->
                    DownloadStatus.Failed(insufficientSpace = reason == DownloadManager.ERROR_INSUFFICIENT_SPACE)
                DownloadManager.STATUS_PAUSED if reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK ||
                    reason == DownloadManager.PAUSED_WAITING_TO_RETRY ->
                    DownloadStatus.WaitingForNetwork(downloaded, total)
                else -> DownloadStatus.Running(downloaded, total)
            }
        }

    override fun remove(id: Long) {
        downloadManager.remove(id)
    }
}
