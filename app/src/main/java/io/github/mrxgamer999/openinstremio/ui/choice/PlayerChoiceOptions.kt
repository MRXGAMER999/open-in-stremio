package io.github.mrxgamer999.openinstremio.ui.choice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.mrxgamer999.openinstremio.R
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.forwarder.Target
import io.github.mrxgamer999.openinstremio.forwarder.icon

/**
 * The Stremio / Fireguy / Both picker, shared by the setup guide and Settings. Each row says
 * whether its app is installed, because a choice of an app that isn't there leads to its
 * "isn't installed" dialog.
 *
 * With [requestInitialFocus], a D-pad (TV remote, keyboard) lands on the selected row. It is
 * skipped in touch mode, where a focused row would look like a second selection.
 */
@Composable
fun PlayerChoiceOptions(
    selected: PlayerChoice,
    stremioInstalled: Boolean,
    fireguyInstalled: Boolean,
    onSelect: (PlayerChoice) -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current

    Column(modifier = modifier.selectableGroup()) {
        PlayerChoice.entries.forEach { choice ->
            ListItem(
                headlineContent = {
                    Text(stringResource(choice.nameRes), style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        stringResource(choice.statusRes(stremioInstalled, fireguyInstalled)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Box(
                        modifier =
                            Modifier.size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(13.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = choice.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                // The row carries the selection; the radio only shows it.
                trailingContent = { RadioButton(selected = choice == selected, onClick = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                // The requester has to come before the focus target `selectable` adds.
                modifier =
                    (if (choice == selected) Modifier.focusRequester(focusRequester) else Modifier)
                        .selectable(
                            selected = choice == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(choice) },
                        ),
            )
        }
    }

    if (requestInitialFocus) {
        LaunchedEffect(Unit) {
            if (inputModeManager.inputMode == InputMode.Keyboard) focusRequester.requestFocus()
        }
    }
}

private val PlayerChoice.nameRes: Int
    get() =
        when (this) {
            PlayerChoice.STREMIO -> R.string.choice_stremio
            PlayerChoice.FIREGUY -> R.string.choice_fireguy
            PlayerChoice.BOTH -> R.string.choice_both
        }

private val PlayerChoice.icon: ImageVector
    get() =
        when (this) {
            PlayerChoice.STREMIO -> Target.STREMIO.icon
            PlayerChoice.FIREGUY -> Target.FIREGUY.icon
            PlayerChoice.BOTH -> Icons.Filled.Apps
        }

private fun PlayerChoice.statusRes(stremioInstalled: Boolean, fireguyInstalled: Boolean): Int =
    when (this) {
        PlayerChoice.STREMIO ->
            if (stremioInstalled) R.string.choice_installed else R.string.choice_not_installed
        PlayerChoice.FIREGUY ->
            if (fireguyInstalled) R.string.choice_installed else R.string.choice_not_installed
        PlayerChoice.BOTH ->
            if (stremioInstalled && fireguyInstalled) R.string.choice_both_installed
            else R.string.choice_both_partly
    }
