package com.docscan.pro.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device OCR (ML Kit Text Recognition, Latin). Returns the recognized text, or
 * an empty string on failure — OCR errors must never break saving a scan. FR-3.8
 */
suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { result -> cont.resume(result.text) }
        .addOnFailureListener { cont.resume("") }
        .addOnCompleteListener { recognizer.close() }
}

/** A recognized word/block with its pixel bounding box in the source bitmap. */
data class OcrBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Word- and block-level OCR geometry for a page, in the coordinate space of [width]x[height]. */
data class PageOcr(
    val width: Int,
    val height: Int,
    val words: List<OcrBox> = emptyList(),
    val blocks: List<OcrBox> = emptyList(),
)

/**
 * OCR that keeps geometry: word boxes (for search highlighting) and block boxes
 * (for the in-layout translation overlay). Coordinates are in [bitmap] pixels.
 */
suspend fun recognizeGeometry(bitmap: Bitmap): PageOcr = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { result ->
            val words = mutableListOf<OcrBox>()
            val blocks = mutableListOf<OcrBox>()
            for (block in result.textBlocks) {
                block.boundingBox?.let { b -> blocks.add(OcrBox(block.text, b.left, b.top, b.right, b.bottom)) }
                for (line in block.lines) {
                    for (el in line.elements) {
                        el.boundingBox?.let { b -> words.add(OcrBox(el.text, b.left, b.top, b.right, b.bottom)) }
                    }
                }
            }
            cont.resume(PageOcr(bitmap.width, bitmap.height, words, blocks))
        }
        .addOnFailureListener { cont.resume(PageOcr(bitmap.width, bitmap.height)) }
        .addOnCompleteListener { recognizer.close() }
}
