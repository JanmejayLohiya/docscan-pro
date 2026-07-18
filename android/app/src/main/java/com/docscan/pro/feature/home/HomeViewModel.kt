package com.docscan.pro.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.DocumentRepository
import com.docscan.pro.domain.Document
import com.docscan.pro.feature.scan.ScannedPages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val documents: List<Document> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DocumentRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        repository.observeDocuments()
            .map { HomeUiState(isLoading = false, documents = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onScanned(scan: ScannedPages) {
        if (scan.pageUris.isEmpty()) return
        viewModelScope.launch {
            repository.saveScannedDocument(defaultName(), scan)
        }
    }

    fun delete(documentId: String) {
        viewModelScope.launch { repository.delete(documentId) }
    }

    private fun defaultName(): String =
        "Scan " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
