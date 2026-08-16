package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.PowerampFolder
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.FolderAccessState
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.rememberFolderAccess
import io.github.abhishekabhi789.lyricsforpoweramp.utils.getTreeDocumentId
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LyricsStorageSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    sendLyricsToPoweramp: Boolean,
    onSendLyricsToPowerampChange: (Boolean) -> Unit,
    saveAsFile: Boolean,
    onSaveAsFileChange: (Boolean) -> Unit,
    saveIdTagsInFile: Boolean,
    onSaveIdTagsInFileChange: (Boolean) -> Unit,
    embedIntoFile: Boolean,
    onEmbedIntoFileChange: (Boolean) -> Unit,
    fixSongMetadata: Boolean,
    onFixSongMetadataChange: (Boolean) -> Unit,
    powerampFolders: List<PowerampFolder>,
    savedUris: List<Uri>,
    accessRequestedPath: Uri?,
    onAccessRequestedPathChange: (String?) -> Unit,
    onSaveNewUri: (Uri) -> Unit,
    onLoadPowerampFolders: (Context) -> Unit
) {
    val context = LocalContext.current
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        BasicSettings(
            label = stringResource(R.string.settings_send_to_poweramp_label),
            description = stringResource(R.string.settings_send_to_poweramp_description),
            onClick = { onSendLyricsToPowerampChange(!sendLyricsToPoweramp) }
        ) { interactionSource ->
            val accessibilityLabel = (if (sendLyricsToPoweramp) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_send_to_poweramp_label)}"
            }
            Switch(
                checked = sendLyricsToPoweramp, onCheckedChange = onSendLyricsToPowerampChange,
                interactionSource = interactionSource,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel })
        }
        BasicSettings(
            label = stringResource(R.string.settings_save_as_file_label),
            description = stringResource(R.string.settings_save_as_file_description),
            onClick = { onSaveAsFileChange(!saveAsFile) }
        ) { interactionSource ->
            val accessibilityLabel = (if (saveAsFile) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_save_as_file_label)}"
            }
            Switch(
                checked = saveAsFile, onCheckedChange = onSaveAsFileChange,
                interactionSource = interactionSource,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel })
        }
        AnimatedVisibility(visible = saveAsFile) {
            BasicSettings(
                label = stringResource(R.string.settings_save_id_tags_in_lrc_file_label),
                description = stringResource(R.string.settings_save_id_tags_in_lrc_file_description),
                onClick = { onSaveIdTagsInFileChange(!saveIdTagsInFile) }
            ) { interactionSource ->
                val accessibilityLabel = (if (saveIdTagsInFile) stringResource(R.string.disable)
                else stringResource(R.string.enable)).let {
                    "$it ${stringResource(R.string.settings_save_id_tags_in_lrc_file_label)}"
                }
                Switch(
                    checked = saveIdTagsInFile, onCheckedChange = onSaveIdTagsInFileChange,
                    interactionSource = interactionSource,
                    modifier = Modifier.semantics { contentDescription = accessibilityLabel }
                )
            }
        }

        BasicSettings(
            label = stringResource(R.string.settings_embed_into_song_file_label),
            description = stringResource(R.string.settings_embed_into_song_file_description),
            onClick = { onEmbedIntoFileChange(!embedIntoFile) }
        ) { interactionSource ->
            val accessibilityLabel =
                stringResource(if (embedIntoFile) R.string.disable else R.string.enable).let {
                    "$it ${stringResource(R.string.settings_embed_into_song_file_label)}"
                }
            Switch(
                checked = embedIntoFile, onCheckedChange = onEmbedIntoFileChange,
                interactionSource = interactionSource,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel })
        }

        BasicSettings(
            label = stringResource(R.string.settings_fix_song_metadata_label),
            description = stringResource(R.string.settings_fix_song_metadata_description),
            onClick = { onFixSongMetadataChange(!fixSongMetadata) }
        ) { interactionSource ->
            val accessibilityLabel =
                stringResource(if (fixSongMetadata) R.string.disable else R.string.enable).let {
                    "$it ${stringResource(R.string.settings_fix_song_metadata_label)}"
                }
            Switch(
                checked = fixSongMetadata, onCheckedChange = onFixSongMetadataChange,
                interactionSource = interactionSource,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel }
            )
        }

        LaunchedEffect(Unit) {
            onLoadPowerampFolders(context)
        }

        if (powerampFolders.isNotEmpty()) {
            SettingsGroup {
                Text(
                    text = stringResource(R.string.settings_poweramp_folders),
                    style = MaterialTheme.typography.titleMedium
                )
                powerampFolders.forEach { folder ->
                    val normalizedPath = StorageHelper.normalizeToDocumentId(folder.path)
                    if (normalizedPath != null) {
                        val folderAccessState = rememberFolderAccess(normalizedPath)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    folder.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (folderAccessState.hasPermission) {
                                IconButton(
                                    enabled = folderAccessState.isRemovable,
                                    onClick = folderAccessState::revokeAccess
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.settings_add_folder_button_remove),
                                        tint = Color.Red.copy(alpha = if (folderAccessState.isRemovable) 0.7f else 0.4f)
                                    )
                                }
                            } else {
                                TextButton(onClick = folderAccessState::requestAccess) {
                                    Text(stringResource(R.string.settings_add_folder_button_grant_access))
                                }
                            }
                        }
                    }
                }
            }
        }

        val pickFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it, FolderAccessState.PERMISSION_MODE_FLAGS
                )
                onSaveNewUri(uri)
            }
        }
        val onPermissionRequest = {
            try {
                pickFolderLauncher.launch(null)
            } catch (e: Exception) {
                Log.e(TAG, "LyricsStorageSettings: request failed", e)
                context.makeToast(R.string.failed_to_open_folder_picker)
            }
        }
        SettingsGroup {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_add_custom_folder_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPermissionRequest) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = stringResource(R.string.settings_add_folder_add_new),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (savedUris.isNotEmpty()) {
                val addedUris by remember(savedUris, powerampFolders) {
                    derivedStateOf {
                        savedUris.filter { uri ->
                            powerampFolders.none { it.path.removeSuffix("/") == uri.getTreeDocumentId() }
                        }
                    }
                }
                for ((i, uri) in addedUris.withIndex()) {
                    val path by remember(uri) { derivedStateOf { uri.getTreeDocumentId() } }
                    val folderAccessState = rememberFolderAccess(path)
                    val onPermissionRequest = { folderAccessState.requestAccess() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${i + 1}.", modifier = Modifier.padding(end = 8.dp))
                        Text(
                            path,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (!folderAccessState.hasPermission) {
                            TextButton(onClick = onPermissionRequest) {
                                Text(stringResource(R.string.settings_add_folder_button_grant_access))
                            }
                        }
                        IconButton(onClick = folderAccessState::revokeAccess) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.settings_add_folder_button_remove),
                                tint = Color.Red.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_add_folder_empty_list),
                )
            }
        }

        accessRequestedPath?.let { pathUri ->
            PermissionDialog(
                explanation = stringResource(
                    R.string.settings_add_folder_permission_explanation,
                    pathUri.getTreeDocumentId()
                ),
                allowToSuppress = false,
                onConfirm = { pickFolderLauncher.launch(pathUri) },
                onDismiss = {
                    onAccessRequestedPathChange(null)
                    Log.w(TAG, "LyricsStorageSettings: user ignored storage access request")
                }
            )
        }
    }
}

@Composable
fun LyricsStorageSettings(
    modifier: Modifier = Modifier, topbar: @Composable (() -> Unit), viewmodel: SettingsViewModel
) {
    val sendLyricsToPoweramp by viewmodel.sendLyricsToPoweramp.collectAsStateWithLifecycle()
    val saveAsFile by viewmodel.saveAsFile.collectAsStateWithLifecycle()
    val saveIdTagsInFile by viewmodel.saveIdTagsInFile.collectAsStateWithLifecycle()
    val embedIntoFile by viewmodel.embedLyricsIntoFile.collectAsStateWithLifecycle()
    val fixSongMetadata by viewmodel.fixSongMetadata.collectAsStateWithLifecycle()
    val accessRequestedPath by viewmodel.accessRequestedPath.collectAsStateWithLifecycle()
    val savedUris by viewmodel.savedUris.collectAsStateWithLifecycle()
    val powerampFolders by viewmodel.powerampFolders.collectAsStateWithLifecycle()

    LyricsStorageSettingsContent(
        modifier = modifier,
        topbar = topbar,
        sendLyricsToPoweramp = sendLyricsToPoweramp,
        onSendLyricsToPowerampChange = viewmodel::setSendLyricsToPoweramp,
        saveAsFile = saveAsFile,
        onSaveAsFileChange = viewmodel::setSaveAsFile,
        saveIdTagsInFile = saveIdTagsInFile,
        onSaveIdTagsInFileChange = viewmodel::setSaveIdTagsInFile,
        embedIntoFile = embedIntoFile,
        fixSongMetadata = fixSongMetadata,
        onFixSongMetadataChange = viewmodel::setFixSongMetadata,
        onEmbedIntoFileChange = viewmodel::setEmbedLyricsIntoFile,
        powerampFolders = powerampFolders,
        savedUris = savedUris,
        accessRequestedPath = accessRequestedPath,
        onAccessRequestedPathChange = viewmodel::setAccessRequestedPath,
        onSaveNewUri = viewmodel::saveNewUri,
        onLoadPowerampFolders = viewmodel::loadPowerampFolders
    )
}

@Preview(showSystemUi = true)
@Composable
private fun LyricsStorageSettingsPreview() {
    var sendLyricsToPoweramp by remember { mutableStateOf(true) }
    var saveAsFile by remember { mutableStateOf(false) }
    var saveIdTagsInFile by remember { mutableStateOf(false) }
    var embedIntoFile by remember { mutableStateOf(false) }
    var fixSongMetadata by remember { mutableStateOf(false) }
    LyricsForPowerAmpTheme {
        LyricsStorageSettingsContent(
            topbar = { TopAppBar(title = { Text("Lyrics Storage Settings") }) },
            sendLyricsToPoweramp = sendLyricsToPoweramp,
            onSendLyricsToPowerampChange = { sendLyricsToPoweramp = it },
            saveAsFile = saveAsFile,
            onSaveAsFileChange = { saveAsFile = it },
            saveIdTagsInFile = saveIdTagsInFile,
            onSaveIdTagsInFileChange = { saveIdTagsInFile = it },
            embedIntoFile = embedIntoFile,
            onEmbedIntoFileChange = { embedIntoFile = it },
            fixSongMetadata = fixSongMetadata,
            onFixSongMetadataChange = { fixSongMetadata = it },
            powerampFolders = listOf(
                PowerampFolder(1, "Music", "primary:Music"),
                PowerampFolder(2, "Downloads", "primary:Download")
            ),
            savedUris = emptyList(),
            accessRequestedPath = null,
            onAccessRequestedPathChange = {},
            onSaveNewUri = {},
            onLoadPowerampFolders = {}
        )
    }
}
