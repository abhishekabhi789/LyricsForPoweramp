package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPI
import com.maxmpz.poweramp.player.PowerampAPIHelper
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference.FILTER

/**
 * Contains functions helping to send and receive data with PowerAmp
 */
object PowerampApiHelper {

    private const val TAG = "PowerampApiHelper"
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

    /**
     * Makes a [Track] for the intent passed by PowerAmp
     * @param intent received from PowerAmp
     * @return an instance of [Track]
     */
    fun makeTrack(context: Context, intent: Intent): Track? {
        val realId = intent.getLongExtra(PowerampAPI.Track.REAL_ID, PowerampAPI.NO_ID)
        val title = intent.getStringExtra(PowerampAPI.Track.TITLE)
        if (realId == PowerampAPI.NO_ID || title.isNullOrEmpty()) {
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
        val durationMs = intent.getIntExtra(PowerampAPI.Track.DURATION, 0)
        val filePath = intent.getStringExtra(PowerampAPI.Track.PATH)
        val duration: Int? = (durationMs / 1000).let { if (it == 0) null else it }
        return processField(context, FILTER.TITLE_FILTER, title)?.let {
            Track(
                trackName = it,
                artistName = processField(context, FILTER.ARTISTS_FILTER, artist),
                albumName = processField(context, FILTER.ALBUM_FILTER, album),
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
    private fun processField(context: Context, field: FILTER, value: String?): String? {
        val filter = AppPreference.getFilter(context, field)?.lines()
        return filter?.fold(value) { cleanedValue, filterItem ->
            cleanedValue?.replace(Regex(filterItem, RegexOption.IGNORE_CASE), "")
        } ?: value
    }

    /**
     * Sends the prepared lyric data to PowerAmp.
     * @param context required for sending intent and accessing app resources
     * @param filePath track file path from poweramp
     * @param powerampId realId from poweramp.
     * @param lyrics lyrics to be returned,
     * @param lyricsType preferred lyrics type set by the user.
     * @param markInstrumental whether if mark the track as instrumental on poweramp.
     * @return [Pair]<Boolean, StorageHelper.Result> where [Boolean] represents success status for sending to Poweramp.
     * and [StorageHelper.Result] represents success status for saving to storage.
     */
    fun sendLyricResponse(
        context: Context,
        filePath: String,
        powerampId: Long?,
        lyrics: Lyrics?,
        lyricsType: LyricsType,
        markInstrumental: Boolean = false
    ): Pair<Boolean, StorageHelper.Result> {
        val sendToPoweramp = AppPreference.getSendLyricsToPoweramp(context)
        val saveToFile = AppPreference.getSaveAsFile(context)
        val lyricsContent = when (lyricsType) {
            LyricsType.PLAIN -> (lyrics?.plainLyrics ?: lyrics?.syncedLyrics)!!
            LyricsType.SYNCED -> (lyrics?.syncedLyrics ?: lyrics?.plainLyrics)!!
            LyricsType.INSTRUMENTAL -> if (markInstrumental) INSTRUMENTAL_MARKING else null
        }
        val sentToPoweramp: Boolean = if (sendToPoweramp) {
            val infoLine = makeInfoLine(context, lyrics)
            val intent = Intent(PowerampAPI.Lyrics.ACTION_UPDATE_LYRICS).apply {
                putExtra(PowerampAPI.EXTRA_ID, powerampId)
                if (lyrics?.instrumental == true) {
                    Log.i(TAG, "sendLyricResponse: track is instrumental")
                    if (markInstrumental == true) {
                        Log.d(TAG, "sendLyricResponse: marking as instrumental")
                        putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, INSTRUMENTAL_MARKING)
                    }
                } else {
                    Log.d(TAG, "sendLyricResponse: track is vocal")
                    putExtra(PowerampAPI.Lyrics.EXTRA_LYRICS, lyricsContent)
                }
                putExtra(PowerampAPI.Lyrics.EXTRA_INFO_LINE, infoLine)
            }

            try {
                val status = PowerampAPIHelper.sendPAIntent(context, intent)
                Log.i(TAG, "sendLyricResponse: Success $status")
                status
            } catch (e: Throwable) {
                e.printStackTrace()
                false
            }
        } else true //not attempted
        val result = if (saveToFile) {
            if (lyricsContent != null) {
                StorageHelper.writeLyricsFile(
                    context = context,
                    filePath = filePath,
                    lyricsContent = lyricsContent,
                    lyricsType = lyricsType,
                ).also {
                    Log.i(TAG, "sendLyricResponse: save to storage $it")
                }
            } else {
                Log.w(TAG, "sendLyricResponse: lyrics is null")
                StorageHelper.Result.INVALID_LYRICS
            }
        } else StorageHelper.Result.SUCCESS //not attempted
        return Pair(sentToPoweramp, result)
    }

    private fun makeInfoLine(context: Context, lyrics: Lyrics?): String {
        return buildString {
            if (lyrics != null && lyrics.trackName.isNotEmpty()) {
                appendLine("${context.getString(R.string.input_track_title_label)}: ${lyrics.trackName}")
                appendLine("${context.getString(R.string.input_track_artists_label)}: ${lyrics.artistName}")
                appendLine("${context.getString(R.string.input_track_album_label)}: ${lyrics.albumName}")
                appendLine()
            }
            appendLine(context.getString(R.string.response_footer_text))
        }
    }
}
