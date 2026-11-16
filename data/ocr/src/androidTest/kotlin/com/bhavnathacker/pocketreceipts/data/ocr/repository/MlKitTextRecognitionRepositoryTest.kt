package com.bhavnathacker.pocketreceipts.data.ocr.repository

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhavnathacker.pocketreceipts.data.ocr.OcrRowAssembler
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs the real bundled ML Kit model on a receipt row drawn onto a bitmap.
 */
@RunWith(AndroidJUnit4::class)
class MlKitTextRecognitionRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var image: File
    private lateinit var underTest: MlKitTextRecognitionRepository

    @Before
    fun setUp() {
        underTest = MlKitTextRecognitionRepository(context, Dispatchers.IO, OcrRowAssembler())
        image = File(context.cacheDir, "ocr-test-receipt.jpg")
        val bitmap = Bitmap.createBitmap(800, 300, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 44f
                isAntiAlias = true
            }
            drawText("TOTAL", 60f, 170f, paint)
            drawText("210.00", 580f, 170f, paint)
        }
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    }

    @After
    fun tearDown() {
        image.delete()
    }

    @Test
    fun test_that_recognize_joins_a_label_and_its_amount_on_one_row() = runTest {
        val text = underTest.recognize(Uri.fromFile(image).toString())

        assertThat(text.lines).contains("TOTAL 210.00")
    }
}
