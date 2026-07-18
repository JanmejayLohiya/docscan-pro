package com.docscan.pro.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.DocumentRepository
import com.docscan.pro.domain.Document
import com.docscan.pro.domain.Page
import com.docscan.pro.feature.scan.ScannedPages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: DocumentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val documentId: String = checkNotNull(savedStateHandle["documentId"])

    val state: StateFlow<EditorUiState> =
        combine(
            repository.observeDocument(documentId),
            repository.observePages(documentId),
        ) { document, pages -> EditorUiState(document, pages) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    fun reorder(fromIndex: Int, toIndex: Int) {
        val ids = state.value.pages.map { it.id }.toMutableList()
        if (fromIndex !in ids.indices || toIndex !in ids.indices) return
        ids.add(toIndex, ids.removeAt(fromIndex))
        viewModelScope.launch { repository.reorderPages(documentId, ids) }
    }

    fun remove(pageId: String) {
        viewModelScope.launch { repository.removePage(documentId, pageId) }
    }

    fun rotate(pageId: String) {
        viewModelScope.launch { repository.rotatePage(documentId, pageId) }
    }

    fun addPages(scan: ScannedPages) {
        if (scan.pageUris.isEmpty()) return
        viewModelScope.launch { repository.addPages(documentId, scan) }
    }
}
