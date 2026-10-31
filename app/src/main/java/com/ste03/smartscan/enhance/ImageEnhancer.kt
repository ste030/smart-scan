package com.ste03.smartscan.enhance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.ste03.smartscan.detect.DocumentDetector
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/** Post-crop enhancement filters, mirroring a document scanner's colour options. */
enum class Filter(val label: String) {
    COLOR("Color"),
    ENHANCE("Enhance"),
    GRAYSCALE("Grayscale"),
    BW("B&W")
}

object ImageEnhancer {

    fun apply(input: Bitmap, filter: Filter): Bitmap = when (filter) {
        Filter.COLOR -> input
        Filter.ENHANCE -> colorMatrix(input, enhanceMatrix())
        Filter.GRAYSCALE -> colorMatrix(input, ColorMatrix().apply { setSaturation(0f) })
        Filter.BW -> blackWhite(input)
    }

    private fun colorMatrix(input: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(input, 0f, 0f, paint)
        return out
    }

    private fun enhanceMatrix(): ColorMatrix {
        val saturation = 1.25f
        val contrast = 1.15f
        val cm = ColorMatrix().apply { setSaturation(saturation) }
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        cm.postConcat(
            ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        return cm
    }

    /** Clean black & white via OpenCV adaptive threshold (handles uneven lighting). */
    private fun blackWhite(input: Bitmap): Bitmap {
        if (!DocumentDetector.init()) return input
        val src = Mat()
        Utils.bitmapToMat(input, src)
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        val bw = Mat()
        Imgproc.adaptiveThreshold(
            gray, bw, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
            31, 15.0
        )
        val rgba = Mat()
        Imgproc.cvtColor(bw, rgba, Imgproc.COLOR_GRAY2RGBA)
        val out = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, out)
        src.release(); gray.release(); bw.release(); rgba.release()
        return out
    }
}
