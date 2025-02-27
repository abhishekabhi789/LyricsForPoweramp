package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Collections

class SearchResultViewmodel : ViewModel() {

    private var _searchResults = MutableStateFlow<List<Lyrics>>(Collections.emptyList())

    /** Search results as [List]<[Lyrics]>*/
    val searchResults = _searchResults.asStateFlow()

    var powerampId: Long? = null

    fun setSearchResults(list: List<Lyrics>) {
        _searchResults.update { list }
    }

    fun setPowerampId(realId: Long) {
        powerampId = realId
    }

    /** Will send the chosen lyrics to PowerAmp. Should call when have realId
     * @return [Boolean] indicating request attempt result*/
    fun sendLyricsToPoweramp(
        context: Context,
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean? = false
    ): Boolean {
        return PowerampApiHelper.sendLyricResponse(
            context = context,
            realId = powerampId!!,
            lyrics = lyrics,
            lyricsType = lyricsType,
            markInstrumental = markInstrumental
        )
    }
}
