package com.ste03.smartscan.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** On-device barcode scanning (QR, Code 39, Code 128, EAN, DataMatrix, etc.). */
object BarcodeScanner {

    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    suspend fun recognize(file: File): List<String> = suspendCancellableCoroutine { cont ->
        val bitmap = try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
        if (bitmap == null) {
            cont.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                cont.resume(barcodes.mapNotNull { it.rawValue })
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
    }
}
