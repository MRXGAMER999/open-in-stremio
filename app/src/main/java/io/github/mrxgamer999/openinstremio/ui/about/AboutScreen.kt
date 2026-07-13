package io.github.mrxgamer999.openinstremio.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mrxgamer999.openinstremio.BuildConfig
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.util.ExternalIntents

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = viewModel { AboutViewModel(BuildConfig.VERSION_NAME) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AboutScreen(uiState = uiState, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(uiState: AboutUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
            // App identity block
            Box(
                modifier =
                    Modifier.padding(top = 14.dp)
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(23.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 13.dp),
            )
            Text(
                text = stringResource(R.string.about_version, uiState.version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Info rows
            Card(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
                AboutRow(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.about_license),
                    subtitle = stringResource(R.string.about_license_value),
                    onClick = { ExternalIntents.openUrl(context, uiState.licenseUrl) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AboutRow(
                    icon = Icons.Filled.Code,
                    title = stringResource(R.string.about_repo),
                    subtitle = stringResource(R.string.about_repo_value),
                    onClick = { ExternalIntents.openUrl(context, uiState.repoUrl) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AboutRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_developer),
                    subtitle = stringResource(R.string.about_developer_value),
                    onClick = { ExternalIntents.openUrl(context, uiState.developerUrl) },
                )
            }

            // TMDb attribution (required wording)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 30.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier.background(Color(0xFF0D253F), RoundedCornerShape(7.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.about_tmdb_chip),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF01D0AC),
                            )
                        }
                        Text(
                            text = stringResource(R.string.about_tmdb_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 9.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.about_tmdb_attribution),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
