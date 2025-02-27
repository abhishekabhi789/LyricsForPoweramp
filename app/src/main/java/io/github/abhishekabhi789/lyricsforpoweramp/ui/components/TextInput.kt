package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import io.github.abhishekabhi789.lyricsforpoweramp.R

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    text: String?,
    isInputValid: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    clearWithoutWarn: Boolean = true,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val sizeScale by animateFloatAsState(
        if (isFocused) 1.025f else 1f,
        label = "searchButtonAnimation"
    )
    val color = MaterialTheme.colorScheme.let { if (isFocused) it.primary else it.secondary }
    var showClearWarningDialog: Boolean by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text ?: "",
        onValueChange = { onValueChange(it) },
        label = { Text(label) },
        singleLine = false,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.let {
                    if (isInputValid) color else it.error
                }
            )
        },
        trailingIcon = {
            if (text?.isNotEmpty() == true) {
                Icon(imageVector = Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.clear_field, label),
                    tint = color,
                    modifier = Modifier.clickable {
                        if (clearWithoutWarn) onValueChange("")
                        else showClearWarningDialog = true
                    }
                )
            }
        },
        supportingText = {
            if (!isInputValid) {
                Text(
                    text = stringResource(R.string.invalid_input_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        isError = !isInputValid,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
            capitalization = KeyboardCapitalization.Words
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .scale(sizeScale)
            .fillMaxWidth()
            .onFocusChanged { state -> isFocused = state.isFocused }
    )
    if (showClearWarningDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.clear_field_title),
            description = stringResource(R.string.input_clear_confirmation_message, label),
            onConfirm = { onValueChange("") },
            onDismiss = { showClearWarningDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTextInput() {
    TextInput(
        label = "Enter an input",
        icon = Icons.Outlined.BugReport,
        text = "Lorem Ipsum",
        isInputValid = false,
        onValueChange = {},
        onDone = {})
}
