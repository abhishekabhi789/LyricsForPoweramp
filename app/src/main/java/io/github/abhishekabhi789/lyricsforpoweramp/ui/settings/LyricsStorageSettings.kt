package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings


import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.getCleanedPath
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.hasAccess
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LyricsStorageSettings(
    modifier: Modifier = Modifier,
    viewmodel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    SettingsGroup(
        title = stringResource(R.string.settings_lyrics_storage_label),
        icon = Icons.Default.Storage
    ) {
        BasicSettings(
            label = stringResource(R.string.settings_send_to_poweramp_label),
            description = stringResource(R.string.settings_send_to_poweramp_description)
        ) {
            var savedChoice by remember {
                mutableStateOf(AppPreference.getSendLyricsToPoweramp(context))
            }
            Switch(checked = savedChoice, onCheckedChange = {
                AppPreference.setSendLyricsToPoweramp(context, it)
                savedChoice = it
            })
        }
        var saveAsFile by remember { mutableStateOf(AppPreference.getSaveAsFile(context)) }
        BasicSettings(
            label = stringResource(R.string.settings_save_as_file_label),
            description = stringResource(R.string.settings_save_as_file_description)
        ) {
            Switch(checked = saveAsFile, onCheckedChange = {
                AppPreference.setSaveAsFile(context, it)
                saveAsFile = it
            })
        }
        AnimatedVisibility(visible = saveAsFile) {
            val accessRequestedPath by viewmodel.accessRequestedPath.collectAsState()
            var savedUris by rememberSaveable { mutableStateOf(AppPreference.getSavedUris(context)) }
            val pickFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    context.contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppPreference.saveFolderUri(context, it)
                    savedUris = savedUris.toMutableSet().apply { add(it) }.toList()
                    accessRequestedPath?.let { path -> viewmodel.setAccessRequestedPath(null) }

                }
            }
            Column(modifier = modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.settings_save_as_file_info),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BasicSettings(label = stringResource(R.string.settings_save_as_file_folder_list_title)) {
                    TextButton(onClick = { pickFolderLauncher.launch(null) }) {
                        Icon(Icons.Default.AddCircle, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.settings_save_as_file_choose_new_folder))
                    }
                }
                if (savedUris.isNotEmpty()) {
                    for ((i, uri) in savedUris.withIndex()) {
                        val path by remember(uri) { derivedStateOf { uri.getCleanedPath() } }
                        val hasAccess by rememberUpdatedState(uri.hasAccess(context))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${i + 1}.", modifier = Modifier.padding(end = 8.dp))
                            Text(
                                path,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (!hasAccess) TextButton(onClick = { uri }) {
                                Text(stringResource(R.string.settings_save_as_file_button_grant_access))
                            }
                            IconButton(onClick = {
                                val success = AppPreference.removeSavedFolder(context, uri)
                                if (success) savedUris = savedUris - uri
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.settings_save_as_file_button_remove_folder),
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }

                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_save_as_file_no_folders),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )

                }
            }
            accessRequestedPath?.let { pathUri ->
                PermissionDialog(
                    explanation = stringResource(
                        R.string.settings_save_as_file_permission_explanation,
                        pathUri.getCleanedPath()
                    ),
                    allowToSuppress = false,
                    onConfirm = { pickFolderLauncher.launch(pathUri) },
                    onDismiss = {
                        viewmodel.setAccessRequestedPath(null)
                        Log.w(TAG, "LyricsStorageSettings: user ignored storage access request")
                    }
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewStorageAccess() {
    LyricsForPowerAmpTheme {
        LyricsStorageSettings()
    }
}
