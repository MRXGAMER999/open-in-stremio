package io.github.mrxgamer999.openinstremio.forwarder

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.mrxgamer999.openinstremio.R

/**
 * How each [Target] presents itself. Kept beside the composables rather than on the enum so the
 * enum stays free of resource ids and usable from [LaunchViewModel] and the extension receiver.
 */

/** The action button's label when this target is the only place a tap can go. */
@get:StringRes
val Target.openLabelRes: Int
    get() =
        when (this) {
            Target.STREMIO -> R.string.action_open_in_stremio
            Target.FIREGUY -> R.string.action_open_in_fireguy
        }

/** The label for a title with no IMDb id, which reaches the target's search instead. */
@get:StringRes
val Target.searchLabelRes: Int
    get() =
        when (this) {
            Target.STREMIO -> R.string.action_search_in_stremio
            Target.FIREGUY -> R.string.action_search_in_fireguy
        }

@get:StringRes
val Target.missingTitleRes: Int
    get() =
        when (this) {
            Target.STREMIO -> R.string.stremio_missing_title
            Target.FIREGUY -> R.string.fireguy_missing_title
        }

@get:StringRes
val Target.missingBodyRes: Int
    get() =
        when (this) {
            Target.STREMIO -> R.string.stremio_missing_body
            Target.FIREGUY -> R.string.fireguy_missing_body
        }

/** Null where there is no store page to send anyone to - Fireguy is sideloaded. */
val Target.storeActionRes: Int?
    get() =
        when (this) {
            Target.STREMIO -> R.string.stremio_missing_get
            Target.FIREGUY -> null
        }

val Target.icon: ImageVector
    get() =
        when (this) {
            Target.STREMIO -> Icons.Filled.PlayArrow
            Target.FIREGUY -> Icons.Filled.Tv
        }

/**
 * The label for the one action SeriesGuide shows per title.
 *
 * With both players installed the tap has to ask, so the button cannot name either of them.
 * With one, it names that one. With none the label stays Stremio's: the tap then leads to the
 * install nudge, which is where it led before there was a second player to name.
 */
@StringRes
internal fun actionLabelRes(installed: List<Target>, open: Boolean): Int =
    if (installed.size > 1) {
        if (open) R.string.action_open_in else R.string.action_search_in
    } else {
        val target = installed.singleOrNull() ?: Target.STREMIO
        if (open) target.openLabelRes else target.searchLabelRes
    }
