package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SearchResultViewmodel
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
            val viewmodel: SearchResultViewmodel = viewModel()
            LaunchedEffect(Unit) {
                searchResult?.let { viewmodel.setSearchResults(it) }
                realId?.let { viewmodel.setPowerampId(it) }
                fileUri?.let { viewmodel.setFilePath(it) }
            }
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    ResultScreen(
                        viewmodel = viewmodel,
                        onNavigateUp = { finish() },//takes to MainActivity
                        onFinish = { finishAffinity() }//takes back to Poweramp
                    )
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
        const val KEY_RESULT = "search_result"
        const val KEY_POWERAMP_REAL_ID = "poweramp_id"
        const val KEY_FILE_PATH = "file_path"
    }
}
