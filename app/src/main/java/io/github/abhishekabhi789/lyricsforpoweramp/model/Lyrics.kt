package io.github.abhishekabhi789.lyricsforpoweramp.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Locale

/**This data class represents each item from the API response.
 * @see <a href="https://lrclib.net/docs#:~:text=Soundtrack)%26duration%3D233-,Example%20response,-200%20OK%3A">LRCLIB#Example response</a>*/
@Parcelize
data class Lyrics(
    @SerializedName("trackName") val trackName: String,
    @SerializedName("artistName") val artistName: String,
    @SerializedName("albumName") val albumName: String,
    @SerializedName("plainLyrics") val plainLyrics: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?,
    @SerializedName("duration") val duration: Double,
    @SerializedName("instrumental") val instrumental: Boolean?,
) : Parcelable {
    /** [duration] in readable format. */
    fun getFormattedDuration(): String {
        val hours = (duration / 3600).toInt()
        val minutes = ((duration % 3600) / 60).toInt()
        val seconds = (duration % 60).toInt()
        val formatted = buildString {
            if (hours > 0) append("$hours:")
            append(String.format(Locale.US, "%02d:%02d", minutes, seconds))
        }
        return formatted
    }

    fun getFormatAsLrcDuration(): String {
        val minutes = (duration / 60).toInt()
        val seconds = duration.toInt() % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
