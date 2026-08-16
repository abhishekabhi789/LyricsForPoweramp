package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.util.Log
import com.kyant.taglib.PropertyMap
import com.maxmpz.poweramp.player.PowerampAPIHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LyricsSavingHelper @Inject constructor(
    private val context: Context,
    private val appPreference: AppPreference,
    private val powerampAPIHelper: PowerampApiHelper,
    private val storageHelper: StorageHelper,
    private val taglibHelper: TaglibHelper,
) {
    fun saveLyrics(
        filePath: String?,
        powerampId: Long,
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean,
        fixMetadata: Boolean
    ): Flow<LyricsSavingState> = flow {
        //these configs are read everytime user saving lyrics, to get the up-to date values
        val shouldSendToPA = appPreference.sendLyricsToPoweramp.first()
        val shouldSaveAsFile = appPreference.saveLyricsAsFile.first()
        val embedIntoFile = appPreference.embedLyricsIntoFile.first()
        val saveIdTagsInFile = appPreference.saveIdTagsInFile.first()

        var progress = 0.1f
        var state = LyricsSavingState(
            progress = progress,
            sendToPoweramp = LyricsSavingState.Operation(shouldPerform = shouldSendToPA),
            saveAsFile = LyricsSavingState.Operation(shouldPerform = shouldSaveAsFile),
            embedIntoFile = LyricsSavingState.Operation(shouldPerform = embedIntoFile),
            fixSongMetaData = LyricsSavingState.Operation(shouldPerform = fixMetadata)
        )
        emit(state)

        val totalSteps =
            listOf(shouldSendToPA, shouldSaveAsFile, embedIntoFile, fixMetadata).count { it }
        val stepSize = 1f / totalSteps

        val lyricsText: String = when (lyricsType) {
            LyricsType.PLAIN -> (lyrics.plainLyrics ?: lyrics.syncedLyrics)
            LyricsType.SYNCED -> (lyrics.syncedLyrics ?: lyrics.plainLyrics)
            LyricsType.INSTRUMENTAL -> PowerampApiHelper.INSTRUMENTAL_MARKING
        } ?: ""

        if (shouldSendToPA) {
            var sentToPa = false
            try {
                val resultIntent = powerampAPIHelper.prepareResponseIntent(
                    context = context,
                    powerampId = powerampId,
                    lyrics = lyrics,
                    lyricsText = lyricsText,
                    markInstrumental = markInstrumental
                )
                sentToPa = PowerampAPIHelper.sendPAIntent(context, resultIntent)
            } catch (e: Exception) {
                Log.e(TAG, "saveLyrics: failed to send to PA", e)
            } finally {
                if (sentToPa) progress += stepSize
                val newState = state.sendToPoweramp.copy(result = sentToPa)
                state = state.copy(progress = progress, sendToPoweramp = newState)
                emit(state)
            }
        }
        if (shouldSaveAsFile && lyricsType != LyricsType.INSTRUMENTAL) {
            val lyricsContent = if (saveIdTagsInFile && lyricsType == LyricsType.SYNCED) {
                buildString {
                    appendLine("[ti:${lyrics.trackName}]")
                    lyrics.artistName?.let { appendLine("[ar:$it]") }
                    lyrics.albumName?.let { appendLine("[al:$it]") }
                    appendLine("[length:${lyrics.getFormatAsLrcDuration()}]")
                    appendLine("[tool:LyricsForPoweramp]")
                    appendLine("[by:LRCLIB.net]")//Author of the LRC file (not the song)
                    appendLine()
                    appendLine(lyricsText)
                }
            } else lyricsText
            val saveResult = storageHelper.writeLyricsFile(
                context = context,
                filePath = filePath,
                lyricsContent = lyricsContent,
                lyricsType = lyricsType,
            )
            Log.i(TAG, "saveLyrics: save to storage $saveResult")
            if (saveResult == StorageHelper.Result.SUCCESS) progress += stepSize
            val savedToFile = state.saveAsFile.copy(result = saveResult)
            state = state.copy(progress = progress, saveAsFile = savedToFile)
            emit(state)
        }
        if (embedIntoFile || fixMetadata) {
            if (filePath != null) {
                val onSessionError = { error: StorageHelper.Result ->
                    state = state.copy(embedIntoFile = state.embedIntoFile.copy(result = error))
                }
                taglibHelper.getTaglibSession(filePath, onError = onSessionError)
                    ?.use { session ->
                        val metadata: PropertyMap = session.getMetadata() ?: PropertyMap()
                        if (embedIntoFile && lyricsType != LyricsType.INSTRUMENTAL) {
                            metadata[TaglibHelper.KEY_LYRICS] = arrayOf(lyricsText)
                            if (session.setMetadata(metadata)) {
                                progress += stepSize
                                state = state.copy(
                                    progress = progress,
                                    embedIntoFile = state.embedIntoFile.copy(result = StorageHelper.Result.SUCCESS)
                                )
                            } else state = state.copy(
                                embedIntoFile = state.embedIntoFile.copy(result = StorageHelper.Result.UNKNOWN_ERROR)
                            )
                        }
                        if (fixMetadata) {
                            metadata[TaglibHelper.KEY_TITLE] = arrayOf(lyrics.trackName)
                            lyrics.artistName?.let {
                                metadata[TaglibHelper.KEY_ARTIST] = arrayOf(it)
                            }
                            lyrics.albumName?.let {
                                metadata[TaglibHelper.KEY_ALBUM] = arrayOf(it)
                            }
                            if (session.setMetadata(metadata)) {
                                progress += stepSize
                                state = state.copy(
                                    progress = progress,
                                    fixSongMetaData = state.fixSongMetaData.copy(result = StorageHelper.Result.SUCCESS)
                                )
                            } else state = state.copy(
                                fixSongMetaData = state.fixSongMetaData.copy(result = StorageHelper.Result.UNKNOWN_ERROR)
                            )
                        }
                        session.saveModifiedFile().let { saved: Boolean ->
                            Log.d(TAG, "saveLyrics: saved modified file $saved")
                        }
                    }
            } else {
                state = state.copy(
                    embedIntoFile = state.embedIntoFile.copy(result = StorageHelper.Result.INVALID_FILEPATH)
                )
            }
            emit(state)
        }

        state = state.copy(progress = state.progress.coerceIn(0.0f, 1f))
        emit(state)
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "LyricsSavingHelper"
    }
}

data class LyricsSavingState(
    val progress: Float = 0f,
    val sendToPoweramp: Operation<Boolean?> = Operation(shouldPerform = false),
    val saveAsFile: Operation<StorageHelper.Result?> = Operation(shouldPerform = false),
    val embedIntoFile: Operation<StorageHelper.Result?> = Operation(shouldPerform = false),
    val fixSongMetaData: Operation<StorageHelper.Result?> = Operation(shouldPerform = false)
) {
    data class Operation<T>(val shouldPerform: Boolean = false, val result: T? = null)
}
