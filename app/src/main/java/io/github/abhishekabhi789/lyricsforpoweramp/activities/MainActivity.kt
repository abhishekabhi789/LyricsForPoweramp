package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.InputState
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.main.AppMain
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.MainActivityViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.workers.LyricsRequestWorker.Companion.MANUAL_SEARCH_ACTION

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainActivityViewModel

    @SuppressLint("InlinedApi")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            viewModel = viewModel()
            LaunchedEffect(Unit) {
                viewModel.updateTheme(AppPreference.getTheme(this@MainActivity))
            }
            val appTheme by viewModel.appTheme.collectAsState()
            val useDarkTheme = AppPreference.isDarkTheme(theme = appTheme)
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
                /* should not ask from here if user disabled notifications from settings*/
                val shouldAskForNotificationPermission = rememberSaveable {
                    AppPreference.getShowNotification(this@MainActivity)
                }
                val permissionState = rememberPermissionState(
                    permission = Manifest.permission.POST_NOTIFICATIONS
                ) { isGranted ->
                    @StringRes val message =
                        if (isGranted) R.string.settings_permission_toast_graned
                        else R.string.settings_permission_toast_denied
                    Toast(this@MainActivity).apply {
                        setText(message)
                        setDuration(Toast.LENGTH_SHORT)
                    }.show()
                }
                var showPermissionDialog by rememberSaveable { mutableStateOf(!permissionState.status.isGranted) }
                if (shouldAskForNotificationPermission && showPermissionDialog) {
                    PermissionDialog(
                        allowDisabling = true,
                        onConfirm = {
                            showPermissionDialog = false
                            if (permissionState.status.shouldShowRationale) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).run {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                    startActivity(this)
                                }
                            } else {
                                permissionState.launchPermissionRequest()
                            }
                        },
                        onDismiss = { disableNotification ->
                            if (disableNotification) {
                                AppPreference.setShowNotification(this@MainActivity, false)
                                Toast(this@MainActivity).run {
                                    setText(R.string.settings_permission_toast_notification_disabled)
                                    setDuration(Toast.LENGTH_SHORT)
                                    show()
                                }
                            }
                            showPermissionDialog = false
                        }
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    when (intent?.action) {
                        PowerampAPI.Lyrics.ACTION_LYRICS_LINK, MANUAL_SEARCH_ACTION -> {
                            val requestedTrack = PowerampApiHelper.makeTrack(this, intent)
                            viewModel.updateInputState(
                                InputState(
                                    queryString = requestedTrack.trackName ?: "",
                                    queryTrack = requestedTrack,
                                    searchMode = if (requestedTrack.artistName.isNullOrEmpty() && requestedTrack.albumName.isNullOrEmpty())
                                        InputState.SearchMode.Coarse else InputState.SearchMode.Fine
                                )
                            )
                        }
                    }
                    AppMain(viewModel = viewModel)
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        val preferredTheme = AppPreference.getTheme(this)
        viewModel.updateTheme(preferredTheme)
    }
}
