package com.bhavnathacker.pocketreceipts.data.ocr.repository

import android.content.Context
import androidx.core.net.toUri
import com.bhavnathacker.pocketreceipts.data.ocr.OcrLine
import com.bhavnathacker.pocketreceipts.data.ocr.OcrRowAssembler
import com.bhavnathacker.pocketreceipts.domain.dispatcher.IoDispatcher
import com.bhavnathacker.pocketreceipts.domain.scan.RecognizedText
import com.bhavnathacker.pocketreceipts.domain.scan.TextRecognitionRepository
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads receipt text with ML Kit's bundled Latin text recognizer, on the device.
 * Decoding the image and recognition run on the I/O dispatcher; a recognizer is
 * created per call and always closed.
 */
internal class MlKitTextRecognitionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val ocrRowAssembler: OcrRowAssembler,
) : TextRecognitionRepository {

    override suspend fun recognize(imageUri: String): RecognizedText = withContext(ioDispatcher) {
        val image = InputImage.fromFilePath(context, imageUri.toUri())
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = recognizer.process(image).await()
            ocrRowAssembler(text.ocrLines())
        } finally {
            recognizer.close()
        }
    }

    private fun Text.ocrLines(): List<OcrLine> =
        textBlocks.flatMap { it.lines }.mapNotNull { line ->
            line.boundingBox?.let { box -> OcrLine(line.text, box.top, box.bottom, box.left) }
        }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}
