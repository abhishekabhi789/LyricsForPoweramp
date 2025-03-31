package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.AppSettings
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewmodel: SettingsViewModel = viewModel()
            val preferredTheme by viewmodel.themeState.collectAsState()
            val useDarkTheme = AppPreference.isDarkTheme(theme = preferredTheme)
            LaunchedEffect(Unit) {
                when (intent?.action) {
                    OPEN_SETTINGS_ACTION -> {
                        viewmodel.setAccessRequestedPath(intent.getStringExtra(EXTRA_REQUIRED_PATH))
                    }
                }
            }
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT,
                ) { useDarkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    lightScrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT,
                ) { useDarkTheme },
            )
            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    AppSettings(viewmodel = viewmodel, onClose = { finish() })
                }
            }
        }
    }

    companion object {
        const val TAG = "SettingsActivity"
        const val OPEN_SETTINGS_ACTION =
            "io.github.abhishekabhi789.lyricsforpoweramp.FOLDER_ACCESS_NEEDED"
        const val EXTRA_REQUIRED_PATH = "need_permissison_for_this_path"
    }
}
