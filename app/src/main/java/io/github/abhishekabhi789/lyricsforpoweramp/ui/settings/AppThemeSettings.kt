package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
fun AppThemeSettings(modifier: Modifier = Modifier, viewmodel: SettingsViewModel) {
    SettingsGroup(
        modifier = modifier,
        title = stringResource(id = R.string.settings_app_theme_label),
        icon = Icons.Default.ColorLens
    ) {
        val context = LocalContext.current
        var expanded by remember { mutableStateOf(false) }
        var currentTheme by remember { mutableStateOf(AppPreference.getTheme(context)) }
        val allThemes = remember { AppPreference.getThemes() }
        BasicSettings(label = stringResource(R.string.settings_app_theme_description)) {
            DropdownSettings(
                modifier = Modifier,
                expanded = expanded,
                currentValue = currentTheme,
                values = allThemes,
                onSelection = { selectedTheme ->
                    currentTheme = selectedTheme
                    AppPreference.setTheme(context, selectedTheme)
                    viewmodel.updateTheme(selectedTheme)
                },
                onExpandedChanged = { expanded = it },
                getLabel = { theme -> stringResource(theme.label) }
            )
        }
    }
}
