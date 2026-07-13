package io.github.mrxgamer999.openinstremio.ui.updates

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mrxgamer999.openinstremio.BuildConfig
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.util.ExternalIntents

@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = viewModel {
        UpdateViewModel(AppGraph.updateRepository(), BuildConfig.VERSION_NAME)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UpdateScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::check, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateScreen(
    uiState: UpdateUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.updates_title)) },
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
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (uiState) {
                UpdateUiState.Checking -> {
                    CenteredState {
                        CircularProgressIndicator()
                        StateText(stringResource(R.string.updates_checking))
                    }
                }
                is UpdateUiState.UpToDate -> {
                    CenteredState {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(56.dp),
                        )
                        StateText(stringResource(R.string.updates_up_to_date))
                        SuggestionChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("v${uiState.currentVersion.removePrefix("v")}") },
                        )
                    }
                }
                is UpdateUiState.UpdateAvailable -> {
                    UpdateAvailableContent(
                        uiState = uiState,
                        onDownload = { ExternalIntents.openUrl(context, uiState.url) },
                    )
                }
                UpdateUiState.NoReleases -> {
                    CenteredState {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp),
                        )
                        StateText(stringResource(R.string.updates_no_releases))
                    }
                }
                UpdateUiState.Error -> {
                    CenteredState {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp),
                        )
                        StateText(stringResource(R.string.updates_error))
                        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.updates_retry)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredState(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        content()
    }
}

@Composable
private fun StateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun UpdateAvailableContent(uiState: UpdateUiState.UpdateAvailable, onDownload: () -> Unit) {
    // Hero card
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier.size(60.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.updates_available),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = "v${uiState.currentVersion.removePrefix("v")}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "v${uiState.latestVersion.removePrefix("v")}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    // What's new
    Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = stringResource(R.string.updates_whats_new),
                style = MaterialTheme.typography.titleMedium,
            )
            val notes =
                uiState.notes.ifEmpty { listOf(stringResource(R.string.updates_notes_fallback)) }
            notes.forEach { note ->
                Row(modifier = Modifier.padding(top = 9.dp)) {
                    Box(
                        modifier =
                            Modifier.padding(top = 7.dp)
                                .size(5.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }

    Button(
        onClick = onDownload,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(54.dp),
    ) {
        Text(stringResource(R.string.updates_download))
    }

    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 24.dp)) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.updates_manual_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
