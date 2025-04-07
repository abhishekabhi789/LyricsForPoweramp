package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultBottomSheet(
    sentToPoweramp: Boolean?,
    saveToStorage: StorageHelper.Result?,
    grantAccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val showSendToPoweramp = remember { AppPreference.getSendLyricsToPoweramp(context) }
    val showSaveToStorage = remember { AppPreference.getSaveAsFile(context) }
    val savedToStorage by remember(saveToStorage) {
        derivedStateOf {
            if (saveToStorage != null) (saveToStorage == StorageHelper.Result.SUCCESS) else null
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min)
        ) {
            val progress by animateFloatAsState(
                targetValue = when {
                    showSendToPoweramp && showSaveToStorage -> {
                        when {
                            sentToPoweramp == true && savedToStorage == true -> 1f
                            sentToPoweramp == true || savedToStorage == true -> 0.5f
                            else -> 0f
                        }
                    }

                    showSendToPoweramp || showSaveToStorage -> {
                        when {
                            sentToPoweramp == true -> 1f
                            savedToStorage == true -> 1f
                            else -> 0.5f
                        }
                    }

                    else -> 1f
                },
                animationSpec = tween(1000)
            )
            VerticalProgressBar(progress = progress)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                StepIndicator(stringResource(R.string.sending_lyrics), true)
                if (showSendToPoweramp) {
                    StepIndicator(stringResource(R.string.lyrics_sent_successfully), sentToPoweramp)
                }
                if (showSaveToStorage) {
                    StepIndicator(
                        text = stringResource(
                            saveToStorage?.message ?: R.string.lyrics_save_to_storage
                        ),
                        isCompleted = savedToStorage,
                        actionLabel = if (savedToStorage == false)
                            stringResource(R.string.settings_save_as_file_button_grant_access) else null,
                        onAction = if (savedToStorage == false) grantAccess else null
                    )
                }
                StepIndicator(
                    stringResource(R.string.done),
                    if (sentToPoweramp == true && savedToStorage == true) true else null
                )
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
            else -> Color.Gray
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
                else -> ImageVector.vectorResource(R.drawable.ic_loading_circle)
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
