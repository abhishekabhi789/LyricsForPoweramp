package io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SearchResultViewmodel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    modifier: Modifier = Modifier,
    viewmodel: SearchResultViewmodel,
    onNavigateUp: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewmodel.searchResults.collectAsState()
    val isLaunchedFromPoweramp by remember { derivedStateOf { viewmodel.powerampId != null } }
    val sendToPowerampState by viewmodel.sendToPowerampState.collectAsState()
    val saveToStorageState by viewmodel.saveToStorageState.collectAsState()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.result_topbar_title),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_action),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(400.dp),
            verticalItemSpacing = 8.dp,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            items(items = result) { lyrics ->
                LyricItem(
                    lyrics = lyrics,
                    isLaunchedFromPowerAmp = isLaunchedFromPoweramp,
                    onLyricChosen = { lyrics, lyricsType ->
                        showBottomSheet = true
                        scope.launch {
                            viewmodel.sendLyricsToPoweramp(
                                context = context,
                                lyrics = lyrics,
                                lyricsType = lyricsType,
                                onComplete = { if (showBottomSheet) onFinish() }
                            )
                        }
                    }
                )
            }
        }
        if (showBottomSheet) {
            ResultBottomSheet(
                sendToPowerampState,
                saveToStorageState,
                onDismiss = {
                    showBottomSheet = false
                    viewmodel.clearResultState()
                },
                grantAccess = {
                    showBottomSheet = false
                    viewmodel.clearResultState()
                    val path = viewmodel.filePath.substringBeforeLast(File.separatorChar)
                    Intent(context, SettingsActivity::class.java).apply {
                        setAction(SettingsActivity.Companion.OPEN_SETTINGS_ACTION)
                        putExtra(SettingsActivity.EXTRA_REQUIRED_PATH, path)
                    }.let { context.startActivity(it) }
                }
            )
        }
    }
}
