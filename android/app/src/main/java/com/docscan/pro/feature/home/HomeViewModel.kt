package com.docscan.pro.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.DocumentRepository
import com.docscan.pro.domain.CompressionLevel
import com.docscan.pro.domain.Document
import com.docscan.pro.domain.Folder
import com.docscan.pro.feature.scan.ScannedPages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val documents: List<Document> = emptyList(),
    val folders: List<Folder> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DocumentRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(repository.observeDocuments(), repository.observeFolders()) { docs, folders ->
            HomeUiState(isLoading = false, documents = docs, folders = folders)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Saves a freshly scanned document. If [newFolderName] is provided a folder is
     * created and used; otherwise [folderId] (which may be null = default) is used.
     */
    fun save(name: String, folderId: String?, newFolderName: String?, scan: ScannedPages) {
        if (scan.pageUris.isEmpty()) return
        viewModelScope.launch {
            val targetFolder = if (!newFolderName.isNullOrBlank()) repository.createFolder(newFolderName) else folderId
            repository.saveScannedDocument(name.trim().ifBlank { defaultName() }, targetFolder, scan)
        }
    }

    fun defaultName(): String =
        "Scan " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createFolder(name) }
    }

    fun move(documentId: String, folderId: String?) {
        viewModelScope.launch { repository.moveDocument(documentId, folderId) }
    }

    fun rename(documentId: String, name: String) {
        viewModelScope.launch { repository.rename(documentId, name) }
    }

    fun compress(documentId: String, level: CompressionLevel) {
        viewModelScope.launch { repository.compressDocument(documentId, level) }
    }

    fun delete(documentId: String) {
        viewModelScope.launch { repository.delete(documentId) }
    }
}
