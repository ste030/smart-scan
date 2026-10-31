package com.ste03.smartscan.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** On-device text recognition (Latin) for OCR + smart document naming. */
object OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(file: File): String = suspendCancellableCoroutine { cont ->
        val bitmap = try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
        if (bitmap == null) {
            cont.resume("")
            return@suspendCancellableCoroutine
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                cont.resume(result.text ?: "")
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
    }

    fun close() {
        recognizer.close()
    }
}
