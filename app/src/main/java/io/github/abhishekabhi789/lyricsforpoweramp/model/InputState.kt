package io.github.abhishekabhi789.lyricsforpoweramp.model

import androidx.annotation.StringRes
import io.github.abhishekabhi789.lyricsforpoweramp.R

/**Stores user inputs*/
data class InputState(
    /**Query for [SearchMode.Coarse]*/
    var queryString: String = "",
    /** An instance of [Track] contains input for [SearchMode.Fine]. */
    val queryTrack: Track = Track(),
    /** Stores which mode of search should be performed*/
    val searchMode: SearchMode = SearchMode.Coarse
) {
    /**Coarse search sends parameters as single string q, while fine search sends parameters as track's title, artist name and album name
     * @see <a href="https://lrclib.net/docs#:~:text=s%20example%20response.-,Search%20for%20lyrics%20records,-GET">LRCLIB#Search for lyrics records</a>*/
    enum class SearchMode(@StringRes val label: Int) {
        Coarse(R.string.coarse_search),
        Fine(R.string.fine_search)
    }
}
