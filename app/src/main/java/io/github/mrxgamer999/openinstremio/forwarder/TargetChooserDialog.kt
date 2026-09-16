package io.github.mrxgamer999.openinstremio.forwarder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mrxgamer999.openinstremio.R

/**
 * Asked when more than one player can open the title. SeriesGuide gives an extension a single
 * button per title, so this dialog is where the per-app choice lives - each row says in full
 * where it goes, the way the button itself does when there is only one place to go.
 *
 * A title with no IMDb id reaches a search rather than a detail page, and the rows say so: the
 * button that led here said "Search in…" too, and a row promising to open a title nobody can
 * address would be a lie the very next screen exposes.
 *
 * The first row requests initial focus so a D-pad remote lands on it immediately.
 */
@Composable
fun TargetChooserDialog(
    targets: List<Target>,
    isSearch: Boolean,
    onPick: (Target) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isSearch) R.string.chooser_title_search else R.string.chooser_title))
        },
        text = {
            Column {
                targets.forEachIndexed { index, target ->
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(
                                    if (isSearch) target.searchLabelRes else target.openLabelRes
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        // The requester has to come before the focus target `clickable` adds,
                        // or it never attaches and requestFocus throws.
                        modifier =
                            (if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                                .clickable { onPick(target) },
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
                                    imageVector = target.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        // The rows are the actions; the only thing left for a button is backing out.
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.missing_not_now)) }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
