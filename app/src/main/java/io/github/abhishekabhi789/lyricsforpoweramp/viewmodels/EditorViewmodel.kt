package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxmpz.poweramp.player.PowerampAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiProvider
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiRewriteHelper
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.RequestState
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingState
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PlaybackHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.EditorInputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewmodel @Inject constructor(
    private val appPreference: AppPreference,
    private val playbackHelper: PlaybackHelper,
    private val aiRewriteHelper: AiRewriteHelper,
    private val lyricsSavingHelper: LyricsSavingHelper
) : ViewModel() {

    private var isInitialized = false
    private var rewriteJob: Job? = null
    private val undoStack = ArrayDeque<EditorInputState>(50)
    private val redoStack = ArrayDeque<EditorInputState>(50)

    val canUndo: MutableStateFlow<Boolean> = MutableStateFlow(undoStack.isNotEmpty())
    val canRedo: MutableStateFlow<Boolean> = MutableStateFlow(redoStack.isNotEmpty())

    private val _inputState = MutableStateFlow(EditorInputState())
    val inputState: StateFlow<EditorInputState> = _inputState.asStateFlow()

    private val _lastSavedLyrics = MutableStateFlow("")
    val isUnsaved: StateFlow<Boolean> = combine(_inputState, _lastSavedLyrics) { input, lastSaved ->
        input.lyrics.trim() != lastSaved.trim()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var powerampId = PowerampAPI.ID_NO_ID

    private lateinit var lyrics: Lyrics

    private val _filepath = MutableStateFlow("")
    val filePath = _filepath.asStateFlow()

    private val _aiRewriteState: MutableStateFlow<RequestState> =
        MutableStateFlow(RequestState.Idle)
    val aiRewriteState = _aiRewriteState.asStateFlow()

    fun resetAiWriter() {
        stopRewriting()
        _aiRewriteState.value = RequestState.Idle
    }

    private val _lyricsSavingState = MutableStateFlow(LyricsSavingState())
    val lyricsSavingState = _lyricsSavingState.asStateFlow()

    val preferredLyricsType = appPreference.preferredLyricsType
        .stateIn(viewModelScope, SharingStarted.Lazily, LyricsType.SYNCED)

    val timestampDelta = appPreference.timestampDelta
        .stateIn(viewModelScope, SharingStarted.Lazily, 10)

    val saveAsFileEnabled = appPreference.saveLyricsAsFile
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val fixSongMetadata = appPreference.fixSongMetadata
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSaveAsFile(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreference.setPreference(AppPreference.SAVE_LYRICS_IN_FILE, enabled)
        }
    }

    val embedLyrics = appPreference.embedLyricsIntoFile
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setEmbedLyrics(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreference.setPreference(AppPreference.EMBED_LYRICS_AS_TAG, enabled)
        }
    }

    val aiProviders = appPreference.aiProvidersFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val chosenAiProvider = appPreference.chosenAiProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProvider.getDefault())

    val editorFontSize = appPreference.editorFontSize
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setEditorFontSize(size: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreference.setPreference(AppPreference.EDITOR_FONT_SIZE_SP, size)
        }
    }

    fun undo() {
        if (canUndo.value) {
            val popped = undoStack.removeLast()
            redoStack.add(inputState.value)
            _inputState.value = popped
            updateStackState()
        }
    }

    fun redo() {
        if (canRedo.value) {
            undoStack.add(inputState.value)
            _inputState.value = redoStack.removeLast()
            updateStackState()
        }
    }

    fun updateInputState(newState: EditorInputState) {
        undoStack.add(this.inputState.value)
        redoStack.clear()
        updateStackState()
        _inputState.value = newState
    }

    fun initialize(
        powerampId: Long, filePath: String, lyrics: Lyrics, preferredLyricsType: LyricsType
    ) {
        if (isInitialized) {
            Log.w(TAG, "initialize: viewmodel already initialized")
            return
        }
        this.lyrics = lyrics
        _filepath.value = filePath
        this.powerampId = powerampId
        val lyrics =
            (if (preferredLyricsType == LyricsType.SYNCED) lyrics.syncedLyrics else lyrics.plainLyrics)
                ?: ""
        val normalizedLyrics = lyrics.replace("\r\n", "\n")
        this._inputState.value = EditorInputState(lyrics = normalizedLyrics)
        Log.i(TAG, "initialize: viewmodel initialized")
        isInitialized = true
    }

    fun saveLyrics() {
        val lyricsContent = _inputState.value.lyrics
        val filePath = _filepath.value
        val fixMetadata = fixSongMetadata.value
        viewModelScope.launch(Dispatchers.IO) {
            resetLyricsSavingState()
            lyricsSavingHelper.saveLyrics(
                filePath = filePath,
                powerampId = powerampId,
                lyrics = lyrics.copy(syncedLyrics = lyricsContent),
                lyricsType = LyricsType.SYNCED,
                markInstrumental = false,
                fixMetadata = fixMetadata
            ).collect { state ->
                _lyricsSavingState.value = state
                if (state.progress == 1f) setLastSavedLyrics(lyricsContent)
            }
        }
    }

    fun setLastSavedLyrics(lyrics: String) {
        _lastSavedLyrics.value = lyrics
    }

    fun resetLyricsSavingState() {
        _lyricsSavingState.value = LyricsSavingState()
    }

    fun updateStackState() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    fun setChosenAiProvider(provider: AiProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreference.setPreference(AppPreference.CHOSEN_AI_PROVIDER, provider.name)
        }
    }

    fun rewriteWithAi(prompt: String) {
        stopRewriting()
        _aiRewriteState.value = RequestState.Processing
        val lyricsContent = _inputState.value.lyrics
        val chosenAiProvider = chosenAiProvider.value

        rewriteJob = viewModelScope.launch(Dispatchers.IO) {
            val result = aiRewriteHelper.transform(
                prompt = prompt,
                lyrics = lyricsContent,
                aiProvider = chosenAiProvider
            )
            Log.d(TAG, "rewriteWithAi: result- $result")
            _aiRewriteState.value = result
            if (result is RequestState.Success<*>) {
                val newLyrics = result.response as String
                val newState = _inputState.value.copy(lyrics = newLyrics)
                updateInputState(newState)
            }
        }
        rewriteJob?.invokeOnCompletion {
            rewriteJob = null
        }
    }

    fun stopRewriting() {
        rewriteJob?.cancel()
        rewriteJob = null
    }

    val playerInitialized = playbackHelper.playerInitialized
    val trackDuration = playbackHelper.trackDurationInSeconds
    val playbackPosition = playbackHelper.playbackSeconds
    val isPlaying = playbackHelper.isPlaying

    fun seekTo(centiSeconds: Long) {
        playbackHelper.seekTo(centiSeconds.times(10))
    }

    fun togglePlayback(play: Boolean) {
        playbackHelper.togglePlayback(play)
    }

    fun setTrackUri(uri: Uri) {
        Log.d(TAG, "setTrackUri: newUri $uri ")
        playbackHelper.setTrackUri(uri)
    }

    fun getCurrentTimestamp(): Timestamp = playbackHelper.getCurrentTimestamp()

    fun refreshAiWriter() {
        resetAiWriter()
        viewModelScope.launch(Dispatchers.IO) { aiRewriteHelper.refreshProviders() }
    }

    init {
        refreshAiWriter()
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: stopping playback")
        playbackHelper.destroy()
    }

    companion object {
        private const val TAG = "EditorViewmodel"
    }
}
