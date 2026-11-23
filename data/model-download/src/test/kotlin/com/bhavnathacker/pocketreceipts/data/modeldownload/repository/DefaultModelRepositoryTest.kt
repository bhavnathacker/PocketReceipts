package com.bhavnathacker.pocketreceipts.data.modeldownload.repository

import app.cash.turbine.test
import com.bhavnathacker.pocketreceipts.data.modeldownload.DownloadStatus
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelDownloader
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelSetupStore
import com.bhavnathacker.pocketreceipts.data.modeldownload.ModelSpec
import com.bhavnathacker.pocketreceipts.data.modeldownload.SetupFailure
import com.bhavnathacker.pocketreceipts.domain.ai.ModelFailure
import com.bhavnathacker.pocketreceipts.domain.ai.ModelState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest

class DefaultModelRepositoryTest {

    @TempDir
    lateinit var directory: File

    private val content = "a tiny model".toByteArray()
    private val spec = ModelSpec(
        url = "https://example.com/model.litertlm",
        fileName = "model.litertlm",
        sizeBytes = content.size.toLong(),
        sha256 = MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) },
    )
    private val requiredBytes = spec.sizeBytes + 100L * 1024 * 1024

    private lateinit var downloader: FakeModelDownloader
    private lateinit var store: FakeModelSetupStore
    private var freeBytes = Long.MAX_VALUE

    @BeforeEach
    fun setUp() {
        downloader = FakeModelDownloader()
        store = FakeModelSetupStore()
        freeBytes = Long.MAX_VALUE
    }

    private fun TestScope.repository() = DefaultModelRepository(
        spec = spec,
        modelDirectory = directory,
        downloader = downloader,
        store = store,
        storageInfo = { freeBytes },
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private val modelFile get() = File(directory, spec.fileName)
    private val partialFile get() = File(directory, spec.fileName + ".download")

    @Test
    fun `test that monitorModelState emits NotDownloaded before a download`() = runTest {
        repository().monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.NotDownloaded(spec.sizeBytes))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that startDownload downloads the model into a partial file`() = runTest {
        repository().startDownload()

        assertThat(downloader.enqueued).containsExactly(spec.url to partialFile)
        assertThat(store.downloadId).isEqualTo(FakeModelDownloader.ID)
    }

    @Test
    fun `test that monitorModelState emits the download's progress`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        downloader.status = DownloadStatus.Running(downloadedBytes = 4, totalBytes = 12)

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Downloading(4, 12))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorModelState uses the expected size when the total is unknown`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        downloader.status = DownloadStatus.Running(downloadedBytes = 0, totalBytes = -1)

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Downloading(0, spec.sizeBytes))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that startDownload reports NotEnoughStorage when storage is short`() = runTest {
        freeBytes = requiredBytes - 1
        val underTest = repository()

        underTest.startDownload()

        assertThat(downloader.enqueued).isEmpty()
        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.NotEnoughStorage(requiredBytes))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorModelState emits NO_CONNECTION while the download waits for the network`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        downloader.status = DownloadStatus.WaitingForNetwork(downloadedBytes = 4, totalBytes = 12)

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Failed(ModelFailure.NO_CONNECTION))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorModelState emits DOWNLOAD_FAILED and removes the download when it fails`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        downloader.status = DownloadStatus.Failed(insufficientSpace = false)

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Failed(ModelFailure.DOWNLOAD_FAILED))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(downloader.removed).containsExactly(FakeModelDownloader.ID)
        assertThat(store.downloadId).isNull()
    }

    @Test
    fun `test that monitorModelState emits NotEnoughStorage when the download runs out of space`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        downloader.status = DownloadStatus.Failed(insufficientSpace = true)

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.NotEnoughStorage(requiredBytes))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorModelState verifies a finished download and emits Loading`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        partialFile.writeBytes(content)
        downloader.status = DownloadStatus.Successful

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Verifying)
            assertThat(awaitItem()).isEqualTo(ModelState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(modelFile.readBytes()).isEqualTo(content)
        assertThat(partialFile.exists()).isFalse()
        assertThat(underTest.modelPath()).isEqualTo(modelFile.absolutePath)
    }

    @Test
    fun `test that monitorModelState emits DOWNLOAD_FAILED when the verified file can't be renamed`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        partialFile.writeBytes(content)
        // A non-empty directory where the model file goes makes the rename fail.
        File(modelFile, "blocker").apply { parentFile.mkdirs() }.writeText("x")
        downloader.status = DownloadStatus.Successful

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Verifying)
            assertThat(awaitItem()).isEqualTo(ModelState.Failed(ModelFailure.DOWNLOAD_FAILED))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(partialFile.exists()).isFalse()
        assertThat(underTest.modelPath()).isNull()
    }

    @Test
    fun `test that monitorModelState deletes a corrupt download and emits CORRUPT`() = runTest {
        val underTest = repository()
        underTest.startDownload()
        partialFile.writeBytes("a tiny modeL".toByteArray())
        downloader.status = DownloadStatus.Successful

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Verifying)
            assertThat(awaitItem()).isEqualTo(ModelState.Failed(ModelFailure.CORRUPT))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(partialFile.exists()).isFalse()
        assertThat(modelFile.exists()).isFalse()
        assertThat(underTest.modelPath()).isNull()
    }

    @Test
    fun `test that monitorModelState emits Ready after markLoadVerified`() = runTest {
        modelFile.writeBytes(content)
        val underTest = repository()

        underTest.markLoadVerified()

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Ready)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorModelState emits Ready on a later launch without verifying again`() = runTest {
        modelFile.writeBytes(content)
        store.loadVerified = true

        repository().monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Ready)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(downloader.enqueued).isEmpty()
    }

    @Test
    fun `test that monitorModelState keeps following a download started before the app restarted`() = runTest {
        store.downloadId = FakeModelDownloader.ID
        downloader.status = DownloadStatus.Running(downloadedBytes = 8, totalBytes = 12)

        repository().monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Downloading(8, 12))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that startDownload does not start a second download while one is running`() = runTest {
        store.downloadId = FakeModelDownloader.ID
        downloader.status = DownloadStatus.Running(downloadedBytes = 8, totalBytes = 12)

        repository().startDownload()

        assertThat(downloader.enqueued).isEmpty()
    }

    @Test
    fun `test that startDownload starts again after a failure`() = runTest {
        store.failure = SetupFailure.DOWNLOAD_FAILED
        val underTest = repository()

        underTest.startDownload()

        assertThat(downloader.enqueued).hasSize(1)
        assertThat(store.failure).isNull()
    }

    @Test
    fun `test that monitorModelState emits NotDownloaded when the download disappeared`() = runTest {
        store.downloadId = FakeModelDownloader.ID
        downloader.status = null

        repository().monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.NotDownloaded(spec.sizeBytes))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(store.downloadId).isNull()
    }

    @Test
    fun `test that startDownload reports DOWNLOAD_FAILED when the download can't start`() = runTest {
        downloader.enqueueError = SecurityException("not allowed")
        val underTest = repository()

        underTest.startDownload()

        underTest.monitorModelState().test {
            assertThat(awaitItem()).isEqualTo(ModelState.Failed(ModelFailure.DOWNLOAD_FAILED))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that modelPath is null before the model is downloaded`() = runTest {
        assertThat(repository().modelPath()).isNull()
    }

    private class FakeModelDownloader : ModelDownloader {
        val enqueued = mutableListOf<Pair<String, File>>()
        val removed = mutableListOf<Long>()
        var status: DownloadStatus? = DownloadStatus.Running(0, -1)
        var enqueueError: RuntimeException? = null

        override fun enqueue(url: String, destination: File): Long {
            enqueueError?.let { throw it }
            enqueued += url to destination
            return ID
        }

        override fun query(id: Long): DownloadStatus? = status.takeIf { id == ID && id !in removed }

        override fun remove(id: Long) {
            removed += id
        }

        companion object {
            const val ID = 42L
        }
    }

    private class FakeModelSetupStore : ModelSetupStore {
        override var downloadId: Long? = null
        override var loadVerified: Boolean = false
        override var failure: SetupFailure? = null
    }
}
