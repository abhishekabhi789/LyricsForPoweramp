package io.github.abhishekabhi789.lyricsforpoweramp.model

data class LyricsSendData(
    val lyrics: Lyrics,
    val type: LyricsType,
    val markInstrumental: Boolean = false,
    val fixSongMetadata: Boolean = false
)
