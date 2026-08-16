package io.github.abhishekabhi789.lyricsforpoweramp.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.maxmpz.poweramp.player.PowerampAPI
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LrclibApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.NotificationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.SettingsPage
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.seconds

@HiltWorker
class LyricsRequestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appPreference: AppPreference,
    private val lrclibApiHelper: LrclibApiHelper,
    private val lyricsSavingHelper: LyricsSavingHelper,
    private val notificationHelper: NotificationHelper
) :
    CoroutineWorker(context, workerParams) {

    private val mContext = applicationContext
    private lateinit var mTrack: Track
    private var powerampTrackId = PowerampAPI.ID_NO_ID

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createWorkerNotification()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        powerampTrackId = inputData.getLong(Track.KEY_REAL_ID, PowerampAPI.ID_NO_ID)
        inputData.getString(Track.KEY_TRACK_NAME)?.let { trackName ->
            mTrack = Track(
                trackName = trackName,
                artistName = inputData.getString(Track.KEY_ARTIST_NAME),
                albumName = inputData.getString(Track.KEY_ALBUM_NAME),
                duration = inputData.getInt(Track.KEY_DURATION, 0).takeIf { it != 0 },
                realId = powerampTrackId,
                filePath = inputData.getString(Track.KEY_FILE_PATH) ?: ""
            )
            Log.i(TAG, "doWork: request for $mTrack")
            return handleLyricsRequest()
        }
        return Result.failure()
    }

    private suspend fun handleLyricsRequest(): Result {
        Log.i(TAG, "handleLyricsRequest: request for $mTrack")
        val sendToPoweramp = appPreference.sendLyricsToPoweramp.first()
        val saveToStorage = appPreference.saveLyricsAsFile.first()
        val embedIntoFile = appPreference.embedLyricsIntoFile.first()
        if (!sendToPoweramp && !saveToStorage && !embedIntoFile) {
            Log.e(TAG, "handleLyricsRequest: all saving options are disabled")
            val extras = Bundle().apply {
                val navData = Json.encodeToString<SettingsPage>(SettingsPage.Storage())
                putString(SettingsActivity.EXTRA_NAV_DATA, navData)
            }
            notificationHelper.postSettingsChangeRequest(
                title = getString(R.string.notification_no_saving_method_title),
                text = getString(R.string.notification_no_saving_method_description),
                intentAction = SettingsActivity.ACTION_OPEN_SETTING,
                extras = extras
            )
            return Result.failure()
        }
        val preferredLyricsType = appPreference.preferredLyricsType.first()
        val fixSongMetadata = appPreference.fixSongMetadata.first()

        return withTimeoutOrNull(POWERAMP_LYRICS_REQUEST_WAIT_TIMEOUT) {
            val lyrics = getLyrics(mTrack, preferredLyricsType)
            if (lyrics == null) {
                Result.failure()
            } else {
                saveLyrics(lyrics, preferredLyricsType, fixSongMetadata)
                Result.success()
            }
        } ?: run {
            notifyFailure("${getString(R.string.timeout_canceled)} - ${mTrack.trackName}")
            Log.e(TAG, "handleLyricsRequest: timeout cancelled")
            Result.retry()
        }
    }

    private suspend fun getLyrics(
        track: Track,
        lyricsType: LyricsType,
    ): Lyrics? = withContext(Dispatchers.IO) {
        val useFallbackMethod = appPreference.fallbackSearch.first()
        Log.i(TAG, "getLyrics: fallback to search permitted- $useFallbackMethod")
        return@withContext when (val getResult = lrclibApiHelper.getLyricsForTrack(track)) {
            is LrclibApiHelper.Result.Success -> getResult.data.first()
            is LrclibApiHelper.Result.Failure -> {
                if (useFallbackMethod) {
                    Log.d(TAG, "getLyrics: trying fallback method")
                    when (val searchResult = lrclibApiHelper.searchLyricsForTrack(track)) {
                        is LrclibApiHelper.Result.Failure -> {
                            notifyError(searchResult.error)
                            null
                        }

                        is LrclibApiHelper.Result.Success -> {
                            searchResult.data.let { lyricsList ->
                                val lyrics = if (lyricsType == LyricsType.SYNCED) {
                                    lyricsList.firstOrNull { it.syncedLyrics != null }
                                        ?: lyricsList.firstOrNull { it.plainLyrics != null }
                                } else {
                                    lyricsList.firstOrNull { it.plainLyrics != null }
                                        ?: lyricsList.firstOrNull { it.syncedLyrics != null }
                                }
                                if (lyrics == null) {
                                    notifyFailure(
                                        getString(R.string.error_no_results, mTrack.trackName)
                                    )
                                    null
                                } else lyrics
                            }
                        }
                    }
                } else {
                    notifyError(getResult.error)
                    null
                }
            }
        }
    }

    private suspend fun saveLyrics(
        lyrics: Lyrics,
        lyricsType: LyricsType,
        fixSongMetadata: Boolean
    ) {
        val markInstrumental = appPreference.markInstrumental.first()
        lyricsSavingHelper.saveLyrics(
            filePath = mTrack.filePath,
            powerampId = mTrack.realId ?: PowerampAPI.ID_NO_ID,
            lyrics = lyrics,
            lyricsType = lyricsType,
            markInstrumental = markInstrumental,
            fixMetadata = fixSongMetadata,
        ).collect { state ->
            val path = mTrack.filePath.substringBeforeLast(File.separatorChar)
            val notificationText = when (state.saveAsFile.result) {
                StorageHelper.Result.NO_PERMISSION -> getString(
                    R.string.notification_storage_missing_access_to_path, path
                )

                else -> null
            }
            notificationText?.let {
                val storagePathData = SettingsPage.Storage(accessRequestedPath = path)
                val extras = Bundle().apply {
                    val navData =
                        runCatching { Json.encodeToString<SettingsPage>(storagePathData) }.getOrNull()
                    putString(SettingsActivity.EXTRA_NAV_DATA, navData)
                }
                notificationHelper.postSettingsChangeRequest(
                    title = mContext.getString(R.string.notification_storage_access_needed_title),
                    text = it,
                    intentAction = SettingsActivity.ACTION_OPEN_SETTING,
                    extras = extras
                )
            }
            val completed: Boolean? =
                if (!state.sendToPoweramp.shouldPerform && !state.saveAsFile.shouldPerform) null
                else state.progress == 1f

            if (completed == true) {
                notificationHelper.cancelRequestNotification()
            }
        }
    }

    private fun notifyFailure(title: String) {
        val subText = getString(R.string.notification_lyrics_request_failed)
        if (::mTrack.isInitialized) {
            val content = getString(R.string.notification_manual_search_suggestion)
            notificationHelper.makeRequestNotification(title, content, subText, mTrack)
        } else {
            notificationHelper.makeRequestNotification(title = title, subText = subText)
        }
    }

    private fun notifyError(error: LrclibApiHelper.Error) {
        return when (error) {
            LrclibApiHelper.Error.TIMEOUT -> {
                notifyFailure("${getString(R.string.timeout_canceled)} - ${mTrack.trackName}")
            }

            LrclibApiHelper.Error.EMPTY_RESPONSE, LrclibApiHelper.Error.NO_RESULTS -> {
                notifyFailure("${getString(error.errMsgResId)} - ${mTrack.trackName}")
            }

            else -> {
                notifyFailure("${getString(error.errMsgResId)} - ${mTrack.trackName}")
            }
        }
    }

    private fun createWorkerNotification(): Notification {
        val channelName = getString(R.string.lyrics_request_handling_notifications)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WORKER_NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(applicationContext, WORKER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.lyrics_request_handling_notifications))
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getString(@StringRes resId: Int, vararg formats: String): String {
        return if (formats.isEmpty()) mContext.getString(resId)
        else mContext.getString(resId, *formats)
    }

    companion object {
        private const val TAG = "LyricsRequestWorker"
        const val MANUAL_SEARCH_ACTION =
            "io.github.abhishekabhi789.lyricsforpoweramp.MANUAL_SEARCH_ACTION"
        val POWERAMP_LYRICS_REQUEST_WAIT_TIMEOUT = 10.seconds
        private const val NOTIFICATION_ID = 1
        private const val WORKER_NOTIFICATION_CHANNEL_ID = "lyrics_request_channel"
    }
}
