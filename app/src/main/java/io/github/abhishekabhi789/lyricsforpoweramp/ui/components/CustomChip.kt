package io.github.abhishekabhi789.lyricsforpoweramp.ui.components

import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomChip(
    label: String,
    selected: Boolean,
    tooltipDescription: String,
    icon: Any?,
    onClick: () -> Unit = {}
) {
    BasicTooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberBasicTooltipState(),
        tooltip = {
            ElevatedCard(
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CardDefaults.shape
                )
            ) {
                Text(
                    text = tooltipDescription,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }) {
        FilterChip(
            onClick = { onClick() },
            label = { Text(label) },
            leadingIcon = {
                if (icon != null) {
                    if (icon is Int)
                        Icon(
                            painterResource(id = icon),
                            contentDescription = null,
                            Modifier.size(FilterChipDefaults.IconSize)
                        )
                    else Icon(
                        imageVector = icon as ImageVector,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            },
            colors = MaterialTheme.colorScheme.let { cs ->
                FilterChipDefaults.filterChipColors(
                    containerColor = cs.surfaceContainer,
                    labelColor = cs.onSurface,
                    iconColor = cs.onSurface,
                    selectedContainerColor = cs.primaryContainer,
                    selectedLabelColor = cs.onPrimaryContainer,
                    selectedLeadingIconColor = cs.onPrimaryContainer
                )
            },
            selected = selected,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChip() {
    Row {
        CustomChip(
            label = stringResource(R.string.plain_lyrics),
            selected = true,
            tooltipDescription = stringResource(R.string.result_type_plain_description),
            icon = R.drawable.ic_plain_lyrics,
            onClick = {}
        )
        CustomChip(
            label = stringResource(R.string.synced_lyrics),
            selected = false,
            tooltipDescription = stringResource(R.string.result_type_synced_description),
            icon = R.drawable.ic_synced_lyrics,
            onClick = {}
        )
    }
}
