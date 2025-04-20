package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.MainActivity
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.workers.LyricsRequestWorker
import java.util.UUID

class NotificationHelper(private val context: Context) {
    private val isReqNotificationEnabled = AppPreference.getShowNotification(context)
    private var reqNotificationId: Int = generateRequestNotificationId(context)
    private val reqNotificationChannelName: String =
        context.getString(R.string.lyrics_request_handling_notifications)
    private val permissionNotificationId: Int = PERMISSION_NOTIFICATION_ID
    private val permissionNotificationChannelName =
        context.getString(R.string.notification_storage_access_needed_title)
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reqChannel = NotificationChannel(
                REQ_CHANNEL_ID,
                reqNotificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(reqChannel)

            val permissionChannel = NotificationChannel(
                PERMISSION_CHANNEL_ID,
                permissionNotificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(permissionChannel)
        }
    }

    fun makeRequestNotification(
        title: String,
        content: String,
        subText: String? = null,
        track: Track? = null
    ) {
        if (!isReqNotificationEnabled) return
        Log.d(TAG, "makeNotification: $content")
        val pendingIntent = if (track != null) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = LyricsRequestWorker.Companion.MANUAL_SEARCH_ACTION
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(PowerampAPI.Track.REAL_ID, track.realId)
                putExtra(PowerampAPI.Track.TITLE, track.trackName)
                putExtra(PowerampAPI.Track.ARTIST, track.artistName)
                putExtra(PowerampAPI.Track.ALBUM, track.albumName)
                putExtra(PowerampAPI.Track.PATH, track.filePath)
            }

            PendingIntent.getActivity(
                context,
                reqNotificationId,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        } else null
        val notification = NotificationCompat.Builder(context, REQ_CHANNEL_ID).run {
            setContentTitle(title)
            setContentText(content)
            setSmallIcon(R.drawable.app_icon)
            if (track != null) setAutoCancel(true)
            if (pendingIntent != null) setContentIntent(pendingIntent)
            if (subText != null) setSubText(subText)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) setPriority(Notification.PRIORITY_LOW)
            build()
        }
        notificationManager.notify(reqNotificationId, notification)
    }

    fun cancelRequestNotification() {
        if (!isReqNotificationEnabled) return
        Log.d(TAG, "cancelNotification: id- $reqNotificationId")
        notificationManager.cancel(reqNotificationId)
    }

    private fun generateRequestNotificationId(context: Context): Int {
        val overwriteNotification = AppPreference.getOverwriteNotification(context)
        return if (overwriteNotification) DEFAULT_REQ_NOTIFICATION_ID else UUID.randomUUID()
            .hashCode()
    }

    fun launchSettings(title: String, text: String, extras: Map<String, String>? = null) {
        val pendingIntent = Intent(context, SettingsActivity::class.java).apply {
            action = SettingsActivity.Companion.OPEN_SETTINGS_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            extras?.let { extras.forEach { extra -> putExtra(extra.key, extra.value) } }
        }.run {
            PendingIntent.getActivity(
                context, permissionNotificationId, this,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        }
        val notification = NotificationCompat.Builder(context, REQ_CHANNEL_ID).run {
            setContentTitle(title)
            setContentText(text)
            setSmallIcon(R.drawable.app_icon)
            setAutoCancel(true)
            if (pendingIntent != null) setContentIntent(pendingIntent)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) setPriority(Notification.PRIORITY_DEFAULT)
            build()
        }
        notificationManager.notify(permissionNotificationId, notification)
    }

    companion object {
        private const val TAG = "NotificationHelper"
        private const val REQ_CHANNEL_ID = "request_handling_notification"
        private const val PERMISSION_CHANNEL_ID = "permission_request_handling_notification"
        private const val DEFAULT_REQ_NOTIFICATION_ID = 789
        private const val PERMISSION_NOTIFICATION_ID = 123
    }
}
