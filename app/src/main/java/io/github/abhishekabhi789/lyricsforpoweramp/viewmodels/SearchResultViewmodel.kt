package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

class SearchResultViewmodel : ViewModel() {

    private var _searchResults = MutableStateFlow<List<Lyrics>>(Collections.emptyList())

    /** Search results as [List]<[Lyrics]>*/
    val searchResults = _searchResults.asStateFlow()

    private val _sendToPowerampState = MutableStateFlow<Boolean?>(null)
    val sendToPowerampState = _sendToPowerampState.asStateFlow()

    private val _saveToStorageState = MutableStateFlow<StorageHelper.Result?>(null)
    val saveToStorageState = _saveToStorageState.asStateFlow()

    var powerampId: Long? = null

    var filePath = ""
        private set

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
        markInstrumental: Boolean = false,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val (sentToPoweramp, writingResult) = powerampId?.let { realId ->
                PowerampApiHelper.sendLyricResponse(
                    context = context,
                    filePath = filePath,
                    powerampId = realId,
                    lyrics = lyrics,
                    lyricsType = lyricsType,
                    markInstrumental = markInstrumental
                )
            } ?: Pair(false, StorageHelper.Result.UNKNOWN_ERROR) //poweramp id null
            "sentToPoweramp $sentToPoweramp result $writingResult".let {
                Log.d(TAG, "sendLyricsToPoweramp: $it")
            }
            _sendToPowerampState.update { sentToPoweramp }
            _saveToStorageState.update { writingResult }
            if (sentToPoweramp && writingResult == StorageHelper.Result.SUCCESS) {
                delay(3.seconds)
                onComplete()
            }
        }
    }

    fun clearResultState() {
        viewModelScope.launch {
            _sendToPowerampState.update { null }
            _saveToStorageState.update { null }
        }
    }

    companion object {
        private const val TAG = "SearchResultViewmodel"
    }
}
