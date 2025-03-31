package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _themeState = MutableStateFlow(AppPreference.getTheme(application))
    val themeState: StateFlow<AppPreference.AppTheme> = _themeState.asStateFlow()

    private val _accessRequestedPath: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val accessRequestedPath = _accessRequestedPath.asStateFlow()

    fun updateTheme(newTheme: AppPreference.AppTheme) {
        _themeState.value = newTheme
        AppPreference.setTheme(getApplication(), newTheme)
    }

    fun setAccessRequestedPath(path: String?) {
        _accessRequestedPath.update {
            if (path.isNullOrBlank()) null else getStorageUriFromPath(path)
        }
    }

    private fun getStorageUriFromPath(path: String): Uri {
        val baseUri = "content://com.android.externalstorage.documents/tree/"
        return when {
            path.startsWith("primary/") -> {
                val subPath = path.removePrefix("primary/")
                (baseUri + Uri.encode("primary:$subPath")).toUri()
            }

            else -> {
                val storageId = path.substringBefore(File.separatorChar)
                val subPath = path.removePrefix("$storageId${File.separator}")
                (baseUri + Uri.encode("$storageId:$subPath")).toUri()
            }
        }
    }
}
