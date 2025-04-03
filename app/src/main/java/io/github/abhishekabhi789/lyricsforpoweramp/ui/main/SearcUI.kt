package io.github.abhishekabhi789.lyricsforpoweramp.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.InterpreterMode
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.InputState.SearchMode
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.TextInput
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.MainActivityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUi(modifier: Modifier = Modifier, viewModel: MainActivityViewModel) {
    val isSearching by viewModel.isSearching.collectAsState()
    val inputState by viewModel.inputState.collectAsState()
    val isInputValid by viewModel.isInputValid.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val tabs = remember { SearchMode.entries }
    val pagerState = rememberPagerState(
        pageCount = { tabs.size },
        initialPage = tabs.indexOf(inputState.searchMode)
    )
    val pageScrollOffset by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }
    LaunchedEffect(Unit) {
        //to prevent calling requestFocus() before initializing textboxes
        viewModel.isInputValid.collectLatest { isInputValid ->
            if (!isInputValid) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        focusManager.clearFocus()
        viewModel.updateInputState(inputState.copy(searchMode = tabs[pagerState.currentPage]))
        if (!isInputValid) viewModel.clearInvalidInputError()
    }
    if (isSearching) {
        Dialog(
            onDismissRequest = { viewModel.abortSearch() },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            ) {
                CircularProgressIndicator()
            }
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = TopAppBarDefaults.topAppBarColors().containerColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { tabIndex, tab ->
                val selected by remember { derivedStateOf { pagerState.currentPage == tabIndex } }
                Tab(
                    text = { Text(stringResource(id = tab.label)) },
                    selected = selected,
                    onClick = {
                        viewModel.updateInputState(inputState.copy(searchMode = tab))
                        scope.launch {
                            pagerState.animateScrollToPage(tabIndex)
                            if (!isInputValid && inputState.searchMode != tab) {
                                viewModel.clearInvalidInputError()
                            }
                        }
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp)
        ) { pageIndex ->
            when (tabs[pageIndex]) {
                SearchMode.Coarse -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        TextInput(
                            label = stringResource(R.string.input_track_query_label),
                            icon = Icons.Outlined.Edit,
                            text = inputState.queryString,
                            isInputValid = isInputValid,
                            modifier = Modifier.focusRequester(focusRequester),
                            onDone = { focusManager.clearFocus() },
                            onValueChange = {
                                if (!isInputValid) viewModel.clearInvalidInputError()
                                viewModel.updateInputState(inputState.copy(queryString = it))
                            })
                        SearchButton(scrollOffset = pageScrollOffset) { viewModel.performSearch() }
                    }
                }

                SearchMode.Fine -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TextInput(
                            label = stringResource(R.string.input_track_title_label),
                            icon = Icons.Outlined.MusicNote,
                            text = inputState.queryTrack.trackName,
                            isInputValid = isInputValid,
                            modifier = Modifier.focusRequester(focusRequester),
                            onDone = { focusManager.clearFocus() },
                            onValueChange = {
                                if (!isInputValid) viewModel.clearInvalidInputError()
                                viewModel.updateInputState(
                                    inputState.copy(
                                        queryTrack = inputState.queryTrack.copy(trackName = it)
                                    )
                                )
                            })
                        TextInput(
                            label = stringResource(R.string.input_track_artists_label),
                            icon = Icons.Outlined.InterpreterMode,
                            text = inputState.queryTrack.artistName,
                            onDone = { focusManager.clearFocus() },
                            onValueChange = {
                                viewModel.updateInputState(
                                    inputState.copy(
                                        queryTrack = inputState.queryTrack.copy(artistName = it)
                                    )
                                )
                            })
                        TextInput(
                            label = stringResource(R.string.input_track_album_label),
                            icon = Icons.Outlined.Album,
                            text = inputState.queryTrack.albumName,
                            onDone = { focusManager.clearFocus() },
                            onValueChange = {
                                viewModel.updateInputState(
                                    inputState.copy(
                                        queryTrack = inputState.queryTrack.copy(albumName = it)
                                    )
                                )
                            })
                        SearchButton(scrollOffset = pageScrollOffset) { viewModel.performSearch() }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchButton(
    modifier: Modifier = Modifier,
    scrollOffset: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val sizeScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        label = "search_btn_click_animation"
    )
    Row(modifier = modifier) {
        Spacer(Modifier.weight((0.5f + scrollOffset).coerceIn(0.01f, 0.9f)))
        OutlinedButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier.scale(sizeScale)
        ) {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text = stringResource(id = R.string.search))
        }
        Spacer(Modifier.weight((0.5f - scrollOffset).coerceIn(0.01f, 0.9f)))
    }
}
