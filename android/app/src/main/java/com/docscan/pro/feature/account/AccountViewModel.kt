package com.docscan.pro.feature.account

import androidx.lifecycle.ViewModel
import com.docscan.pro.data.Profile
import com.docscan.pro.data.ProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val store: ProfileStore,
) : ViewModel() {

    private val _profile = MutableStateFlow(store.load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    fun save(name: String, email: String) {
        val updated = Profile(name.trim(), email.trim())
        store.save(updated)
        _profile.value = updated
    }
}
