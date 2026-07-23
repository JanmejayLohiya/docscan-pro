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
