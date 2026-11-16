package com.bhavnathacker.pocketreceipts.data.ocr.repository

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileScanImageRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val underTest = FileScanImageRepository(context, Dispatchers.IO)

    private val scanInCache = File(context.cacheDir, "mlkit_docscan_ui_client/test-scan.jpg")
    private val fileOutsideCache = File(context.filesDir, "test-scan.jpg")

    @After
    fun tearDown() {
        scanInCache.delete()
        fileOutsideCache.delete()
    }

    @Test
    fun test_that_delete_removes_a_scan_in_the_cache_directory() = runTest {
        scanInCache.parentFile?.mkdirs()
        scanInCache.writeText("scan")

        underTest.delete(Uri.fromFile(scanInCache).toString())

        assertThat(scanInCache.exists()).isFalse()
    }

    @Test
    fun test_that_delete_keeps_a_file_outside_the_cache_directory() = runTest {
        fileOutsideCache.writeText("not a scan")

        underTest.delete(Uri.fromFile(fileOutsideCache).toString())

        assertThat(fileOutsideCache.exists()).isTrue()
    }

    @Test
    fun test_that_delete_keeps_a_file_reached_through_a_path_outside_the_cache() = runTest {
        fileOutsideCache.writeText("not a scan")
        val escapingUri = "file://${context.cacheDir.path}/../files/${fileOutsideCache.name}"

        underTest.delete(escapingUri)

        assertThat(fileOutsideCache.exists()).isTrue()
    }

    @Test
    fun test_that_delete_ignores_a_content_uri() = runTest {
        underTest.delete("content://media/external/images/media/1")
    }

    @Test
    fun test_that_delete_ignores_a_scan_that_is_already_gone() = runTest {
        underTest.delete(Uri.fromFile(scanInCache).toString())

        assertThat(scanInCache.exists()).isFalse()
    }
}
