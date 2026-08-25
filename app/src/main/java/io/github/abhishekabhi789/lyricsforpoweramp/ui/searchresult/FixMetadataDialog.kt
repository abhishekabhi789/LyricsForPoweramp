package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kyant.taglib.PropertyMap
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.TaglibHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixMetadataDialog(
    modifier: Modifier = Modifier,
    taglibSession: TaglibHelper.TagLibSession,
    lyrics: Lyrics,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val metadata by produceState(initialValue = PropertyMap(), taglibSession) {
        value = withContext(Dispatchers.IO) {
            taglibSession.getMetadata() ?: PropertyMap()
        }
    }
    val titleCached: String by remember(metadata) {
        derivedStateOf { metadata[TaglibHelper.KEY_TITLE]?.firstOrNull() ?: "" }
    }
    val artistCached: String by remember(metadata) {
        derivedStateOf { metadata[TaglibHelper.KEY_ARTIST]?.firstOrNull() ?: "" }
    }
    val albumCached: String by remember(metadata) {
        derivedStateOf { metadata[TaglibHelper.KEY_ALBUM]?.firstOrNull() ?: "" }
    }

    val newMetadata = remember(metadata) { PropertyMap(metadata) }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val surfaceElevation = 6.dp
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = surfaceElevation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                var topPadding by remember { mutableStateOf(Dp.Hairline) }
                var bottomPadding by remember { mutableStateOf(Dp.Hairline) }

                LazyColumn(
                    contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    item {
                        MetadataField(
                            fieldName = stringResource(R.string.input_track_title_label),
                            savedValue = titleCached,
                            suggestedValue = lyrics.trackName,
                            onUpdate = { newMetadata[TaglibHelper.KEY_TITLE] = arrayOf(it) })
                    }
                    item {
                        MetadataField(
                            fieldName = stringResource(R.string.input_track_artists_label),
                            savedValue = artistCached,
                            suggestedValue = lyrics.artistName ?: "",
                            onUpdate = { newMetadata[TaglibHelper.KEY_ARTIST] = arrayOf(it) })
                    }
                    item {
                        MetadataField(
                            fieldName = stringResource(R.string.input_track_album_label),
                            savedValue = albumCached,
                            suggestedValue = lyrics.albumName ?: "",
                            onUpdate = { newMetadata[TaglibHelper.KEY_ALBUM] = arrayOf(it) })
                    }
                }
                Text(
                    text = stringResource(R.string.edit_metadata),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(
                                surfaceElevation
                            )
                        )
                        .align(Alignment.TopStart)
                        .onGloballyPositioned {
                            topPadding = with(density) { it.size.height.toDp() }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)

                )
                val onSaveClick = suspend {
                    if (taglibSession.setMetadata(newMetadata)) {
                        taglibSession.saveModifiedFile()
                        context.makeToast(R.string.saved)
                        PowerampApiHelper.refreshPowerampLibrary(context)
                        onDismiss()
                    } else context.makeToast(R.string.failed)
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(
                                surfaceElevation
                            )
                        )
                        .align(Alignment.BottomEnd)
                        .onGloballyPositioned {
                            bottomPadding = with(density) { it.size.height.toDp() }
                        }
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    TextButton(onClick = { scope.launch { onSaveClick() } }) {
                        Text(stringResource(id = R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataField(
    modifier: Modifier = Modifier,
    fieldName: String,
    savedValue: String,
    suggestedValue: String,
    onUpdate: (newValue: String) -> Unit
) {
    val textFieldState = rememberTextFieldState(initialText = savedValue)
    ElevatedCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(fieldName, style = MaterialTheme.typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (savedValue.isNotEmpty()) {
                    SuggestedField(
                        label = "Saved $fieldName",
                        value = savedValue,
                        onClick = { textFieldState.setTextAndPlaceCursorAtEnd(savedValue) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (suggestedValue.isNotEmpty()) {
                    SuggestedField(
                        label = "Suggested $fieldName",
                        value = suggestedValue,
                        onClick = { textFieldState.setTextAndPlaceCursorAtEnd(suggestedValue) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                label = { Text("New $fieldName") },
                state = textFieldState,
                trailingIcon = {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.clearText() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_field, fieldName)
                            )
                        }
                    }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth()
            )
            LaunchedEffect(textFieldState.text) {
                onUpdate(textFieldState.text.toString())
            }
        }
    }
}

@Composable
fun SuggestedField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(modifier) {
        Surface(
            onClick = onClick,
            shape = SuggestionChipDefaults.shape,
            border = SuggestionChipDefaults.suggestionChipBorder(enabled = value.isNotEmpty()),
            color = Color.Transparent,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value)
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .padding(top = 4.dp)
                .padding(start = 12.dp)
                .background(CardDefaults.elevatedCardColors().containerColor)
                .padding(horizontal = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMetadataField() {
    LyricsForPowerAmpTheme {
        MetadataField(
            fieldName = "Track Title",
            savedValue = "Saved Title",
            suggestedValue = "New Title",
            onUpdate = {})
    }
}
