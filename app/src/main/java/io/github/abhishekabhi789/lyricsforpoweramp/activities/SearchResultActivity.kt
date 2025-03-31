package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SearchResultViewmodel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class SearchResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val searchResult: List<Lyrics>? = intent.extras?.let {
            BundleCompat.getParcelableArrayList(it, KEY_RESULT, Lyrics::class.java)
        }
        val preferredTheme = AppPreference.getTheme(this)
        val realId: Long? = getSerializableExtra(intent, KEY_POWERAMP_REAL_ID)
        val fileUri: String? = getSerializableExtra(intent, KEY_FILE_PATH)

        setContent {
            val scope = rememberCoroutineScope()
            val viewmodel: SearchResultViewmodel = viewModel()
            LaunchedEffect(Unit) {
                searchResult?.let { viewmodel.setSearchResults(it) }
                realId?.let { viewmodel.setPowerampId(it) }
                fileUri?.let { viewmodel.setFilePath(it) }
            }
            val lyricsList by viewmodel.searchResults.collectAsState()
            val isLaunchedFromPoweramp = remember { realId != null }
            val useDarkTheme = AppPreference.isDarkTheme(theme = preferredTheme)
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ) { useDarkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ) { useDarkTheme },
            )

            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    ResultScreen(
                        result = lyricsList,
                        snackbarHostState = snackbarHostState,
                        launchedFromPoweramp = isLaunchedFromPoweramp,
                        onLyricChosen = { lyrics, lyricsType ->
                            scope.launch {
                                sendLyrics(
                                    viewmodel = viewmodel,
                                    snackbarHostState = snackbarHostState,
                                    lyrics = lyrics,
                                    lyricsType = lyricsType
                                ) {
                                    finishAffinity()
                                }
                            }
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private suspend fun sendLyrics(
        viewmodel: SearchResultViewmodel,
        snackbarHostState: SnackbarHostState,
        lyrics: Lyrics,
        lyricsType: LyricsType? = null,
        onComplete: () -> Unit
    ) {
        withContext(Dispatchers.Main) {
            val chosenLyricsType = lyricsType
                ?: AppPreference.getPreferredLyricsType(this@SearchResultActivity)
            val sent =
                viewmodel.sendLyricsToPoweramp(
                    context = this@SearchResultActivity,
                    lyrics = lyrics,
                    lyricsType = chosenLyricsType,
                    markInstrumental = AppPreference.getMarkInstrumental(this@SearchResultActivity)
                )
            if (sent) {
                Log.d(TAG, "sendLyrics: sent")
                when (snackbarHostState.showSnackbar(
                    message = getString(R.string.snackbar_lyrics_sent_successfully),
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )) {
                    SnackbarResult.Dismissed -> onComplete()
                    else -> {}
                }
            } else {
                Log.d(TAG, "sendLyrics: failed")
                when (snackbarHostState.showSnackbar(
                    getString(R.string.snackbar_failed_to_send_lyrics),
                    getString(R.string.snackbar_retry_sending_lyrics),
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )) {
                    SnackbarResult.ActionPerformed -> sendLyrics(
                        viewmodel = viewmodel,
                        snackbarHostState = snackbarHostState,
                        lyrics = lyrics,
                        lyricsType = lyricsType,
                        onComplete = onComplete
                    )

                    SnackbarResult.Dismissed -> onComplete()
                }
            }
        }
    }

    private inline fun <reified T : Serializable> getSerializableExtra(
        intent: Intent,
        key: String
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(key) as? T
        }
    }

    companion object {
        private const val TAG = "SearchResultActivity"
        const val KEY_RESULT = "search_result"
        const val KEY_POWERAMP_REAL_ID = "poweramp_id"
        const val KEY_FILE_PATH = "file_path"
    }
}
