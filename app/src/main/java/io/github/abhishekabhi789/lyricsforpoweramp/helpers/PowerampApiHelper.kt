package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.TableDefs
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.di.ApplicationScope
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.PowerampFolder
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.FilterType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Contains functions helping to send and receive data with PowerAmp
 */
class PowerampApiHelper @Inject constructor(
    private val appPreference: AppPreference,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var filters: Map<FilterType, List<String>> = emptyMap()


    init {
        observePreferences()
    }

    private fun observePreferences() {
        scope.launch {
            appPreference.filters.collect { filters = it }
        }
    }

    /**
     * Makes a [Track] for the intent passed by PowerAmp
     * @param intent received from PowerAmp
     * @return an instance of [Track]
     */
    fun makeTrack(intent: Intent): Track? {
        val realId = intent.getLongExtra(PowerampAPI.Track.REAL_ID, PowerampAPI.ID_NO_ID)
        val title = intent.getStringExtra(PowerampAPI.Track.TITLE)
        if (realId == PowerampAPI.ID_NO_ID || title.isNullOrEmpty()) {
            Log.e(
                TAG,
                buildString {
                    append("makeTrack: Failed to parse details.")
                    append(" | realId: $realId")
                    append(" | title: $title")
                },
            )
        }
        val album = intent.getStringExtra(PowerampAPI.Track.ALBUM)
        val artist = intent.getStringExtra(PowerampAPI.Track.ARTIST)
        val duration = intent.getIntExtra(PowerampAPI.Track.DURATION, 0)
        val filePath = intent.getStringExtra(PowerampAPI.Track.PATH)?.let { path ->
            if (!path.contains(":")) path.replaceFirst("/", ":") else path
        }
        return processField(FilterType.TITLE_FILTER, title)?.let {
            Track(
                trackName = it,
                artistName = processField(FilterType.ARTISTS_FILTER, artist),
                albumName = processField(FilterType.ALBUM_FILTER, album),
                duration = duration,
                filePath = filePath ?: "",
                realId = realId,
                lyrics = null
            )
        }
    }

    /**
     * Corresponding filter words will be removed from the value.
     */
    fun processField(type: FilterType, value: String?): String? {
        val filter = filters[type] ?: emptyList()
        return filter.fold(value) { cleanedValue, filterItem ->
            try {
                cleanedValue?.replace(Regex(filterItem, RegexOption.IGNORE_CASE), "")
            } catch (e: Exception) {
                Log.w(TAG, "Invalid regex in filter for $type: '$filterItem'", e)
                cleanedValue?.replace(filterItem, "")
            }
        } ?: value
    }

    fun prepareResponseIntent(
        context: Context,
        powerampId: Long,
        lyrics: Lyrics,
        lyricsText: String,
        markInstrumental: Boolean
    ): Intent {
        val infoLine = makeInfoLine(context, lyrics)
        return Intent(PowerampAPI.Lyrics.ACTION_UPDATE_LYRICS).apply {
            putExtra(PowerampAPI.EXTRA_ID, powerampId)
            if (lyrics.instrumental == true) {
                Log.i(TAG, "sendLyrics: track is instrumental")
                if (markInstrumental) {
                    Log.d(TAG, "sendLyrics: marking as instrumental")
                    putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, INSTRUMENTAL_MARKING)
                }
            } else {
                Log.d(TAG, "sendLyrics: track is vocal")
                putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, lyricsText)
            }
            putExtra(PowerampAPI.Lyrics.EXTRA_INFO_LINE, infoLine)
        }
    }

    private fun makeInfoLine(context: Context, lyrics: Lyrics?): String {
        return buildString {
            if (lyrics != null && lyrics.trackName.isNotEmpty()) {
                appendLine("${context.getString(R.string.input_track_title_label)}: ${lyrics.trackName}")
                lyrics.artistName?.let {
                    appendLine("${context.getString(R.string.input_track_artists_label)}: $it")
                }
                lyrics.albumName?.let {
                    appendLine("${context.getString(R.string.input_track_album_label)}: $it")
                }
                appendLine()
            }
            appendLine(context.getString(R.string.response_footer_text))
        }
    }

    suspend fun getPowerampFolders(context: Context): List<PowerampFolder> =
        withContext(Dispatchers.IO) {
            val folders = mutableListOf<PowerampFolder>()
            val uri = PowerampAPI.ROOT_URI.buildUpon().appendPath("folders").build()
            try {
                context.contentResolver.query(uri, FOLDER_TABLE_PROJECTION, null, null, null)
                    ?.use { cursor ->
                        val idIndex = cursor.getColumnIndex(FOLDER_TABLE_COL_ID)
                        val nameIndex = cursor.getColumnIndex(FOLDER_TABLE_COL_NAME)
                        val pathIndex = cursor.getColumnIndex(FOLDER_TABLE_COL_PATH)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLongOrNull(idIndex) ?: PowerampAPI.ID_NO_ID
                            val name = cursor.getStringOrNull(nameIndex)
                            val path = cursor.getStringOrNull(pathIndex)?.replaceFirst("/", ":")
                            if (name != null && path != null) {
                                folders.add(PowerampFolder(id, name, path))
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "getPowerampFolders: failed to query folders", e)
            }
            return@withContext folders
        }

    companion object {
        private const val TAG = "PowerampApiHelper"
        private const val FOLDER_TABLE_COL_ID = "_id"
        private const val FOLDER_TABLE_COL_NAME = "name"
        private const val FOLDER_TABLE_COL_PATH = "path"
        private val FOLDER_TABLE_PROJECTION = arrayOf(
            "${TableDefs.Folders._ID} AS $FOLDER_TABLE_COL_ID",
            "${TableDefs.Folders.NAME} AS $FOLDER_TABLE_COL_NAME",
            "${TableDefs.Folders.PATH} AS $FOLDER_TABLE_COL_PATH"
        )
        const val INSTRUMENTAL_MARKING = "Instrumental Track" +
                """
                  .♫♫♫.
                  ♫♫♫♫'
                ♫
                ♫
                ♫
                ♫
                ♫
    ,♫♫♫♫♫
    ♫♫♫♫♫'
    `♫♫♫'
"""

        fun refreshPowerampLibrary(context: Context) {
            val intent = Intent(PowerampAPI.Scanner.ACTION_SCAN_TAGS)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "refreshPowerampLibrary: failed to refresh library", e)
            }
        }
    }
}
