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

data class PdfViewerUiState(
    val title: String = "",
    val filePath: String = "",
    val ocrText: String = "",
    val pages: List<Bitmap> = emptyList(),
    val loading: Boolean = true,
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
        }
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
