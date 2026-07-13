package io.github.mrxgamer999.openinstremio.forwarder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mrxgamer999.openinstremio.R

/**
 * The "Stremio isn't installed" dialog from the design. Shown by the forwarder when the
 * user taps an action but Stremio is missing. The confirm button requests initial focus
 * so a D-pad (NVIDIA Shield remote) lands on the primary action immediately.
 */
@Composable
fun StremioMissingDialog(onGetStremio: () -> Unit, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier =
                    Modifier.size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = { Text(stringResource(R.string.stremio_missing_title)) },
        text = { Text(stringResource(R.string.stremio_missing_body)) },
        confirmButton = {
            Button(onClick = onGetStremio, modifier = Modifier.focusRequester(focusRequester)) {
                Text(stringResource(R.string.stremio_missing_get))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.stremio_missing_not_now)) }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
