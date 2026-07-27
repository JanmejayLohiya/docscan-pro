package com.docscan.pro.feature.account

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscan.pro.data.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val codeSent: Boolean = false,
    val signedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var verificationId: String? = null

    fun emailAuth(signUp: Boolean, email: String, password: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = if (signUp) repository.signUpEmail(email, password) else repository.signInEmail(email, password)
            result.fold(
                onSuccess = { _state.update { it.copy(loading = false, signedIn = true) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Authentication failed") } },
            )
        }
    }

    fun sendCode(activity: Activity, phoneNumber: String) {
        _state.update { it.copy(loading = true, error = null) }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                _state.update { it.copy(loading = false, error = e.message ?: "Verification failed") }
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                _state.update { it.copy(loading = false, codeSent = true) }
            }
        }
        repository.startPhoneVerification(activity, phoneNumber, callbacks)
    }

    fun verifyCode(code: String) {
        val id = verificationId ?: return
        signInWithCredential(repository.credential(id, code))
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.signInWithCredential(credential).fold(
                onSuccess = { _state.update { it.copy(loading = false, signedIn = true) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Sign-in failed") } },
            )
        }
    }
}
