package com.docscan.pro.feature.account

import androidx.lifecycle.ViewModel
import com.docscan.pro.data.Profile
import com.docscan.pro.data.ProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val store: ProfileStore,
) : ViewModel() {

    val profile: StateFlow<Profile> = store.profile

    fun updateInfo(name: String, email: String) = store.updateInfo(name.trim(), email.trim())

    fun signIn(email: String, phone: String) = store.signIn(email.trim(), phone.trim())

    fun signOut() = store.signOut()
}
