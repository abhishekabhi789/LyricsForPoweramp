package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.abhishekabhi789.lyricsforpoweramp.R

@Composable
fun PermissionDialog(
    modifier: Modifier = Modifier,
    explanation: String,
    allowToSuppress: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: (suppress: Boolean) -> Unit
) {
    var disableNotification by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        modifier = modifier,
        icon = { Icon(Icons.Default.Security, null) },
        title = { Text(stringResource(R.string.settings_permission_dialog_title)) },
        text = {
            Column {
                Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
                if (allowToSuppress) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = disableNotification,
                            onCheckedChange = { disableNotification = it })
                        Text(stringResource(R.string.settings_permission_dialog_disable_notification))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !disableNotification) {
                Text(stringResource(R.string.settings_permission_dialog_ask))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(disableNotification) }) {
                Text(stringResource(R.string.settings_permission_dialog_deny))
            }
        },
        onDismissRequest = { onDismiss(disableNotification) },
    )
}
