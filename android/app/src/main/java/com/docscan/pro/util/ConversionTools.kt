package com.docscan.pro.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * On-device file conversions used by the Tools hub. All outputs are written to
 * cacheDir/exports and returned as Files; the caller shares them via [shareFiles].
 */
private fun exportsDir(context: Context): File =
    File(context.cacheDir, "exports").apply { mkdirs() }

private fun stamp(): Long = System.currentTimeMillis()

private fun decode(context: Context, uri: Uri): Bitmap? =
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

/** Combines the selected images into a single multi-page PDF. */
suspend fun imagesToPdf(context: Context, uris: List<Uri>): File = withContext(Dispatchers.IO) {
    val out = File(exportsDir(context), "images_${stamp()}.pdf")
    val pdf = PdfDocument()
    try {
        uris.forEachIndexed { index, uri ->
            val bmp = decode(context, uri) ?: return@forEachIndexed
            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
            val page = pdf.startPage(info)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            pdf.finishPage(page)
            bmp.recycle()
        }
        FileOutputStream(out).use { pdf.writeTo(it) }
    } finally {
        pdf.close()
    }
    out
}

/** Converts an image to the opposite format (PNG <-> JPEG). Returns the new file. */
suspend fun convertImageFormat(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val isPng = (context.contentResolver.getType(uri) ?: "").contains("png", ignoreCase = true)
    val toPng = !isPng
    val bmp = decode(context, uri) ?: error("Cannot read image")
    val ext = if (toPng) "png" else "jpg"
    val out = File(exportsDir(context), "image_${stamp()}.$ext")
    FileOutputStream(out).use {
        bmp.compress(if (toPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, if (toPng) 100 else 92, it)
    }
    bmp.recycle()
    out
}

/** Renders each page of a PDF to a PNG image. */
suspend fun pdfToImages(context: Context, uri: Uri): List<File> = withContext(Dispatchers.IO) {
    val files = mutableListOf<File>()
    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        val renderer = PdfRenderer(pfd)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val scale = 2
            val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            val f = File(exportsDir(context), "page_${i + 1}_${stamp()}.png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            files.add(f)
        }
        renderer.close()
    }
    files
}

/** Extracts a representative frame from a video as a PNG image. */
suspend fun videoToImage(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val frame = retriever.getFrameAtTime(0) ?: error("Cannot read video frame")
        val out = File(exportsDir(context), "frame_${stamp()}.png")
        FileOutputStream(out).use { frame.compress(Bitmap.CompressFormat.PNG, 100, it) }
        frame.recycle()
        out
    } finally {
        retriever.release()
    }
}

/** Opens the system share sheet for one or more converted files. */
fun shareFiles(context: Context, files: List<File>, mimeType: String) {
    if (files.isEmpty()) return
    val authority = "${context.packageName}.fileprovider"
    val uris = ArrayList(files.map { FileProvider.getUriForFile(context, authority, it) })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uris[0]) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = mimeType; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
