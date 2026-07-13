package io.github.mrxgamer999.openinstremio.ui.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.data.Packages
import io.github.mrxgamer999.openinstremio.util.ExternalIntents
import kotlinx.coroutines.launch

@Composable
fun SetupGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupGuideViewModel = viewModel { SetupGuideViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SetupGuideScreen(uiState = uiState, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetupGuideScreen(uiState: SetupGuideUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sgMissingMessage = stringResource(R.string.setup_sg_missing_snackbar)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                val page = uiState.pages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PageIllustration(pageIndex)
                    Text(
                        text = stringResource(page.title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    Text(
                        text = stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    when (page.action) {
                        SetupPageAction.OPEN_SG_EXTENSIONS ->
                            FilledTonalButton(
                                onClick = {
                                    if (!ExternalIntents.openSeriesGuideExtensions(context)) {
                                        scope.launch { snackbarHostState.showSnackbar(sgMissingMessage) }
                                    }
                                },
                                modifier = Modifier.padding(top = 24.dp),
                            ) {
                                Text(stringResource(R.string.setup_open_sg_extensions))
                            }
                        SetupPageAction.GET_STREMIO ->
                            FilledTonalButton(
                                onClick = { ExternalIntents.openPlayStore(context, Packages.STREMIO) },
                                modifier = Modifier.padding(top = 24.dp),
                            ) {
                                Text(stringResource(R.string.setup_get_stremio))
                            }
                        null -> Unit
                    }
                }
            }

            // Animated page dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(uiState.pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dotWidth")
                    val color by
                        animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            label = "dotColor",
                        )
                    Box(
                        modifier =
                            Modifier.padding(horizontal = 4.dp)
                                .width(width)
                                .height(8.dp)
                                .background(color, CircleShape)
                    )
                }
            }

            val isLastPage = pagerState.currentPage == uiState.pages.lastIndex
            Button(
                onClick = {
                    if (isLastPage) {
                        onBack()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp).height(54.dp),
            ) {
                Text(
                    stringResource(if (isLastPage) R.string.setup_finish else R.string.setup_next)
                )
            }
        }
    }
}

@Composable
private fun PageIllustration(pageIndex: Int) {
    val (icon, container, content) =
        when (pageIndex) {
            0 ->
                Triple(
                    Icons.Filled.PlayArrow,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            1 ->
                Triple(
                    Icons.AutoMirrored.Filled.List,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
            2 ->
                Triple(
                    Icons.Filled.Download,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            else ->
                Triple(
                    Icons.Filled.CheckCircle,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
        }
    Illustration(icon = icon, container = container, content = content)
}

@Composable
private fun Illustration(
    icon: ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier.size(112.dp).background(container, RoundedCornerShape(34.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(50.dp))
    }
}
