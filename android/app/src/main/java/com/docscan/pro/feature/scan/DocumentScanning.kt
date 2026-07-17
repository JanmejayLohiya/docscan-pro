package com.docscan.pro.feature.scan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/** Result of a scan session: the generated PDF (if any) and per-page images. */
data class ScannedPages(
    val pdfUri: Uri?,
    val pageUris: List<Uri>,
)

private fun scannerOptions(): GmsDocumentScannerOptions =
    GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)              // FR-3.6 / FR-3.9 (import photos)
        .setScannerMode(SCANNER_MODE_FULL)          // edge detect, filters, reorder, retake
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .build()

/**
 * Returns a lambda that launches the ML Kit document scanner UI. On success the
 * captured pages + generated PDF are delivered to [onScanned]. Multi-page and
 * enhancement are handled inside the ML Kit component (FR-3.1..3.4).
 */
@Composable
fun rememberScanLauncher(onScanned: (ScannedPages) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scan != null) {
                onScanned(
                    ScannedPages(
                        pdfUri = scan.pdf?.uri,
                        pageUris = scan.pages?.mapNotNull { it.imageUri } ?: emptyList(),
                    ),
                )
            }
        }
    }
    return remember(context, launcher) {
        {
            val activity = context.findActivity()
            GmsDocumentScanning.getClient(scannerOptions())
                .getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
        }
    }
}

private fun Context.findActivity(): Activity {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found from context")
}
