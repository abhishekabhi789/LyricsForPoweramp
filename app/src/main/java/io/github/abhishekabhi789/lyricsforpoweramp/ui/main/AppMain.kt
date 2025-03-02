package io.github.abhishekabhi789.lyricsforpoweramp.ui.main

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SearchResultActivity
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.MainActivityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AppMain(modifier: Modifier = Modifier, viewModel: MainActivityViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRemoverModifier = Modifier.clickable(
        interactionSource = interactionSource,
        indication = null
    ) {
        focusManager.clearFocus()
    }
    LaunchedEffect(snackbarHostState) {
        keyboardController.let { keyboard ->
            if (snackbarHostState.currentSnackbarData == null) keyboard?.show() else keyboard?.hide()
        }
    }
    LaunchedEffect(viewModel.searchErrorFlow) {
        viewModel.searchErrorFlow.collectLatest { errMsg ->
            keyboardController?.hide()
            scope.launch {
                when (snackbarHostState.showSnackbar(
                    message = listOfNotNull(
                        context.getString(errMsg.errMsg),
                        errMsg.moreInfo
                    ).joinToString(" "),
                    withDismissAction = true
                )) {
                    SnackbarResult.Dismissed -> keyboardController?.show()
                    else -> {}
                }
            }
        }
    }
    LaunchedEffect(viewModel.searchResultFlow) {
        viewModel.searchResultFlow.collectLatest { result ->
            val intent = Intent(context, SearchResultActivity::class.java).apply {
                putParcelableArrayListExtra(SearchResultActivity.KEY_RESULT, ArrayList(result))
                putExtra(
                    SearchResultActivity.KEY_POWERAMP_REAL_ID,
                    viewModel.inputState.value.queryTrack.realId
                )
            }
            context.startActivity(intent)
        }
    }
    Scaffold(
        topBar = { TopBar() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
            .then(focusRemoverModifier)
    ) { paddingValues ->
        SearchUi(
            viewModel = viewModel,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        )
    }
}
