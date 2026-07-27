package com.docscan.pro.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class Profile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val signedIn: Boolean = false,
)

/**
 * Local profile storage. Real authentication (phone OTP / email verification)
 * arrives with Firebase in Phase 4; for now sign-in just records the identifier.
 */
@Singleton
class ProfileStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private fun read() = Profile(
        name = prefs.getString("name", "").orEmpty(),
        email = prefs.getString("email", "").orEmpty(),
        phone = prefs.getString("phone", "").orEmpty(),
        signedIn = prefs.getBoolean("signedIn", false),
    )

    fun updateInfo(name: String, email: String) {
        prefs.edit().putString("name", name).putString("email", email).apply()
        _profile.value = _profile.value.copy(name = name, email = email)
    }

    fun signIn(email: String, phone: String) {
        prefs.edit()
            .putString("email", email)
            .putString("phone", phone)
            .putBoolean("signedIn", true)
            .apply()
        _profile.value = _profile.value.copy(email = email, phone = phone, signedIn = true)
    }

    fun signOut() {
        prefs.edit().putBoolean("signedIn", false).apply()
        _profile.value = _profile.value.copy(signedIn = false)
    }
}
