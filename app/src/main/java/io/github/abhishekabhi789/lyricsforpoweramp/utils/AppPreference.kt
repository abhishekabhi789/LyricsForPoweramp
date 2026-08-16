package io.github.abhishekabhi789.lyricsforpoweramp.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider
import io.github.abhishekabhi789.lyricsforpoweramp.di.ApplicationScope
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

val Context.appDataStore by preferencesDataStore(
    name = "app_preferences",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, AppPreference.OTHER_PREF),
            SharedPreferencesMigration(context, AppPreference.UI_PREF_NAME),
            SharedPreferencesMigration(context, AppPreference.FILTER_PREF_NAME)
        )
    }
)

@Singleton
class AppPreference @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope
) {

    val appTheme = dataStore.data.map { preferences ->
        preferences[APP_THEME]?.let { AppTheme.valueOf(it) } ?: defaultTheme
    }.stateIn(scope, SharingStarted.Eagerly, defaultTheme)

    val firstTimeInfoShown = dataStore.data.map { it[FIRST_TIME_INFO_SHOWN] ?: false }

    val fallbackSearch = dataStore.data.map { it[FALLBACK_SEARCH] ?: false }

    val notifyOnRequestFailure =
        dataStore.data.map { it[SHOW_LYRICS_REQUEST_NOTIFICATION] ?: true }

    val overwriteNotification = dataStore.data.map { it[OVERWRITE_NOTIFICATION] ?: false }

    val preferredLyricsType = dataStore.data.map { preference ->
        preference[PREFERRED_LYRICS_TYPE]?.let { LyricsType.valueOf(it) } ?: LyricsType.SYNCED
    }

    val markInstrumental = dataStore.data.map { it[MARK_INSTRUMENTAL_LYRICS] ?: false }

    val sendLyricsToPoweramp = dataStore.data.map { it[SEND_LYRICS_TO_POWERAMP] ?: true }

    val saveLyricsAsFile = dataStore.data.map { it[SAVE_LYRICS_IN_FILE] ?: false }

    val saveIdTagsInFile = dataStore.data.map { it[SAVE_ID_TAGS_IN_FILE] ?: false }

    val embedLyricsIntoFile = dataStore.data.map { it[EMBED_LYRICS_AS_TAG] ?: false }

    val fixSongMetadata = dataStore.data.map { it[FIX_SONG_METADATA] ?: false }

    val savedUris = dataStore.data.map { preference ->
        preference[FOLDER_URIS]?.map { it.toUri() } ?: emptyList()
    }

    val timestampDelta = dataStore.data.map { it[TIMESTAMP_DELTA] ?: 10 }

    val editorFontSize = dataStore.data.map { it[EDITOR_FONT_SIZE_SP] }

    val filters: Flow<Map<FilterType, List<String>>> = dataStore.data.map { preference ->
        FilterType.entries.associateWith {
            preference[stringPreferencesKey(it.key)]?.lines() ?: emptyList()
        }
    }

    val aiProvidersFlow = dataStore.data.map { preferences ->
        AiProvider.entries.associateWith { preferences[stringPreferencesKey(it.key)] }
    }

    val aiProvidersModelFlow = dataStore.data.map { preferences ->
        AiProvider.entries.associateWith { provider ->
            preferences[getKeyForModel(provider)] ?: provider.defaultModel
        }
    }

    val chosenAiProvider = dataStore.data.map { preferences ->
        preferences[CHOSEN_AI_PROVIDER]?.let { selectedProviderName ->
            if (AiProvider.entries.map { it.name }.contains(selectedProviderName)) {
                AiProvider.valueOf(selectedProviderName)
            } else AiProvider.getDefault()
        } ?: AiProvider.getDefault()
    }

    val lrclibApiInstances =
        dataStore.data.map { it[SAVED_LRCLIB_API_URL]?.toList() ?: DEFAULT_LRCLIB_API_URLS }

    val selectedLrcLibInstanceUrl =
        dataStore.data.map { it[SELECTED_LRCLIB_API_URL] ?: DEFAULT_API_URL }
            .stateIn(scope, SharingStarted.Eagerly, DEFAULT_API_URL)

    suspend fun saveUri(uri: Uri) {
        val savedUris = runCatching { savedUris.first() }
            .onFailure { Log.e(TAG, "saveUri: failed to get latest savedUri", it) }
            .getOrNull() ?: emptyList()
        val updatedUris = savedUris.toMutableList().apply { add(uri) }
        val uriSet = updatedUris.map { it.toString() }.toSet()
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply { this[FOLDER_URIS] = uriSet }
        }
    }

    suspend fun removeUri(uri: Uri) {
        val savedUris = runCatching { savedUris.first() }
            .onFailure { Log.e(TAG, "removeUri: failed to get latest savedUri", it) }
            .getOrNull()
        if (savedUris.isNullOrEmpty()) return
        val updatedUris = savedUris.toMutableList().apply { remove(uri) }
        val uriSet = updatedUris.map { it.toString() }.toSet()
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply { this[FOLDER_URIS] = uriSet }
        }
    }

    suspend fun setAiProviderApiKey(aiProvider: AiProvider, apiKey: String) {
        setPreference(stringPreferencesKey(aiProvider.key), apiKey)
    }

    suspend fun setAiProviderModel(provider: AiProvider, model: String) {
        setPreference(getKeyForModel(provider), model)
    }

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply { this[key] = value }
        }
    }

    suspend fun <T> removePreference(key: Preferences.Key<T>) {
        dataStore.edit { preferences -> preferences.remove(key) }
    }

    suspend fun <T> getPreference(key: Preferences.Key<T>): T? {
        return runCatching { dataStore.data.map { it[key] }.firstOrNull() }.getOrNull()
    }

    companion object {
        private const val TAG = "AppPreference"

        @Deprecated("migrated to datastore")
        const val FILTER_PREF_NAME = "filter_preference"

        @Deprecated("migrated to datastore")
        const val UI_PREF_NAME = "ui_preference"

        @Deprecated("migrated to datastore")
        const val OTHER_PREF = "other_preference"

        val FIRST_TIME_INFO_SHOWN = booleanPreferencesKey("first_time_info_shown")
        val APP_THEME = stringPreferencesKey("app_theme")
        val FALLBACK_SEARCH = booleanPreferencesKey("perform_search_if_get_failed")
        val SHOW_LYRICS_REQUEST_NOTIFICATION =
            booleanPreferencesKey("lyrics_requests_show_notification")
        val OVERWRITE_NOTIFICATION =
            booleanPreferencesKey("lyrics_requests_overwrite_existing_notification")
        val PREFERRED_LYRICS_TYPE = stringPreferencesKey("preferred_lyrics_type")
        val SEND_LYRICS_TO_POWERAMP = booleanPreferencesKey("send_lyrics_to_poweramp")
        val SAVE_LYRICS_IN_FILE = booleanPreferencesKey("save_lyrics_in_file")
        val SAVE_ID_TAGS_IN_FILE = booleanPreferencesKey("save_id_tags_in_file")
        val EMBED_LYRICS_AS_TAG = booleanPreferencesKey("embed_lyrics_as_tag")
        val FIX_SONG_METADATA = booleanPreferencesKey("fix_song_metadata")
        val FOLDER_URIS = stringSetPreferencesKey("folder_uri_list")
        val MARK_INSTRUMENTAL_LYRICS = booleanPreferencesKey("mark_instrumental_lyrics")
        val TIMESTAMP_DELTA = intPreferencesKey("timestamp_delta_in_centi_seconds")
        val CHOSEN_AI_PROVIDER = stringPreferencesKey("chosen_ai_provider_for_rewrite")
        val EDITOR_FONT_SIZE_SP = floatPreferencesKey("editor_font_size_sp")
        val SAVED_LRCLIB_API_URL = stringSetPreferencesKey("lrclib_api_urls")
        val SELECTED_LRCLIB_API_URL = stringPreferencesKey("lrclib_api_url")

        const val LRCLIB_API_URL = "https://lrclib.net/api"
        const val DEFAULT_API_URL = "https://l4pa-server.abhishekabhi789.workers.dev/api"
        val DEFAULT_LRCLIB_API_URLS = listOf(DEFAULT_API_URL, LRCLIB_API_URL)
        val defaultTheme =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppTheme.Auto else AppTheme.Light

        fun getThemes(): List<AppTheme> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppTheme.entries.toList()
            else listOf(AppTheme.Light, AppTheme.Dark)
        }

        fun getKeyForModel(provider: AiProvider): Preferences.Key<String> {
            //key is already used for storing api key
            return stringPreferencesKey(provider.key + "_model")
        }
    }
}
