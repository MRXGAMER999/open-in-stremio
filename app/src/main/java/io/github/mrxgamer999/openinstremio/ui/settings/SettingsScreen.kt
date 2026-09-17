package io.github.mrxgamer999.openinstremio.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.data.AndroidPackageChecker
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.ui.choice.PlayerChoiceOptions

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(AppGraph.playerChoiceStore(appContext), AndroidPackageChecker(appContext))
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(uiState = uiState, onBack = onBack, onChoose = viewModel::choose, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onChoose: (PlayerChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        ) {
            Text(
                text = stringResource(R.string.settings_choice_header),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
            )
            if (uiState is SettingsUiState.Ready) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    PlayerChoiceOptions(
                        selected = uiState.choice,
                        stremioInstalled = uiState.stremioInstalled,
                        fireguyInstalled = uiState.fireguyInstalled,
                        onSelect = onChoose,
                        requestInitialFocus = true,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_choice_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
