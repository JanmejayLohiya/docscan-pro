package com.docscan.pro.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/** Longest edge (px) we render page images at, to bound memory during PDF build. */
private const val MAX_EDGE = 2200

/** Decodes an image file, downsampling large images to keep memory in check. */
fun decodeSampled(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val longEdge = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    var sample = 1
    while (longEdge / sample > MAX_EDGE) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(path, opts)
}

/** Builds a multi-page PDF from the given ordered image paths. One image per page. */
fun buildPdf(imagePaths: List<String>, out: File) {
    val pdf = PdfDocument()
    try {
        imagePaths.forEachIndexed { index, path ->
            val bmp = decodeSampled(path) ?: return@forEachIndexed
            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
            val page = pdf.startPage(info)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            pdf.finishPage(page)
            bmp.recycle()
        }
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { pdf.writeTo(it) }
    } finally {
        pdf.close()
    }
}

/** Scales the image at [path] down so its longest edge is at most [maxEdge], in place. */
fun scaleImageFile(path: String, maxEdge: Int) {
    val src = BitmapFactory.decodeFile(path) ?: return
    val longEdge = maxOf(src.width, src.height)
    if (longEdge <= maxEdge) {
        src.recycle()
        return
    }
    val ratio = maxEdge.toFloat() / longEdge
    val scaled = Bitmap.createScaledBitmap(src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true)
    FileOutputStream(path).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    if (scaled != src) src.recycle()
    scaled.recycle()
}

/** Rotates the image at [path] in place by [degrees] and re-saves it as JPEG. */
fun rotateImageFile(path: String, degrees: Int) {
    val src = BitmapFactory.decodeFile(path) ?: return
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    FileOutputStream(path).use { rotated.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    if (rotated != src) src.recycle()
    rotated.recycle()
}
