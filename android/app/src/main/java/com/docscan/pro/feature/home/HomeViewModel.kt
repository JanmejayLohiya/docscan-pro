package com.docscan.pro.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.DocumentRepository
import com.docscan.pro.network.DocumentDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val documents: List<DocumentDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DocumentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getDocuments().fold(
                onSuccess = { docs -> _state.update { it.copy(isLoading = false, documents = docs) } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load documents") } },
            )
        }
    }
}
