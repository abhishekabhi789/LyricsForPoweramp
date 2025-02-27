package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.InterpreterMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.CustomChip
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricItem(
    modifier: Modifier = Modifier,
    lyrics: Lyrics,
    isLaunchedFromPowerAmp: Boolean,
    onLyricChosen: (Lyrics, preferredLyricsType: LyricsType) -> Unit
) {
    val context = LocalContext.current
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
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
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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
                    text = lyrics.artistName,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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
                    text = lyrics.albumName,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(verticalArrangement = Arrangement.Bottom) {
                    if (lyrics.plainLyrics != null) {
                        CustomChip(
                            label = stringResource(R.string.plain_lyrics_short),
                            selected = lyricPages[pagerState.currentPage] == lyrics.plainLyrics,
                            tooltipDescription = stringResource(R.string.result_type_plain_description),
                            icon = R.drawable.ic_plain_lyrics
                        ) { scope.launch { pagerState.animateScrollToPage(0) } }
                    }
                    if (lyrics.syncedLyrics != null) {
                        CustomChip(
                            label = stringResource(R.string.synced_lyrics_short),
                            selected = lyricPages[pagerState.currentPage] == lyrics.syncedLyrics,
                            tooltipDescription = stringResource(R.string.result_type_synced_description),
                            icon = R.drawable.ic_synced_lyrics
                        ) { scope.launch { pagerState.animateScrollToPage(lyricPages.lastIndex) } }
                    }
                }
                if (isLaunchedFromPowerAmp) {
                    val preferredLyricsType by rememberSaveable {
                        mutableStateOf(AppPreference.getPreferredLyricsType(context))
                    }
                    var preferredLyricsTypeName = stringResource(preferredLyricsType.label)
                    val availableLyrics = buildList {
                        if (!lyrics.syncedLyrics.isNullOrBlank()) add(LyricsType.SYNCED.label)
                        if (!lyrics.plainLyrics.isNullOrBlank()) add(LyricsType.PLAIN.label)
                        if (lyrics.instrumental == true) add(R.string.mark_this_track_instrumental)
                    }.map { stringResource(it) }

                    val chosenType = when {
                        preferredLyricsTypeName in availableLyrics -> preferredLyricsTypeName
                        else -> availableLyrics.first()
                    }

                    SendLyricsButton(
                        availableLyricsTypes = availableLyrics,
                        preferredLyricsType = chosenType,
                        onTypeChanged = { preferredLyricsTypeName = it }
                    ) { onLyricChosen(lyrics, preferredLyricsType) }
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
                    val lyricsInView by remember(pageIndex) { derivedStateOf { lyricPages[pageIndex] } }
                    Text(
                        text = if (expanded) lyricsInView
                        else lyricsInView.lines().run {
                            subList(0, size.coerceAtMost(6)).joinToString("\n")
                        },
                        style = TextStyle(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable(interactionSource = null, indication = null) {
                                expanded = !expanded
                            }
                    )
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
                            alpha =
                                abs(1 - abs(pagerState.currentPageOffsetFraction * 2)).coerceIn(
                                    0f,
                                    1f
                                )
                        }
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)

                )

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendLyricsButton(
    modifier: Modifier = Modifier,
    preferredLyricsType: String,
    availableLyricsTypes: List<String>,
    onTypeChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = AssistChipDefaults.shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.clickable { onSend() }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = preferredLyricsType,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ExposedDropdownMenuBox(
                modifier = modifier,
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                ) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    availableLyricsTypes.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(text = value) },
                            colors = MenuDefaults.itemColors()
                                .copy(
                                    textColor = if (value == preferredLyricsType)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                ),
                            onClick = {
                                onTypeChanged(value)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewLyricItem() {
    val data = Lyrics(
        trackName = "Fireworks (feat. Moss Kena & The knocks) [Breakbot & Irfane Remix]",
        artistName = "Artists Name 1",
        albumName = "Album Name 1",
        duration = 200.0,
        instrumental = false,
        plainLyrics = "1 Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n Nunc sit amet turpis et odio egestas finibus vel quis nisi.\n Duis aliquam tortor non dui tempor, et sodales orci tempus.\n Mauris fermentum mauris quis commodo viverra.\n Suspendisse scelerisque lorem eu dolor fringilla ultrices.\n Suspendisse scelerisque lorem eu dolor fringilla ultrices.\n Suspendisse scelerisque lorem eu dolor fringilla ultrices.",
        syncedLyrics = "[00:10.00] 1 Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n [00:20.10] Nunc sit amet turpis et odio egestas finibus vel quis nisi.\n [00:30.20] Duis aliquam tortor non dui tempor, et sodales orci tempus.\n [00:40.30] Mauris fermentum mauris quis commodo viverra.\n [00:50.40] Suspendisse scelerisque lorem eu dolor fringilla ultrices.\n [01:00.50] Suspendisse scelerisque lorem eu dolor fringilla ultrices.\n [01:10.00] Suspendisse scelerisque lorem eu dolor fringilla ultrices."
    )
    LyricItem(lyrics = data, isLaunchedFromPowerAmp = true, onLyricChosen = { _, _ -> })
}
