package io.github.abhishekabhi789.lyricsforpoweramp.model

import androidx.annotation.StringRes
import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class LyricsType(@StringRes val label: Int) {
    PLAIN(R.string.plain_lyrics_short),
    SYNCED(R.string.synced_lyrics_short),
    INSTRUMENTAL(R.string.settings_mark_instrumental_tracks)
}
