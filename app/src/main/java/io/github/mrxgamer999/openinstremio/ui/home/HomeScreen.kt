package io.github.mrxgamer999.openinstremio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import io.github.mrxgamer999.openinstremio.AboutKey
import io.github.mrxgamer999.openinstremio.BuildConfig
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.SetupGuideKey
import io.github.mrxgamer999.openinstremio.UpdatesKey
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.data.Packages
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import io.github.mrxgamer999.openinstremio.util.ExternalIntents

@Composable
fun HomeScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(AppGraph.extensionStatusRepository(appContext))
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onNavigate = onNavigate, modifier = modifier)
}

@Composable
internal fun HomeScreen(uiState: HomeUiState, onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier.size(80.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState is HomeUiState.Ready) {
            StatusCard(variant = uiState.statusVariant)
        }

        // SeriesGuide -> Stremio flow card
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowTile(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = stringResource(R.string.home_flow_seriesguide),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
                FlowTile(
                    icon = Icons.Filled.PlayArrow,
                    label = stringResource(R.string.home_flow_stremio),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Menu
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            MenuRow(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = stringResource(R.string.home_menu_setup),
                onClick = { onNavigate(SetupGuideKey) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.home_menu_about),
                onClick = { onNavigate(AboutKey) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(
                icon = Icons.Filled.Update,
                label = stringResource(R.string.home_menu_updates),
                onClick = { onNavigate(UpdatesKey) },
            )
        }

        Text(
            text = stringResource(R.string.home_footer, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
    }
}

@Composable
private fun StatusCard(variant: StatusVariant, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container: Color
    val content: Color
    val icon: ImageVector
    val title: String
    val body: String
    val onClick: (() -> Unit)?
    when (variant) {
        StatusVariant.ACTIVE -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
            icon = Icons.Filled.CheckCircle
            title = stringResource(R.string.home_status_active_title)
            body = stringResource(R.string.home_status_active_body)
            onClick = null
        }
        StatusVariant.NOT_ENABLED -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            content = MaterialTheme.colorScheme.onSecondaryContainer
            icon = Icons.Filled.Warning
            title = stringResource(R.string.home_status_not_enabled_title)
            body = stringResource(R.string.home_status_not_enabled_body)
            onClick = { ExternalIntents.openSeriesGuideExtensions(context) }
        }
        StatusVariant.SERIESGUIDE_MISSING -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
            icon = Icons.Filled.Warning
            title = stringResource(R.string.home_status_sg_missing_title)
            body = stringResource(R.string.home_status_sg_missing_body)
            onClick = { ExternalIntents.openPlayStore(context, Packages.SERIESGUIDE) }
        }
    }

    Card(
        modifier =
            modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FlowTile(icon: ImageVector, label: String, container: Color, content: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(52.dp).background(container, RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = content)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.titleMedium) },
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
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    OpenInStremioTheme {
        HomeScreen(uiState = HomeUiState.Ready(StatusVariant.ACTIVE), onNavigate = {})
    }
}
