package com.ste03.smartscan.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ste03.smartscan.detect.DocumentDetector
import com.ste03.smartscan.enhance.Filter
import com.ste03.smartscan.enhance.ImageEnhancer
import com.ste03.smartscan.vm.AppViewModel
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Point

/**
 * Crop/adjust screen: shows the image with a draggable four-corner quad overlay.
 * The user drags corners to select the document area; "Apply" perspective-corrects it.
 */
@Composable
fun CropScreen(viewModel: AppViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary
    val source by viewModel.cropSource.collectAsStateWithLifecycle()

    val bitmap = remember(source) {
        source?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
    }

    var corners by remember(bitmap) { mutableStateOf<List<Offset>?>(null) }
    var applying by remember { mutableStateOf(false) }
    var failed by remember(bitmap) { mutableStateOf(false) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }

    // Two-stage flow: crop corners, then pick an enhancement filter.
    var phase by remember { mutableStateOf("crop") }
    var cropped by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(Filter.COLOR) }
    var filtered by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bitmap) {
        corners = null
        failed = bitmap == null
        if (bitmap != null) {
            corners = withContext(Dispatchers.Default) {
                val ok = DocumentDetector.init()
                val pts = if (ok) DocumentDetector.detectCorners(bitmap) else null
                if (pts != null) pts.map { Offset(it.x.toFloat(), it.y.toFloat()) }
                else defaultCorners(bitmap.width, bitmap.height)
            }
        }
    }

    LaunchedEffect(cropped, selectedFilter) {
        val c = cropped
        if (c != null) {
            filtered = withContext(Dispatchers.Default) {
                ImageEnhancer.apply(c, selectedFilter)
            }
        }
    }

    fun applyCrop() {
        val b = bitmap ?: return
        val c = corners ?: return
        if (applying) return
        applying = true
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    DocumentDetector.warp(b, c.map { Point(it.x.toDouble(), it.y.toDouble()) })
                }.getOrElse { b }
            }
            cropped = result
            phase = "enhance"
            applying = false
        }
    }

    fun save() {
        val result = filtered ?: cropped ?: return
        viewModel.finishCrop(result)
        onDone()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val display = filtered ?: cropped
        if (phase == "enhance" && display != null) {
            Image(
                bitmap = display.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { phase = "crop" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    "Enhance",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { save() }) {
                    Icon(Icons.Filled.Check, "Save", tint = accent)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Filter.values().forEach { f ->
                    val selected = f == selectedFilter
                    Text(
                        f.label,
                        color = if (selected) accent else Color.White.copy(alpha = 0.85f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clickable { selectedFilter = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        } else if (bitmap != null && corners != null) {
            val density = LocalDensity.current
            val handleSizePx = with(density) { 30.dp.toPx() }.roundToInt()

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val cw = constraints.maxWidth.toFloat()
                val ch = constraints.maxHeight.toFloat()
                val bw = bitmap.width.toFloat()
                val bh = bitmap.height.toFloat()
                val scale = min(cw / bw, ch / bh)
                val offX = (cw - bw * scale) / 2f
                val offY = (ch - bh * scale) / 2f

                fun toScreen(p: Offset) = Offset(offX + p.x * scale, offY + p.y * scale)
                val imageBitmap = bitmap.asImageBitmap()

                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                val screenCorners = corners!!.map { toScreen(it) }

                Canvas(Modifier.fillMaxSize()) {
                    val quad = Path().apply {
                        moveTo(screenCorners[0].x, screenCorners[0].y)
                        lineTo(screenCorners[1].x, screenCorners[1].y)
                        lineTo(screenCorners[2].x, screenCorners[2].y)
                        lineTo(screenCorners[3].x, screenCorners[3].y)
                        close()
                    }
                    val mask = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addPath(quad)
                    }
                    drawPath(mask, Color.Black.copy(alpha = 0.45f))
                    drawPath(quad, accent, style = Stroke(width = 3f))
                }

                corners!!.forEachIndexed { i, p ->
                    val sp = toScreen(p)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (sp.x - handleSizePx / 2f).roundToInt(),
                                    (sp.y - handleSizePx / 2f).roundToInt()
                                )
                            }
                            .size(30.dp)
                            .background(accent, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .pointerInput(i) {
                                detectDragGestures(
                                    onDragStart = { draggingIndex = i },
                                    onDragEnd = { draggingIndex = null },
                                    onDragCancel = { draggingIndex = null },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val imgDelta = Offset(drag.x / scale, drag.y / scale)
                                        val current = corners!![i]
                                        val next = Offset(
                                            (current.x + imgDelta.x).coerceIn(0f, bw),
                                            (current.y + imgDelta.y).coerceIn(0f, bh)
                                        )
                                        corners = corners!!.toMutableList().also { it[i] = next }
                                    }
                                )
                            }
                    )
                }

                // Magnifier loupe while dragging a corner
                val dragging = draggingIndex
                if (dragging != null) {
                    val focus = corners!!.getOrNull(dragging)?.let { toScreen(it) }
                    if (focus != null) {
                        val zoom = 2.5f
                        val radius = 52.dp
                        val radiusPx = with(density) { radius.toPx() }
                        val gapPx = with(density) { 28.dp.toPx() }
                        val diamPx = radiusPx * 2f

                        val lcx = focus.x.coerceIn(radiusPx, cw - radiusPx)
                        var lcy = focus.y - radiusPx - gapPx
                        if (lcy < radiusPx) lcy = focus.y + radiusPx + gapPx

                        val imgFocusX = (focus.x - offX) / scale
                        val imgFocusY = (focus.y - offY) / scale
                        val magScale = scale * zoom
                        val translateX = radiusPx - imgFocusX * magScale
                        val translateY = radiusPx - imgFocusY * magScale

                        Canvas(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (lcx - radiusPx).roundToInt(),
                                        (lcy - radiusPx).roundToInt()
                                    )
                                }
                                .size(radius * 2)
                        ) {
                            clipPath(
                                Path().apply { addOval(Rect(0f, 0f, diamPx, diamPx)) }
                            ) {
                                drawImage(
                                    image = imageBitmap,
                                    dstOffset = IntOffset(
                                        translateX.roundToInt(),
                                        translateY.roundToInt()
                                    ),
                                    dstSize = IntSize(
                                        (bitmap.width * magScale).roundToInt(),
                                        (bitmap.height * magScale).roundToInt()
                                    ),
                                    filterQuality = FilterQuality.Low
                                )
                            }
                            drawCircle(
                                color = Color.White,
                                radius = radiusPx,
                                center = Offset(radiusPx, radiusPx),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = accent,
                                radius = 3f,
                                center = Offset(radiusPx, radiusPx)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, "Cancel", tint = Color.White)
                }
                Text(
                    "Adjust corners",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                if (applying) {
                    CircularProgressIndicator(
                        color = accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                } else {
                    IconButton(onClick = { applyCrop() }) {
                        Icon(Icons.Filled.Check, "Apply", tint = accent)
                    }
                }
            }
        } else if (failed) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Could not load image", color = Color.White)
                TextButton(onClick = onCancel) { Text("Back", color = accent) }
            }
        }
    }
}

private fun defaultCorners(w: Int, h: Int): List<Offset> {
    val m = 2f
    return listOf(
        Offset(m, m),
        Offset(w - m, m),
        Offset(w - m, h - m),
        Offset(m, h - m)
    )
}
