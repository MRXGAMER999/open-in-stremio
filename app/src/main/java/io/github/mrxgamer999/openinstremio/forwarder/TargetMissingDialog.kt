package io.github.mrxgamer999.openinstremio.forwarder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * The "app isn't installed" dialog from the design, shown by the forwarder when the user picks a
 * target that is missing. Whichever button is the primary one requests initial focus so a D-pad
 * (NVIDIA Shield remote) lands on it immediately.
 *
 * Only Stremio has a store page to send anyone to; Fireguy is sideloaded, so its dialog says what
 * is wrong and stops there rather than offering a route that would dead-end.
 */
@Composable
fun TargetMissingDialog(target: Target, onGetTarget: () -> Unit, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val storeAction = target.storeActionRes

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
                    imageVector = target.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = { Text(stringResource(target.missingTitleRes)) },
        text = { Text(stringResource(target.missingBodyRes)) },
        confirmButton = {
            // With no store to open, dismissing is the only thing left to do, so it becomes the
            // primary button rather than a lone quiet option beside nothing.
            Button(
                onClick = if (storeAction != null) onGetTarget else onDismiss,
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(stringResource(storeAction ?: R.string.missing_not_now))
            }
        },
        dismissButton = {
            if (storeAction != null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.missing_not_now)) }
            }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
