package com.docscan.pro.feature.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.DocumentRepository
import com.docscan.pro.util.PageOcr
import com.docscan.pro.util.recognizeGeometry
import com.docscan.pro.util.translateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** A translated text block, positioned in the page's pixel coordinate space. */
data class TranslatedBlock(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

data class PdfViewerUiState(
    val title: String = "",
    val filePath: String = "",
    val ocrText: String = "",
    val pages: List<Bitmap> = emptyList(),
    val pageOcr: List<PageOcr> = emptyList(),
    val loading: Boolean = true,
    val translating: Boolean = false,
    val translatedPages: List<List<TranslatedBlock>>? = null,
    val translatedLang: String? = null,
    val translateError: String? = null,
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    repository: DocumentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val documentId: String = checkNotNull(savedStateHandle["documentId"])
    private val _state = MutableStateFlow(PdfViewerUiState())
    val state: StateFlow<PdfViewerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val doc = repository.observeDocument(documentId).filterNotNull().first()
            _state.update { it.copy(title = doc.name, filePath = doc.filePath, ocrText = doc.ocrText.orEmpty()) }
            val pages = withContext(Dispatchers.IO) { renderPdf(doc.filePath) }
            _state.update { it.copy(loading = false, pages = pages) }
            // OCR geometry in the background — powers in-page highlight + translation overlay.
            val ocr = withContext(Dispatchers.IO) { pages.map { recognizeGeometry(it) } }
            _state.update { it.copy(pageOcr = ocr) }
        }
    }

    /** Translates every text block on every page into [targetTag], keeping positions. */
    fun translate(targetTag: String, targetLabel: String) {
        if (_state.value.translating) return
        val ocr = _state.value.pageOcr
        if (ocr.isEmpty() || ocr.none { it.blocks.isNotEmpty() }) {
            _state.update { it.copy(translateError = "No recognized text to translate yet — try again in a moment.") }
            return
        }
        _state.update {
            it.copy(translating = true, translateError = null, translatedPages = null, translatedLang = targetLabel)
        }
        viewModelScope.launch {
            runCatching {
                val flat = ocr.flatMap { page -> page.blocks }
                val translated = translateAll(flat.map { it.text }, targetTag)
                var i = 0
                ocr.map { page ->
                    page.blocks.map { b ->
                        TranslatedBlock(translated[i++], b.left, b.top, b.right, b.bottom)
                    }
                }
            }.fold(
                onSuccess = { pages -> _state.update { it.copy(translating = false, translatedPages = pages) } },
                onFailure = { e ->
                    _state.update { it.copy(translating = false, translateError = "Translation failed: ${e.message ?: "try again"}") }
                },
            )
        }
    }

    fun clearTranslation() = _state.update {
        it.copy(translatedPages = null, translatedLang = null, translateError = null)
    }

    private fun renderPdf(path: String): List<Bitmap> {
        val file = File(path)
        if (!file.exists()) return emptyList()
        val out = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 2
                val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                Canvas(bmp).drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                out.add(bmp)
            }
            renderer.close()
        }
        return out
    }
}
