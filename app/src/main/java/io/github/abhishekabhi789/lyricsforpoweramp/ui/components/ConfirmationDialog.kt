package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.abhishekabhi789.lyricsforpoweramp.R

@Composable
fun ConfirmationDialog(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null) },
        title = { Text(title) },
        text = { Text(description) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        },
        modifier = modifier
    )
}
