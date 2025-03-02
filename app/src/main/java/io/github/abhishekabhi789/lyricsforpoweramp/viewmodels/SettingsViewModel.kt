package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _themeState = MutableStateFlow(AppPreference.getTheme(application))
    val themeState: StateFlow<AppPreference.AppTheme> = _themeState.asStateFlow()

    fun updateTheme(newTheme: AppPreference.AppTheme) {
        _themeState.value = newTheme
        AppPreference.setTheme(getApplication(), newTheme)
    }
}
