package io.github.abhishekabhi789.lyricsforpoweramp.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.AboutActivity.Companion.TAG

@Preview(showSystemUi = true)
@Composable
fun PreviewAboutScreen() {
    AboutScreen { }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier, onFinish: () -> Unit) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val playStoreUrl =
        BuildConfig.PLAY_STORE_URL + "&referrer=utm_source=app&utm_medium=l4pa&utm_campaign=about"
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back_action)
                        )
                    }
                })
        },
        modifier = modifier
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = StaggeredGridCells.Adaptive(300.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 8.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        if (isPreview) Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            colorFilter = ColorFilter.tint(Color.White), contentDescription = null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .scale(1.5f)
                                .background(colorResource(id = R.color.ic_launcher_background))

                        )
                        else {
                            ElevatedCard {
                                Icon(
                                    painter = painterResource(R.drawable.app_icon),
                                    tint = MaterialTheme.colorScheme.background,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(256.dp)
                                        .background(colorResource(R.color.ic_launcher_background))
                                )
                            }
                        }
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                shadow = Shadow(
                                    color = MaterialTheme.colorScheme.secondary,
                                    offset = Offset(1f, 1f),
                                    blurRadius = 1f
                                )
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Cursive,
                        )
                        Text(
                            text = "${stringResource(R.string.version)}:  ${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        ),
                    ) {
                        AssistChip(
                            onClick = { browseUrl(context, BuildConfig.GITHUB_REPO_URL) },
                            label = { Text("Visit Github Repo") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_github), null) }
                        )
                        AssistChip(
                            onClick = { browseUrl(context, playStoreUrl) },
                            label = { Text("Get from Play Store") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_playstore), null) }
                        )
                        AssistChip(
                            onClick = { browseUrl(context, BuildConfig.KO_FI_URL) },
                            label = { Text("Donate via Ko-Fi") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_kofi), null) }
                        )
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                    }) {
                        Text(text = stringResource(R.string.third_party_licenses))
                    }
                }
            }
        }
    }
}

fun browseUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Log.e(TAG, "browseUrl: unable to open link $url")
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}
