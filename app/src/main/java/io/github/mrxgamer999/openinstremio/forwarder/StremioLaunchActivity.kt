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
import io.github.mrxgamer999.openinstremio.theme.OpenInStremioTheme
import io.github.mrxgamer999.openinstremio.util.ExternalIntents

/**
 * Invisible trampoline that every published SeriesGuide action points at. On the fast path
 * (one player installed) it fires the deep link and finishes without drawing a single frame;
 * otherwise it draws over the caller - the chooser when both players can open the title, or the
 * "isn't installed" dialog when the chosen one is missing.
 *
 * Exported because SeriesGuide launches it from its own process, but it has no intent
 * filter, so it can only be addressed explicitly. Extras are parsed defensively.
 *
 * The Stremio in the name is now too narrow, and it stays anyway: this class is named inside
 * every `Action.viewIntent` SeriesGuide is holding, and those Intents outlive an app update in
 * a running SeriesGuide process. Renaming it would break every button currently on screen until
 * SeriesGuide next asked for the title again.
 */
class StremioLaunchActivity : ComponentActivity() {

    private val viewModel: LaunchViewModel by viewModels {
        viewModelFactory {
            initializer {
                LaunchViewModel(
                    packageChecker = AndroidPackageChecker(applicationContext),
                    isTv = isTelevision(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = intent.toLaunchRequest()
        act(viewModel.decide(request), request)
    }

    private fun act(decision: LaunchDecision, request: LaunchRequest) {
        when (decision) {
            is LaunchDecision.Launch -> {
                if (launch(decision.target, decision.uri)) finish()
                else showMissingDialog(decision.target)
            }
            is LaunchDecision.ShowChooser ->
                showChooser(decision.targets, decision.isSearch, request)
            is LaunchDecision.ShowMissing -> showMissingDialog(decision.target)
            LaunchDecision.Finish -> finish()
        }
    }

    private fun launch(target: Target, uri: String): Boolean =
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri.toUri())
                    .setPackage(target.packageId)
                    .addFlags(launchFlags(target))
            )
            true
        } catch (e: ActivityNotFoundException) {
            // Installed-check passed but the launch still failed (e.g. package disabled).
            false
        }

    /**
     * Fireguy's entry activity is `standard`, and its own measurement is that NEW_TASK together
     * with SINGLE_TOP is what reaches a running instance's onNewIntent instead of stacking a
     * second copy on its task. Stremio keeps the bare NEW_TASK it has always been sent.
     */
    private fun launchFlags(target: Target): Int =
        when (target) {
            Target.STREMIO -> Intent.FLAG_ACTIVITY_NEW_TASK
            Target.FIREGUY -> Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    private fun showChooser(targets: List<Target>, isSearch: Boolean, request: LaunchRequest) {
        setContent {
            OpenInStremioTheme {
                TargetChooserDialog(
                    targets = targets,
                    isSearch = isSearch,
                    onPick = { target -> act(viewModel.choose(request, target), request) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun showMissingDialog(target: Target) {
        setContent {
            OpenInStremioTheme {
                TargetMissingDialog(
                    target = target,
                    onGetTarget = {
                        ExternalIntents.openPlayStore(this, target.packageId)
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
