package com.docscan.pro.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.AuthRepository
import com.docscan.pro.data.ProfileStore
import com.docscan.pro.data.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val name: String = "",
    val email: String = "",
    val signedIn: Boolean = false,
    val identifier: String = "",
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val store: ProfileStore,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    val state: StateFlow<AccountUiState> =
        combine(store.profile, authRepository.authState) { profile, user ->
            AccountUiState(
                name = profile.name,
                email = profile.email,
                signedIn = user != null,
                identifier = user?.email ?: user?.phoneNumber ?: "",
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun updateInfo(name: String, email: String) = store.updateInfo(name.trim(), email.trim())

    fun signOut() = authRepository.signOut()

    fun backUp() {
        _backupMessage.value = "Backing up…"
        viewModelScope.launch {
            syncRepository.backUpMetadata().fold(
                onSuccess = { _backupMessage.value = "Backed up $it item(s) to the cloud" },
                onFailure = { _backupMessage.value = "Backup failed: ${it.message ?: "check connection"}" },
            )
        }
    }
}
