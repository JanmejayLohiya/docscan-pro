package com.docscan.pro.feature.editor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Full-screen erase tool: drag to paint over unwanted areas (watermarks, marks);
 * on Apply the strokes are flattened onto the page image in paper-white. FR-E.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EraseOverlay(
    imagePath: String,
    onCancel: () -> Unit,
    onApply: (strokes: List<FloatArray>, displayW: Float, displayH: Float, brushPx: Float) -> Unit,
) {
    val committed = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val brushPx = with(LocalDensity.current) { 26.dp.toPx() }

    // Match the drawing box to the image aspect so display↔pixel scaling is uniform.
    val aspect = remember(imagePath) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, opts)
        if (opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight else 1f
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Erase") },
            navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
            actions = {
                TextButton(onClick = { if (committed.isNotEmpty()) committed.removeAt(committed.lastIndex) }) {
                    Text("Undo")
                }
                TextButton(onClick = {
                    onApply(
                        committed.map { it.toFloatArray() },
                        canvasSize.width.toFloat(),
                        canvasSize.height.toFloat(),
                        brushPx,
                    )
                }) { Text("Apply") }
            },
        )
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().aspectRatio(aspect)) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Page",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> current = listOf(offset) },
                                onDrag = { change, _ ->
                                    current = current + change.position
                                    change.consume()
                                },
                                onDragEnd = { committed.add(current); current = emptyList() },
                                onDragCancel = { committed.add(current); current = emptyList() },
                            )
                        },
                ) {
                    committed.forEach { drawStroke(it, brushPx) }
                    drawStroke(current, brushPx)
                }
            }
        }
    }
}

private fun DrawScope.drawStroke(points: List<Offset>, width: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(Color.White, radius = width / 2f, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, Color.White, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun List<Offset>.toFloatArray(): FloatArray {
    val out = FloatArray(size * 2)
    forEachIndexed { i, o -> out[i * 2] = o.x; out[i * 2 + 1] = o.y }
    return out
}
