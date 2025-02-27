package io.github.abhishekabhi789.lyricsforpoweramp.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.HttpClient
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LrclibApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.NotificationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper.sendLyricResponse
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.receivers.LyricsRequestReceiver
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LyricsRequestWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val mContext = applicationContext
    private var mLrclibApiHelper = LrclibApiHelper(HttpClient.okHttpClient)
    private lateinit var mNotificationHelper: NotificationHelper
    private lateinit var mTrack: Track
    private var powerampTrackId = PowerampAPI.NO_ID

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createWorkerNotification()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        mNotificationHelper = NotificationHelper(mContext)
        powerampTrackId = inputData.getLong(LyricsRequestReceiver.KEY_REAL_ID, PowerampAPI.NO_ID)
        mTrack = Track(
            trackName = inputData.getString(LyricsRequestReceiver.KEY_TRACK_NAME),
            artistName = inputData.getString(LyricsRequestReceiver.KEY_ARTIST_NAME),
            albumName = inputData.getString(LyricsRequestReceiver.KEY_ALBUM_NAME),
            duration = inputData.getInt(LyricsRequestReceiver.KEY_DURATION, 0)
                .let { if (it == 0) null else it },
            realId = powerampTrackId
        )
        Log.i(TAG, "doWork: request for $mTrack")
        return handleLyricsRequest()
    }

    private suspend fun handleLyricsRequest(dispatcher: CoroutineDispatcher = Dispatchers.IO): Result {
        notify(content = mContext.getString(R.string.preparing_search_track))
        Log.i(TAG, "handleLyricsRequest: request for $mTrack")
        return withTimeoutOrNull(POWERAMP_LYRICS_REQUEST_WAIT_TIMEOUT) {
            var result: Result = Result.failure()
            CoroutineScope(dispatcher).launch {
                getLyrics(
                    track = mTrack,
                    dispatcher = dispatcher,
                    onSuccess = {
                        notify(mContext.getString(R.string.sending_lyrics))
                        sendLyrics(it).also { success ->
                            if (success) mNotificationHelper.cancelNotification()
                            else suggestManualSearch()
                            result = Result.success()
                        }
                    },
                    onError = { error ->
                        notify(mContext.getString(error.errMsg) + error.moreInfo?.let { " $it" })
                        Log.e(TAG, "handleLyricsRequest: $error")
                        suggestManualSearch()
                        result = Result.failure()
                    },
                )
            }.join()
            result
        } ?: run {
            notify(mContext.getString(R.string.timeout_cancelled))
            Log.e(TAG, "handleLyricsRequest: timeout cancelled")
            Result.retry()
        }
    }

    private suspend fun getLyrics(
        track: Track,
        dispatcher: CoroutineDispatcher,
        onSuccess: (Lyrics) -> Unit,
        onError: (LrclibApiHelper.Error) -> Unit
    ) {
        val useFallbackMethod = AppPreference.getSearchIfGetFailed(mContext)
        Log.i(TAG, "getLyrics: fallback to search permitted- $useFallbackMethod")
        notify(content = mContext.getString(R.string.performing_get_method))
        mLrclibApiHelper.getLyricsForTracks(
            track = track,
            dispatcher = dispatcher,
            onResult = onSuccess,
            onError = { error ->
                Log.e(TAG, "getLyrics: get request failed $error")
                if (useFallbackMethod && error == LrclibApiHelper.Error.NO_RESULTS) {
                    notify(mContext.getString(R.string.performing_search_method))
                    Log.i(TAG, "getLyrics: trying with search method")
                    CoroutineScope(dispatcher).launch {
                        mLrclibApiHelper.searchLyricsForTrack(
                            query = track,
                            dispatcher = dispatcher,
                            onResult = { onSuccess(it.first()) },
                            onError = onError
                        )
                    }
                } else {
                    Log.e(TAG, "getLyrics: no results, fallback not possible")
                    onError(error)
                }
            }
        )
    }

    private fun sendLyrics(lyrics: Lyrics?): Boolean {
        val lyricsType = AppPreference.getPreferredLyricsType(mContext)
        val markInstrumental = AppPreference.getMarkInstrumental(mContext)
        val sent = sendLyricResponse(
            context = mContext,
            realId = powerampTrackId,
            lyrics = lyrics,
            lyricsType = lyricsType,
            markInstrumental = markInstrumental
        )
        val status = if (sent) R.string.sent else R.string.failed_to_send
        notify("${mContext.getString(R.string.lyrics)} ${mContext.getString(status)}")
        return sent
    }

    private fun suggestManualSearch() {
        notify(mContext.getString(R.string.notification_manual_search_suggestion))
    }

    private fun notify(content: String) {
        val titleString = mContext.getString(R.string.request_handling_notification_title)
        if (::mTrack.isInitialized) {
            val (title, subText) = Pair(
                "$titleString - ${mTrack.trackName}",
                "${mContext.getString(R.string.track)}: ${mTrack.trackName}"
            )
            mNotificationHelper.makeNotification(title, content, subText, mTrack)
        } else {
            val (title, subText) = Pair(titleString, null)
            mNotificationHelper.makeNotification(title, content, subText)
        }
    }

    private fun createWorkerNotification(): Notification {
        val channelName = mContext.getString(R.string.lyrics_request_handling_notifications)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WORKER_NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(applicationContext, WORKER_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(mContext.getString(R.string.lyrics_request_handling_notifications))
            .setContentText(mContext.getString(R.string.request_handling_notification_title))
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "LyricsRequestWorker"
        const val MANUAL_SEARCH_ACTION =
            "io.github.abhishekabhi789.lyricsforpoweramp.MANUAL_SEARCH_ACTION"
        const val POWERAMP_LYRICS_REQUEST_WAIT_TIMEOUT = 10_000L
        private const val NOTIFICATION_ID = 1
        private const val WORKER_NOTIFICATION_CHANNEL_ID = "lyrics_request_channel"
    }
}
