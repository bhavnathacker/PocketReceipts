package com.bhavnathacker.pocketreceipts.data.modeldownload.repository

import com.bhavnathacker.pocketreceipts.data.modeldownload.DownloadStatus
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelDownloader
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelSetupStore
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelSpec
import com.bhavnathacker.pocketreceipts.data.modeldownload.SetupFailure
import com.bhavnathacker.pocketreceipts.data.modeldownload.StorageInfo
import com.bhavnathacker.pocketreceipts.domain.ai.ModelFailure
import com.bhavnathacker.pocketreceipts.domain.ai.ModelRepository
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import com.bhavnathacker.pocketreceipts.domain.dispatcher.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Downloads the model with [ModelDownloader] into [modelDirectory], checks its size
 * and SHA-256 once it has finished, and only then gives it its final name. The
 * final file's existence (with the expected size) is what "downloaded" means on
 * later launches; it isn't hashed again.
 */
@Singleton
internal class DefaultModelRepository @Inject constructor(
    private val spec: ModelSpec,
    @param:Named(MODEL_DIRECTORY) private val modelDirectory: File,
    private val downloader: ModelDownloader,
    private val store: ModelSetupStore,
    private val storageInfo: StorageInfo,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ModelRepository {

    private val mutex = Mutex()
    private val modelFile get() = File(modelDirectory, spec.fileName)
    private val partialFile get() = File(modelDirectory, spec.fileName + PARTIAL_SUFFIX)

    override fun monitorModelState(): Flow<ModelState> = flow {
        while (true) {
            emit(mutex.withLock { currentState { emit(it) } })
            delay(POLL_INTERVAL_MILLIS)
        }
    }.distinctUntilChanged().flowOn(ioDispatcher)

    override suspend fun startDownload() = withContext(ioDispatcher) {
        mutex.withLock {
            if (isDownloaded()) return@withLock
            val active = store.downloadId?.let { id -> downloader.query(id)?.takeIf { it.isActive() }?.let { id } }
            if (active != null) return@withLock
            store.downloadId?.let { downloader.remove(it) }
            store.downloadId = null
            store.failure = null
            modelDirectory.mkdirs()
            partialFile.delete()
            if (storageInfo.freeBytes(modelDirectory) < requiredBytes()) {
                store.failure = SetupFailure.NOT_ENOUGH_STORAGE
                return@withLock
            }
            try {
                store.downloadId = downloader.enqueue(spec.url, partialFile)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Model download could not start")
                store.failure = SetupFailure.DOWNLOAD_FAILED
            } catch (e: SecurityException) {
                Timber.e(e, "Model download could not start")
                store.failure = SetupFailure.DOWNLOAD_FAILED
            }
        }
    }

    override suspend fun markLoadVerified() = withContext(ioDispatcher) {
        store.loadVerified = true
    }

    override suspend fun modelPath(): String? = withContext(ioDispatcher) {
        modelFile.takeIf { isDownloaded() }?.absolutePath
    }

    /** Works out the state, verifying a finished download; [onProgress] reports `Verifying` first. */
    private suspend fun currentState(onProgress: suspend (ModelState) -> Unit): ModelState {
        if (isDownloaded()) return if (store.loadVerified) ModelState.Ready else ModelState.Loading
        val id = store.downloadId ?: return failureState() ?: ModelState.NotDownloaded(spec.sizeBytes)
        return when (val status = downloader.query(id)) {
            null -> {
                store.downloadId = null
                failureState() ?: ModelState.NotDownloaded(spec.sizeBytes)
            }
            is DownloadStatus.Running -> ModelState.Downloading(status.downloadedBytes, status.total())
            is DownloadStatus.WaitingForNetwork -> ModelState.Failed(ModelFailure.NO_CONNECTION)
            is DownloadStatus.Failed -> {
                fail(id, if (status.insufficientSpace) SetupFailure.NOT_ENOUGH_STORAGE else SetupFailure.DOWNLOAD_FAILED)
                checkNotNull(failureState())
            }
            DownloadStatus.Successful -> {
                onProgress(ModelState.Verifying)
                if (verify(partialFile)) {
                    partialFile.renameTo(modelFile)
                    store.downloadId = null
                    store.loadVerified = false
                    ModelState.Loading
                } else {
                    fail(id, SetupFailure.CORRUPT)
                    ModelState.Failed(ModelFailure.CORRUPT)
                }
            }
        }
    }

    private fun failureState(): ModelState? = when (store.failure) {
        SetupFailure.DOWNLOAD_FAILED -> ModelState.Failed(ModelFailure.DOWNLOAD_FAILED)
        SetupFailure.CORRUPT -> ModelState.Failed(ModelFailure.CORRUPT)
        SetupFailure.NOT_ENOUGH_STORAGE -> ModelState.NotEnoughStorage(requiredBytes())
        null -> null
    }

    private fun fail(id: Long, failure: SetupFailure) {
        Timber.w("Model download failed: %s", failure)
        downloader.remove(id)
        partialFile.delete()
        store.downloadId = null
        store.failure = failure
    }

    private fun isDownloaded(): Boolean = modelFile.isFile && modelFile.length() == spec.sizeBytes

    private fun verify(file: File): Boolean {
        if (!file.isFile || file.length() != spec.sizeBytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(BUFFER_SIZE).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == spec.sha256
    }

    private fun requiredBytes(): Long = spec.sizeBytes + STORAGE_MARGIN_BYTES

    private fun DownloadStatus.Running.total(): Long = totalBytes.takeIf { it > 0 } ?: spec.sizeBytes

    private fun DownloadStatus.isActive(): Boolean =
        this is DownloadStatus.Running || this is DownloadStatus.WaitingForNetwork || this == DownloadStatus.Successful

    companion object {
        /** Names the directory the model is stored in. */
        const val MODEL_DIRECTORY = "modelDirectory"

        private const val PARTIAL_SUFFIX = ".download"
        private const val STORAGE_MARGIN_BYTES = 100L * 1024 * 1024
        private const val POLL_INTERVAL_MILLIS = 500L
        private const val BUFFER_SIZE = 1 shl 20
    }
}
