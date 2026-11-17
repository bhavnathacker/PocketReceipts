package com.bhavnathacker.pocketreceipts.data.ocr.repository

import android.content.Context
import androidx.core.net.toUri
import com.bhavnathacker.pocketreceipts.domain.dispatcher.IoDispatcher
import com.bhavnathacker.pocketreceipts.domain.scan.ScanImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Deletes scanned images. The document scanner writes its results into the app's
 * cache directory, so only `file:` URIs inside it are deleted; anything else is
 * ignored (and logged without its path).
 */
internal class FileScanImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScanImageRepository {

    override suspend fun delete(imageUri: String) = withContext(ioDispatcher) {
        val uri = imageUri.toUri()
        val path = uri.path
        if (uri.scheme != "file" || path == null) {
            Timber.w("Not deleting a scan image that isn't a file")
            return@withContext
        }
        val file = File(path).canonicalFile
        val cacheDir = context.cacheDir.canonicalFile
        if (!file.path.startsWith(cacheDir.path + File.separator)) {
            Timber.w("Not deleting a scan image outside the cache directory")
            return@withContext
        }
        if (file.exists() && !file.delete()) {
            Timber.w("Could not delete a scan image")
        }
    }
}
