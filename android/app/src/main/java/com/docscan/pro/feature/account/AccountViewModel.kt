package com.docscan.pro.feature.account

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.AuthRepository
import com.docscan.pro.data.GoogleDriveRepository
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
    private val driveRepository: GoogleDriveRepository,
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

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _driveEmail = MutableStateFlow(driveRepository.connectedEmail())
    val driveEmail: StateFlow<String?> = _driveEmail.asStateFlow()

    private val _driveMessage = MutableStateFlow<String?>(null)
    val driveMessage: StateFlow<String?> = _driveMessage.asStateFlow()

    fun updateInfo(name: String, email: String) = store.updateInfo(name.trim(), email.trim())

    fun signOut() = authRepository.signOut()

    // ---- Google Drive backup ----
    fun driveSignInIntent(): Intent = driveRepository.signInIntent()

    fun onDriveSignInResult(data: Intent?) {
        driveRepository.handleSignInResult(data).fold(
            onSuccess = {
                _driveEmail.value = it
                _driveMessage.value = "Connected as $it"
            },
            onFailure = { _driveMessage.value = "Couldn't connect Google Drive" },
        )
    }

    fun disconnectDrive() {
        driveRepository.disconnect()
        _driveEmail.value = null
        _driveMessage.value = "Disconnected"
    }

    fun backUpToDrive() {
        if (_busy.value) return
        _busy.value = true
        _driveMessage.value = "Backing up to Drive…"
        viewModelScope.launch {
            driveRepository.backUp().fold(
                onSuccess = { _driveMessage.value = "Backed up $it file(s) to Google Drive" },
                onFailure = { _driveMessage.value = "Drive backup failed: ${it.message ?: "check connection"}" },
            )
            _busy.value = false
        }
    }

    fun restoreFromDrive() {
        if (_busy.value) return
        _busy.value = true
        _driveMessage.value = "Restoring from Drive…"
        viewModelScope.launch {
            driveRepository.restore().fold(
                onSuccess = {
                    _driveMessage.value =
                        if (it == 0) "Drive is already up to date" else "Restored $it document(s) from Drive"
                },
                onFailure = { _driveMessage.value = "Drive restore failed: ${it.message ?: "check connection"}" },
            )
            _busy.value = false
        }
    }

    fun backUp() {
        if (_busy.value) return
        _busy.value = true
        _backupMessage.value = "Backing up…"
        viewModelScope.launch {
            syncRepository.backUp().fold(
                onSuccess = { _backupMessage.value = "Backed up $it file(s) to the cloud" },
                onFailure = { _backupMessage.value = "Backup failed: ${it.message ?: "check connection"}" },
            )
            _busy.value = false
        }
    }

    fun restore() {
        if (_busy.value) return
        _busy.value = true
        _restoreMessage.value = "Restoring…"
        viewModelScope.launch {
            syncRepository.restore().fold(
                onSuccess = {
                    _restoreMessage.value =
                        if (it == 0) "Everything is already up to date" else "Restored $it document(s)"
                },
                onFailure = { _restoreMessage.value = "Restore failed: ${it.message ?: "check connection"}" },
            )
            _busy.value = false
        }
    }
}
