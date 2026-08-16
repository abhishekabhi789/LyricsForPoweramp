package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.InterpreterMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.CustomChip
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LyricItem(
    modifier: Modifier = Modifier,
    lyrics: Lyrics,
    trackDuration: Int? = null,
    isLaunchedFromPowerAmp: Boolean,
    preferredLyricsType: LyricsType,
    fixSongMetaData: Boolean,
    onLyricChosen: (preferredLyricsType: LyricsType, fixMetadata: Boolean) -> Unit,
    onEditLyrics: (preferredLyricsType: LyricsType) -> Unit,
    onFixMetadata: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val lyricPages = remember(lyrics) { listOfNotNull(lyrics.plainLyrics, lyrics.syncedLyrics) }
    val pagerState = rememberPagerState(pageCount = { lyricPages.size }, initialPage = 0)
    var expanded by remember { mutableStateOf(false) }
    val defaultCardColor = CardDefaults.elevatedCardColors().containerColor

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors()
            .copy(containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else defaultCardColor),
        modifier = modifier.scale(if (expanded) 1.01f else 1.0f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(8.dp)
        ) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                        Text(
                            text = lyrics.trackName,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    lyrics.artistName?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.InterpreterMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    lyrics.albumName?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
                Text(
                    text = lyrics.getFormattedDuration(),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
                trackDuration?.takeIf { it > 0 }?.let { duration ->
                    Spacer(Modifier.width(8.dp))
                    val difference = remember(duration, lyrics.duration) {
                        lyrics.duration.toInt() - duration
                    }
                    val bgColor = MaterialTheme.colorScheme.secondaryContainer
                    Surface(
                        color = bgColor.copy(alpha = 0.3f),
                        contentColor = contentColorFor(bgColor),
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = 2.dp,
                        border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            if (difference == 0) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = stringResource(R.string.result_same_duration),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                val text = buildString {
                                    append(if (difference > 0) '+' else '-')
                                    append(abs(difference))
                                    append('s')
                                }
                                Text(text = text, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    if (lyrics.plainLyrics != null) {
                        CustomChip(
                            label = stringResource(R.string.plain_lyrics_short),
                            selected = lyricPages.getOrNull(pagerState.currentPage) == lyrics.plainLyrics,
                            tooltipDescription = stringResource(R.string.result_type_plain_description),
                            icon = R.drawable.ic_plain_lyrics
                        ) { scope.launch { pagerState.animateScrollToPage(0) } }
                    }
                    if (lyrics.syncedLyrics != null) {
                        CustomChip(
                            label = stringResource(R.string.synced_lyrics_short),
                            selected = lyricPages.getOrNull(pagerState.currentPage) == lyrics.syncedLyrics,
                            tooltipDescription = stringResource(R.string.result_type_synced_description),
                            icon = R.drawable.ic_synced_lyrics
                        ) { scope.launch { pagerState.animateScrollToPage(lyricPages.lastIndex) } }
                    }
                }
                if (isLaunchedFromPowerAmp) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        val availableLyrics = remember(lyrics) {
                            buildList {
                                if (lyrics.syncedLyrics != null) add(LyricsType.SYNCED)
                                if (lyrics.plainLyrics != null) add(LyricsType.PLAIN)
                                if (lyrics.instrumental == true) add(LyricsType.INSTRUMENTAL)
                            }
                        }
                        val showLyricsAction by remember(lyrics) {
                            derivedStateOf {
                                lyrics.instrumental == false && (!lyrics.syncedLyrics.isNullOrEmpty() || !lyrics.plainLyrics.isNullOrEmpty())
                            }
                        }
                        if (showLyricsAction) {
                            LyricsAction(
                                actionIcon = Icons.Default.Edit,
                                actionLabel = stringResource(R.string.edit_lyrics),
                                availableLyricsTypes = availableLyrics,
                                preferredLyricsType = preferredLyricsType,
                                onConfirm = { type, _ -> onEditLyrics(type) }
                            )
                            LyricsAction(
                                actionIcon = Icons.Default.Save,
                                actionLabel = stringResource(R.string.save),
                                availableLyricsTypes = availableLyrics,
                                preferredLyricsType = preferredLyricsType,
                                fixSongMetaData = fixSongMetaData,
                                selected = true,
                                onConfirm = onLyricChosen
                            )
                            CustomChip(
                                label = stringResource(R.string.edit_metadata),
                                icon = Icons.Default.Code,
                                onClick = onFixMetadata
                            )
                        }
                    }
                }
            }
        }
        if (lyricPages.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(4.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                HorizontalPager(
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.animateContentSize()
                ) { pageIndex ->
                    val lyricsInView by remember(pageIndex) {
                        derivedStateOf { lyricPages.getOrNull(pageIndex) ?: "" }
                    }
                    SelectionContainer {
                        Text(
                            text = lyricsInView,
                            maxLines = if (expanded) Int.MAX_VALUE else 6,
                            style = TextStyle(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable(interactionSource = null, indication = null) {
                                    expanded = !expanded
                                })
                    }
                }

                val rotationAnimation = animateFloatAsState(
                    targetValue = if (expanded) -180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "expand icon rotation animation"
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 12.dp, top = 8.dp)
                        .graphicsLayer {
                            translationY = -60 * abs(pagerState.currentPageOffsetFraction)
                            rotationZ = rotationAnimation.value
                            alpha = abs(1 - abs(pagerState.currentPageOffsetFraction * 2))
                                .coerceIn(0f, 1f)
                        }
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LyricsAction(
    actionIcon: ImageVector,
    actionLabel: String,
    availableLyricsTypes: List<LyricsType>,
    selected: Boolean = false,
    preferredLyricsType: LyricsType,
    fixSongMetaData: Boolean? = null,
    onConfirm: (chosenType: LyricsType, fixMetadata: Boolean) -> Unit,
) {
    var fixMetadataChecked by rememberSaveable(fixSongMetaData) {
        mutableStateOf(fixSongMetaData)
    }
    var showDialog by remember { mutableStateOf(false) }
    val invokeAction = {
        if (availableLyricsTypes.size > 1) {
            showDialog = true
        } else {
            onConfirm(availableLyricsTypes.first(), fixMetadataChecked ?: false)
        }
    }
    CustomChip(
        label = actionLabel,
        icon = actionIcon,
        onClick = invokeAction,
        selected = selected
    )
    if (showDialog) {
        val dialogShape = MaterialTheme.shapes.large
        BasicAlertDialog(
            onDismissRequest = { showDialog = false }, modifier = Modifier.background(
                MaterialTheme.colorScheme.background, shape = dialogShape
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$actionLabel lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.result_action_dialog_common_description))
                fixMetadataChecked?.let { fixMetaData ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .clickable { fixMetadataChecked = !fixMetaData }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Checkbox(checked = fixMetaData, onCheckedChange = null)
                        Text(stringResource(R.string.settings_fix_song_metadata_label))
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp, Alignment.CenterHorizontally
                    ), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableLyricsTypes.forEach { lyricsType ->
                        InputChip(
                            selected = lyricsType == preferredLyricsType,
                            onClick = {
                                onConfirm(lyricsType, fixMetadataChecked ?: false)
                                showDialog = false
                            },
                            label = { Text(stringResource(lyricsType.longLabelResId)) })
                    }
                    OutlinedButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewLyricItem() {
    val lyrics = LoremIpsum(words = 20).values.joinToString(" ")
    val data = Lyrics(
        trackName = "Track Name",
        artistName = "Artists Name",
        albumName = "Album Name",
        duration = 200.0,
        instrumental = false,
        plainLyrics = lyrics,
        syncedLyrics = lyrics
    )
    LyricItem(
        lyrics = data,
        trackDuration = 200,
        isLaunchedFromPowerAmp = true,
        preferredLyricsType = LyricsType.SYNCED,
        fixSongMetaData = true,
        onLyricChosen = { _, _-> },
        onEditLyrics = {},
        onFixMetadata = {})
}
