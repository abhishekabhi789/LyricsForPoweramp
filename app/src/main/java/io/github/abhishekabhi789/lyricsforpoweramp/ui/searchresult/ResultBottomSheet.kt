package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingState
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultBottomSheet(
    lyricsSavingState: LyricsSavingState,
    grantAccess: () -> Unit,
    onDismiss: () -> Unit,
    onFinish: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val timeout = 3.seconds
    val sendToPoweramp by remember(lyricsSavingState) { derivedStateOf { lyricsSavingState.sendToPoweramp } }
    val saveToStorage by remember(lyricsSavingState) { derivedStateOf { lyricsSavingState.saveAsFile } }
    val embedIntoFile by remember(lyricsSavingState) { derivedStateOf { lyricsSavingState.embedIntoFile } }
    val fixSongMetadata by remember(lyricsSavingState) { derivedStateOf { lyricsSavingState.fixSongMetaData } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min)
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = lyricsSavingState.progress,
                animationSpec = tween(1000)
            )
            VerticalProgressBar(progress = animatedProgress)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                StepIndicator(stringResource(R.string.lyrics_save_search_result), true)
                if (!sendToPoweramp.shouldPerform && !saveToStorage.shouldPerform && !embedIntoFile.shouldPerform) {
                    StepIndicator(
                        stringResource(R.string.notification_no_saving_method_description),
                        false
                    )
                }
                if (sendToPoweramp.shouldPerform) {
                    when (sendToPoweramp.result) {
                        true, false -> StepIndicator(
                            text = stringResource(R.string.lyrics_sent_to_pa),
                            isCompleted = sendToPoweramp.result
                        )

                        null -> StepIndicator(
                            text = stringResource(R.string.lyrics_send_to_pa), false
                        )
                    }
                }
                if (saveToStorage.shouldPerform) {
                    val label = when (saveToStorage.result) {
                        StorageHelper.Result.SUCCESS -> R.string.lyrics_saved_to_storage
                        else -> saveToStorage.result?.messageResId
                    }

                    saveToStorage.result?.messageResId?.let { stringResource(it) }
                    StepIndicator(
                        text = stringResource(label ?: R.string.settings_save_as_file_label),
                        isCompleted = saveToStorage.result?.let { it == StorageHelper.Result.SUCCESS },
                        actionLabel = if (saveToStorage.result == StorageHelper.Result.NO_PERMISSION)
                            stringResource(R.string.settings_add_folder_button_grant_access) else null,
                        onAction = if (saveToStorage.result == StorageHelper.Result.NO_PERMISSION)
                            grantAccess else null
                    )
                }
                if (embedIntoFile.shouldPerform) {
                    val label = when (embedIntoFile.result) {
                        StorageHelper.Result.SUCCESS -> R.string.lyrics_embedded_to_track
                        else -> embedIntoFile.result?.messageResId
                    }
                    StepIndicator(
                        text = stringResource(
                            label ?: R.string.settings_embed_into_song_file_label
                        ),
                        isCompleted = embedIntoFile.result?.let { it == StorageHelper.Result.SUCCESS },
                        actionLabel = if (embedIntoFile.result == StorageHelper.Result.NO_PERMISSION)
                            stringResource(R.string.settings_add_folder_button_grant_access) else null,
                        onAction = if (embedIntoFile.result == StorageHelper.Result.NO_PERMISSION) grantAccess else null
                    )
                }

                if (fixSongMetadata.shouldPerform) {
                    val label = when (fixSongMetadata.result) {
                        StorageHelper.Result.SUCCESS -> R.string.lyrics_metadata_fixed
                        else -> fixSongMetadata.result?.messageResId
                    }
                    StepIndicator(
                        text = stringResource(
                            label ?: R.string.settings_fix_song_metadata_label
                        ),
                        isCompleted = fixSongMetadata.result?.let { it == StorageHelper.Result.SUCCESS },
                        actionLabel = if (fixSongMetadata.result == StorageHelper.Result.NO_PERMISSION)
                            stringResource(R.string.settings_add_folder_button_grant_access) else null,
                        onAction = if (fixSongMetadata.result == StorageHelper.Result.NO_PERMISSION) grantAccess else null
                    )
                }

                val completed: Boolean? by remember(lyricsSavingState) {
                    derivedStateOf {
                        if (lyricsSavingState.progress == 1f) return@derivedStateOf true
                        val sendToPa: Boolean? =
                            if (sendToPoweramp.shouldPerform) sendToPoweramp.result else true
                        val saveToFile: Boolean? = if (saveToStorage.shouldPerform) {
                            if (saveToStorage.result == null) null else saveToStorage.result == StorageHelper.Result.SUCCESS
                        } else true
                        val embedIntoFile: Boolean? = if (embedIntoFile.shouldPerform) {
                            if (embedIntoFile.result == null) null else embedIntoFile.result == StorageHelper.Result.SUCCESS
                        } else true
                        return@derivedStateOf listOf(sendToPa, saveToFile, embedIntoFile).run {
                            when {
                                any { it == null } -> null
                                any { it == false } -> false
                                all { it == true } -> true
                                else -> null
                            }
                        }
                    }
                }
                LaunchedEffect(completed) {
                    if (completed == true) {
                        PowerampApiHelper.refreshPowerampLibrary(context)
                    }
                }
                StepIndicator(stringResource(R.string.done), completed)
            }
        }
        AnimatedVisibility(onFinish != null && lyricsSavingState.progress == 1f) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
                    .padding(horizontal = 24.dp)
            ) {
                val durationSaver = Saver<Duration, Long>(
                    save = { it.inWholeSeconds },
                    restore = { it.seconds }
                )
                var exitTimeout by rememberSaveable(stateSaver = durationSaver) {
                    mutableStateOf(timeout)
                }
                LaunchedEffect(Unit) {
                    while (exitTimeout > Duration.ZERO) {
                        delay(1.seconds)
                        exitTimeout = exitTimeout.minus(1.seconds)
                    }
                    onFinish?.invoke()
                }
                Text(
                    stringResource(R.string.closing_with_timeout, exitTimeout.inWholeSeconds),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun VerticalProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(8.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(progress)
                .width(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
fun StepIndicator(
    text: String,
    isCompleted: Boolean? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading circle rotation"
    )
    val color by animateColorAsState(
        when (isCompleted) {
            true -> MaterialTheme.colorScheme.primary
            false -> MaterialTheme.colorScheme.error
            null -> Color.Gray
        }
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = when (isCompleted) {
                true -> Icons.Default.CheckCircle
                false -> Icons.Default.Error
                null -> ImageVector.vectorResource(R.drawable.ic_loading_circle)
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = if (isCompleted == null) rotation else 0f }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = color, modifier = Modifier.weight(1f))
        actionLabel?.let {
            TextButton(onClick = { onAction?.invoke() }) {
                Text(it)
            }
        }
    }
}
