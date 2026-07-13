package io.github.mrxgamer999.openinstremio.forwarder

import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.mrxgamer999.openinstremio.data.AndroidPackageChecker
import io.github.mrxgamer999.openinstremio.data.Packages
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import io.github.mrxgamer999.openinstremio.util.ExternalIntents

/**
 * Invisible trampoline that every published SeriesGuide action points at. On the fast path
 * (Stremio installed) it fires the deep link and finishes without drawing a single frame;
 * otherwise it shows the "Stremio isn't installed" dialog over the caller.
 *
 * Exported because SeriesGuide launches it from its own process, but it has no intent
 * filter, so it can only be addressed explicitly. Extras are parsed defensively.
 */
class StremioLaunchActivity : ComponentActivity() {

    private val viewModel: StremioLaunchViewModel by viewModels {
        viewModelFactory {
            initializer {
                StremioLaunchViewModel(
                    packageChecker = AndroidPackageChecker(applicationContext),
                    isTv = isTelevision(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (val decision = viewModel.decide(intent.toLaunchRequest())) {
            is LaunchDecision.LaunchStremio -> {
                if (launchStremio(decision.uri)) finish() else showMissingDialog()
            }
            is LaunchDecision.ShowMissingDialog -> showMissingDialog()
            LaunchDecision.Finish -> finish()
        }
    }

    private fun launchStremio(uri: String): Boolean =
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri.toUri())
                    .setPackage(Packages.STREMIO)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: ActivityNotFoundException) {
            // Installed-check passed but the launch still failed (e.g. package disabled).
            false
        }

    private fun showMissingDialog() {
        setContent {
            OpenInStremioTheme {
                StremioMissingDialog(
                    onGetStremio = {
                        ExternalIntents.openPlayStore(this, Packages.STREMIO)
                        finish()
                    },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun isTelevision(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_IMDB_ID = "imdbId"
        const val EXTRA_SEASON = "season"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_TITLE = "title"

        private fun Intent.toLaunchRequest() =
            LaunchRequest(
                type = getStringExtra(EXTRA_TYPE),
                imdbId = getStringExtra(EXTRA_IMDB_ID),
                season = getIntExtra(EXTRA_SEASON, -1),
                episode = getIntExtra(EXTRA_EPISODE, -1),
                title = getStringExtra(EXTRA_TITLE),
            )
    }
}
