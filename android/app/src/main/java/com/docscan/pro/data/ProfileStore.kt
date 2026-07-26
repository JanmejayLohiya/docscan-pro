package com.docscan.pro.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class Profile(val name: String, val email: String)

/** Local profile storage (until cloud accounts arrive in Phase 4). */
@Singleton
class ProfileStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    fun load(): Profile = Profile(
        name = prefs.getString("name", "").orEmpty(),
        email = prefs.getString("email", "").orEmpty(),
    )

    fun save(profile: Profile) {
        prefs.edit().putString("name", profile.name).putString("email", profile.email).apply()
    }
}
