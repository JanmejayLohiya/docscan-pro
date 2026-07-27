package com.docscan.pro.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.AuthRepository
import com.docscan.pro.data.ProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    fun updateInfo(name: String, email: String) = store.updateInfo(name.trim(), email.trim())

    fun signOut() = authRepository.signOut()
}
