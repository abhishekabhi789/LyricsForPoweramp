package io.github.abhishekabhi789.lyricsforpoweramp.model

/**This data class carries track info from and to PowerAmp.*/
data class Track(
    val trackName: String = "",
    val artistName: String? = null,
    val albumName: String? = null,
    val filePath: String = "",
    val duration: Int? = null,//nullable for manual search
    val realId: Long? = null,//nullable direct search(from launcher)
    val lyrics: Lyrics? = null
)
