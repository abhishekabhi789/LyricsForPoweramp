package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
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

    private var filePath = ""

    fun setSearchResults(list: List<Lyrics>) {
        _searchResults.update { list }
    }

    fun setPowerampId(realId: Long) {
        powerampId = realId
    }

    fun setFilePath(path: String) {
        filePath = path
    }

    /** Will send the chosen lyrics to PowerAmp. Should call when have realId
     * @return [Boolean] indicating request attempt result*/
    fun sendLyricsToPoweramp(
        context: Context,
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean = false
    ): Boolean {
        val (sentToPoweramp, writingResult) = powerampId?.let { realId ->
            PowerampApiHelper.sendLyricResponse(
                context = context,
                filePath = filePath,
                powerampId = realId,
                lyrics = lyrics,
                lyricsType = lyricsType,
                markInstrumental = markInstrumental
            )
        } ?: Pair(false, StorageHelper.Result.UNKNOWN_ERROR)//poweramp id null
        return sentToPoweramp && writingResult == StorageHelper.Result.SUCCESS
    }
}
