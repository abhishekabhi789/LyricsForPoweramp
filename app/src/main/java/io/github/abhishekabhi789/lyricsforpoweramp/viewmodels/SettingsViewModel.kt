package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.PowerampFolder
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.FilterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreference: AppPreference,
    private val powerampApiHelper: PowerampApiHelper
) :
    ViewModel() {
    val appTheme = appPreference.appTheme

    private val _accessRequestedPath: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val accessRequestedPath = _accessRequestedPath.asStateFlow()

    fun updateTheme(newTheme: AppTheme) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.APP_THEME, newTheme.name)
        }
    }

    fun setAccessRequestedPath(path: String?) {
        Log.d(TAG, "setAccessRequestedPath: path- $path")
        _accessRequestedPath.update {
            if (path.isNullOrBlank()) null else getStorageUriFromPath(path)
        }
    }

    val fallbackToSearch = appPreference.fallbackSearch
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setFallbackToSearchMode(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.FALLBACK_SEARCH, enabled)
        }
    }

    val showNotification = appPreference.notifyOnRequestFailure
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setShowNotification(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SHOW_LYRICS_REQUEST_NOTIFICATION, enabled)
        }
    }

    val overwriteNotification = appPreference.overwriteNotification
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setOverwriteNotification(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.OVERWRITE_NOTIFICATION, enabled)
        }
    }

    val preferredLyricsType = appPreference.preferredLyricsType
        .stateIn(viewModelScope, SharingStarted.Lazily, LyricsType.SYNCED)

    fun setPreferredLyricsType(type: LyricsType) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.PREFERRED_LYRICS_TYPE, type.name)
        }
    }

    val markInstrumental = appPreference.markInstrumental
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setMarkInstrumental(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.MARK_INSTRUMENTAL_LYRICS, enabled)
        }
    }

    val sendLyricsToPoweramp = appPreference.sendLyricsToPoweramp
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setSendLyricsToPoweramp(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SEND_LYRICS_TO_POWERAMP, enabled)
        }
    }

    val saveAsFile = appPreference.saveLyricsAsFile
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setSaveAsFile(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SAVE_LYRICS_IN_FILE, enabled)
        }
    }

    val saveIdTagsInFile = appPreference.saveIdTagsInFile
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setSaveIdTagsInFile(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SAVE_ID_TAGS_IN_FILE, enabled)
        }
    }

    val embedLyricsIntoFile = appPreference.embedLyricsIntoFile
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setEmbedLyricsIntoFile(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.EMBED_LYRICS_AS_TAG, enabled)
        }
    }

    val fixSongMetadata = appPreference.fixSongMetadata
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setFixSongMetadata(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.FIX_SONG_METADATA, enabled)
        }
    }

    private val _powerampFolders = MutableStateFlow<List<PowerampFolder>>(emptyList())
    val powerampFolders = _powerampFolders.asStateFlow()

    fun loadPowerampFolders(context: Context) {
        viewModelScope.launch {
            _powerampFolders.value = powerampApiHelper.getPowerampFolders(context)
        }
    }

    val savedUris = appPreference.savedUris
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveNewUri(uri: Uri) {
        viewModelScope.launch { appPreference.saveUri(uri) }
        setAccessRequestedPath(null)
    }

    fun removeUri(uri: Uri) {
        viewModelScope.launch { appPreference.removeUri(uri) }
    }

    val lrclibApiInstances = appPreference.lrclibApiInstances
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateLrclibInstancesList(list: List<String>) {
        val finalList = buildList {
            //default server should be preserved as first item
            addAll(AppPreference.DEFAULT_LRCLIB_API_URLS)
            list.filterNot { AppPreference.DEFAULT_LRCLIB_API_URLS.contains(it) }
                .distinct()
                .forEach { add(it) }
        }
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SAVED_LRCLIB_API_URL, finalList.toSet())
        }
    }

    val selectedLrcLibInstanceUrl = appPreference.selectedLrcLibInstanceUrl

    fun updateSelectedLrclibUrl(url: String) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SELECTED_LRCLIB_API_URL, url)
        }
    }

    val timestampDelta = appPreference.timestampDelta
        .stateIn(viewModelScope, SharingStarted.Lazily, 10)

    fun setTimestampDelta(value: Int) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.TIMESTAMP_DELTA, value)
        }
    }

    val aiApiKeys = appPreference.aiProvidersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setAiProviderApiKey(provider: AiProvider, apiKey: String) {
        viewModelScope.launch { appPreference.setAiProviderApiKey(provider, apiKey) }
    }

    val aiModels = appPreference.aiProvidersModelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setAiProviderModel(provider: AiProvider, model: String) {
        viewModelScope.launch { appPreference.setAiProviderModel(provider, model) }
    }


    val filters = appPreference.filters
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun setFilter(type: FilterType, value: List<String>) {
        val prefKey = stringPreferencesKey(type.key)
        viewModelScope.launch {
            if (value.isEmpty()) {
                appPreference.removePreference(prefKey)
            } else {
                appPreference.setPreference(prefKey, value.joinToString("\n"))
            }
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

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
