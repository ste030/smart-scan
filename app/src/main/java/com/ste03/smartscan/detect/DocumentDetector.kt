package com.ste03.smartscan.detect

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max

/**
 * Detects the document quad in a photo and returns a perspective-corrected crop.
 * Uses OpenCV: grayscale -> GaussianBlur -> Canny -> contours -> 4-point approx -> warp.
 */
object DocumentDetector {

    @Volatile
    private var initialized = false

    fun init(): Boolean {
        if (initialized) return true
        synchronized(this) {
            if (initialized) return true
            initialized = OpenCVLoader.initLocal()
        }
        return initialized
    }

    /**
     * Detect the document edges and return the corrected bitmap.
     * Returns null when no clear 4-corner document is found (caller falls back to full frame).
     */
    fun cropToDocument(input: Bitmap): Bitmap? {
        if (!init()) return null
        val corners = detectCorners(input) ?: return null
        return warp(input, corners)
    }

    /** Return the four document corners (TL, TR, BR, BL) in source-pixel coordinates, or null. */
    fun detectCorners(input: Bitmap): List<Point>? {
        if (!init()) return null
        val src = Mat()
        Utils.bitmapToMat(input, src)

        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges, contours, hierarchy,
            Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE
        )

        contours.sortByDescending { Imgproc.contourArea(it) }

        var result: List<Point>? = null
        val totalArea = (src.width() * src.height()).toDouble()
        for (contour in contours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
            if (approx.total() == 4L && Imgproc.contourArea(approx) > totalArea * 0.05) {
                result = expandCorners(sortCorners(approx.toList()), src.width(), src.height(), 0.05)
                break
            }
        }

        src.release(); gray.release(); edges.release(); hierarchy.release()
        return result
    }

    fun warp(input: Bitmap, corners: List<Point>): Bitmap {
        if (!init()) return input
        val (tl, tr, br, bl) = sortCorners(corners)
        val maxWidth = max(distance(tl, tr), distance(bl, br)).toInt().coerceAtLeast(1)
        val maxHeight = max(distance(tl, bl), distance(tr, br)).toInt().coerceAtLeast(1)

        val srcMat = MatOfPoint2f(tl, tr, br, bl)
        val dstMat = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )
        val transform = Imgproc.getPerspectiveTransform(srcMat, dstMat)

        val src = Mat()
        Utils.bitmapToMat(input, src)
        val dst = Mat()
        Imgproc.warpPerspective(
            src, dst, transform, Size(maxWidth.toDouble(), maxHeight.toDouble())
        )

        val out = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, out)
        src.release(); dst.release(); transform.release()
        return out
    }

    private fun sortCorners(corners: List<Point>): List<Point> {
        val tl = corners.minByOrNull { it.x + it.y }!!
        val br = corners.maxByOrNull { it.x + it.y }!!
        val tr = corners.maxByOrNull { it.x - it.y }!!
        val bl = corners.minByOrNull { it.x - it.y }!!
        return listOf(tl, tr, br, bl)
    }

    /**
     * Push each corner slightly away from the quad's centroid, clamped to the image bounds.
     * Edge detection tends to hug just inside the paper edge; expanding outward prevents
     * the crop from cutting off the document border.
     */
    private fun expandCorners(corners: List<Point>, w: Int, h: Int, fraction: Double): List<Point> {
        val cx = corners.map { it.x }.average()
        val cy = corners.map { it.y }.average()
        val bw = corners.maxOf { it.x } - corners.minOf { it.x }
        val bh = corners.maxOf { it.y } - corners.minOf { it.y }
        val margin = fraction * max(bw, bh)
        return corners.map { p ->
            val dx = p.x - cx
            val dy = p.y - cy
            val len = hypot(dx, dy).coerceAtLeast(1.0)
            Point(
                (p.x + (dx / len) * margin).coerceIn(0.0, (w - 1).toDouble()),
                (p.y + (dy / len) * margin).coerceIn(0.0, (h - 1).toDouble())
            )
        }
    }

    private fun distance(a: Point, b: Point): Double = hypot(a.x - b.x, a.y - b.y)
}
