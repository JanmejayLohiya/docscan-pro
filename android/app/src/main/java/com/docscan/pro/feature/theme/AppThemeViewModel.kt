package com.docscan.pro.feature.theme

import androidx.lifecycle.ViewModel
import com.docscan.pro.data.ThemeMode
import com.docscan.pro.data.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Exposes the app-wide theme preference to the root composable. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    store: ThemeStore,
) : ViewModel() {
    val mode: StateFlow<ThemeMode> = store.mode
}
