package com.docscan.pro.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** App-wide appearance preference. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class ThemeStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private fun read(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _mode.value = mode
    }
}
