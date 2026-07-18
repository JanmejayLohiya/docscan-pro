package com.docscan.pro.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.FileOutputStream

/**
 * Paints [strokes] onto the image at [path] in paper-white ("mask + fill", the
 * Phase-2 erase). Each stroke is a flat [x0,y0,x1,y1,...] array in *display*
 * coordinates; we scale it to the bitmap's pixel space before drawing. FR-E.5
 */
fun eraseImage(
    srcPath: String,
    dstPath: String,
    strokes: List<FloatArray>,
    displayW: Float,
    displayH: Float,
    brushDisplayPx: Float,
) {
    if (strokes.isEmpty() || displayW <= 0f || displayH <= 0f) return
    val decoded = BitmapFactory.decodeFile(srcPath) ?: return
    val bmp = if (decoded.isMutable) decoded else decoded.copy(Bitmap.Config.ARGB_8888, true)
    if (bmp !== decoded) decoded.recycle()

    val scaleX = bmp.width / displayW
    val scaleY = bmp.height / displayH
    val canvas = Canvas(bmp)
    val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
        strokeWidth = brushDisplayPx * scaleX
    }
    val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }

    strokes.forEach { pts ->
        if (pts.size < 2) return@forEach
        if (pts.size == 2) {
            canvas.drawCircle(pts[0] * scaleX, pts[1] * scaleY, paint.strokeWidth / 2f, dotPaint)
        } else {
            val p = Path().apply {
                moveTo(pts[0] * scaleX, pts[1] * scaleY)
                var i = 2
                while (i + 1 < pts.size) {
                    lineTo(pts[i] * scaleX, pts[i + 1] * scaleY)
                    i += 2
                }
            }
            canvas.drawPath(p, paint)
        }
    }

    FileOutputStream(dstPath).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    bmp.recycle()
}
