package com.docscan.pro.feature.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.DocumentRepository
import com.docscan.pro.domain.Document
import com.docscan.pro.domain.Page
import com.docscan.pro.feature.scan.ScannedPages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: DocumentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val documentId: String = checkNotNull(savedStateHandle["documentId"])

    // Session-scoped undo/redo of page snapshots. Image files are versioned
    // (never overwritten), so restoring an old snapshot's paths just works.
    private val undoStack = ArrayDeque<List<Page>>()
    private val redoStack = ArrayDeque<List<Page>>()
    private val history = MutableStateFlow(false to false)

    val state: StateFlow<EditorUiState> =
        combine(
            repository.observeDocument(documentId),
            repository.observePages(documentId),
            history,
        ) { document, pages, h -> EditorUiState(document, pages, h.first, h.second) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    private fun snapshot() {
        undoStack.addLast(state.value.pages)
        redoStack.clear()
        emitHistory()
    }

    private fun emitHistory() {
        history.value = undoStack.isNotEmpty() to redoStack.isNotEmpty()
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val ids = state.value.pages.map { it.id }.toMutableList()
        if (fromIndex !in ids.indices || toIndex !in ids.indices) return
        snapshot()
        ids.add(toIndex, ids.removeAt(fromIndex))
        viewModelScope.launch { repository.reorderPages(documentId, ids) }
    }

    fun remove(pageId: String) {
        snapshot()
        viewModelScope.launch { repository.removePage(documentId, pageId) }
    }

    fun rotate(pageId: String) {
        snapshot()
        viewModelScope.launch { repository.rotatePage(documentId, pageId) }
    }

    fun resize(pageId: String) {
        snapshot()
        viewModelScope.launch { repository.resizePage(documentId, pageId) }
    }

    fun addPages(scan: ScannedPages) {
        if (scan.pageUris.isEmpty()) return
        snapshot()
        viewModelScope.launch { repository.addPages(documentId, scan) }
    }

    fun insertImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        snapshot()
        viewModelScope.launch { repository.insertImages(documentId, uris) }
    }

    fun erase(pageId: String, strokes: List<FloatArray>, displayW: Float, displayH: Float, brushPx: Float) {
        if (strokes.isEmpty()) return
        snapshot()
        viewModelScope.launch { repository.erasePage(documentId, pageId, strokes, displayW, displayH, brushPx) }
    }

    // ---- Crop (result comes back from the cropper Activity) ----
    private var pendingCropPageId: String? = null

    fun beginCrop(pageId: String) { pendingCropPageId = pageId }

    fun applyCrop(resultUri: Uri) {
        val pageId = pendingCropPageId ?: return
        pendingCropPageId = null
        snapshot()
        viewModelScope.launch { repository.replacePageImage(documentId, pageId, resultUri) }
    }

    // ---- Undo / redo ----
    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(state.value.pages)
        val target = undoStack.removeLast()
        emitHistory()
        viewModelScope.launch { repository.restorePages(documentId, target) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(state.value.pages)
        val target = redoStack.removeLast()
        emitHistory()
        viewModelScope.launch { repository.restorePages(documentId, target) }
    }
}
